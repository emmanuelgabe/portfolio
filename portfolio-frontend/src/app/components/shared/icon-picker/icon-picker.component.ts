import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface IconItem {
  class: string;
  name: string;
}

@Component({
  selector: 'app-icon-picker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './icon-picker.component.html',
  styleUrls: ['./icon-picker.component.scss'],
})
export class IconPickerComponent {
  @Input() selectedIcon = '';
  @Input() previewColor = '#007bff';
  @Output() iconSelected = new EventEmitter<string>();

  searchTerm = '';
  activeTab: 'brands' | 'solid' = 'brands';

  readonly brandIcons: IconItem[] = [
    { class: 'fa-brands fa-angular', name: 'Angular' },
    { class: 'fa-brands fa-react', name: 'React' },
    { class: 'fa-brands fa-vuejs', name: 'Vue.js' },
    { class: 'fa-brands fa-node-js', name: 'Node.js' },
    { class: 'fa-brands fa-java', name: 'Java' },
    { class: 'fa-brands fa-python', name: 'Python' },
    { class: 'fa-brands fa-js', name: 'JavaScript' },
    { class: 'fa-brands fa-html5', name: 'HTML5' },
    { class: 'fa-brands fa-css3-alt', name: 'CSS3' },
    { class: 'fa-brands fa-sass', name: 'Sass' },
    { class: 'fa-brands fa-bootstrap', name: 'Bootstrap' },
    { class: 'fa-brands fa-docker', name: 'Docker' },
    { class: 'fa-brands fa-aws', name: 'AWS' },
    { class: 'fa-brands fa-google-drive', name: 'Google Cloud' },
    { class: 'fa-brands fa-microsoft', name: 'Azure' },
    { class: 'fa-brands fa-git-alt', name: 'Git' },
    { class: 'fa-brands fa-github', name: 'GitHub' },
    { class: 'fa-brands fa-gitlab', name: 'GitLab' },
    { class: 'fa-brands fa-bitbucket', name: 'Bitbucket' },
    { class: 'fa-brands fa-linux', name: 'Linux' },
    { class: 'fa-brands fa-ubuntu', name: 'Ubuntu' },
    { class: 'fa-brands fa-windows', name: 'Windows' },
    { class: 'fa-brands fa-apple', name: 'Apple' },
    { class: 'fa-brands fa-android', name: 'Android' },
    { class: 'fa-brands fa-swift', name: 'Swift' },
    { class: 'fa-brands fa-php', name: 'PHP' },
    { class: 'fa-brands fa-laravel', name: 'Laravel' },
    { class: 'fa-brands fa-symfony', name: 'Symfony' },
    { class: 'fa-brands fa-wordpress', name: 'WordPress' },
    { class: 'fa-brands fa-shopify', name: 'Shopify' },
    { class: 'fa-brands fa-figma', name: 'Figma' },
    { class: 'fa-brands fa-sketch', name: 'Sketch' },
    { class: 'fa-brands fa-npm', name: 'npm' },
    { class: 'fa-brands fa-yarn', name: 'Yarn' },
    { class: 'fa-brands fa-rust', name: 'Rust' },
    { class: 'fa-brands fa-golang', name: 'Go' },
    { class: 'fa-brands fa-flutter', name: 'Flutter' },
    { class: 'fa-brands fa-slack', name: 'Slack' },
    { class: 'fa-brands fa-jira', name: 'Jira' },
    { class: 'fa-brands fa-confluence', name: 'Confluence' },
    { class: 'fa-brands fa-trello', name: 'Trello' },
    { class: 'fa-brands fa-jenkins', name: 'Jenkins' },
    { class: 'fa-brands fa-stripe', name: 'Stripe' },
    { class: 'fa-brands fa-paypal', name: 'PayPal' },
  ];

