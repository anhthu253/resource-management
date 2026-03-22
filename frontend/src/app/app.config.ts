import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { timeoutInterceptor } from './core/services/timeout-interceptor';
import { ConfigService } from './core/services/config.service';
import { authInterceptor } from './core/services/auth-interceptor';

const configService = new ConfigService();

function loadConfig(): () => Promise<void> {
  return () => configService.load();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([timeoutInterceptor, authInterceptor])),
    { provide: ConfigService, useValue: configService },
    {
      provide: APP_INITIALIZER,
      useFactory: loadConfig,
      multi: true,
    },
  ],
};
