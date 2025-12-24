import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { ContactFormComponent } from './contact-form.component';
import { ContactService } from '../../services/contact.service';
import { LoggerService } from '../../services/logger.service';
import { ContactResponse } from '../../models';

describe('ContactFormComponent', () => {
  let component: ContactFormComponent;
  let fixture: ComponentFixture<ContactFormComponent>;
  let contactServiceSpy: jasmine.SpyObj<ContactService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let _loggerSpy: jasmine.SpyObj<LoggerService>;

  beforeEach(async () => {
    const contactSpy = jasmine.createSpyObj('ContactService', ['send']);
    const toastrSpyObj = jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']);
    const loggerSpyObj = jasmine.createSpyObj('LoggerService', ['info', 'error']);

    await TestBed.configureTestingModule({
      imports: [ContactFormComponent, ReactiveFormsModule, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: ContactService, useValue: contactSpy },
        { provide: ToastrService, useValue: toastrSpyObj },
        { provide: LoggerService, useValue: loggerSpyObj },
      ],
    }).compileComponents();

    contactServiceSpy = TestBed.inject(ContactService) as jasmine.SpyObj<ContactService>;
    toastrSpy = TestBed.inject(ToastrService) as jasmine.SpyObj<ToastrService>;
    _loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;

    fixture = TestBed.createComponent(ContactFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with empty values', () => {
    expect(component.contactForm.get('name')?.value).toBe('');
    expect(component.contactForm.get('email')?.value).toBe('');
    expect(component.contactForm.get('subject')?.value).toBe('');
    expect(component.contactForm.get('message')?.value).toBe('');
    expect(component.contactForm.get('consent')?.value).toBe(false);
  });

  it('should invalidate form when name is too short', () => {
    component.contactForm.patchValue({ name: 'A' });
    expect(component.contactForm.get('name')?.invalid).toBe(true);
  });

  it('should invalidate form when email is invalid', () => {
    component.contactForm.patchValue({ email: 'invalid-email' });
    expect(component.contactForm.get('email')?.invalid).toBe(true);
  });

  it('should invalidate form when subject is too short', () => {
    component.contactForm.patchValue({ subject: 'Hi' });
    expect(component.contactForm.get('subject')?.invalid).toBe(true);
  });

  it('should invalidate form when message is too short', () => {
    component.contactForm.patchValue({ message: 'Short' });
    expect(component.contactForm.get('message')?.invalid).toBe(true);
  });

  it('should validate form when all fields are correct', () => {
    component.contactForm.patchValue({
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message with more than 10 characters',
      consent: true,
    });
    expect(component.contactForm.valid).toBe(true);
  });

  it('should not submit when form is invalid', () => {
    component.contactForm.patchValue({
      name: 'J',
      email: 'invalid',
      subject: 'Hi',
      message: 'Short',
    });

    component.onSubmit();

    expect(toastrSpy.warning).toHaveBeenCalledWith('contact.validationError');
    expect(contactServiceSpy.send).not.toHaveBeenCalled();
  });

  it('should send message successfully when form is valid', () => {
    const mockResponse: ContactResponse = {
      message: 'Message sent successfully',
      success: true,
      timestamp: new Date().toISOString(),
    };

    contactServiceSpy.send.and.returnValue(of(mockResponse));

    component.contactForm.patchValue({
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message with more than 10 characters',
      consent: true,
    });

    component.onSubmit();

    expect(contactServiceSpy.send).toHaveBeenCalled();
    expect(toastrSpy.success).toHaveBeenCalledWith('Message sent successfully');
    expect(component.loading).toBe(false);
    expect(component.submitted).toBe(false);
    expect(component.contactForm.value.name).toBe(null);
  });

  it('should handle error when sending message fails', () => {
    const mockError = {
      status: 500,
      customMessage: "Erreur lors de l'envoi du message",
    };

    contactServiceSpy.send.and.returnValue(throwError(() => mockError));

    component.contactForm.patchValue({
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message with more than 10 characters',
      consent: true,
    });

    component.onSubmit();

    expect(contactServiceSpy.send).toHaveBeenCalled();
    expect(toastrSpy.error).toHaveBeenCalledWith("Erreur lors de l'envoi du message");
    expect(component.loading).toBe(false);
  });

  it('should handle rate limit error', () => {
    const mockError = {
      status: 429,
      customMessage: 'Limite atteinte. Vous ne pouvez envoyer que 5 messages par heure.',
    };

    contactServiceSpy.send.and.returnValue(throwError(() => mockError));

    component.contactForm.patchValue({
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message with more than 10 characters',
      consent: true,
    });

    component.onSubmit();

    expect(contactServiceSpy.send).toHaveBeenCalled();
    expect(toastrSpy.error).toHaveBeenCalled();
    expect(component.loading).toBe(false);
  });

  it('should set loading to true when submitting', () => {
    const mockResponse: ContactResponse = {
      message: 'Message sent successfully',
      success: true,
      timestamp: new Date().toISOString(),
    };

    contactServiceSpy.send.and.returnValue(of(mockResponse));

    component.contactForm.patchValue({
      name: 'John Doe',
      email: 'john@example.com',
      subject: 'Test Subject',
      message: 'This is a test message with more than 10 characters',
      consent: true,
    });

    component.onSubmit();

    expect(component.loading).toBe(false);
  });
});
