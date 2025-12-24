import { TestBed } from '@angular/core/testing';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { ModalService, ConfirmationConfig } from './modal.service';
import { ConfirmationModalComponent } from '../components/shared/confirmation-modal/confirmation-modal.component';

describe('ModalService', () => {
  let service: ModalService;
  let ngbModalSpy: jasmine.SpyObj<NgbModal>;
  let mockModalRef: jasmine.SpyObj<NgbModalRef>;
  let mockComponentInstance: Partial<ConfirmationModalComponent>;

  beforeEach(() => {
    mockComponentInstance = {
      title: 'Confirmation',
      message: 'Default message',
      confirmText: 'Confirmer',
      cancelText: 'Annuler',
      confirmButtonClass: 'btn-danger',
      disableConfirm: false,
    };

    mockModalRef = jasmine.createSpyObj('NgbModalRef', [], {
      componentInstance: mockComponentInstance,
      result: Promise.resolve(true),
    });

    ngbModalSpy = jasmine.createSpyObj('NgbModal', ['open']);
    ngbModalSpy.open.and.returnValue(mockModalRef);

    TestBed.configureTestingModule({
      providers: [ModalService, { provide: NgbModal, useValue: ngbModalSpy }],
    });

    service = TestBed.inject(ModalService);
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ========== confirm Tests ==========

  it('should_openModal_when_confirmCalled', () => {
    // Arrange / Act
    service.confirm().subscribe();

    // Assert
    expect(ngbModalSpy.open).toHaveBeenCalledWith(
      ConfirmationModalComponent,
      jasmine.objectContaining({
        centered: true,
        backdrop: 'static',
      })
    );
  });

  it('should_returnTrue_when_modalConfirmed', (done) => {
    // Arrange
    Object.defineProperty(mockModalRef, 'result', {
      get: () => Promise.resolve(true),
    });

    // Act
    service.confirm().subscribe((result) => {
      // Assert
      expect(result).toBeTrue();
      done();
    });
  });

  it('should_returnFalse_when_modalDismissed', (done) => {
    // Arrange
    Object.defineProperty(mockModalRef, 'result', {
      get: () => Promise.reject(false),
    });

    // Act
    service.confirm().subscribe((result) => {
      // Assert
      expect(result).toBeFalse();
      done();
    });
  });

  it('should_setTitle_when_configHasTitle', () => {
    // Arrange
    const config: ConfirmationConfig = {
      title: 'Custom Title',
    };

    // Act
    service.confirm(config).subscribe();

    // Assert
    expect(mockComponentInstance.title).toBe('Custom Title');
  });

  it('should_setMessage_when_configHasMessage', () => {
    // Arrange
    const config: ConfirmationConfig = {
      message: 'Are you sure you want to proceed?',
    };

    // Act
    service.confirm(config).subscribe();

    // Assert
    expect(mockComponentInstance.message).toBe('Are you sure you want to proceed?');
  });

  it('should_setConfirmText_when_configHasConfirmText', () => {
    // Arrange
    const config: ConfirmationConfig = {
      confirmText: 'Yes, do it',
    };

    // Act
    service.confirm(config).subscribe();

    // Assert
    expect(mockComponentInstance.confirmText).toBe('Yes, do it');
  });

  it('should_setCancelText_when_configHasCancelText', () => {
    // Arrange
    const config: ConfirmationConfig = {
      cancelText: 'No, cancel',
    };

    // Act
    service.confirm(config).subscribe();

    // Assert
    expect(mockComponentInstance.cancelText).toBe('No, cancel');
  });

  it('should_setConfirmButtonClass_when_configHasConfirmButtonClass', () => {
    // Arrange
    const config: ConfirmationConfig = {
      confirmButtonClass: 'btn-primary',
    };

    // Act
    service.confirm(config).subscribe();

    // Assert
    expect(mockComponentInstance.confirmButtonClass).toBe('btn-primary');
  });

  it('should_setDisableConfirm_when_configHasDisableConfirm', () => {
    // Arrange
    const config: ConfirmationConfig = {
      disableConfirm: true,
    };

    // Act
    service.confirm(config).subscribe();

    // Assert
    expect(mockComponentInstance.disableConfirm).toBeTrue();
  });

  it('should_setAllConfigOptions_when_fullConfigProvided', () => {
    // Arrange
    const config: ConfirmationConfig = {
      title: 'Full Config Title',
      message: 'Full config message',
      confirmText: 'Proceed',
      cancelText: 'Abort',
      confirmButtonClass: 'btn-warning',
      disableConfirm: true,
    };

    // Act
    service.confirm(config).subscribe();

    // Assert
    expect(mockComponentInstance.title).toBe('Full Config Title');
    expect(mockComponentInstance.message).toBe('Full config message');
    expect(mockComponentInstance.confirmText).toBe('Proceed');
    expect(mockComponentInstance.cancelText).toBe('Abort');
    expect(mockComponentInstance.confirmButtonClass).toBe('btn-warning');
    expect(mockComponentInstance.disableConfirm).toBeTrue();
  });

  it('should_useDefaultValues_when_emptyConfigProvided', () => {
    // Arrange
    const originalTitle = mockComponentInstance.title;
    const originalMessage = mockComponentInstance.message;

    // Act
    service.confirm({}).subscribe();

    // Assert
    expect(mockComponentInstance.title).toBe(originalTitle);
    expect(mockComponentInstance.message).toBe(originalMessage);
  });

  // ========== confirmDelete Tests ==========

  it('should_openModal_when_confirmDeleteCalled', () => {
    // Arrange / Act
    service.confirmDelete('Test Item').subscribe();

    // Assert
    expect(ngbModalSpy.open).toHaveBeenCalled();
  });

  it('should_setDeleteTitle_when_confirmDeleteCalled', () => {
    // Arrange / Act
    service.confirmDelete('Test Item').subscribe();

    // Assert
    expect(mockComponentInstance.title).toBe('Êtes-vous sûr ?');
  });

  it('should_setDeleteMessage_when_confirmDeleteCalled', () => {
    // Arrange / Act
    service.confirmDelete('My Project').subscribe();

    // Assert
    expect(mockComponentInstance.message).toContain('My Project');
    expect(mockComponentInstance.message).toContain('irréversible');
  });

  it('should_setDeleteConfirmText_when_confirmDeleteCalled', () => {
    // Arrange / Act
    service.confirmDelete('Item').subscribe();

    // Assert
    expect(mockComponentInstance.confirmText).toBe('Supprimer');
  });

  it('should_setDeleteCancelText_when_confirmDeleteCalled', () => {
    // Arrange / Act
    service.confirmDelete('Item').subscribe();

    // Assert
    expect(mockComponentInstance.cancelText).toBe('Annuler');
  });

  it('should_setDangerButtonClass_when_confirmDeleteCalled', () => {
    // Arrange / Act
    service.confirmDelete('Item').subscribe();

    // Assert
    expect(mockComponentInstance.confirmButtonClass).toBe('btn-danger');
  });

  it('should_disableConfirm_when_confirmDeleteCalledWithDisableTrue', () => {
    // Arrange / Act
    service.confirmDelete('Item', true).subscribe();

    // Assert
    expect(mockComponentInstance.disableConfirm).toBeTrue();
  });

  it('should_enableConfirm_when_confirmDeleteCalledWithDisableFalse', () => {
    // Arrange / Act
    service.confirmDelete('Item', false).subscribe();

    // Assert
    expect(mockComponentInstance.disableConfirm).toBeFalse();
  });

  it('should_enableConfirmByDefault_when_confirmDeleteCalledWithoutDisable', () => {
    // Arrange / Act
    service.confirmDelete('Item').subscribe();

    // Assert
    expect(mockComponentInstance.disableConfirm).toBeFalse();
  });

  it('should_returnTrue_when_deleteConfirmed', (done) => {
    // Arrange
    Object.defineProperty(mockModalRef, 'result', {
      get: () => Promise.resolve(true),
    });

    // Act
    service.confirmDelete('Test Item').subscribe((result) => {
      // Assert
      expect(result).toBeTrue();
      done();
    });
  });

  it('should_returnFalse_when_deleteCancelled', (done) => {
    // Arrange
    Object.defineProperty(mockModalRef, 'result', {
      get: () => Promise.reject(false),
    });

    // Act
    service.confirmDelete('Test Item').subscribe((result) => {
      // Assert
      expect(result).toBeFalse();
      done();
    });
  });
});
