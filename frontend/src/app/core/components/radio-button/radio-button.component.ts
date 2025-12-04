import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Component, forwardRef, Input } from '@angular/core';
import { MatRadioChange, MatRadioModule } from '@angular/material/radio';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  selector: 'app-radio-button',
  templateUrl: './radio-button.component.html',
  styleUrl: './radio-button.component.css',
  imports: [MatRadioModule, CommonModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RadioButton),
      multi: true,
    },
  ],
})
export class RadioButton implements ControlValueAccessor {
  @Input() options: string[] = [];
  value: any;
  disabled: boolean = false;
  onChange = (val: string) => {};
  onTouched = () => {};

  onSelect(event: MatRadioChange) {
    this.value = event.value;
    this.onChange(event.value);
    this.onTouched();
  }

  writeValue(val: any): void {
    this.value = val;
  }
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
