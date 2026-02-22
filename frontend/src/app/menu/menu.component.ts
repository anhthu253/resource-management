import { Component, OnInit } from '@angular/core';
import { MatTabChangeEvent, MatTabsModule } from '@angular/material/tabs';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
@Component({
  standalone: true,
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css',
  imports: [MatTabsModule],
})
export class MenuComponent implements OnInit {
  selectedIndex = 0;
  constructor(private router: Router) {}
  ngOnInit(): void {
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
      const url = this.router.url;
      if (url.startsWith('/new-booking')) {
        this.selectedIndex = 0;
      } else if (url.startsWith('/my-bookings')) {
        this.selectedIndex = 1;
      } else if (url.startsWith('/resources')) this.selectedIndex = 2;
    });
  }
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
