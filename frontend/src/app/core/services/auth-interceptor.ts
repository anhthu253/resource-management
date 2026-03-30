import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { UserService } from './user.service';
import { NotificationDialog } from '../components/pop-up/notification-component';
import { MatDialog } from '@angular/material/dialog';

let isRedirecting = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const userService = inject(UserService);
  const dialog = inject(MatDialog);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if ((error.status === 401 || error.status === 403) && !isRedirecting) {
        isRedirecting = true; // Prevent multiple redirects
        dialog.open(NotificationDialog, {
          width: '350px',
          data: {
            message: 'Your session has expired. Please log in again.',
          },
        });

        userService.clearUser();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
