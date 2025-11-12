import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home.component';
import { ProjectService } from '../../services/project.service';
import { ProjectResponse } from '../../models';
import { of, throwError } from 'rxjs';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let mockProjectService: jasmine.SpyObj<ProjectService>;
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
        tags: [{ id: 1, name: 'Angular', color: '#dd0031' }]
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
        tags: [{ id: 2, name: 'Spring Boot', color: '#6db33f' }]
      }
    ];

    mockProjectService = jasmine.createSpyObj('ProjectService', ['getFeatured']);
    mockProjectService.getFeatured.and.returnValue(of(mockFeaturedProjects));

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: ProjectService, useValue: mockProjectService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load featured projects on init', () => {
    fixture.detectChanges();
    expect(mockProjectService.getFeatured).toHaveBeenCalled();
    expect(component.featuredProjects.length).toBe(2);
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
    expect(title?.textContent).toContain('Your Name');
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
    component.isLoadingProjects = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const spinner = compiled.querySelector('.spinner-border');
    expect(spinner).toBeTruthy();
  });

  it('should handle error when loading featured projects fails', () => {
    mockProjectService.getFeatured.and.returnValue(throwError(() => new Error('Network error')));
    fixture.detectChanges();

    expect(component.projectsError).toBe('Unable to load featured projects');
    expect(component.isLoadingProjects).toBeFalse();
  });

  it('should display error message when error occurs', () => {
    component.projectsError = 'Test error';
    component.isLoadingProjects = false;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const alert = compiled.querySelector('.alert-warning');
    expect(alert).toBeTruthy();
    expect(alert?.textContent).toContain('Test error');
  });

  it('should display no featured projects message when empty', () => {
    mockProjectService.getFeatured.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.hasFeaturedProjects).toBeFalse();

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

  it('should display "View All Projects" button when there are featured projects', () => {
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const viewAllButton = compiled.querySelector('a[routerLink="/projects"]');
    expect(viewAllButton).toBeTruthy();
  });

  it('should display CTA section', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const ctaSection = compiled.querySelector('.cta-section');
    expect(ctaSection).toBeTruthy();
  });

  it('should return true for hasFeaturedProjects when projects exist', () => {
    component.featuredProjects = mockFeaturedProjects;
    expect(component.hasFeaturedProjects).toBeTrue();
  });

  it('should return false for hasFeaturedProjects when no projects', () => {
    component.featuredProjects = [];
    expect(component.hasFeaturedProjects).toBeFalse();
  });

  it('should have correct skill colors', () => {
    const angularSkill = component.skills.find(s => s.name === 'Angular');
    expect(angularSkill?.color).toBe('#dd0031');

    const springBootSkill = component.skills.find(s => s.name === 'Spring Boot');
    expect(springBootSkill?.color).toBe('#6db33f');
  });

  it('should have correct skill icons', () => {
    const angularSkill = component.skills.find(s => s.name === 'Angular');
    expect(angularSkill?.icon).toBe('bi-code-square');

    const dockerSkill = component.skills.find(s => s.name === 'Docker');
    expect(dockerSkill?.icon).toBe('bi-box');
  });
});
