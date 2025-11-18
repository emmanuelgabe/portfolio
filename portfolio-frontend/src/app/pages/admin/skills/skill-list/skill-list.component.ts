import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SkillService } from '../../../../services/skill.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { Skill } from '../../../../models/skill.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-skill-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './skill-list.component.html',
  styleUrls: ['./skill-list.component.scss'],
})
export class SkillListComponent implements OnInit {
  private readonly skillService = inject(SkillService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);

  skills: Skill[] = [];
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    this.loadSkills();
  }

  loadSkills(): void {
    this.loading = true;
    this.error = null;

    this.logger.info('[ADMIN_SKILLS] Loading skills list');

    this.skillService.getAll().subscribe({
      next: (skills) => {
        this.skills = skills.sort((a, b) => a.displayOrder - b.displayOrder);
        this.loading = false;
        this.logger.info('[ADMIN_SKILLS] Skills loaded', { count: skills.length });
      },
      error: (error) => {
        this.error = 'Failed to load skills';
        this.loading = false;
        this.logger.error('[ADMIN_SKILLS] Failed to load skills', { error });
        this.toastr.error('Erreur lors du chargement des compétences');
      },
    });
  }

  deleteSkill(skill: Skill): void {
    this.logger.info('[ADMIN_SKILLS] Delete requested', { id: skill.id, name: skill.name });

    this.modalService.confirmDelete(skill.name).subscribe({
      next: (confirmed) => {
        if (confirmed) {
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

    this.skillService.delete(skill.id).subscribe({
      next: () => {
        this.skills = this.skills.filter((s) => s.id !== skill.id);
        this.logger.info('[ADMIN_SKILLS] Skill deleted', { id: skill.id });
        this.toastr.success(`Compétence "${skill.name}" supprimée`, 'Suppression réussie');
      },
      error: (error) => {
        this.logger.error('[ADMIN_SKILLS] Failed to delete skill', { id: skill.id, error });
        this.toastr.error('Erreur lors de la suppression', 'Erreur');
      },
    });
  }
}
