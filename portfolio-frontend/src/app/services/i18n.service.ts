import { Injectable } from '@angular/core';

/**
 * Simple i18n service for managing French translations
 * Provides centralized string management for UI messages
 */
@Injectable({
  providedIn: 'root',
})
export class I18nService {
  private readonly translations: Record<string, string> = {
    // Article list
    'article.list.loadError': 'Erreur lors du chargement des articles',
    'article.list.deleteSuccess': 'Suppression réussie',
    'article.list.deleteError': 'Erreur lors de la suppression',
    'article.list.deleteConfirm': 'supprimé',
    'article.list.publishSuccess': 'Succès',
    'article.list.published': 'publié',
    'article.list.unpublished': 'dépublié',
    'article.list.publishError': 'publication',
    'article.list.unpublishError': 'dépublication',
    'article.list.statusDraft': 'Brouillon',
    'article.list.statusPublished': 'Publié',
    'article.list.notPublished': 'Non publié',
    'article.error': 'Erreur',

    // Article form
    'article.form.saveWarning': "Veuillez d'abord sauvegarder l'article avant d'ajouter des images",
    'article.form.warningTitle': 'Avertissement',
    'article.form.imageUploadSuccess': 'Image uploadée avec succès',
    'article.form.imageUploadSuccessTitle': 'Succès',
    'article.form.imageUploadError': "Erreur lors de l'upload de l'image",
    'article.form.imageUploadErrorTitle': 'Erreur',
    'article.form.tagsLoadError': 'Erreur lors du chargement des tags',
    'article.form.articleLoadError': "Erreur lors du chargement de l'article",
    'article.form.articleLoadErrorTitle': 'Erreur',
    'article.form.validationError': 'Veuillez remplir tous les champs requis',
    'article.form.validationErrorTitle': 'Formulaire invalide',
    'article.form.createSuccessDraft': 'créé en brouillon',
    'article.form.createSuccessPublished': 'créé et publié',
    'article.form.createSuccessTitle': 'Succès',
    'article.form.createError': "Erreur lors de la création de l'article",
    'article.form.createErrorTitle': 'Erreur',
    'article.form.updateSuccessDraft': 'mis à jour',
    'article.form.updateSuccessPublished': 'mis à jour et publié',
    'article.form.updateSuccessTitle': 'Succès',
    'article.form.updateError': "Erreur lors de la mise à jour de l'article",
    'article.form.updateErrorTitle': 'Erreur',

    // Common
    'common.error': 'Erreur',
    'common.success': 'Succès',
  };

  get(key: string): string {
    return this.translations[key] || key;
  }

  /**
   * Format string with variables
   * Example: format('Article {0} deleted', 'Test') => 'Article Test deleted'
   */
  format(key: string, ...args: string[]): string {
    let text = this.get(key);
    args.forEach((arg, index) => {
      text = text.replace(`{${index}}`, arg);
    });
    return text;
  }
}
