import { ChangeDetectorRef, Component, DestroyRef, inject, OnInit } from '@angular/core';

import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MultipleSelection } from '../../core/components/multi-select-dropdown/multi-select-dropdown.component';
import { ActivatedRoute, Router } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';
import { ResourceResponseDto } from '../../core/dtos/resource.dto';
import { DatePicker } from '../../core/components/date-picker/date-picker.component';
import { MatInput } from '../../core/components/input/input.component';
import { UserService } from '../../core/services/user.service';
import { UserDto } from '../../core/dtos/user.dto';
import { startWith, take } from 'rxjs';
import { BookingRequestDto } from '../../core/dtos/booking.dto';
import { ValidationService } from '../../core/services/validation.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
@Component({
  standalone: true,
  selector: 'app-new-booking',
  templateUrl: './create-new-booking.component.html',
  styleUrl: './create-new-booking.component.css',
  imports: [ReactiveFormsModule, CommonModule, DatePicker, MultipleSelection, MatInput],
})
export class NewBookingComponent implements OnInit {
  resourceList: ResourceResponseDto[] = [];
  resourceNormalizedList: { id: number; name: string }[] = [];
  bookingFormGroup: FormGroup;
  dateFilter = (d: Date | null): boolean => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return d !== null && d >= today;
  };
  private destroyRef = inject(DestroyRef);
  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private bookingService: BookingService,
    private userService: UserService,
    private validationService: ValidationService,
    private fb: FormBuilder,
    private cf: ChangeDetectorRef
  ) {
    this.bookingFormGroup = fb.group(
      {
        period: fb.group(
          {
            startedAt: [null],
            endedAt: [null],
          },
          { validators: validationService.dateRangeValidator }
        ),
        resourceIds: [[]],
        purpose: [null],
      },
      { validators: validationService.bookingFormValidation }
    );
  }
  get periodGroup(): FormGroup {
    return this.bookingFormGroup.get('period') as FormGroup;
  }

  get selectedResourceNames(): string[] {
    const selectedResourceIds = this.bookingFormGroup.get('resourceIds')?.value;
    return this.resourceNormalizedList
      .filter((resource) => selectedResourceIds.includes(resource.id))
      .map((resource) => resource.name);
  }

  get totalPrice(): number {
    const selectedResourceIds = this.bookingFormGroup.get('resourceIds')?.value;
    return this.resourceList
      .filter((resource) => selectedResourceIds.includes(resource.resourceId))
      .reduce((acc, curr) => acc + this.getPricePerResource(curr.basePrice, curr.priceUnit), 0);
  }

  ngOnInit(): void {
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
  }

  fetchResources = () => {
    const startedAt = this.periodGroup.get('startedAt')?.value;
    const endedAt = this.periodGroup.get('endedAt')?.value;
    this.bookingService.getResources(startedAt, endedAt).subscribe({
      next: (res) => {
        this.resourceList = res;
        this.resourceNormalizedList = res.map((entry) => {
          return {
            id: entry.resourceId,
            name: `${entry.resourceName} ${entry.basePrice} ${entry.priceUnit}`,
          };
        });
        this.cf.detectChanges();
      },
      error: (err) => console.log(err.message),
    });
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
    console.log('cancel');
  };
  toPayment = () => {
    const { period, ...rest } = this.bookingFormGroup.value;
    this.userService.user$.pipe(take(1)).subscribe((user) => {
      if (!user) return;
      const postData = { ...period, ...rest, userId: user.userId, totalPrice: this.totalPrice };

      this.bookingService.createBooking(postData).subscribe({
        next: (res) => {
          this.router.navigate(['../payment', res], {
            relativeTo: this.route,
          });
        },
        error: (err) => {
          console.error('Create booking failed', err);
        },
      });
    });
  };
}
