import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, Self } from '@angular/core';
import { ControlValueAccessor, NgControl } from '@angular/forms';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { Subscription } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-multi-select',
  templateUrl: './multi-select-dropdown.component.html',
  styleUrl: './multi-select-dropdown.component.css',
  imports: [MatSelectModule, MatOptionModule, MatFormFieldModule],
})
export class MultipleSelection implements ControlValueAccessor, OnInit, OnDestroy {
  @Input() options: { id: number; name: string }[] = [];
  @Input() label!: string;
  value: number[] = [];
  error = '';
  onChange: any = () => {};
  onTouched: any = () => {};
  disabled: boolean = false;
  private sub?: Subscription;

  constructor(
    @Self() public ngControl: NgControl,
    private cd: ChangeDetectorRef,
  ) {
    this.ngControl.valueAccessor = this;
  }

  ngOnInit(): void {
    this.sub = this.ngControl.control?.statusChanges.subscribe(() => {
      const errors = this.ngControl.control?.errors;
      this.error = errors ? Object.values(errors)[0] : null;
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  onSelect(event: MatSelectChange) {
    this.value = event.value;
    this.onChange(event.value);
    this.onTouched();
  }

  writeValue(obj: number[]): void {
    this.value = obj ?? [];
    this.cd.markForCheck();
  }
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.cd.markForCheck();
  }
}
