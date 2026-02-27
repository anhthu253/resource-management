import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  OnDestroy,
  OnInit,
  resource,
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router } from '@angular/router';
import { take } from 'rxjs';
import { DatePicker } from '../../core/components/date-picker/date-picker.component';
import { MultipleSelection } from '../../core/components/multi-select-dropdown/multi-select-dropdown.component';
import { ResourceDto } from '../../core/dtos/resource.dto';
import { BookingService } from '../../core/services/booking.service';
import { BookingStateService } from '../../core/services/booking.state.service';
import { UserService } from '../../core/services/user.service';
import { ValidationService } from '../../core/services/validation.service';
import { BookingDto } from '../../core/dtos/booking.dto';
import { HttpStatusCode } from '@angular/common/http';
import { ConfirmDialog } from '../../core/components/pop-up/confirm-dialog-component';
import { MatDialog } from '@angular/material/dialog';
import { NotificationDialog } from '../../core/components/pop-up/notification-component';
import { MatSpinner } from '@angular/material/progress-spinner';

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
    ConfirmDialog,
    MatSpinner,
  ],
})
export class NewBookingComponent implements OnInit, OnDestroy {
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
    private route: ActivatedRoute,
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
            startedAt: [null, Validators.required],
            endedAt: [null, Validators.required],
          },
          { validators: validationService.dateRangeValidator },
        ),
        resourceIds: [[]],
      },
      { validators: validationService.bookingFormValidation },
    );
  }
  ngOnDestroy(): void {
    this.bookingStateService.clearBooking();
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

  get bookingFormData() {
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
      resourceIds: this.currentBooking
        ? this.currentBooking.resources.map((resource) => resource.resourceId)
        : [],
    };
    return formData;
  }

  ngOnInit(): void {
    if (this.currentBooking && this.currentBooking.bookingId) {
      this.bookingFormGroup.patchValue(this.bookingFormData); //populate controls with current booking values
      this.totalPrice = this.currentBooking.totalPrice; //total price of the current booking
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
    this.bookingFormGroup.get('resourceIds')?.valueChanges.subscribe((selectedResourceIds) => {
      if (selectedResourceIds && selectedResourceIds.length)
        this.totalPrice = this.getTotalPrice(selectedResourceIds);
    });
  }

  openSucceededRefundConfirm = (message: string) => {
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
    const dialogRef = this.dialog.open(NotificationDialog, {
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
    this.bookingService.getAvailableResources(startedAt, endedAt).subscribe({
      next: (res: ResourceDto[]) => {
        this.isResourceLoading = false;
        if (!res.length) {
          this.bookingFormGroup
            .get('resourceIds')
            ?.setErrors({ empty: 'There is no resources available during this period.' });
        }
        if (this.currentBooking && this.currentBooking.bookingId) {
          this.resourceList = [...res, ...this.currentBooking.resources];
        } else {
          this.resourceList = res;
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
        console.log('error type', err.status, err.message);
      },
    });
  };

  private getTotalPrice = (selectedResourceIds: number[]): number => {
    return this.resourceList
      .filter((resource) => selectedResourceIds.includes(resource.resourceId))
      .reduce((acc, curr) => acc + this.getPricePerResource(curr.basePrice, curr.priceUnit), 0);
  };

  private getPricePerResource = (pricePerUnit: number, priceUnit: string): number => {
    if (this.periodGroup.invalid) return 0;
    const { startedAt, endedAt } = this.periodGroup.value;
    const diffMs = endedAt.getTime() - startedAt.getTime();
    let diffUnits = 1;
    if (priceUnit === 'hourly') {
      diffUnits = diffMs / (1000 * 60 * 60);
    } else if (priceUnit === 'daily') {
      diffUnits = diffMs / (1000 * 60 * 60 * 24);
    }
    return pricePerUnit * diffUnits;
  };

  onCancelBooking = () => {
    this.clearBooking();
    if (this.currentBooking && this.currentBooking.bookingId) {
      this.router.navigate(['/my-bookings']);
    }
  };

  clearBooking = () => {
    this.bookingStateService.clearBooking();
    this.bookingFormGroup.patchValue(this.bookingFormData);
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
        resources: resources,
        userId: user.userId,
        totalPrice: this.totalPrice,
      };

      this.bookingService.createBooking(postData).subscribe({
        next: (res) => {
          console.log('navigate to payment with ', res);
          this.router.navigate(['/payment'], {
            queryParams: {
              bookingId: res.bookingId,
              paymentId: res.paymentId,
            },
          });
        },
        error: (err) => {
          this.openFailureAlert(err.error);
        },
      });
    });
  };

  updateBooking = () => {
    if (!this.currentBooking || !this.currentBooking.bookingId) return;
    if (JSON.stringify(this.bookingFormGroup.value) === JSON.stringify(this.bookingFormData)) {
      return; // if user hasn't changed the current booking, stop proceeding further
    }

    this.bookingService.updateBooking(this.currentBooking!.bookingId).subscribe({
      next: (res) => {
        this.openSucceededRefundConfirm(res);
      },
      error: (err) => {
        this.openFailureAlert(err.error); //refund failed due to stripe. Will be tried in the backend.
      },
    });
  };

  toPayment = () => {
    if (this.currentBooking && this.currentBooking.bookingId)
      //change booking
      this.updateBooking();
    else this.createBooking();
  };
}
