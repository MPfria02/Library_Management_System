import { Routes } from '@angular/router';
import { LoginComponent, RegisterComponent } from './auth';
import { authGuard } from './auth';
import { BookListComponent } from './features/books/components/book-list/book-list.component';
import { BookDetailsComponent } from './features/books/components/book-details/book-details.component';
import { adminGuard } from './auth/guards/admin.guard';
import { BookManagementComponent } from './features/books/components/book-management/book-management.component';
import { BookFormComponent } from './features/books/components/book-form/book-form.component';
import { UserManagementComponent } from './features/users/components/user-management/user-management.component';

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
  // Admin routes
  {
    path: 'admin/books',
    component: BookManagementComponent,
    canActivate: [authGuard, adminGuard],
    title: 'Admin Dashboard'
  },
  {
    path: 'admin/books/new',
    component: BookFormComponent,
    canActivate: [authGuard, adminGuard],
    title: 'New Book'
  },
  {
    path: 'admin/books/:id/edit',
    component: BookFormComponent,
    canActivate: [authGuard, adminGuard],
    title: 'Edit Book'
  },
  {
    path: 'admin/users',
    component: UserManagementComponent,
    canActivate: [authGuard, adminGuard],
    title: 'User Management'
  },
  { path: '**', redirectTo: '/login' },
];
