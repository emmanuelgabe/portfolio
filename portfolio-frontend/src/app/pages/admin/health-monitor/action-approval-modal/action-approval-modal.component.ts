import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ActionLevel, ProposedAction } from '../../../../models/health-monitor.model';

export type ActionDecisionMode = 'approve' | 'reject';

/**
 * Modal dialog asking the admin to confirm an approve/reject decision on a
 * proposed action. Reuses {@link NgbActiveModal} to close itself with the
 * decision payload or dismiss on cancel.
 *
 * Reason field rules (enforced server-side too):
 * - approve + RED level  -> reason required
 * - reject (any level)   -> reason required
 * - approve + ORANGE     -> reason optional
 */
@Component({
  selector: 'app-action-approval-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './action-approval-modal.component.html',
  styleUrls: ['./action-approval-modal.component.scss'],
})
export class ActionApprovalModalComponent {
  readonly activeModal = inject(NgbActiveModal);

  @Input() action!: ProposedAction;
  @Input() mode: ActionDecisionMode = 'approve';

  reason = '';

  get isReject(): boolean {
    return this.mode === 'reject';
  }

  get isRedApprove(): boolean {
    return this.mode === 'approve' && this.action?.level === 'RED';
  }

  get reasonRequired(): boolean {
    return this.isReject || this.isRedApprove;
  }

  get titleKey(): string {
    return this.isReject
      ? 'admin.healthMonitor.modal.rejectTitle'
      : 'admin.healthMonitor.modal.approveTitle';
  }

  get confirmKey(): string {
    return this.isReject ? 'admin.healthMonitor.modal.reject' : 'admin.healthMonitor.modal.approve';
  }

  get confirmClass(): string {
    return this.isReject ? 'btn-danger' : this.isRedApprove ? 'btn-danger' : 'btn-success';
  }

  get paramsEntries(): { key: string; value: string }[] {
    if (!this.action?.params) {
      return [];
    }
    return Object.entries(this.action.params).map(([key, value]) => ({
      key,
      value: typeof value === 'string' ? value : JSON.stringify(value),
    }));
  }

  get levelBadgeClass(): string {
    return levelBadgeClass(this.action?.level);
  }

  get canConfirm(): boolean {
    if (!this.reasonRequired) {
      return true;
    }
    return this.reason.trim().length > 0;
  }

  confirm(): void {
    const trimmed = this.reason.trim();
    const reason = trimmed.length > 0 ? trimmed : undefined;
    this.activeModal.close({ reason });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}

function levelBadgeClass(level: ActionLevel | undefined): string {
  switch (level) {
    case 'RED':
      return 'bg-danger';
    case 'ORANGE':
      return 'bg-warning text-dark';
    case 'GREEN':
      return 'bg-success';
    default:
      return 'bg-secondary';
  }
}
