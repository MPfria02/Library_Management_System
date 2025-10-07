import { Routes } from '@angular/router';
import { LoginComponent, RegisterComponent } from './auth';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, title:'Login'},
  { path: 'register', component: RegisterComponent, title: 'Register'},
  { path: '**', redirectTo: '/login' }
];
