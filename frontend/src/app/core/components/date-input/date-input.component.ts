import { Component, Input, Output, EventEmitter } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDatepickerInputEvent } from '@angular/material/datepicker';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerToggle } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

@Component({
  standalone: true,
  selector: 'app-date-input',
  templateUrl: './date-input.component.html',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatIconModule,
    ReactiveFormsModule,
    MatDatepickerToggle,
    MatNativeDateModule,
  ],
})
export class DateInput {
  @Input() control!: FormControl<Date | null>;
  @Input() label: string = 'Select date';
  @Output() dateChange = new EventEmitter<Date | null>();
  onDateChange = (event: MatDatepickerInputEvent<Date>) => {
    this.dateChange.emit(event.value ?? null);
  };
}
