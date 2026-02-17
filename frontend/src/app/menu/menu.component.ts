import { Component } from '@angular/core';
import { MatTabChangeEvent, MatTabsModule } from '@angular/material/tabs';
import { Router } from '@angular/router';
@Component({
  standalone: true,
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css',
  imports: [MatTabsModule],
})
export class MenuComponent {
  constructor(private router: Router) {}
  onTabChange = (event: MatTabChangeEvent) => {
    switch (event.index) {
      case 0:
        this.router.navigate(['/new-booking']);
        break;
      case 1:
        this.router.navigate(['/my-bookings']);
        break;
      case 2:
        this.router.navigate(['/resources']);
        break;
    }
  };
}
