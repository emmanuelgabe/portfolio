import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, withInMemoryScrolling, Router } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { isDevMode, ErrorHandler, APP_INITIALIZER } from '@angular/core';
import { provideServiceWorker } from '@angular/service-worker';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import * as Sentry from '@sentry/angular';
import { Chart, registerables } from 'chart.js';

// Register Chart.js components
Chart.register(...registerables);
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { jwtInterceptor } from './app/interceptors/jwt.interceptor';
import { retryInterceptor } from './app/interceptors/retry.interceptor';
import { loggingInterceptor } from './app/interceptors/logging.interceptor';
import { environment } from './environments/environment';

// Initialize Sentry before bootstrapping
if (environment.sentry.enabled && environment.sentry.dsn) {
  Sentry.init({
    dsn: environment.sentry.dsn,
    environment: environment.production ? 'production' : 'development',
    release: environment.version,
    integrations: [Sentry.browserTracingIntegration()],
    tracesSampleRate: environment.sentry.tracesSampleRate,
    sendDefaultPii: false,
  });
}

bootstrapApplication(AppComponent, {
  providers: [
    provideAnimations(),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      progressBar: true,
    }),
    provideHttpClient(withInterceptors([jwtInterceptor, retryInterceptor, loggingInterceptor])),
    provideRouter(
      routes,
      withInMemoryScrolling({
        scrollPositionRestoration: 'top',
        anchorScrolling: 'enabled',
      })
    ),
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
    provideTranslateService({
      defaultLanguage: 'fr',
    }),
    provideTranslateHttpLoader({
      prefix: './assets/i18n/',
      suffix: '.json',
    }),
    // Sentry error handler and tracing
    {
      provide: ErrorHandler,
      useValue: Sentry.createErrorHandler({
        showDialog: false,
      }),
    },
    {
      provide: Sentry.TraceService,
      deps: [Router],
    },
    {
      provide: APP_INITIALIZER,
      useFactory: () => () => {},
      deps: [Sentry.TraceService],
      multi: true,
    },
  ],
}).catch((err) => {
  console.error(err);
  if (environment.sentry.enabled) {
    Sentry.captureException(err);
  }
});
