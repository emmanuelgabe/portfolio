import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectCardComponent } from './project-card.component';
import { ProjectResponse } from '../../models';

describe('ProjectCardComponent', () => {
  let component: ProjectCardComponent;
  let fixture: ComponentFixture<ProjectCardComponent>;
  let mockProject: ProjectResponse;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectCardComponent);
    component = fixture.componentInstance;

    mockProject = {
      id: 1,
      title: 'Test Project',
      description: 'This is a test project description that should be displayed on the card.',
      techStack: 'Angular, TypeScript, Bootstrap',
      githubUrl: 'https://github.com/test/project',
      imageUrl: 'https://example.com/image.png',
      demoUrl: 'https://demo.example.com',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      featured: true,
      tags: [
        { id: 1, name: 'Angular', color: '#dd0031' },
        { id: 2, name: 'TypeScript', color: '#3178c6' },
      ],
    };

    component.project = mockProject;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display project title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const title = compiled.querySelector('.card-title');
    expect(title?.textContent).toContain('Test Project');
  });

  it('should truncate long descriptions', () => {
    const longDescription = 'a'.repeat(200);
    const truncated = component.truncateDescription(longDescription, 150);
    expect(truncated.length).toBeLessThanOrEqual(153); // 150 + '...'
    expect(truncated.endsWith('...')).toBeTruthy();
  });

  it('should not truncate short descriptions', () => {
    const shortDescription = 'Short description';
    const result = component.truncateDescription(shortDescription, 150);
    expect(result).toBe(shortDescription);
  });

  it('should detect GitHub URL', () => {
    expect(component.hasGithubUrl).toBeTruthy();
    component.project.githubUrl = undefined;
    expect(component.hasGithubUrl).toBeFalsy();
  });

  it('should detect demo URL', () => {
    expect(component.hasDemoUrl).toBeTruthy();
    component.project.demoUrl = undefined;
    expect(component.hasDemoUrl).toBeFalsy();
  });

  it('should detect image', () => {
    expect(component.hasImage).toBeTruthy();
    component.project.imageUrl = undefined;
    expect(component.hasImage).toBeFalsy();
  });

  it('should return placeholder image when no image is available', () => {
    component.project.imageUrl = undefined;
    expect(component.imageSrc).toBe('assets/images/project-placeholder.png');
  });

  it('should return project image when available', () => {
    expect(component.imageSrc).toBe('https://example.com/image.png');
  });

  it('should display tags', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const tags = compiled.querySelectorAll('.badge');
    expect(tags.length).toBe(2);
    expect(tags[0].textContent?.trim()).toBe('Angular');
    expect(tags[1].textContent?.trim()).toBe('TypeScript');
  });

  it('should display featured badge when project is featured', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const featuredBadge = compiled.querySelector('.badge.bg-warning');
    expect(featuredBadge).toBeTruthy();
    expect(featuredBadge?.textContent).toContain('Featured');
  });

  it('should not display featured badge when project is not featured', () => {
    component.project.featured = false;
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const featuredBadge = compiled.querySelector('.badge.bg-warning');
    expect(featuredBadge).toBeFalsy();
  });

  it('should display GitHub button when GitHub URL is available', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const githubButton = compiled.querySelector('a[title="View on GitHub"]');
    expect(githubButton).toBeTruthy();
  });

  it('should display demo button when demo URL is available', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const demoButton = compiled.querySelector('a[title="View Demo"]');
    expect(demoButton).toBeTruthy();
  });
});
