import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SkeletonTableRowComponent } from '../../../../components/shared/skeleton';
import { SkillService } from '../../../../services/skill.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { Skill } from '../../../../models/skill.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-skill-list',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, SkeletonTableRowComponent],
  templateUrl: './skill-list.component.html',
  styleUrls: ['./skill-list.component.scss'],
})
export class SkillListComponent implements OnInit, OnDestroy {
  private readonly skillService = inject(SkillService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  skills: Skill[] = [];
  loading = false;
  error?: string;

  reordering = false;

  ngOnInit(): void {
    this.loadSkills();
  }

  loadSkills(): void {
    this.loading = true;
    this.error = undefined;

    this.logger.info('[ADMIN_SKILLS] Loading skills list');

    this.skillService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (skills) => {
          this.skills = skills.sort((a, b) => a.displayOrder - b.displayOrder);
          this.loading = false;
          this.logger.info('[ADMIN_SKILLS] Skills loaded', { count: skills.length });
        },
        error: (error) => {
          this.error = this.translate.instant('admin.skills.loadError');
          this.loading = false;
          this.logger.error('[ADMIN_SKILLS] Failed to load skills', { error });
          this.toastr.error(this.translate.instant('admin.skills.loadError'));
        },
      });
  }

  moveUp(index: number): void {
    if (index <= 0 || this.reordering || this.demoModeService.isDemo()) {
      if (this.demoModeService.isDemo()) {
        this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
      }
      return;
    }

    this.swapAndSave(index, index - 1);
  }

  moveDown(index: number): void {
    if (index >= this.skills.length - 1 || this.reordering || this.demoModeService.isDemo()) {
      if (this.demoModeService.isDemo()) {
        this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
      }
      return;
    }

    this.swapAndSave(index, index + 1);
  }

  private swapAndSave(fromIndex: number, toIndex: number): void {
    this.reordering = true;

    // Swap items in the array
    const temp = this.skills[fromIndex];
    this.skills[fromIndex] = this.skills[toIndex];
    this.skills[toIndex] = temp;

    // Update display order values
    this.skills.forEach((skill, i) => (skill.displayOrder = i));

    // Get ordered IDs
    const orderedIds = this.skills.map((s) => s.id);

    this.skillService
      .reorder(orderedIds)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.reordering = false;
          this.logger.info('[ADMIN_SKILLS] Skills reordered');
        },
        error: (error) => {
          this.reordering = false;
          this.logger.error('[ADMIN_SKILLS] Failed to reorder skills', { error });
          this.toastr.error(this.translate.instant('admin.common.reorderError'));
          this.loadSkills(); // Reload to restore original order
        },
      });
  }

  deleteSkill(skill: Skill): void {
    this.logger.info('[ADMIN_SKILLS] Delete requested', { id: skill.id, name: skill.name });

    this.modalService.confirmDelete(skill.name, this.demoModeService.isDemo()).subscribe({
      next: (confirmed) => {
        if (confirmed) {
          if (this.demoModeService.isDemo()) {
            this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
            return;
          }
          this.performDelete(skill);
        } else {
          this.logger.info('[ADMIN_SKILLS] Delete cancelled', { id: skill.id });
        }
      },
      error: () => {
        this.logger.info('[ADMIN_SKILLS] Delete modal dismissed', { id: skill.id });
      },
    });
  }

  private performDelete(skill: Skill): void {
    this.logger.info('[ADMIN_SKILLS] Deleting skill', { id: skill.id });

    this.skillService
      .delete(skill.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.skills = this.skills.filter((s) => s.id !== skill.id);
          this.logger.info('[ADMIN_SKILLS] Skill deleted', { id: skill.id });
          this.toastr.success(
            this.translate.instant('admin.skills.deleteSuccess', { name: skill.name })
          );
        },
        error: (error) => {
          this.logger.error('[ADMIN_SKILLS] Failed to delete skill', { id: skill.id, error });
          this.toastr.error(this.translate.instant('admin.skills.deleteError'));
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
