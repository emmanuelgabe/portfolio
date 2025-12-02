import { Injectable, inject } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmationModalComponent } from '../components/shared/confirmation-modal/confirmation-modal.component';
import { Observable, from, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export interface ConfirmationConfig {
  title?: string;
  message?: string;
  confirmText?: string;
  cancelText?: string;
  confirmButtonClass?: string;
  disableConfirm?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class ModalService {
  private readonly modalService = inject(NgbModal);

  /**
   * Open confirmation modal
   * @param config Modal configuration
   * @returns Observable<boolean> - true if confirmed, false if cancelled
   */
  confirm(config: ConfirmationConfig = {}): Observable<boolean> {
    const modalRef: NgbModalRef = this.modalService.open(ConfirmationModalComponent, {
      centered: true,
      backdrop: 'static',
    });

    const instance = modalRef.componentInstance as ConfirmationModalComponent;

    if (config.title) instance.title = config.title;
    if (config.message) instance.message = config.message;
    if (config.confirmText) instance.confirmText = config.confirmText;
    if (config.cancelText) instance.cancelText = config.cancelText;
    if (config.confirmButtonClass) instance.confirmButtonClass = config.confirmButtonClass;
    if (config.disableConfirm !== undefined) instance.disableConfirm = config.disableConfirm;

    return from(modalRef.result).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  /**
   * Confirmation modal specifically for delete actions
   * @param itemName Name of the item to delete
   * @param disableConfirm Whether to disable the confirm button (for demo mode)
   * @returns Observable<boolean>
   */
  confirmDelete(itemName: string, disableConfirm = false): Observable<boolean> {
    return this.confirm({
      title: 'Êtes-vous sûr ?',
      message: `Cette action est irréversible. Voulez-vous vraiment supprimer "${itemName}" ?`,
      confirmText: 'Supprimer',
      cancelText: 'Annuler',
      confirmButtonClass: 'btn-danger',
      disableConfirm,
    });
  }
}
