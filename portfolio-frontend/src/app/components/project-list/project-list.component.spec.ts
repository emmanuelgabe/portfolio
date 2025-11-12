import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectListComponent } from './project-list.component';
import { ProjectService } from '../../services/project.service';
import { ProjectResponse } from '../../models';
import { of, throwError } from 'rxjs';

describe('ProjectListComponent', () => {
  let component: ProjectListComponent;
  let fixture: ComponentFixture<ProjectListComponent>;
  let mockProjectService: jasmine.SpyObj<ProjectService>;
  let mockProjects: ProjectResponse[];

  beforeEach(async () => {
    mockProjects = [
      {
        id: 1,
        title: 'Project 1',
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
        title: 'Project 2',
        description: 'Description 2',
        techStack: 'React, JavaScript',
        githubUrl: 'https://github.com/test/project2',
        imageUrl: 'https://example.com/image2.png',
        createdAt: '2024-01-02T00:00:00Z',
        updatedAt: '2024-01-02T00:00:00Z',
        featured: false,
        tags: [{ id: 2, name: 'React', color: '#61dafb' }]
      }
    ];

    mockProjectService = jasmine.createSpyObj('ProjectService', ['getAll']);
    mockProjectService.getAll.and.returnValue(of(mockProjects));

    await TestBed.configureTestingModule({
      imports: [ProjectListComponent],
      providers: [
        { provide: ProjectService, useValue: mockProjectService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load projects on init', () => {
    fixture.detectChanges();
    expect(mockProjectService.getAll).toHaveBeenCalled();
    expect(component.projects.length).toBe(2);
    expect(component.isLoading).toBeFalse();
    expect(component.error).toBeNull();
  });

  it('should display loading spinner while loading', () => {
    component.isLoading = true;
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const spinner = compiled.querySelector('.spinner-border');
    expect(spinner).toBeTruthy();
  });

  it('should display error message on error', () => {
    mockProjectService.getAll.and.returnValue(throwError(() => new Error('Network error')));
    fixture.detectChanges();

    expect(component.error).toBe('Failed to load projects. Please try again later.');
    expect(component.isLoading).toBeFalse();
  });

  it('should display error alert when error occurs', () => {
    component.error = 'Test error message';
    component.isLoading = false;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const alert = compiled.querySelector('.alert-danger');
    expect(alert).toBeTruthy();
    expect(alert?.textContent).toContain('Test error message');
  });

  it('should retry loading when retry button is clicked', () => {
    component.error = 'Test error';
    component.isLoading = false;
    fixture.detectChanges();

    mockProjectService.getAll.and.returnValue(of(mockProjects));

    const compiled = fixture.nativeElement as HTMLElement;
    const retryButton = compiled.querySelector('.btn-outline-danger') as HTMLButtonElement;
    retryButton.click();

    expect(mockProjectService.getAll).toHaveBeenCalled();
  });

  it('should display no projects message when projects array is empty', () => {
    mockProjectService.getAll.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.hasNoProjects).toBeTrue();

    const compiled = fixture.nativeElement as HTMLElement;
    const noProjectsMessage = compiled.querySelector('.bi-inbox');
    expect(noProjectsMessage).toBeTruthy();
  });

  it('should display projects in grid when loaded', () => {
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const projectCards = compiled.querySelectorAll('app-project-card');
    expect(projectCards.length).toBe(2);
  });

  it('should use responsive grid classes', () => {
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const gridColumns = compiled.querySelectorAll('.col-12.col-md-6.col-lg-4');
    expect(gridColumns.length).toBe(2);
  });

  it('should close error alert when close button is clicked', () => {
    component.error = 'Test error';
    component.isLoading = false;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const closeButton = compiled.querySelector('.btn-close') as HTMLButtonElement;
    closeButton.click();

    expect(component.error).toBeNull();
  });

  it('should return false for hasNoProjects when loading', () => {
    component.isLoading = true;
    component.projects = [];
    expect(component.hasNoProjects).toBeFalse();
  });

  it('should return false for hasNoProjects when error exists', () => {
    component.isLoading = false;
    component.error = 'Error';
    component.projects = [];
    expect(component.hasNoProjects).toBeFalse();
  });

  it('should return true for hasNoProjects when no error, not loading, and empty array', () => {
    component.isLoading = false;
    component.error = null;
    component.projects = [];
    expect(component.hasNoProjects).toBeTrue();
  });
});
