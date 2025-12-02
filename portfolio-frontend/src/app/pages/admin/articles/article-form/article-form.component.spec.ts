import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Location } from '@angular/common';
import { ArticleFormComponent } from './article-form.component';
import { ArticleService } from '../../../../services/article.service';
import { TagService } from '../../../../services/tag.service';
import { ToastrService } from 'ngx-toastr';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { ArticleResponse } from '../../../../models/article.model';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';

describe('ArticleFormComponent', () => {
  let component: ArticleFormComponent;
  let fixture: ComponentFixture<ArticleFormComponent>;
  let articleService: jasmine.SpyObj<ArticleService>;
  let tagService: jasmine.SpyObj<TagService>;
  let toastrService: jasmine.SpyObj<ToastrService>;
  let router: jasmine.SpyObj<Router>;
  let location: jasmine.SpyObj<Location>;
  let paramMapSubject: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  const mockArticle: ArticleResponse = {
    id: 1,
    title: 'Test Article',
    slug: 'test-article',
    content: 'Test content',
    contentHtml: '<p>Test content</p>',
    excerpt: 'Test excerpt',
    draft: true,
    publishedAt: '2025-01-01T00:00:00',
    createdAt: '2025-01-01T00:00:00',
    updatedAt: '2025-01-01T00:00:00',
    readingTimeMinutes: 5,
    authorName: 'Admin',
    tags: [],
    images: [],
  };

  beforeEach(async () => {
    const articleServiceSpy = jasmine.createSpyObj('ArticleService', [
      'create',
      'update',
      'getById',
    ]);
    const tagServiceSpy = jasmine.createSpyObj('TagService', ['getAll']);
    const toastrServiceSpy = jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const locationSpy = jasmine.createSpyObj('Location', ['back']);

    // Initialize paramMapSubject with empty params (create mode)
    paramMapSubject = new BehaviorSubject(convertToParamMap({}));

    await TestBed.configureTestingModule({
      imports: [ArticleFormComponent, ReactiveFormsModule, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ArticleService, useValue: articleServiceSpy },
        { provide: TagService, useValue: tagServiceSpy },
        { provide: ToastrService, useValue: toastrServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: Location, useValue: locationSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: paramMapSubject.asObservable(),
          },
        },
      ],
    }).compileComponents();

    articleService = TestBed.inject(ArticleService) as jasmine.SpyObj<ArticleService>;
    tagService = TestBed.inject(TagService) as jasmine.SpyObj<TagService>;
    toastrService = TestBed.inject(ToastrService) as jasmine.SpyObj<ToastrService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    location = TestBed.inject(Location) as jasmine.SpyObj<Location>;

    tagService.getAll.and.returnValue(of([]));

    fixture = TestBed.createComponent(ArticleFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    expect(component.articleForm).toBeDefined();
    expect(component.articleForm.get('title')?.value).toBe('');
    expect(component.articleForm.get('content')?.value).toBe('');
    expect(component.articleForm.get('draft')?.value).toBe(true);
  });

  it('should mark form as invalid when title is empty', () => {
    const titleControl = component.articleForm.get('title');
    titleControl?.setValue('');

    expect(titleControl?.invalid).toBeTrue();
    expect(titleControl?.errors?.['required']).toBeTrue();
  });

  it('should mark form as valid when required fields are filled', () => {
    component.articleForm.patchValue({
      title: 'Test Title',
      content: 'Test Content',
    });

    expect(component.articleForm.valid).toBeTrue();
  });

  it('should call articleService.create when creating new article', () => {
    articleService.create.and.returnValue(of(mockArticle));

    component.articleForm.patchValue({
      title: 'New Article',
      content: 'New Content',
      draft: true,
    });

    component.onSubmit(false);

    expect(articleService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        title: 'New Article',
        content: 'New Content',
        draft: true,
      })
    );
  });

  it('should navigate to article list after successful creation', () => {
    articleService.create.and.returnValue(of(mockArticle));

    component.articleForm.patchValue({
      title: 'New Article',
      content: 'New Content',
    });

    component.onSubmit(false);

    expect(router.navigate).toHaveBeenCalledWith(['/admin/articles']);
    expect(toastrService.success).toHaveBeenCalled();
  });

  it('should show error message when article creation fails', () => {
    articleService.create.and.returnValue(throwError(() => new Error('Server error')));

    component.articleForm.patchValue({
      title: 'New Article',
      content: 'New Content',
    });

    component.onSubmit(false);

    expect(toastrService.error).toHaveBeenCalledWith(
      "Erreur lors de la création de l'article",
      'Erreur'
    );
    expect(component.submitting).toBeFalse();
  });

  it('should show warning when form is invalid on submit', () => {
    component.articleForm.patchValue({
      title: '',
      content: '',
    });

    component.onSubmit(false);

    expect(toastrService.warning).toHaveBeenCalledWith(
      'Veuillez remplir tous les champs requis',
      'Formulaire invalide'
    );
    expect(articleService.create).not.toHaveBeenCalled();
  });

  it('should toggle tag selection', () => {
    component.toggleTag(1);
    expect(component.selectedTags).toContain(1);

    component.toggleTag(1);
    expect(component.selectedTags).not.toContain(1);
  });

  it('should check if tag is selected', () => {
    component.selectedTags = [1, 2, 3];

    expect(component.isTagSelected(1)).toBeTrue();
    expect(component.isTagSelected(2)).toBeTrue();
    expect(component.isTagSelected(4)).toBeFalse();
  });

  it('should set draft to false when publishing', () => {
    articleService.create.and.returnValue(of(mockArticle));

    component.articleForm.patchValue({
      title: 'New Article',
      content: 'New Content',
    });

    component.onSubmit(true);

    expect(articleService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        draft: false,
      })
    );
  });

  it('should navigate back when cancel is clicked', () => {
    component.goBack();

    expect(location.back).toHaveBeenCalled();
  });

  it('should include tagIds in request when tags are selected', () => {
    articleService.create.and.returnValue(of(mockArticle));

    component.articleForm.patchValue({
      title: 'New Article',
      content: 'New Content',
    });
    component.selectedTags = [1, 2];

    component.onSubmit(false);

    expect(articleService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        tagIds: [1, 2],
      })
    );
  });

  it('should set excerpt to undefined when empty', () => {
    articleService.create.and.returnValue(of(mockArticle));

    component.articleForm.patchValue({
      title: 'New Article',
      content: 'New Content',
      excerpt: '',
    });

    component.onSubmit(false);

    expect(articleService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        excerpt: undefined,
      })
    );
  });
});
