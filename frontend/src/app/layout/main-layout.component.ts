import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MenuComponent } from '../menu/menu.component';
import { HeaderComponent } from '../header/header-component';
import { UserService } from '../core/services/user.service';
import { CommonModule } from '@angular/common';
@Component({
  standalone: true,
  selector: 'app-main-layout',
  imports: [RouterOutlet, MenuComponent, HeaderComponent, CommonModule],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent implements OnInit {
  isLogin!: boolean;
  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef,
  ) {}
  ngOnInit(): void {
    this.userService.user$.subscribe((user) => {
      this.isLogin = !!user;
      this.cdr.detectChanges();
    });
  }
}
