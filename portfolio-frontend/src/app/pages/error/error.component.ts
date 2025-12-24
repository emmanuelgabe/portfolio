import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LoggerService } from '../../services/logger.service';

interface ErrorConfig {
  code: string;
  title: string;
  message: string;
  image: string;
  linkText: string;
  linkUrl: string;
}

const ERROR_CONFIGS: Record<string, ErrorConfig> = {
  '403': {
    code: '403',
    title: 'Accès refusé',
    message: "Vous n'avez pas la permission d'accéder à cette page.",
    image: 'assets/images/errors/403.jpg',
    linkText: 'Se connecter',
    linkUrl: '/login',
  },
  '404': {
    code: '404',
    title: 'Page non trouvée',
    message: "La page que vous recherchez n'existe pas ou a été déplacée.",
    image: 'assets/images/errors/404.jpg',
    linkText: "Retour à l'accueil",
    linkUrl: '/',
  },
  '429': {
    code: '429',
    title: 'Trop de requêtes',
    message: 'Vous avez effectué trop de requêtes. Veuillez patienter quelques instants.',
    image: 'assets/images/errors/429.jpg',
    linkText: "Retour à l'accueil",
    linkUrl: '/',
  },
  '500': {
    code: '500',
    title: 'Erreur serveur',
    message: "Une erreur inattendue s'est produite sur nos serveurs.",
    image: 'assets/images/errors/500.jpg',
    linkText: "Retour à l'accueil",
    linkUrl: '/',
  },
  offline: {
    code: 'offline',
    title: 'Connexion perdue',
    message: 'Vous semblez être hors ligne. Vérifiez votre connexion internet.',
    image: 'assets/images/errors/Offline.jpg',
    linkText: 'Réessayer',
    linkUrl: '/',
  },
};

const DEFAULT_ERROR = ERROR_CONFIGS['404'];

@Component({
  selector: 'app-error',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './error.component.html',
  styleUrl: './error.component.css',
})
export class ErrorComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly logger = inject(LoggerService);

  config: ErrorConfig = DEFAULT_ERROR;

  ngOnInit(): void {
    const code = this.route.snapshot.paramMap.get('code') || '404';
    this.config = ERROR_CONFIGS[code] || DEFAULT_ERROR;

    this.logger.warn('[ERROR_PAGE] Error page displayed', {
      code: this.config.code,
      title: this.config.title,
    });
  }

  reload(): void {
    this.logger.info('[USER_ACTION] Page reload requested', { code: this.config.code });
    window.location.reload();
  }
}
