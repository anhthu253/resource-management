import { Component, OnInit } from '@angular/core';
import { ResourceService } from '../../core/services/resource.service';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { DateInput } from '../../core/components/date-input/date-input.component';

@Component({
  standalone: true,
  selector: 'app-new-booking',
  templateUrl: './create-new-booking.component.html',
  styleUrl: './create-new-booking.component.css',
  imports: [ReactiveFormsModule, CommonModule, MatSelectModule, MatFormFieldModule, DateInput],
})
export class NewBookingComponent implements OnInit {
  resourceList: string[] = [];
  resources = new FormControl('');
  startedAt = new FormControl<Date | null>(null);
  endedAt = new FormControl<Date | null>(null);

  constructor(private resourceService: ResourceService) {}

  ngOnInit(): void {
    this.resourceService.getResources().subscribe({
      next: (res) => (this.resourceList = res),
      error: (err) => console.log(err.message),
    });
  }
  onStartDateChange = (date: Date | null) => {
    this.startedAt.setValue(date);
  };
  onEndDateChange = (date: Date | null) => {
    this.endedAt.setValue(date);
  };
}
