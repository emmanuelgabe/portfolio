import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
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
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toastr = inject(ToastrService);
  private readonly logger = inject(LoggerService);

  loginForm!: FormGroup;
  isLoading = false;
  returnUrl = '/';

  ngOnInit(): void {
    // Redirect if already logged in
    if (this.authService.isAuthenticated()) {
      this.logger.info('[LOGIN] Already authenticated, redirecting');
      this.router.navigate(['/']);
      return;
    }

    // Get return URL from query params or default to home
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';

    // Initialize login form
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
      this.markFormGroupTouched(this.loginForm);
      this.toastr.warning('Veuillez remplir tous les champs correctement', 'Formulaire invalide');
      return;
    }

    this.isLoading = true;
    this.loginForm.disable();

    const credentials: LoginRequest = {
      username: this.loginForm.value.username,
      password: this.loginForm.value.password,
    };
    const rememberMe = this.loginForm.value.rememberMe;

    this.logger.info('[LOGIN] Login attempt', { username: credentials.username });

    this.authService.login(credentials, rememberMe).subscribe({
      next: () => {
        this.logger.info('[LOGIN] Login successful, redirecting', { returnUrl: this.returnUrl });
        this.toastr.success(`Bienvenue ${credentials.username} !`, 'Connexion réussie');
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
          this.toastr.error("Nom d'utilisateur ou mot de passe incorrect", 'Échec de connexion');
        } else if (error.status === 429) {
          this.toastr.error(
            'Limite de tentatives atteinte. Veuillez réessayer dans 1 heure.',
            'Trop de tentatives'
          );
        } else if (error.status === 0) {
          this.toastr.error('Impossible de contacter le serveur', 'Erreur réseau');
        } else {
          this.toastr.error('Une erreur est survenue lors de la connexion', 'Erreur');
        }
      },
    });
  }

  /**
   * Mark all form fields as touched to trigger validation messages
   */
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
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
