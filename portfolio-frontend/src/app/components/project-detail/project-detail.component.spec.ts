import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { provideToastr } from 'ngx-toastr';
import { TranslateModule } from '@ngx-translate/core';
import { ProjectDetailComponent } from './project-detail.component';
import { ProjectService } from '../../services/project.service';
import { LoggerService } from '../../services/logger.service';
import { ProjectResponse } from '../../models';
import { of, throwError } from 'rxjs';

describe('ProjectDetailComponent', () => {
  let component: ProjectDetailComponent;
  let fixture: ComponentFixture<ProjectDetailComponent>;
  let mockProjectService: jasmine.SpyObj<ProjectService>;
  let mockLoggerService: jasmine.SpyObj<LoggerService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockActivatedRoute: { snapshot: { paramMap: { get: jasmine.Spy } } };
  let mockProject: ProjectResponse;

  beforeEach(async () => {
    mockProject = {
      id: 1,
      title: 'Test Project',
      description:
        'This is a detailed test project description that explains what the project does.',
      techStack: 'Angular, TypeScript, Bootstrap',
      githubUrl: 'https://github.com/test/project',
      imageUrl: 'https://example.com/image.png',
      demoUrl: 'https://demo.example.com',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-15T00:00:00Z',
      featured: true,
      hasDetails: true,
      displayOrder: 0,
      tags: [
        { id: 1, name: 'Angular', color: '#dd0031' },
        { id: 2, name: 'TypeScript', color: '#3178c6' },
      ],
      images: [],
    };

    mockProjectService = jasmine.createSpyObj('ProjectService', ['getById']);
    mockProjectService.getById.and.returnValue(of(mockProject));
    mockLoggerService = jasmine.createSpyObj('LoggerService', ['info', 'warn', 'error', 'debug']);

    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    mockActivatedRoute = {
      snapshot: {
        paramMap: {
          get: jasmine.createSpy('get').and.returnValue('1'),
        },
      },
    };

    await TestBed.configureTestingModule({
      imports: [ProjectDetailComponent, TranslateModule.forRoot()],
      providers: [
        { provide: ProjectService, useValue: mockProjectService },
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        provideToastr(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load project on init', () => {
    fixture.detectChanges();
    expect(mockProjectService.getById).toHaveBeenCalledWith(1);
    expect(component.project).toEqual(mockProject);
    expect(component.isLoading).toBeFalse();
    expect(component.error).toBeUndefined();
  });

  it('should display loading skeleton while loading', () => {
    fixture.detectChanges(); // Let ngOnInit complete first
    component.isLoading = true;
    fixture.detectChanges(); // Now render with isLoading = true
    const compiled = fixture.nativeElement as HTMLElement;
    const skeleton = compiled.querySelector('app-skeleton-project-detail');
    expect(skeleton).toBeTruthy();
  });

  it('should handle error when loading project fails', () => {
    mockProjectService.getById.and.returnValue(throwError(() => new Error('Not found')));
    fixture.detectChanges();

    expect(component.error).toBe('errors.loadingFailed');
    expect(component.isLoading).toBeFalse();
  });

  it('should display error message when error occurs', () => {
    fixture.detectChanges(); // Let ngOnInit complete first
    component.error = 'Test error';
    component.isLoading = false;
    fixture.detectChanges(); // Now render with error state

    const compiled = fixture.nativeElement as HTMLElement;
    const alert = compiled.querySelector('.alert-danger');
    expect(alert).toBeTruthy();
    expect(alert?.textContent).toContain('Test error');
  });

  it('should navigate back to projects section on home page', () => {
    component.goBack();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/'], { fragment: 'projects' });
  });

  it('should retry loading project', () => {
    fixture.detectChanges();
    mockProjectService.getById.calls.reset();

    component.retry();
    expect(mockProjectService.getById).toHaveBeenCalledWith(1);
  });

  it('should display project title', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const title = compiled.querySelector('h1.display-4');
    expect(title?.textContent).toContain('Test Project');
  });

  it('should display project description', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const description = compiled.querySelector('.card-text');
    expect(description?.textContent).toContain('This is a detailed test project description');
  });

  it('should display tech stack as list', () => {
    fixture.detectChanges();
    expect(component.techStackArray).toEqual(['Angular', 'TypeScript', 'Bootstrap']);

    const compiled = fixture.nativeElement as HTMLElement;
    const techItems = compiled.querySelectorAll('.tech-stack-list li');
    expect(techItems.length).toBe(3);
  });

  it('should display tags', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const badges = compiled.querySelectorAll('.badges .badge');
    expect(badges.length).toBeGreaterThanOrEqual(2);
  });

  it('should display GitHub button when GitHub URL is available', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const githubButton = compiled.querySelector('a.btn-dark');
    expect(githubButton).toBeTruthy();
    expect(githubButton?.textContent).toContain('projects.viewOnGithub');
  });

  it('should display demo button when demo URL is available', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const demoButton = compiled.querySelector('a.btn-success');
    expect(demoButton).toBeTruthy();
    expect(demoButton?.textContent).toContain('projects.liveDemo');
  });

  it('should display project image when available', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const image = compiled.querySelector('.project-image-container img') as HTMLImageElement;
    expect(image).toBeTruthy();
    expect(image?.src).toContain('example.com/image.png');
  });

  it('should open Twitter share dialog', () => {
    spyOn(window, 'open');
    fixture.detectChanges();

    component.shareOnTwitter();
    expect(window.open).toHaveBeenCalled();
    const args = (window.open as jasmine.Spy).calls.mostRecent().args;
    expect(args[0]).toContain('twitter.com');
  });

  it('should open LinkedIn share dialog', () => {
    spyOn(window, 'open');
    fixture.detectChanges();

    component.shareOnLinkedIn();
    expect(window.open).toHaveBeenCalled();
    const args = (window.open as jasmine.Spy).calls.mostRecent().args;
    expect(args[0]).toContain('linkedin.com');
  });

  it('should copy link to clipboard', async () => {
    const mockClipboard = {
      writeText: jasmine.createSpy('writeText').and.returnValue(Promise.resolve()),
    };
    Object.defineProperty(navigator, 'clipboard', {
      value: mockClipboard,
      writable: true,
    });
    spyOn(window, 'alert');

    fixture.detectChanges();
    await component.copyLink();

    expect(mockClipboard.writeText).toHaveBeenCalled();
  });

  it('should handle invalid project ID', () => {
    mockActivatedRoute.snapshot.paramMap.get.and.returnValue(null);
    fixture.detectChanges();

    expect(component.error).toBe('Invalid project ID');
    expect(component.isLoading).toBeFalse();
  });
});
