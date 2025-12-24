import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideToastr } from 'ngx-toastr';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';
import { NavbarComponent } from './navbar';
import { AuthService } from '../../services/auth.service';
import { ArticleService } from '../../services/article.service';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { ScrollService } from '../../services/scroll.service';
import { SiteConfigurationResponse } from '../../models/site-configuration.model';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockArticleService: jasmine.SpyObj<ArticleService>;
  let mockSiteConfigService: jasmine.SpyObj<SiteConfigurationService>;
  let mockScrollService: jasmine.SpyObj<ScrollService>;

  const mockSiteConfig: SiteConfigurationResponse = {
    id: 1,
    fullName: 'Emmanuel Gabe',
    email: 'contact@emmanuelgabe.com',
    heroTitle: 'Welcome',
    heroDescription: 'Description',
    siteTitle: 'Portfolio',
    seoDescription: 'SEO Description',
    profileImageUrl: undefined,
    githubUrl: 'https://github.com/emmanuelgabe',
    linkedinUrl: 'https://linkedin.com/in/egabe',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj(
      'AuthService',
      ['logout', 'isAdmin', 'isAuthenticated'],
      {
        currentUser$: of(null),
      }
    );
    mockAuthService.isAdmin.and.returnValue(false);
    mockAuthService.isAuthenticated.and.returnValue(false);
    mockArticleService = jasmine.createSpyObj('ArticleService', ['getAll']);
    mockArticleService.getAll.and.returnValue(of([]));
    mockSiteConfigService = jasmine.createSpyObj('SiteConfigurationService', [
      'getSiteConfiguration',
    ]);
    mockSiteConfigService.getSiteConfiguration.and.returnValue(of(mockSiteConfig));
    mockScrollService = jasmine.createSpyObj('ScrollService', ['scrollToTop']);

    await TestBed.configureTestingModule({
      imports: [NavbarComponent, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        provideToastr(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: ArticleService, useValue: mockArticleService },
        { provide: SiteConfigurationService, useValue: mockSiteConfigService },
        { provide: ScrollService, useValue: mockScrollService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
