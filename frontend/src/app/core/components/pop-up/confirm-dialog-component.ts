import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';

@Component({
  standalone: true,
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog-component.html',
  styleUrl: './confirm-dialog-component.css',
  imports: [MatDialogActions, MatDialogContent, MatIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  message = '';
  constructor(
    private dialogRef: MatDialogRef<ConfirmDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { message: string },
    private cdr: ChangeDetectorRef,
  ) {
    this.message = data.message;
  }

  updateMessage(msg: string) {
    this.message = msg;
    this.cdr.detectChanges(); // 👈 force UI update
  }

  onYes() {
    this.dialogRef.close(true);
  }

  onNo() {
    this.dialogRef.close(false);
  }
}
