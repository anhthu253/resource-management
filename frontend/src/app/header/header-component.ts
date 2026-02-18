import { Component, Input, OnInit } from '@angular/core';
import { AuthService } from '../core/services/auth.service';
import { UserService } from '../core/services/user.service';
import { Route, Router } from '@angular/router';
import { MatButton } from '@angular/material/button';
import { CommonModule } from '@angular/common';
@Component({
  standalone: true,
  selector: 'app-header',
  templateUrl: './header-component.html',
  styleUrl: './header-component.css',
  imports: [MatButton, CommonModule],
})
export class HeaderComponent {
  @Input() isLogin!: boolean;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private router: Router,
  ) {}

  onLogout = () => {
    this.auth.logout().subscribe({
      next: () => {
        this.userService.clearUser();
        this.router.navigate(['/login']);
        this.isLogin = false;
      },
      error: (err) => {
        if (err.status === 401) {
          this.router.navigate(['/login']);
          this.isLogin = false;
        }
      },
    });
  };
}
