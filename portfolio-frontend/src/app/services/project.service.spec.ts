import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProjectService } from './project.service';
import { ProjectResponse, CreateProjectRequest, UpdateProjectRequest } from '../models';
import { provideHttpClient } from '@angular/common/http';

describe('ProjectService', () => {
  let service: ProjectService;
  let httpMock: HttpTestingController;

  const mockProject: ProjectResponse = {
    id: 1,
    title: 'Test Project',
    description: 'Test Description',
    techStack: 'Java, Spring Boot',
    githubUrl: 'https://github.com/test',
    imageUrl: 'https://example.com/image.jpg',
    demoUrl: 'https://example.com/demo',
    createdAt: '2025-01-01T00:00:00',
    updatedAt: '2025-01-01T00:00:00',
    featured: true,
    hasDetails: true,
    tags: [],
    images: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAll', () => {
    it('should return all projects', () => {
      const mockProjects: ProjectResponse[] = [mockProject];

      service.getAll().subscribe((projects) => {
        expect(projects).toEqual(mockProjects);
        expect(projects.length).toBe(1);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/projects') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProjects);
    });
  });

  describe('getById', () => {
    it('should return a project by id', () => {
      service.getById(1).subscribe((project) => {
        expect(project).toEqual(mockProject);
        expect(project.id).toBe(1);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/projects/1') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProject);
    });
  });

  describe('create', () => {
    it('should create a new project', () => {
      const createRequest: CreateProjectRequest = {
        title: 'New Project',
        description: 'New Description',
        techStack: 'Java, Spring',
        featured: false,
      };

      service.create(createRequest).subscribe((project) => {
        expect(project).toEqual(mockProject);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/projects') && request.method === 'POST'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      req.flush(mockProject);
    });
  });

  describe('update', () => {
    it('should update an existing project', () => {
      const updateRequest: UpdateProjectRequest = {
        title: 'Updated Project',
      };

      service.update(1, updateRequest).subscribe((project) => {
        expect(project).toEqual(mockProject);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/projects/1') && request.method === 'PUT'
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateRequest);
      req.flush(mockProject);
    });
  });

  describe('delete', () => {
    it('should delete a project', () => {
      service.delete(1).subscribe((response) => {
        expect(response).toBeNull();
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/projects/1') && request.method === 'DELETE'
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('getFeatured', () => {
    it('should return featured projects', () => {
      const mockProjects: ProjectResponse[] = [mockProject];

      service.getFeatured().subscribe((projects) => {
        expect(projects).toEqual(mockProjects);
        expect(projects[0].featured).toBe(true);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/projects/featured') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProjects);
    });
  });

  describe('searchByTitle', () => {
    it('should search projects by title', () => {
      const mockProjects: ProjectResponse[] = [mockProject];
      const searchTitle = 'Test';

      service.searchByTitle(searchTitle).subscribe((projects) => {
        expect(projects).toEqual(mockProjects);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/projects/search/title') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('title')).toBe(searchTitle);
      req.flush(mockProjects);
    });
  });

  describe('searchByTechnology', () => {
    it('should search projects by technology', () => {
      const mockProjects: ProjectResponse[] = [mockProject];
      const searchTech = 'Java';

      service.searchByTechnology(searchTech).subscribe((projects) => {
        expect(projects).toEqual(mockProjects);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url.includes('/api/projects/search/technology') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('technology')).toBe(searchTech);
      req.flush(mockProjects);
    });
  });
});
