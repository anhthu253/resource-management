import { Component, Inject } from '@angular/core';
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
})
export class NotificationDialog {
  constructor(
    private dialogRef: MatDialogRef<NotificationDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { message: string },
  ) {}

  onOK() {
    this.dialogRef.close(true);
  }
}