  readonly solidIcons: IconItem[] = [
    { class: 'fa-solid fa-code', name: 'Code' },
    { class: 'fa-solid fa-database', name: 'Base de donnees' },
    { class: 'fa-solid fa-server', name: 'Serveur' },
    { class: 'fa-solid fa-cloud', name: 'Cloud' },
    { class: 'fa-solid fa-terminal', name: 'Terminal' },
    { class: 'fa-solid fa-gear', name: 'Parametres' },
    { class: 'fa-solid fa-gears', name: 'Engrenages' },
    { class: 'fa-solid fa-microchip', name: 'Processeur' },
    { class: 'fa-solid fa-laptop-code', name: 'Laptop Code' },
    { class: 'fa-solid fa-desktop', name: 'Desktop' },
    { class: 'fa-solid fa-mobile-screen', name: 'Mobile' },
    { class: 'fa-solid fa-tablet-screen-button', name: 'Tablette' },
    { class: 'fa-solid fa-diagram-project', name: 'Diagramme' },
    { class: 'fa-solid fa-sitemap', name: 'Sitemap' },
    { class: 'fa-solid fa-network-wired', name: 'Reseau' },
    { class: 'fa-solid fa-wifi', name: 'WiFi' },
    { class: 'fa-solid fa-shield-halved', name: 'Securite' },
    { class: 'fa-solid fa-lock', name: 'Cadenas' },
    { class: 'fa-solid fa-key', name: 'Cle' },
    { class: 'fa-solid fa-bug', name: 'Bug' },
    { class: 'fa-solid fa-vial', name: 'Test' },
    { class: 'fa-solid fa-flask', name: 'Labo' },
    { class: 'fa-solid fa-rocket', name: 'Fusee' },
    { class: 'fa-solid fa-bolt', name: 'Eclair' },
    { class: 'fa-solid fa-fire', name: 'Feu' },
    { class: 'fa-solid fa-star', name: 'Etoile' },
    { class: 'fa-solid fa-chart-line', name: 'Graphique' },
    { class: 'fa-solid fa-chart-pie', name: 'Camembert' },
    { class: 'fa-solid fa-cube', name: 'Cube' },
    { class: 'fa-solid fa-cubes', name: 'Cubes' },
    { class: 'fa-solid fa-layer-group', name: 'Layers' },
    { class: 'fa-solid fa-puzzle-piece', name: 'Puzzle' },
    { class: 'fa-solid fa-toolbox', name: 'Boite outils' },
    { class: 'fa-solid fa-wrench', name: 'Cle molette' },
    { class: 'fa-solid fa-screwdriver-wrench', name: 'Outils' },
    { class: 'fa-solid fa-hammer', name: 'Marteau' },
    { class: 'fa-solid fa-pen-ruler', name: 'Design' },
    { class: 'fa-solid fa-palette', name: 'Palette' },
    { class: 'fa-solid fa-paintbrush', name: 'Pinceau' },
    { class: 'fa-solid fa-wand-magic-sparkles', name: 'Magie' },
    { class: 'fa-solid fa-brain', name: 'IA/ML' },
    { class: 'fa-solid fa-robot', name: 'Robot' },
    { class: 'fa-solid fa-eye', name: 'Vision' },
    { class: 'fa-solid fa-magnifying-glass', name: 'Recherche' },
    { class: 'fa-solid fa-filter', name: 'Filtre' },
    { class: 'fa-solid fa-sort', name: 'Tri' },
    { class: 'fa-solid fa-list', name: 'Liste' },
    { class: 'fa-solid fa-table', name: 'Tableau' },
    { class: 'fa-solid fa-folder', name: 'Dossier' },
    { class: 'fa-solid fa-file-code', name: 'Fichier code' },
    { class: 'fa-solid fa-file-lines', name: 'Document' },
    { class: 'fa-solid fa-book', name: 'Livre' },
    { class: 'fa-solid fa-graduation-cap', name: 'Education' },
  ];

  get filteredIcons(): IconItem[] {
    const icons = this.activeTab === 'brands' ? this.brandIcons : this.solidIcons;
    if (!this.searchTerm) {
      return icons;
    }
    const term = this.searchTerm.toLowerCase();
    return icons.filter(
      (icon) => icon.name.toLowerCase().includes(term) || icon.class.toLowerCase().includes(term)
    );
  }

  selectIcon(iconClass: string): void {
    this.selectedIcon = iconClass;
    this.iconSelected.emit(iconClass);
  }

  setTab(tab: 'brands' | 'solid'): void {
    this.activeTab = tab;
    this.searchTerm = '';
  }
}
