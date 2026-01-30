import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-popup',
  templateUrl: './pop-up.component.html',
  styleUrl: './pop-up.component.css',
})
export class Popup {
  @Input() title?: string;
  @Input() content = '';
  @Output() close = new EventEmitter();

  closePopup = () => {
    this.close.emit();
  };
}
