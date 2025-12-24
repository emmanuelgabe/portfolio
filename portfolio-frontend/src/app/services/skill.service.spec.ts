import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SkillService } from './skill.service';
import { LoggerService } from './logger.service';
import {
  Skill,
  CreateSkillRequest,
  UpdateSkillRequest,
  SkillCategory,
  IconType,
} from '../models/skill.model';
import { environment } from '../../environments/environment';

describe('SkillService', () => {
  let service: SkillService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockSkill: Skill = {
    id: 1,
    name: 'Angular',
    icon: 'angular',
    iconType: IconType.FONT_AWESOME,
    color: '#dd0031',
    category: SkillCategory.FRONTEND,
    categoryDisplayName: 'Frontend',
    displayOrder: 1,
    createdAt: '2024-01-01T12:00:00',
    updatedAt: '2024-01-01T12:00:00',
  };

  const mockSkillsArray: Skill[] = [
    mockSkill,
    {
      id: 2,
      name: 'TypeScript',
      icon: 'typescript',
      iconType: IconType.FONT_AWESOME,
      color: '#3178c6',
      category: SkillCategory.FRONTEND,
      categoryDisplayName: 'Frontend',
      displayOrder: 2,
      createdAt: '2024-01-01T12:00:00',
      updatedAt: '2024-01-01T12:00:00',
    },
  ];

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'warn', 'debug']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        SkillService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(SkillService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ========== getAll Tests ==========

  it('should_returnAllSkills_when_getAllCalled', () => {
    // Arrange / Act
    service.getAll().subscribe((skills) => {
      // Assert
      expect(skills).toEqual(mockSkillsArray);
      expect(skills.length).toBe(2);
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSkillsArray);
  });

  it('should_logDebug_when_getAllCalled', () => {
    // Arrange / Act
    service.getAll().subscribe();

    // Assert
    expect(loggerSpy.debug).toHaveBeenCalledWith('[HTTP_REQUEST] Fetching all skills');

    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills`);
    req.flush(mockSkillsArray);
  });

  it('should_logSuccess_when_getAllSucceeds', () => {
    // Arrange / Act
    service.getAll().subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills`);
    req.flush(mockSkillsArray);

    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Skills fetched', { count: 2 });
  });

  it('should_logError_when_getAllFails', () => {
    // Arrange / Act
    service.getAll().subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(500);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Failed to fetch skills',
          jasmine.objectContaining({ status: 500 })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills`);
    req.flush({ message: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });
  });

  // ========== getById Tests ==========

  it('should_returnSkill_when_getByIdCalledWithValidId', () => {
    // Arrange
    const skillId = 1;

    // Act
    service.getById(skillId).subscribe((skill) => {
      // Assert
      expect(skill).toEqual(mockSkill);
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills/${skillId}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSkill);
  });

  it('should_logDebug_when_getByIdCalled', () => {
    // Arrange
    const skillId = 1;

    // Act
    service.getById(skillId).subscribe();

    // Assert
    expect(loggerSpy.debug).toHaveBeenCalledWith('[HTTP_REQUEST] Fetching skill', { id: 1 });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills/${skillId}`);
    req.flush(mockSkill);
  });

  it('should_logError_when_getByIdFails', () => {
    // Arrange
    const skillId = 999;

    // Act
    service.getById(skillId).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(404);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Failed to fetch skill',
          jasmine.objectContaining({ id: 999, status: 404 })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills/${skillId}`);
    req.flush({ message: 'Skill not found' }, { status: 404, statusText: 'Not Found' });
  });

  // ========== getByCategory Tests ==========

  it('should_returnSkillsByCategory_when_getByCategoryCalled', () => {
    // Arrange
    const category = SkillCategory.FRONTEND;

    // Act
    service.getByCategory(category).subscribe((skills) => {
      // Assert
      expect(skills).toEqual(mockSkillsArray);
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills/category/${category}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSkillsArray);
  });

  it('should_logDebug_when_getByCategoryCalled', () => {
    // Arrange
    const category = SkillCategory.BACKEND;

    // Act
    service.getByCategory(category).subscribe();

    // Assert
    expect(loggerSpy.debug).toHaveBeenCalledWith('[HTTP_REQUEST] Fetching skills by category', {
      category: 'BACKEND',
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills/category/${category}`);
    req.flush([]);
  });

  it('should_logSuccess_when_getByCategorySucceeds', () => {
    // Arrange
    const category = SkillCategory.FRONTEND;

    // Act
    service.getByCategory(category).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills/category/${category}`);
    req.flush(mockSkillsArray);

    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Skills by category fetched', {
      category: 'FRONTEND',
      count: 2,
    });
  });

  it('should_logError_when_getByCategoryFails', () => {
    // Arrange
    const category = SkillCategory.DATABASE;

    // Act
    service.getByCategory(category).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(500);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Failed to fetch skills by category',
          jasmine.objectContaining({ category: 'DATABASE' })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/skills/category/${category}`);
    req.flush({ message: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });
  });

  // ========== create Tests ==========

  it('should_createSkill_when_createCalledWithValidRequest', () => {
    // Arrange
    const request: CreateSkillRequest = {
      name: 'React',
      icon: 'react',
      iconType: IconType.FONT_AWESOME,
      color: '#61dafb',
      category: SkillCategory.FRONTEND,
      displayOrder: 3,
    };

    // Act
    service.create(request).subscribe((skill) => {
      // Assert
      expect(skill.name).toBe('React');
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ ...mockSkill, id: 3, name: 'React' });
  });

  it('should_logRequest_when_createCalled', () => {
    // Arrange
    const request: CreateSkillRequest = {
      name: 'Vue',
      icon: 'vuejs',
      iconType: IconType.FONT_AWESOME,
      color: '#42b883',
      category: SkillCategory.FRONTEND,
      displayOrder: 4,
    };

    // Act
    service.create(request).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_REQUEST] Creating skill', { name: 'Vue' });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills`);
    req.flush({ ...mockSkill, name: 'Vue' });
  });

  it('should_logSuccess_when_createSucceeds', () => {
    // Arrange
    const request: CreateSkillRequest = {
      name: 'Node.js',
      icon: 'node',
      iconType: IconType.FONT_AWESOME,
      color: '#339933',
      category: SkillCategory.BACKEND,
      displayOrder: 1,
    };

    // Act
    service.create(request).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills`);
    req.flush({ ...mockSkill, id: 5, name: 'Node.js' });

    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Skill created', {
      id: 5,
      name: 'Node.js',
    });
  });

  it('should_logError_when_createFails', () => {
    // Arrange
    const request: CreateSkillRequest = {
      name: 'Invalid',
      icon: '',
      iconType: IconType.FONT_AWESOME,
      color: '#000',
      category: SkillCategory.FRONTEND,
      displayOrder: 0,
    };

    // Act
    service.create(request).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(400);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Failed to create skill',
          jasmine.objectContaining({ name: 'Invalid', status: 400 })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills`);
    req.flush({ message: 'Validation failed' }, { status: 400, statusText: 'Bad Request' });
  });

  // ========== update Tests ==========

  it('should_updateSkill_when_updateCalledWithValidRequest', () => {
    // Arrange
    const skillId = 1;
    const request: UpdateSkillRequest = {
      name: 'Angular Updated',
      color: '#c3002f',
    };

    // Act
    service.update(skillId, request).subscribe((skill) => {
      // Assert
      expect(skill.name).toBe('Angular Updated');
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush({ ...mockSkill, name: 'Angular Updated' });
  });

  it('should_logRequest_when_updateCalled', () => {
    // Arrange
    const skillId = 1;
    const request: UpdateSkillRequest = { name: 'Updated Name' };

    // Act
    service.update(skillId, request).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_REQUEST] Updating skill', { id: 1 });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}`);
    req.flush(mockSkill);
  });

  it('should_logSuccess_when_updateSucceeds', () => {
    // Arrange
    const skillId = 1;
    const request: UpdateSkillRequest = { displayOrder: 5 };

    // Act
    service.update(skillId, request).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}`);
    req.flush(mockSkill);

    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Skill updated', {
      id: 1,
      name: 'Angular',
    });
  });

  it('should_logError_when_updateFails', () => {
    // Arrange
    const skillId = 999;
    const request: UpdateSkillRequest = { name: 'Test' };

    // Act
    service.update(skillId, request).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(404);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Failed to update skill',
          jasmine.objectContaining({ id: 999, status: 404 })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}`);
    req.flush({ message: 'Skill not found' }, { status: 404, statusText: 'Not Found' });
  });

  // ========== delete Tests ==========

  it('should_deleteSkill_when_deleteCalledWithValidId', () => {
    // Arrange
    const skillId = 1;

    // Act
    service.delete(skillId).subscribe(() => {
      // Assert
      expect(true).toBeTrue();
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should_logRequest_when_deleteCalled', () => {
    // Arrange
    const skillId = 1;

    // Act
    service.delete(skillId).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_REQUEST] Deleting skill', { id: 1 });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}`);
    req.flush(null);
  });

  it('should_logSuccess_when_deleteSucceeds', () => {
    // Arrange
    const skillId = 1;

    // Act
    service.delete(skillId).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}`);
    req.flush(null);

    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Skill deleted', { id: 1 });
  });

  it('should_logError_when_deleteFails', () => {
    // Arrange
    const skillId = 999;

    // Act
    service.delete(skillId).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(404);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Failed to delete skill',
          jasmine.objectContaining({ id: 999, status: 404 })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}`);
    req.flush({ message: 'Skill not found' }, { status: 404, statusText: 'Not Found' });
  });

  // ========== uploadIcon Tests ==========

  it('should_uploadIcon_when_uploadIconCalledWithValidFile', () => {
    // Arrange
    const skillId = 1;
    const mockFile = new File(['<svg></svg>'], 'icon.svg', { type: 'image/svg+xml' });

    // Act
    service.uploadIcon(skillId, mockFile).subscribe((skill) => {
      // Assert
      expect(skill.customIconUrl).toBe('/uploads/skills/icon.svg');
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}/icon`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush({ ...mockSkill, customIconUrl: '/uploads/skills/icon.svg' });
  });

  it('should_logRequest_when_uploadIconCalled', () => {
    // Arrange
    const skillId = 1;
    const mockFile = new File(['<svg></svg>'], 'custom-icon.svg', { type: 'image/svg+xml' });

    // Act
    service.uploadIcon(skillId, mockFile).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_REQUEST] Uploading skill icon', {
      id: 1,
      fileName: 'custom-icon.svg',
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}/icon`);
    req.flush({ ...mockSkill, customIconUrl: '/uploads/skills/custom-icon.svg' });
  });

  it('should_logSuccess_when_uploadIconSucceeds', () => {
    // Arrange
    const skillId = 1;
    const mockFile = new File(['<svg></svg>'], 'icon.svg', { type: 'image/svg+xml' });
    const customIconUrl = '/uploads/skills/icon.svg';

    // Act
    service.uploadIcon(skillId, mockFile).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}/icon`);
    req.flush({ ...mockSkill, customIconUrl });

    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Skill icon uploaded', {
      id: 1,
      customIconUrl,
    });
  });

  it('should_logError_when_uploadIconFails', () => {
    // Arrange
    const skillId = 1;
    const mockFile = new File(['invalid'], 'icon.txt', { type: 'text/plain' });

    // Act
    service.uploadIcon(skillId, mockFile).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(400);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Failed to upload skill icon',
          jasmine.objectContaining({ id: 1, status: 400 })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}/icon`);
    req.flush({ message: 'Invalid file type' }, { status: 400, statusText: 'Bad Request' });
  });

  it('should_includeFileInFormData_when_uploadIconCalled', () => {
    // Arrange
    const skillId = 1;
    const mockFile = new File(['<svg></svg>'], 'icon.svg', { type: 'image/svg+xml' });

    // Act
    service.uploadIcon(skillId, mockFile).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/skills/${skillId}/icon`);
    const formData = req.request.body as FormData;
    expect(formData.has('file')).toBeTrue();
    req.flush(mockSkill);
  });
});
