import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, Self } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { DateFilterFn, MatDatepickerModule } from '@angular/material/datepicker';
import { MatDatepickerToggle } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { NgControl } from '@angular/forms';
import { ControlValueAccessor } from '@angular/forms';
import { distinctUntilChanged } from 'rxjs/operators';
import {
  MatTimepickerModule,
  MatTimepickerSelected,
  MatTimepickerToggle,
} from '@angular/material/timepicker';
import { Subscription } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-date-picker',
  templateUrl: './date-picker.component.html',
  styleUrl: './date-picker.component.css',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatDatepickerToggle,
    MatTimepickerModule,
    MatTimepickerToggle,
    MatNativeDateModule,
  ],
})
export class DatePicker implements ControlValueAccessor, OnInit, OnDestroy {
  @Input() dateLabel?: string | null;
  @Input() timeLabel?: string | null;
  @Input() filter!: DateFilterFn<Date | null>;
  @Input() timeFilter!: DateFilterFn<Date | null>;
  dateValue: Date | null = null;
  timeValue: Date | null = null;
  disabled: boolean = false;
  error = '';
  private sub?: Subscription;

  private onChange = (date: Date | null) => {};
  private onTouched = () => {};

  constructor(private cd: ChangeDetectorRef, @Self() public ngControl: NgControl) {
    this.ngControl.valueAccessor = this;
  }

  ngOnInit(): void {
    this.sub = this.ngControl.control?.statusChanges.subscribe(() => {
      const errors = this.ngControl.control?.errors;
      this.error = errors ? Object.values(errors)[0] : null;
      this.cd.markForCheck();
    });
  }
  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  onDateChange = (d: Date | null) => {
    this.dateValue = d;
    this.emitValue();
  };
  onTimeSelected(event: MatTimepickerSelected<Date | string>) {
    const t = event.value;
    if (!t) return;

    if (typeof t === 'string') {
      const [h, m] = t.split(':').map(Number);
      this.timeValue = new Date();
      this.timeValue.setHours(h, m, 0, 0);
    } else {
      // If it emits Date object
      this.timeValue = t;
    }
    this.emitValue();
  }

  writeValue(obj: Date | null): void {
    this.dateValue = obj;
    this.timeValue = obj;
    this.cd.markForCheck();
  }
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.cd.markForCheck();
  }

  private emitValue() {
    if (!this.dateValue) {
      this.onChange(null);
      return;
    }

    const combined = new Date(this.dateValue);
    if (this.timeValue)
      combined.setHours(this.timeValue.getHours(), this.timeValue.getMinutes(), 0, 0);
    else combined.setHours(0, 0, 0, 0);

    this.onChange(combined);
    this.onTouched();
  }
}
