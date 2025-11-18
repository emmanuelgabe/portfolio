import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HomeComponent } from './home.component';
import { ProjectService } from '../../services/project.service';
import { SkillService } from '../../services/skill.service';
import { ProjectResponse } from '../../models';
import { Skill, SkillCategory } from '../../models/skill.model';
import { of, throwError } from 'rxjs';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let mockProjectService: jasmine.SpyObj<ProjectService>;
  let mockSkillService: jasmine.SpyObj<SkillService>;
  let mockFeaturedProjects: ProjectResponse[];

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
        tags: [{ id: 1, name: 'Angular', color: '#dd0031' }],
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
        tags: [{ id: 2, name: 'Spring Boot', color: '#6db33f' }],
      },
    ];

    mockProjectService = jasmine.createSpyObj('ProjectService', ['getAll']);
    mockProjectService.getAll.and.returnValue(of(mockFeaturedProjects));

    const mockSkills: Skill[] = [
      {
        id: 1,
        name: 'Angular',
        icon: 'bi-code-square',
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

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: ProjectService, useValue: mockProjectService },
        { provide: SkillService, useValue: mockSkillService },
        provideRouter([]),
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
    expect(component.projectsError).toBeNull();
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
    const title = compiled.querySelector('.hero-section h1');
    expect(title).toBeTruthy();
    expect(title?.textContent).toContain('Emmanuel Gabe');
  });

  it('should display CTA buttons in hero', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const buttons = compiled.querySelectorAll('.hero-section .btn');
    expect(buttons.length).toBeGreaterThanOrEqual(2);
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
    fixture.detectChanges(); // Let ngOnInit complete first
    component.isLoadingProjects = true;
    fixture.detectChanges(); // Render with loading state

    const compiled = fixture.nativeElement as HTMLElement;
    const spinner = compiled.querySelector('.spinner-border');
    expect(spinner).toBeTruthy();
  });

  it('should handle error when loading featured projects fails', () => {
    mockProjectService.getAll.and.returnValue(throwError(() => new Error('Network error')));
    fixture.detectChanges();

    expect(component.projectsError).toBe('Unable to load projects');
    expect(component.isLoadingProjects).toBeFalse();
  });

  it('should display error message when error occurs', () => {
    fixture.detectChanges(); // Let ngOnInit complete first
    component.projectsError = 'Test error';
    component.isLoadingProjects = false;
    fixture.detectChanges(); // Render with error state

    const compiled = fixture.nativeElement as HTMLElement;
    const alert = compiled.querySelector('.alert-warning');
    expect(alert).toBeTruthy();
    expect(alert?.textContent).toContain('Test error');
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
    fixture.detectChanges(); // Let ngOnInit complete first
    component.allProjects = [...mockFeaturedProjects, { ...mockFeaturedProjects[0], id: 3, featured: false }];
    fixture.detectChanges(); // Render with updated projects

    expect(component.hasMoreProjects).toBeTrue();
    const compiled = fixture.nativeElement as HTMLElement;
    const toggleButton = compiled.querySelector('button.btn-outline-primary');
    expect(toggleButton).toBeTruthy();
  });

  it('should display contact section', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const contactSection = compiled.querySelector('.contact-section');
    expect(contactSection).toBeTruthy();
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
