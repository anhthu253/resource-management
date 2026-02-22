import { ChangeDetectorRef, Component, DestroyRef, inject, OnDestroy, OnInit } from '@angular/core';

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
import { BookingDto, BookingRequestDto } from '../../core/dtos/booking.dto';

@Component({
  standalone: true,
  selector: 'app-new-booking',
  templateUrl: './create-new-booking.component.html',
  styleUrl: './create-new-booking.component.css',
  imports: [ReactiveFormsModule, CommonModule, DatePicker, MultipleSelection, MatButtonModule],
})
export class NewBookingComponent implements OnInit, OnDestroy {
  resourceList: ResourceDto[] = [];
  resourceNormalizedList: { id: number; name: string }[] = [];
  totalPrice?: number;
  bookingFormGroup: FormGroup;
  dateFilter = (d: Date | null): boolean => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return d !== null && d >= today;
  };
  changeNotification = '';
  errorMessage = '';
  private destroyRef = inject(DestroyRef);
  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private bookingService: BookingService,
    private userService: UserService,
    private bookingStateService: BookingStateService,
    private validationService: ValidationService,
    private fb: FormBuilder,
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

    this.bookingFormGroup.get('resourceIds')?.disable();
    if (this.periodGroup?.valid) {
      this.bookingFormGroup.get('resourceIds')?.enable();
      this.fetchResources();
    }

    this.periodGroup?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.periodGroup?.valid) {
        this.bookingFormGroup.get('resourceIds')?.enable();
        this.fetchResources();
      } else this.bookingFormGroup.get('resourceIds')?.disable();
    });

    //update total price upon selecting resources
    this.bookingFormGroup.get('resourceIds')?.valueChanges.subscribe((selectedResourceIds) => {
      if (selectedResourceIds && selectedResourceIds.length)
        this.totalPrice = this.getTotalPrice(selectedResourceIds);
    });
  }

  //get resources for the choosen period
  fetchResources = () => {
    const startedAt = this.periodGroup.get('startedAt')?.value;
    const endedAt = this.periodGroup.get('endedAt')?.value;
    this.bookingService.getAvailableResources(startedAt, endedAt).subscribe({
      next: (res: ResourceDto[]) => {
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
      error: (err) => (this.errorMessage = err.message),
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
    if (this.currentBooking && this.currentBooking.bookingId) {
      this.bookingStateService.clearBooking();
      this.router.navigate(['/my-bookings']);
    }
    this.bookingFormGroup.patchValue(this.bookingFormData);
    this.totalPrice = 0;
    this.changeNotification = '';
    this.errorMessage = '';
  };

  toPayment = () => {
    if (JSON.stringify(this.bookingFormGroup.value) === JSON.stringify(this.bookingFormData)) {
      return; // if user hasn't changed the current booking, stop proceeding further
    }
    const { period, ...rest } = this.bookingFormGroup.value;
    this.userService.user$.pipe(take(1)).subscribe((user) => {
      if (!user) return;
      const postData: BookingRequestDto = {
        ...period,
        ...rest,
        bookingId: this.currentBooking?.bookingId,
        userId: user.userId,
        totalPrice: this.totalPrice,
      };

      this.bookingService.createBooking(postData).subscribe({
        next: (res) => {
          this.bookingStateService.setBookingResponse(res);
          this.router.navigate(['/payment']);
        },
        error: (err) => {
          this.errorMessage = err.error;
          this.cf.detectChanges();
        },
      });
    });
  };
}
