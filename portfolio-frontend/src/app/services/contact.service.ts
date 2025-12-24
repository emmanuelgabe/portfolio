import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ContactRequest, ContactResponse } from '../models';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class ContactService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/contact`;

  send(request: ContactRequest): Observable<ContactResponse> {
    this.logger.info('[HTTP] POST /api/contact', {
      name: request.name,
      email: request.email,
      subject: request.subject,
    });

    return this.http.post<ContactResponse>(this.apiUrl, request).pipe(
      catchError((error) => {
        const errorMessage = this.getErrorMessage(error);
        this.logger.error('[HTTP_ERROR] Failed to send message', {
          status: error.status,
          message: errorMessage,
          email: request.email,
        });
        return throwError(() => ({ ...error, customMessage: errorMessage }));
      })
    );
  }

  private getErrorMessage(error: { status: number; error?: { message?: string } }): string {
    if (error.status === 429) {
      return 'Limite atteinte. Vous ne pouvez envoyer que 5 messages par heure. Veuillez réessayer plus tard.';
    } else if (error.status === 400) {
      return 'Données invalides. Veuillez vérifier le formulaire.';
    } else if (error.status === 500) {
      return "Erreur lors de l'envoi du message. Veuillez réessayer plus tard.";
    } else if (error.status === 0) {
      return 'Impossible de contacter le serveur. Vérifiez votre connexion internet.';
    } else {
      return error.error?.message || 'Une erreur inattendue est survenue.';
    }
  }
}
