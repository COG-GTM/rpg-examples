import { Routes } from '@angular/router';

import { ClientDetailComponent } from './clients/client-detail/client-detail.component';
import { ClientFormComponent } from './clients/client-form/client-form.component';
import { ClientListComponent } from './clients/client-list/client-list.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'clients' },
  { path: 'clients', component: ClientListComponent, title: 'Clients' },
  { path: 'clients/new', component: ClientFormComponent, data: { mode: 'create' }, title: 'New client' },
  { path: 'clients/:dni', component: ClientDetailComponent, title: 'Client detail' },
  { path: 'clients/:dni/edit', component: ClientFormComponent, data: { mode: 'edit' }, title: 'Edit client' },
  { path: '**', redirectTo: 'clients' },
];
