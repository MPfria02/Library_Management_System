import { Routes } from '@angular/router';
import { LoginComponent, RegisterComponent } from './auth';
import { authGuard } from './auth';
import { BookListComponent } from './features/books/components/book-list/book-list.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, title: 'Login' },
  { path: 'register', component: RegisterComponent, title: 'Register' },
  {
    path: 'dashboard',
    component: BookListComponent,
    canActivate: [authGuard]
  },
  // {
  //   path: 'books/:id',
  //   // TODO: Implement BookDetailsComponent in Phase 2.4
  //   component: BookListComponent, // Placeholder until BookDetailsComponent is created
  //   canActivate: [authGuard]
  // },
  { path: '**', redirectTo: '/login' },
];
