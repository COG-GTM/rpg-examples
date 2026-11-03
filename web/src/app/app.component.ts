import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  template: `
    <header>
      <a routerLink="/clients" class="brand">Client Management</a>
      <span class="muted">TRCLIE &middot; CLIESRV</span>
    </header>
    <main><router-outlet /></main>
  `,
})
export class AppComponent {}
