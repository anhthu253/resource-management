import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { Router } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { UserService } from '../core/services/user.service';
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
