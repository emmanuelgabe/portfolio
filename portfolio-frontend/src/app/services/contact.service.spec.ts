import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ContactService } from './contact.service';
import { LoggerService } from './logger.service';
import { environment } from '../../environments/environment';
import { ContactRequest, ContactResponse } from '../models';

describe('ContactService', () => {
  let service: ContactService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    const loggerSpyObj = jasmine.createSpyObj('LoggerService', ['info', 'error']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ContactService, { provide: LoggerService, useValue: loggerSpyObj }],
    });

    service = TestBed.inject(ContactService);
    httpMock = TestBed.inject(HttpTestingController);
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send contact message successfully', (done) => {
    const mockRequest: ContactRequest = {
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message',
    };

    const mockResponse: ContactResponse = {
      message: 'Message sent successfully',
      success: true,
      timestamp: new Date().toISOString(),
    };

    service.send(mockRequest).subscribe({
      next: (response) => {
        expect(response).toEqual(mockResponse);
        expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP] POST /api/contact', {
          name: mockRequest.name,
          email: mockRequest.email,
          subject: mockRequest.subject,
        });
        done();
      },
      error: () => fail('should not have failed'),
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/contact`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockRequest);
    req.flush(mockResponse);
  });

  it('should handle 429 rate limit error', (done) => {
    const mockRequest: ContactRequest = {
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message',
    };

    service.send(mockRequest).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.customMessage).toContain('Limite atteinte');
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Failed to send message',
          jasmine.objectContaining({
            status: 429,
          })
        );
        done();
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/contact`);
    req.flush(null, { status: 429, statusText: 'Too Many Requests' });
  });

  it('should handle 400 bad request error', (done) => {
    const mockRequest: ContactRequest = {
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message',
    };

    service.send(mockRequest).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.customMessage).toContain('Données invalides');
        expect(loggerSpy.error).toHaveBeenCalled();
        done();
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/contact`);
    req.flush(null, { status: 400, statusText: 'Bad Request' });
  });

  it('should handle 500 server error', (done) => {
    const mockRequest: ContactRequest = {
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message',
    };

    service.send(mockRequest).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.customMessage).toContain("Erreur lors de l'envoi");
        expect(loggerSpy.error).toHaveBeenCalled();
        done();
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/contact`);
    req.flush(null, { status: 500, statusText: 'Internal Server Error' });
  });

  it('should handle network error', (done) => {
    const mockRequest: ContactRequest = {
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message',
    };

    service.send(mockRequest).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.customMessage).toContain('Impossible de contacter le serveur');
        expect(loggerSpy.error).toHaveBeenCalled();
        done();
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/contact`);
    req.flush(null, { status: 0, statusText: 'Network Error' });
  });
});
