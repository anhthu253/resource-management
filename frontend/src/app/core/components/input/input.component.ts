import {
  ChangeDetectorRef,
  Component,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  Self,
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  NgControl,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Subscription } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';

@Component({
  standalone: true,
  selector: 'app-input',
  templateUrl: './input.component.html',
  styleUrl: './input.component.css',
  imports: [MatFormFieldModule, MatInputModule],
})
export class MatInput implements ControlValueAccessor, OnInit, OnDestroy {
  @Input() label: string = '';
  @Input() placeholder: string = '';
  @Input() type: string = 'text';

  value: any;
  error: string | null = null;
  disabled: boolean = false;
  private sub?: Subscription;

  onChange = (val: any) => {};
  onTouched = () => {};
  constructor(private cdr: ChangeDetectorRef, @Self() public ngControl: NgControl) {
    this.ngControl.valueAccessor = this;
  }
  ngOnInit() {
    const control = this.ngControl.control;
    if (!control) return;

    // Subscribe to statusChanges so we always get up-to-date errors
    this.sub = control.statusChanges.pipe(distinctUntilChanged()).subscribe((v) => {
      const errors = control.errors;
      this.error = errors ? (Object.values(errors)[0] as string) : null;
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  onValueChange(event: Event) {
    const input = event.target as HTMLInputElement;
    this.value = input.value;
    this.onChange(this.value);
    this.onTouched();
  }

  writeValue(obj: any): void {
    this.value = obj;
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
