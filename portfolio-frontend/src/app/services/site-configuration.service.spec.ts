import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SiteConfigurationService } from './site-configuration.service';
import {
  SiteConfigurationResponse,
  UpdateSiteConfigurationRequest,
} from '../models/site-configuration.model';
import { provideHttpClient } from '@angular/common/http';

describe('SiteConfigurationService', () => {
  let service: SiteConfigurationService;
  let httpMock: HttpTestingController;

  const mockConfig: SiteConfigurationResponse = {
    id: 1,
    fullName: 'Emmanuel Gabe',
    email: 'contact@emmanuelgabe.com',
    heroTitle: 'Developpeur Backend',
    heroDescription: 'Je cree des applications web modernes et evolutives.',
    siteTitle: 'Portfolio - Emmanuel Gabe',
    seoDescription: 'Portfolio de Emmanuel Gabe, developpeur backend Java/Spring Boot.',
    profileImageUrl: '/uploads/profile/profile.webp',
    githubUrl: 'https://github.com/emmanuelgabe',
    linkedinUrl: 'https://linkedin.com/in/egabe',
    createdAt: '2025-01-01T00:00:00',
    updatedAt: '2025-01-01T00:00:00',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SiteConfigurationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getSiteConfiguration', () => {
    it('should return site configuration', () => {
      service.getSiteConfiguration().subscribe((config) => {
        expect(config).toEqual(mockConfig);
        expect(config.fullName).toBe('Emmanuel Gabe');
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/configuration') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockConfig);
    });
  });

  describe('updateSiteConfiguration', () => {
    it('should update site configuration', () => {
      const updateRequest: UpdateSiteConfigurationRequest = {
        fullName: 'Updated Name',
        email: 'updated@example.com',
        heroTitle: 'Updated Title',
        heroDescription: 'Updated Description',
        siteTitle: 'Updated Site Title',
        seoDescription: 'Updated SEO Description',
        githubUrl: 'https://github.com/updated',
        linkedinUrl: 'https://linkedin.com/in/updated',
      };

      service.updateSiteConfiguration(updateRequest).subscribe((config) => {
        expect(config).toEqual(mockConfig);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/configuration') && request.method === 'PUT'
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateRequest);
      req.flush(mockConfig);
    });
  });

  describe('uploadProfileImage', () => {
    it('should upload profile image', () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

      service.uploadProfileImage(file).subscribe((config) => {
        expect(config).toEqual(mockConfig);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url.includes('/api/admin/configuration/profile-image') &&
          request.method === 'POST'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBe(true);
      req.flush(mockConfig);
    });
  });

  describe('deleteProfileImage', () => {
    it('should delete profile image', () => {
      service.deleteProfileImage().subscribe((config) => {
        expect(config).toEqual(mockConfig);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url.includes('/api/admin/configuration/profile-image') &&
          request.method === 'DELETE'
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(mockConfig);
    });
  });
});
