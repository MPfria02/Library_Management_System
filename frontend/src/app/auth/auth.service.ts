import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterCredentials {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone: string;
}

interface AuthResponse {
  token: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private readonly TOKEN_KEY = 'auth_token';

  async login(credentials: LoginCredentials): Promise<void> {
    try {
      const response = await firstValueFrom(
        this.http.post<AuthResponse>('/api/auth/login', credentials)
      );
      
      if (response?.token) {
        localStorage.setItem(this.TOKEN_KEY, response.token);
      } else {
        throw new Error('Invalid response from server');
      }
    } catch (error) {
      throw this.handleError(error);
    }
  }

  async register(credentials: RegisterCredentials): Promise<void> {
    try {
      const response = await firstValueFrom(
        this.http.post('/api/auth/register', credentials)
      );
    } catch (error) {
      throw this.handleError(error);
    }
  }

  isLoggedIn(): boolean {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) return false;
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expirationTime = payload.exp * 1000;
      return Date.now() < expirationTime;
    } catch {
      return false;
    }
  }

  getUserRole(): string | null {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) return null;
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role || null;
    } catch {
      return null;
    }
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  private handleError(error: unknown): Error {
    if (error instanceof HttpErrorResponse) {
      const message = error.error?.message || error.statusText || 'Unknown error';
      return new Error(message);
    }
    
    if (error instanceof Error) {
      return error;
    }
    
    return new Error('An unknown error occurred');
  }
}