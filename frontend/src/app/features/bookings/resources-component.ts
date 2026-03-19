import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  OnInit,
  ViewChild,
} from '@angular/core';
import { ResourceDto } from '../../core/dtos/resource.dto';
import { BookingService } from '../../core/services/booking.service';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';

@Component({
  standalone: true,
  selector: 'app-resources',
  templateUrl: './resources-component.html',
  imports: [MatTableModule, MatPaginatorModule, CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    table {
      width: 100%;
    }
  `,
})
export class ResourcesComponent implements OnInit {
  isLoading = false;
  message = '';
  private destroyRef = inject(DestroyRef);

  dataSource!: MatTableDataSource<ResourceDto>;
  displayedColumns: string[] = ['resourceName', 'basePrice', 'priceUnit'];
  @ViewChild(MatPaginator) paginator!: MatPaginator;

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
          this.dataSource = new MatTableDataSource<ResourceDto>(res);
          this.dataSource.paginator = this.paginator;
          this.message = '';
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.isLoading = false;
          this.message =
            err.error || err.message || 'Failed to fetch resources. Please try again later.';
          this.cdr.detectChanges();
        },
      });
  }
}
