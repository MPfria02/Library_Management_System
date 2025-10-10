import { Routes } from '@angular/router';
import { LoginComponent, RegisterComponent } from './auth';
import { authGuard } from './auth';
import { BookListComponent } from './features/books/components/book-list/book-list.component';
import { BookDetailsComponent } from './features/books/components/book-details/book-details.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, title: 'Login' },
  { path: 'register', component: RegisterComponent, title: 'Register' },
  {
    path: 'dashboard',
    component: BookListComponent,
    canActivate: [authGuard],
    title: 'Dashboard'
  },
  {
    path: 'books/:id',
    component: BookDetailsComponent,
    canActivate: [authGuard],
    title: 'Book Details'
  },
  { path: '**', redirectTo: '/login' },
];
