import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ContactRequest } from '../../models';
import { ContactService } from '../../services/contact.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-contact-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslateModule],
  templateUrl: './contact-form.component.html',
  styleUrl: './contact-form.component.css',
})
export class ContactFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly contactService = inject(ContactService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  private readonly logger = inject(LoggerService);

  loading = false;
  submitted = false;

  contactForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    subject: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    message: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(5000)]],
    consent: [false, [Validators.requiredTrue]],
  });

  get name() {
    return this.contactForm.get('name');
  }

  get email() {
    return this.contactForm.get('email');
  }

  get subject() {
    return this.contactForm.get('subject');
  }

  get message() {
    return this.contactForm.get('message');
  }

  get consent() {
    return this.contactForm.get('consent');
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      this.toastr.warning(this.translate.instant('contact.validationError'));
      return;
    }

    this.loading = true;
    const request: ContactRequest = this.contactForm.value as ContactRequest;

    this.contactService.send(request).subscribe({
      next: (response) => {
        this.logger.info('[CONTACT_FORM] Message sent successfully', {
          success: response.success,
        });
        this.toastr.success(response.message || this.translate.instant('contact.success'));
        this.contactForm.reset();
        this.submitted = false;
        this.loading = false;
      },
      error: (error) => {
        this.logger.error('[CONTACT_FORM] Failed to send message', {
          status: error.status,
        });
        const errorMessage = error.customMessage || this.translate.instant('contact.error');
        this.toastr.error(errorMessage);
        this.loading = false;
      },
    });
  }
}
