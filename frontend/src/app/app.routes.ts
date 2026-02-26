import { Routes } from '@angular/router';
import { PaymentGuard } from './core/services/payment.service';
import { LoginComponent } from './features/auth/login.component';
import { BookingSummaryComponent } from './features/bookings/booking-summary-component';
import { NewBookingComponent } from './features/bookings/create-new-booking.component';
import { MyBookingComponent } from './features/bookings/my-booking-component';
import { PaymentComponent } from './features/bookings/payment.components';
import { ResourcesComponent } from './features/bookings/resources-component';
import { MainLayoutComponent } from './layout/main-layout.component';
import { PendingBookingsComponent } from './features/bookings/pending-bookings-component';
export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      // { path: 'dashboard', component: DashboardComponent },
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      { path: 'login', component: LoginComponent },
      { path: 'new-booking', component: NewBookingComponent },
      { path: 'payment', component: PaymentComponent, canActivate: [PaymentGuard] },
      { path: 'booking-summary', component: BookingSummaryComponent },
      { path: 'my-bookings', component: MyBookingComponent },
      { path: 'pending-bookings', component: PendingBookingsComponent },
      { path: 'resources', component: ResourcesComponent },
    ],
  },
];
