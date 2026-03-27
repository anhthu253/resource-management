import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  OnDestroy,
  OnInit,
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { take } from 'rxjs';
import { DatePicker } from '../../core/components/date-picker/date-picker.component';
import { MultipleSelection } from '../../core/components/multi-select-dropdown/multi-select-dropdown.component';
import { ResourceDto } from '../../core/dtos/resource.dto';
import { BookingService } from '../../core/services/booking.service';
import { BookingStateService } from '../../core/services/booking.state.service';
import { UserService } from '../../core/services/user.service';
import { ValidationService } from '../../core/services/validation.service';
import { BookingDto } from '../../core/dtos/booking.dto';
import { MatDialog } from '@angular/material/dialog';
import { NotificationDialog } from '../../core/components/pop-up/notification-component';
import { MatSpinner } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';

@Component({
  standalone: true,
  selector: 'app-new-booking',
  templateUrl: './create-new-booking.component.html',
  styleUrl: './create-new-booking.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    CommonModule,
    DatePicker,
    MultipleSelection,
    MatButtonModule,
    MatSpinner,
    MatCardModule,
  ],
})
export class NewBookingComponent implements OnInit, OnDestroy {
  fromEdit = false;
  fromPayment = false;
  resourceList: ResourceDto[] = [];
  resourceNormalizedList: { id: number; name: string }[] = [];
  isResourceLoading = false;
  availResourceMessage = '';
  totalPrice?: number;
  bookingFormGroup: FormGroup;
  dateFilter = (d: Date | null): boolean => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return d !== null && d >= today;
  };
  changeNotification = '';
  errorMessage = '';
  confirmDialog: boolean = false;
  alert: boolean = false;

  private destroyRef = inject(DestroyRef);
  constructor(
    private router: Router,
    private bookingService: BookingService,
    private userService: UserService,
    private bookingStateService: BookingStateService,
    private validationService: ValidationService,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private cf: ChangeDetectorRef,
  ) {
    this.bookingFormGroup = fb.group(
      {
        period: fb.group(
          {
            startedAt: [null],
            endedAt: [null],
          },
          { validators: validationService.dateRangeValidator },
        ),
        resourceIds: [[]],
      },
      { validators: validationService.bookingFormValidation },
    );
  }
  ngOnDestroy(): void {
    this.changeNotification = '';
  }

  get periodGroup(): FormGroup {
    return this.bookingFormGroup.get('period') as FormGroup;
  }

  get selectedResources(): ResourceDto[] {
    const selectedResourceIds = this.bookingFormGroup.get('resourceIds')?.value;
    return this.resourceList.filter((resource) =>
      selectedResourceIds.includes(resource.resourceId),
    );
  }

  get currentBooking(): BookingDto | null {
    return this.bookingStateService.getBooking();
  }

  get currentBookingFormData() {
    const startedAt =
      typeof this.currentBooking?.startedAt == 'string'
        ? new Date(this.currentBooking.startedAt)
        : this.currentBooking?.startedAt;
    const endedAt =
      typeof this.currentBooking?.endedAt == 'string'
        ? new Date(this.currentBooking.endedAt)
        : this.currentBooking?.endedAt;
    const formData = {
      period: {
        startedAt: startedAt,
        endedAt: endedAt,
      },
      resourceIds: this.currentBooking?.resources.map((resource) => resource.resourceId),
    };
    return formData;
  }

  ngOnInit(): void {
    this.fromEdit = history.state?.fromEdit;
    this.fromPayment = history.state?.fromPayment;
    if (!this.fromEdit && !this.fromPayment) {
      this.bookingStateService.clearBooking();
    }

    if (this.currentBooking?.bookingId) {
      this.bookingFormGroup.patchValue(this.currentBookingFormData); //populate controls with current booking values
      this.totalPrice = this.currentBooking.totalPrice; //total price of the current booking
      if (this.fromEdit)
        this.changeNotification =
          'You are modifying an existing booking. Please review and update your details below.';
    }
    if (this.periodGroup?.valid) {
      this.fetchResources();
    } else {
      this.resourceList = [];
      this.resourceNormalizedList = [];
      this.cf.detectChanges();
    }

    this.periodGroup?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.periodGroup?.valid) {
        this.fetchResources();
      } else {
        this.resourceList = [];
        this.resourceNormalizedList = [];
        this.cf.detectChanges();
      }
    });

    //update total price upon selecting resources
    this.bookingFormGroup.get('resourceIds')?.valueChanges.subscribe(() => {
      this.bookingService
        .getTotalPrice({ ...this.periodGroup.value, resources: this.selectedResources })
        .subscribe((price) => {
          this.totalPrice = price;
        });
    });
  }

  openUpdateConfirm = (message: string) => {
    const dialogRef = this.dialog.open(NotificationDialog, {
      width: '350px',
      data: {
        message: message + ' Please press OK to complete the new booking.',
      },
    });
    dialogRef.afterClosed().subscribe(() => {
      this.createBooking(); //execute new booking upon closing dialog
    });
  };

  openFailureAlert = (message: string) => {
    this.dialog.open(NotificationDialog, {
      width: '350px',
      data: {
        message: message,
      },
    });
  };

  //get resources for the choosen period
  fetchResources = () => {
    const startedAt = this.periodGroup.get('startedAt')?.value;
    const endedAt = this.periodGroup.get('endedAt')?.value;
    this.isResourceLoading = true;
    this.bookingService
      .getAvailableResources(startedAt, endedAt)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: ResourceDto[]) => {
          this.isResourceLoading = false;

          if (this.currentBooking?.bookingId) {
            if (
              this.currentBooking.startedAt &&
              new Date(this.currentBooking.startedAt) <=
                new Date(this.periodGroup.get('startedAt')?.value) &&
              this.currentBooking.endedAt &&
              new Date(this.currentBooking.endedAt) >=
                new Date(this.periodGroup.get('endedAt')?.value)
            ) {
              const map = new Map();
              [...res, ...this.currentBooking.resources].forEach((item) => {
                map.set(item.resourceId, item);
              }); // combine available resources with current booking resources to make sure all current booking resources are shown in the resource list even they are not available for the selected period, which will avoid confusion for users when they modify an existing booking.
              this.resourceList = [...map.values()];
            } else {
              this.resourceList = res;
            }
          } else this.resourceList = res;

          if (!this.resourceList.length) {
            this.bookingFormGroup
              .get('resourceIds')
              ?.setErrors({ empty: 'There is no resources available during this period.' });
          }

          this.resourceNormalizedList = this.resourceList.map((entry) => {
            return {
              id: entry.resourceId,
              name: `${entry.resourceName} ${entry.basePrice} ${entry.priceUnit}`,
            };
          });
          this.cf.detectChanges();
        },
        error: (err) => {
          this.isResourceLoading = false;
          this.bookingFormGroup
            .get('resourceIds')
            ?.setErrors({ noConnection: 'Internal server error. Please try again later.' });
          this.cf.detectChanges();
          this.openFailureAlert('Failed to fetch available resources. Please try again later.');
        },
      });
  };

  //cancel an unfinished booking and clear the form. If there is an existing booking, navigate to my-bookings page.
  onCancelBooking = () => {
    if (this.currentBooking?.bookingId) this.router.navigate(['/my-bookings']);
    else this.clearBooking();
  };

  clearBooking = () => {
    this.bookingStateService.clearBooking();
    this.bookingFormGroup.patchValue(this.currentBookingFormData);
    this.totalPrice = 0;
    this.changeNotification = '';
    this.errorMessage = '';
  };

  createBooking = () => {
    const { period, ...rest } = this.bookingFormGroup.value;
    var resourceIds = rest.resourceIds;
    if (!resourceIds) return;
    const resources = this.resourceList.filter((resource) =>
      resourceIds.includes(resource.resourceId),
    );
    this.userService.user$.pipe(take(1)).subscribe((user) => {
      if (!user) return;
      const postData: BookingDto = {
        ...period,
        bookingGroupId: this.currentBooking?.bookingId ? this.currentBooking.bookingGroupId : null, //if there is an existing booking, use its bookingGroupId for creating new booking, which will link these two bookings together. Otherwise, use null as default bookingGroupId for new booking.
        resources: resources,
        userId: user.userId,
        totalPrice: this.totalPrice,
      };

      this.bookingService.createBooking(postData).subscribe({
        next: (res) => {
          this.bookingStateService.setBooking({
            ...res,
            replacedBookingId: this.currentBooking?.bookingId,
          });
          this.router.navigate(['/payment']);
        },
        error: (err) => {
          this.openFailureAlert(err.error || err.message);
        },
      });
    });
  };

  updateBooking = () => {
    if (
      JSON.stringify(this.bookingFormGroup.value) === JSON.stringify(this.currentBookingFormData)
    ) {
      if (this.fromEdit) return; // if user hasn't changed the current booking, stop proceeding further
      if (this.fromPayment)
        //if nothing changed, going back to payment without updating the booking
        this.router.navigate(['/payment']);
    }

    this.bookingService
      .updateBooking(this.currentBooking!.bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.openUpdateConfirm(res);
        },
        error: (err) => {
          this.openFailureAlert(err.error);
        },
      });
  };

  toPayment = () => {
    if (this.currentBooking?.bookingId)
      //modify booking
      this.updateBooking();
    else this.createBooking();
  };
}
