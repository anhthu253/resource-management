import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
} from '@angular/core';
import { ResourceDto } from '../../core/dtos/resource.dto';
import { BookingService } from '../../core/services/booking.service';
import { MatList, MatListItem } from '@angular/material/list';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  standalone: true,
  selector: 'app-resources',
  templateUrl: './resources-component.html',
  imports: [MatList, MatListItem, CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResourcesComponent {
  resources!: ResourceDto[];
  isLoading = false;
  message = '';
  private destroyRef = inject(DestroyRef);
  constructor(
    private bookingService: BookingService,
    private cdr: ChangeDetectorRef,
  ) {}
  ngOnInit(): void {
    this.isLoading = true;
    this.bookingService
      .getAllResources()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: ResourceDto[]) => {
          this.isLoading = false;
          this.resources = res;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.isLoading = false;
          this.message =
            err.error || err.message || 'Failed to fetch resources. Please try again later.';
          console.log(err);
        },
      });
  }
}
