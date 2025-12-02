import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideToastr } from 'ngx-toastr';
import { HomeComponent } from './home.component';
import { ProjectService } from '../../services/project.service';
import { SkillService } from '../../services/skill.service';
import { CvService } from '../../services/cv.service';
import { ExperienceService } from '../../services/experience.service';
import { ArticleService } from '../../services/article.service';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { ProjectResponse } from '../../models';
import { Skill, SkillCategory, IconType } from '../../models/skill.model';
import { SiteConfigurationResponse } from '../../models/site-configuration.model';
import { of, throwError, NEVER } from 'rxjs';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let mockProjectService: jasmine.SpyObj<ProjectService>;
  let mockSkillService: jasmine.SpyObj<SkillService>;
  let mockCvService: jasmine.SpyObj<CvService>;
  let mockExperienceService: jasmine.SpyObj<ExperienceService>;
  let mockArticleService: jasmine.SpyObj<ArticleService>;
  let mockSiteConfigService: jasmine.SpyObj<SiteConfigurationService>;
  let mockFeaturedProjects: ProjectResponse[];

  const mockSiteConfig: SiteConfigurationResponse = {
    id: 1,
    fullName: 'Emmanuel Gabe',
    email: 'contact@emmanuelgabe.com',
    heroTitle: 'Welcome to My Portfolio',
    heroDescription: 'Full-stack developer with passion for clean code',
    siteTitle: 'Portfolio - Emmanuel Gabe',
    seoDescription: 'Portfolio de Emmanuel Gabe',
    profileImageUrl: undefined,
    githubUrl: 'https://github.com/emmanuelgabe',
    linkedinUrl: 'https://linkedin.com/in/egabe',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    mockFeaturedProjects = [
      {
        id: 1,
        title: 'Featured Project 1',
        description: 'Description 1',
        techStack: 'Angular, TypeScript',
        githubUrl: 'https://github.com/test/project1',
        imageUrl: 'https://example.com/image1.png',
        demoUrl: 'https://demo1.example.com',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        featured: true,
        hasDetails: true,
        tags: [{ id: 1, name: 'Angular', color: '#dd0031' }],
        images: [],
      },
      {
        id: 2,
        title: 'Featured Project 2',
        description: 'Description 2',
        techStack: 'Spring Boot, Java',
        githubUrl: 'https://github.com/test/project2',
        createdAt: '2024-01-02T00:00:00Z',
        updatedAt: '2024-01-02T00:00:00Z',
        featured: true,
        hasDetails: true,
        tags: [{ id: 2, name: 'Spring Boot', color: '#6db33f' }],
        images: [],
      },
    ];

    mockProjectService = jasmine.createSpyObj('ProjectService', ['getAll']);
    mockProjectService.getAll.and.returnValue(of(mockFeaturedProjects));

    const mockSkills: Skill[] = [
      {
        id: 1,
        name: 'Angular',
        icon: 'bi-code-square',
        iconType: IconType.FONT_AWESOME,
        color: '#dd0031',
        category: SkillCategory.FRONTEND,
        categoryDisplayName: 'Frontend',
        displayOrder: 0,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 2,
        name: 'Spring Boot',
        icon: 'bi-gear',
        iconType: IconType.FONT_AWESOME,
        color: '#6db33f',
        category: SkillCategory.BACKEND,
        categoryDisplayName: 'Backend',
        displayOrder: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 3,
        name: 'Java',
        icon: 'bi-code',
        iconType: IconType.FONT_AWESOME,
        color: '#007396',
        category: SkillCategory.BACKEND,
        categoryDisplayName: 'Backend',
        displayOrder: 2,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 4,
        name: 'TypeScript',
        icon: 'bi-filetype-ts',
        iconType: IconType.FONT_AWESOME,
        color: '#3178c6',
        category: SkillCategory.FRONTEND,
        categoryDisplayName: 'Frontend',
        displayOrder: 3,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 5,
        name: 'PostgreSQL',
        icon: 'bi-database',
        iconType: IconType.FONT_AWESOME,
        color: '#336791',
        category: SkillCategory.DATABASE,
        categoryDisplayName: 'Database',
        displayOrder: 4,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 6,
        name: 'Docker',
        icon: 'bi-box',
        iconType: IconType.FONT_AWESOME,
        color: '#2496ed',
        category: SkillCategory.DEVOPS,
        categoryDisplayName: 'DevOps',
        displayOrder: 5,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 7,
        name: 'Git',
        icon: 'bi-git',
        iconType: IconType.FONT_AWESOME,
        color: '#f05032',
        category: SkillCategory.TOOLS,
        categoryDisplayName: 'Tools',
        displayOrder: 6,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 8,
        name: 'REST API',
        icon: 'bi-braces',
        iconType: IconType.FONT_AWESOME,
        color: '#009688',
        category: SkillCategory.BACKEND,
        categoryDisplayName: 'Backend',
        displayOrder: 7,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
    ];

    mockSkillService = jasmine.createSpyObj('SkillService', ['getAll']);
    mockSkillService.getAll.and.returnValue(of(mockSkills));

    mockCvService = jasmine.createSpyObj('CvService', ['getCurrentCv', 'downloadCv']);
    mockCvService.getCurrentCv.and.returnValue(of(null));

    mockExperienceService = jasmine.createSpyObj('ExperienceService', ['getAll']);
    mockExperienceService.getAll.and.returnValue(of([]));

    mockArticleService = jasmine.createSpyObj('ArticleService', ['getAll']);
    mockArticleService.getAll.and.returnValue(of([]));

    mockSiteConfigService = jasmine.createSpyObj('SiteConfigurationService', [
      'getSiteConfiguration',
    ]);
    mockSiteConfigService.getSiteConfiguration.and.returnValue(of(mockSiteConfig));

    await TestBed.configureTestingModule({
      imports: [HomeComponent, RouterModule.forRoot([])],
      providers: [
        { provide: ProjectService, useValue: mockProjectService },
        { provide: SkillService, useValue: mockSkillService },
        { provide: CvService, useValue: mockCvService },
        { provide: ExperienceService, useValue: mockExperienceService },
        { provide: ArticleService, useValue: mockArticleService },
        { provide: SiteConfigurationService, useValue: mockSiteConfigService },
        provideHttpClient(),
        provideToastr(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load featured projects on init', () => {
    fixture.detectChanges();
    expect(mockProjectService.getAll).toHaveBeenCalled();
    expect(component.allProjects.length).toBe(2);
    expect(component.isLoadingProjects).toBeFalse();
    expect(component.projectsError).toBeUndefined();
  });

  it('should display hero section', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const heroSection = compiled.querySelector('.hero-section');
    expect(heroSection).toBeTruthy();
  });

  it('should display hero title', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const terminalText = compiled.querySelector('.terminal-text');
    expect(terminalText).toBeTruthy();
  });

  it('should display CTA buttons in hero', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const buttons = compiled.querySelectorAll('.hero-section .btn-style15');
    expect(buttons.length).toBeGreaterThanOrEqual(1);
  });

  it('should display skills section', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const skillsSection = compiled.querySelector('.skills-section');
    expect(skillsSection).toBeTruthy();
  });

  it('should display all skills', () => {
    fixture.detectChanges();
    expect(component.skills.length).toBe(8);

    const compiled = fixture.nativeElement as HTMLElement;
    const skillCards = compiled.querySelectorAll('.skill-card');
    expect(skillCards.length).toBe(8);
  });

  it('should display skill names', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const skillNames = compiled.querySelectorAll('.skill-name');

    expect(skillNames[0].textContent).toContain('Angular');
    expect(skillNames[1].textContent).toContain('Spring Boot');
  });

  it('should display loading spinner while loading projects', () => {
    // Configure mock to never complete (keeps loading state)
    mockProjectService.getAll.and.returnValue(NEVER);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const projectsSection = compiled.querySelector('#projects');
    const spinner = projectsSection?.querySelector('.spinner-border');
    expect(spinner).toBeTruthy();
  });

  it('should handle error when loading featured projects fails', () => {
    mockProjectService.getAll.and.returnValue(throwError(() => new Error('Network error')));
    fixture.detectChanges();

    expect(component.projectsError).toBe('Unable to load projects');
    expect(component.isLoadingProjects).toBeFalse();
  });

  it('should display error message when error occurs', () => {
    // Configure mock to return error
    mockProjectService.getAll.and.returnValue(throwError(() => new Error('Test error')));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const alert = compiled.querySelector('.alert-warning');
    expect(alert).toBeTruthy();
    expect(alert?.textContent).toContain('Unable to load projects');
  });

  it('should display no featured projects message when empty', () => {
    mockProjectService.getAll.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.allProjects.length).toBe(0);

    const compiled = fixture.nativeElement as HTMLElement;
    const noProjectsMessage = compiled.querySelector('.bi-inbox');
    expect(noProjectsMessage).toBeTruthy();
  });

  it('should display featured projects when loaded', () => {
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const projectCards = compiled.querySelectorAll('app-project-card');
    expect(projectCards.length).toBe(2);
  });

  it('should display toggle button when there are more projects', () => {
    // Configure mock to return more than 3 projects (triggers "show more" button)
    const manyProjects = [
      ...mockFeaturedProjects,
      { ...mockFeaturedProjects[0], id: 3, featured: false },
      { ...mockFeaturedProjects[0], id: 4, featured: false },
    ];
    mockProjectService.getAll.and.returnValue(of(manyProjects));
    fixture.detectChanges();

    expect(component.hasMoreProjects).toBeTrue();
    const compiled = fixture.nativeElement as HTMLElement;
    const toggleButton = compiled.querySelector('button.btn-outline-primary');
    expect(toggleButton).toBeTruthy();
  });

  it('should return true for hasFeaturedProjects when projects exist', () => {
    component.allProjects = mockFeaturedProjects;
    expect(component.featuredProjects.length).toBeGreaterThan(0);
  });

  it('should return false for hasFeaturedProjects when no projects', () => {
    component.allProjects = [];
    expect(component.featuredProjects.length).toBe(0);
  });

  it('should have correct skill colors', () => {
    fixture.detectChanges(); // Let ngOnInit load skills
    const angularSkill = component.skills.find((s) => s.name === 'Angular');
    expect(angularSkill?.color).toBe('#dd0031');

    const springBootSkill = component.skills.find((s) => s.name === 'Spring Boot');
    expect(springBootSkill?.color).toBe('#6db33f');
  });

  it('should have correct skill icons', () => {
    fixture.detectChanges(); // Let ngOnInit load skills
    const angularSkill = component.skills.find((s) => s.name === 'Angular');
    expect(angularSkill?.icon).toBe('bi-code-square');

    const dockerSkill = component.skills.find((s) => s.name === 'Docker');
    expect(dockerSkill?.icon).toBe('bi-box');
  });
});
