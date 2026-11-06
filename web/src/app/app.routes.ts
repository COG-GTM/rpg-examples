import { Routes } from '@angular/router';

import { ClientDetailComponent } from './clients/client-detail/client-detail.component';
import { ClientFormComponent } from './clients/client-form/client-form.component';
import { ClientListComponent } from './clients/client-list/client-list.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'clients' },
  { path: 'clients', component: ClientListComponent, title: 'Clients' },
  { path: 'clients/new', component: ClientFormComponent, title: 'New client' },
  { path: 'clients/:dni', component: ClientDetailComponent, title: 'Client' },
  { path: 'clients/:dni/edit', component: ClientFormComponent, title: 'Edit client' },
  { path: '**', redirectTo: 'clients' },
];
