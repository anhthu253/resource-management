import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ResourceDto } from '../../core/dtos/resource.dto';
import { BookingService } from '../../core/services/booking.service';
import { MatList, MatListItem } from '@angular/material/list';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  selector: 'app-resources',
  templateUrl: './resources-component.html',
  imports: [MatList, MatListItem, CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResourcesComponent {
  resources!: ResourceDto[];
  constructor(
    private bookingService: BookingService,
    private cdr: ChangeDetectorRef,
  ) {}
  ngOnInit(): void {
    this.bookingService.getAllResources().subscribe({
      next: (res: ResourceDto[]) => {
        this.resources = res;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.log(err);
      },
    });
  }
}
