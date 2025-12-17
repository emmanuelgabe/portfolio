import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { LoggerService } from '../../services/logger.service';
import { LoginRequest } from '../../models/auth.model';

/**
 * Login page component
 * Provides user authentication with username/password form
 * Supports remember me functionality and return URL redirection
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  private readonly logger = inject(LoggerService);

  loginForm!: FormGroup;
  isLoading = false;
  returnUrl = '/';

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.logger.info('[LOGIN] Already authenticated, redirecting');
      this.router.navigate(['/']);
      return;
    }

    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';

    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false],
    });
  }

  /**
   * Handle form submission
   */
  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.toastr.warning(
        this.translate.instant('login.fillFields'),
        this.translate.instant('login.formInvalid')
      );
      return;
    }

    this.isLoading = true;
    this.loginForm.disable();

    const credentials: LoginRequest = {
      username: this.loginForm.value.username,
      password: this.loginForm.value.password,
    };

    this.logger.info('[LOGIN] Login attempt', { username: credentials.username });

    this.authService.login(credentials).subscribe({
      next: () => {
        this.logger.info('[LOGIN] Login successful, redirecting', { returnUrl: this.returnUrl });
        this.toastr.success(
          `${this.translate.instant('login.welcome')} ${credentials.username} !`,
          this.translate.instant('login.successTitle')
        );
        this.router.navigate([this.returnUrl]);
      },
      error: (error) => {
        this.isLoading = false;
        this.loginForm.enable();
        this.logger.error('[LOGIN] Login failed', {
          username: credentials.username,
          status: error.status,
          message: error.message,
        });

        if (error.status === 401) {
          this.toastr.error(
            this.translate.instant('login.invalidCredentials'),
            this.translate.instant('login.failedTitle')
          );
        } else if (error.status === 429) {
          this.toastr.error(
            this.translate.instant('login.rateLimited'),
            this.translate.instant('login.rateLimitedTitle')
          );
        } else if (error.status === 0) {
          this.toastr.error(
            this.translate.instant('login.networkError'),
            this.translate.instant('login.networkErrorTitle')
          );
        } else {
          this.toastr.error(
            this.translate.instant('login.genericError'),
            this.translate.instant('login.failedTitle')
          );
        }
      },
    });
  }

  /**
   * Check if a form field has validation errors
   */
  hasError(fieldName: string, errorType: string): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field?.hasError(errorType) && (field?.touched || field?.dirty));
  }

  /**
   * Check if a form field is invalid
   */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field?.invalid && (field?.touched || field?.dirty));
  }
}
