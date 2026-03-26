import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
} from '@angular/material/dialog';

@Component({
  standalone: true,
  selector: 'app-popup',
  templateUrl: './notification-component.html',
  styleUrl: './notification-component.css',
  imports: [MatDialogActions, MatDialogContent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationDialog {
  message = '';
  constructor(
    private dialogRef: MatDialogRef<NotificationDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { message: string },
    private cdr: ChangeDetectorRef,
  ) {
    this.message = data.message;
  }
  updateMessage(msg: string) {
    this.message = msg;
    this.cdr.detectChanges(); // 👈 force UI update
  }
  onOK() {
    this.dialogRef.close(true);
  }
}
