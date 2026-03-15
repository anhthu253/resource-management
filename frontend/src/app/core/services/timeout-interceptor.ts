import { HttpInterceptorFn } from '@angular/common/http';
import { timeout, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const timeoutInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    timeout(60000), // 60 seconds
    catchError((error) => {
      if (error.name === 'TimeoutError') {
        console.error('Request timed out');
      }
      return throwError(() => error);
    }),
  );
};
