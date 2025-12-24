import { Component, inject, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ArticleService } from '../../../../services/article.service';
import { ArticleImageService } from '../../../../services/article-image.service';
import { TagService } from '../../../../services/tag.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ToastrService } from 'ngx-toastr';
import {
  ArticleResponse,
  CreateArticleRequest,
  UpdateArticleRequest,
} from '../../../../models/article.model';
import { TagResponse } from '../../../../models/tag.model';
import EasyMDE from 'easymde';

@Component({
  selector: 'app-article-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslateModule],
  templateUrl: './article-form.component.html',
  styleUrls: ['./article-form.component.scss'],
})
export class ArticleFormComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly articleService = inject(ArticleService);
  private readonly articleImageService = inject(ArticleImageService);
  private readonly tagService = inject(TagService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);

  articleForm!: FormGroup;
  isEditMode = false;
  articleId: number | undefined;
  loading = false;
  submitting = false;
  uploadingImage = false;
  availableTags: TagResponse[] = [];
  selectedTags: number[] = [];

  private mde: EasyMDE | undefined;
  private readonly destroy$ = new Subject<void>();
  private initTimeout?: ReturnType<typeof setTimeout>;

  private readonly AUTOSAVE_DELAY_MS = 1000;

  ngOnInit(): void {
    this.initForm();
    this.loadTags();

    // Subscribe to route params to detect changes even when component is reused
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const id = params.get('id');

      // Reset editor and form when switching modes
      this.resetEditor();

      if (id) {
        this.isEditMode = true;
        this.articleId = +id;
        this.loadArticle(this.articleId);
      } else {
        this.isEditMode = false;
        this.articleId = undefined;
        // Form is already reset by resetEditor()
      }
    });
  }

  ngAfterViewInit(): void {
    // Only initialize EasyMDE in create mode
    // In edit mode, it will be initialized after article data is loaded
    if (!this.isEditMode) {
      this.initializeEasyMDE();
    }
  }

  ngOnDestroy(): void {
    // Complete all subscriptions
    this.destroy$.next();
    this.destroy$.complete();

    // Clear pending timeout
    if (this.initTimeout) {
      clearTimeout(this.initTimeout);
    }

    // Destroy EasyMDE instance
    if (this.mde) {
      this.mde.toTextArea();
      this.mde = undefined;
    }
  }

  initForm(): void {
    this.articleForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(200)]],
      content: ['', Validators.required],
      excerpt: ['', Validators.maxLength(500)],
      draft: [true],
    });
  }

  initializeEasyMDE(): void {
    const textarea = document.getElementById('content-editor') as HTMLTextAreaElement;
    if (!textarea) {
      this.logger.error('[EASYMDE] Textarea not found');
      return;
    }

    this.mde = new EasyMDE({
      element: textarea,
      placeholder: 'Écrivez votre article en Markdown...',
      spellChecker: false,
      autosave: {
        enabled: true,
        uniqueId: this.isEditMode ? `article-${this.articleId}` : 'new-article',
        delay: this.AUTOSAVE_DELAY_MS,
      },
      toolbar: [
        'bold',
        'italic',
        'heading',
        '|',
        'code',
        'quote',
        'unordered-list',
        'ordered-list',
        '|',
        'link',
        {
          name: 'image',
          action: () => this.insertImage(),
          className: 'fa fa-picture-o',
          title: 'Insérer une image',
        },
        '|',
        'preview',
        'side-by-side',
        'fullscreen',
        '|',
        'guide',
      ],
      status: ['lines', 'words', 'cursor'],
      renderingConfig: {
        codeSyntaxHighlighting: true,
      },
    });

    // Sync EasyMDE content with form control
    this.mde.codemirror.on('change', () => {
      if (this.mde) {
        this.articleForm.patchValue({ content: this.mde.value() });
      }
    });
  }

  insertImage(): void {
    if (this.demoModeService.isDemo()) {
      this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
      return;
    }

    if (!this.isEditMode || !this.articleId) {
      this.toastr.warning(
        this.translate.instant('admin.articles.saveBeforeImages'),
        this.translate.instant('admin.common.warning')
      );
      return;
    }

    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';

    input.onchange = (event: Event) => {
      const target = event.target as HTMLInputElement;
      const file = target.files?.[0];
      if (file) {
        this.uploadImageFile(file);
      }
    };

    input.click();
  }

  uploadImageFile(file: File): void {
    if (this.demoModeService.isDemo()) {
      return;
    }

    if (!this.articleId) return;

    this.uploadingImage = true;
    this.logger.info('[HTTP_REQUEST] Uploading article image', { filename: file.name });

    this.articleImageService
      .uploadImage(this.articleId, file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.logger.info('[HTTP_SUCCESS] Image uploaded', { url: response.imageUrl });

          // Insert image markdown syntax at cursor position
          if (this.mde) {
            const cm = this.mde.codemirror;
            const imageMarkdown = `\n![${file.name}](${response.imageUrl})\n`;
            cm.replaceSelection(imageMarkdown);
          }

          this.uploadingImage = false;
          this.toastr.success(this.translate.instant('admin.articles.imageUploadSuccess'));
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to upload image', { error });
          this.uploadingImage = false;
          this.toastr.error(this.translate.instant('admin.articles.imageUploadError'));
        },
      });
  }

  loadTags(): void {
    this.tagService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tags) => {
          this.availableTags = tags;
          this.logger.info('[HTTP_SUCCESS] Tags loaded', { count: tags.length });
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to load tags', { error });
          this.toastr.error(this.translate.instant('admin.articles.loadTagsError'));
        },
      });
  }

  clearAutosave(): void {
    // Clear EasyMDE autosave from localStorage
    const storageKey = this.isEditMode ? `smde_article-${this.articleId}` : 'smde_new-article';
    try {
      localStorage.removeItem(storageKey);
      this.logger.info('[EASYMDE] Autosave cleared from localStorage', { key: storageKey });
    } catch (error) {
      this.logger.error('[EASYMDE] Failed to clear autosave', { error });
    }
  }

  resetEditor(): void {
    // Destroy existing EasyMDE instance if present
    if (this.mde) {
      this.mde.toTextArea();
      this.mde = undefined;
    }

    // Clear autosave data from localStorage
    if (this.isEditMode && this.articleId) {
      const editStorageKey = `smde_article-${this.articleId}`;
      localStorage.removeItem(editStorageKey);
    }
    const newStorageKey = 'smde_new-article';
    localStorage.removeItem(newStorageKey);

    // Reset form to initial state
    this.articleForm.reset({
      title: '',
      content: '',
      excerpt: '',
      draft: true,
    });

    // Reset selected tags
    this.selectedTags = [];
  }

  loadArticle(id: number): void {
    this.loading = true;
    const isDemo = this.demoModeService.isDemo();
    this.logger.info('[HTTP_REQUEST] Loading article', { id, isDemo });

    // In demo mode, use public endpoint and filter by ID
    if (isDemo) {
      this.articleService
        .getAll()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (articles) => {
            const article = articles.find((a) => a.id === id);
            if (article) {
              this.populateForm(article);
            } else {
              this.logger.warn('[HTTP_WARNING] Article not found in public list (may be draft)', {
                id,
              });
              this.toastr.warning(
                this.translate.instant('admin.articles.draftNotAvailableDemo'),
                this.translate.instant('admin.articles.notAvailable')
              );
              this.loading = false;
              this.router.navigate(['/admindemo/articles']);
            }
          },
          error: (error) => {
            this.logger.error('[HTTP_ERROR] Failed to load articles', { error });
            this.toastr.error(this.translate.instant('admin.articles.loadError'));
            this.loading = false;
            this.router.navigate(['/admindemo/articles']);
          },
        });
    } else {
      // In admin mode, use admin endpoint
      this.articleService
        .getById(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (article) => {
            this.populateForm(article);
          },
          error: (error) => {
            this.logger.error('[HTTP_ERROR] Failed to load article', { id, error });
            this.toastr.error(this.translate.instant('admin.articles.loadError'));
            this.loading = false;
            this.router.navigate(['/admin/articles']);
          },
        });
    }
  }

  private populateForm(article: ArticleResponse): void {
    this.logger.info('[HTTP_SUCCESS] Article loaded', { id: article.id, title: article.title });

    this.articleForm.patchValue({
      title: article.title,
      content: article.content,
      excerpt: article.excerpt || '',
      draft: article.draft,
    });

    // Set selected tags from TagResponse array
    this.selectedTags = article.tags ? article.tags.map((tag) => tag.id) : [];

    this.loading = false;

    // Initialize EasyMDE in edit mode after data is loaded and loading is false
    // This ensures the textarea is visible in the DOM
    this.initTimeout = setTimeout(() => {
      if (!this.mde) {
        this.initializeEasyMDE();
      }
      if (this.mde) {
        this.mde.value(article.content);
      }
    }, 0);
  }

  toggleTag(tagId: number): void {
    const index = this.selectedTags.indexOf(tagId);
    if (index > -1) {
      this.selectedTags.splice(index, 1);
    } else {
      this.selectedTags.push(tagId);
    }
  }

  isTagSelected(tagId: number): boolean {
    return this.selectedTags.includes(tagId);
  }

  onSubmit(publishNow: boolean = false): void {
    if (this.demoModeService.isDemo()) {
      return;
    }

    if (this.articleForm.invalid) {
      this.toastr.warning(
        this.translate.instant('admin.common.fillRequiredFields'),
        this.translate.instant('admin.common.invalidForm')
      );
      Object.keys(this.articleForm.controls).forEach((key) => {
        const control = this.articleForm.get(key);
        if (control?.invalid) {
          control.markAsTouched();
        }
      });
      return;
    }

    this.submitting = true;

    if (this.isEditMode && this.articleId) {
      this.updateArticle(publishNow);
    } else {
      this.createArticle(publishNow);
    }
  }

  createArticle(publishNow: boolean): void {
    const formValue = this.articleForm.value;
    const request: CreateArticleRequest = {
      title: formValue.title,
      content: formValue.content,
      excerpt: formValue.excerpt || undefined,
      tagIds: this.selectedTags.length > 0 ? this.selectedTags : undefined,
      draft: !publishNow,
    };

    this.logger.info('[HTTP_REQUEST] Creating article', { title: request.title });

    this.articleService
      .create(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (article) => {
          this.logger.info('[HTTP_SUCCESS] Article created', { id: article.id });
          this.clearAutosave(); // Clear autosave after successful creation
          this.submitting = false;
          const message = publishNow
            ? this.translate.instant('admin.articles.createdAndPublished', { title: article.title })
            : this.translate.instant('admin.articles.createdAsDraft', { title: article.title });
          this.toastr.success(message);
          this.router.navigate(['/admin/articles']);
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to create article', { error });
          this.submitting = false;
          this.toastr.error(this.translate.instant('admin.articles.createError'));
        },
      });
  }

  updateArticle(publishNow: boolean): void {
    if (!this.articleId) return;

    const formValue = this.articleForm.value;
    const request: UpdateArticleRequest = {
      title: formValue.title,
      content: formValue.content,
      excerpt: formValue.excerpt || undefined,
      tagIds: this.selectedTags.length > 0 ? this.selectedTags : undefined,
      draft: !publishNow,
    };

    this.logger.info('[HTTP_REQUEST] Updating article', { id: this.articleId });

    this.articleService
      .update(this.articleId, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (article) => {
          this.logger.info('[HTTP_SUCCESS] Article updated', { id: article.id });
          this.clearAutosave(); // Clear autosave after successful update
          this.submitting = false;
          const message = publishNow
            ? this.translate.instant('admin.articles.updatedAndPublished', { title: article.title })
            : this.translate.instant('admin.articles.updated', { title: article.title });
          this.toastr.success(message);
          this.router.navigate(['/admin/articles']);
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to update article', { id: this.articleId, error });
          this.submitting = false;
          this.toastr.error(this.translate.instant('admin.articles.updateError'));
        },
      });
  }

  saveDraft(): void {
    this.onSubmit(false);
  }

  saveAndPublish(): void {
    this.onSubmit(true);
  }

  goBack(): void {
    this.location.back();
  }
}
