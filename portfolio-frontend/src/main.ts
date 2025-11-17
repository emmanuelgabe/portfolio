import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { retryInterceptor } from './app/interceptors/retry.interceptor';
import { loggingInterceptor } from './app/interceptors/logging.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    provideAnimations(),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      progressBar: true,
    }),
    provideHttpClient(withInterceptors([retryInterceptor, loggingInterceptor])),
    provideRouter(routes),
  ],
}).catch((err) => console.error(err));
