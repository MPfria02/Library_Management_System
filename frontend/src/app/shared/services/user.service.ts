import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserResponse, UserRole } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = '/api/users';
  private http = inject(HttpClient);

  /**
   * Get a single user by ID.
   * Admin only operation.
   */
  getById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get all members (users with MEMBER role).
   * Returns list of all members without pagination.
   */
  getMembers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.apiUrl);
  }

  /**
   * Search users by first name and/or last name.
   * Both parameters are optional.
   */
  search(firstName?: string, lastName?: string): Observable<UserResponse[]> {
    let params = new HttpParams();
    
    if (firstName !== null && firstName !== undefined) {
      params = params.set('firstName', firstName);
    }
    
    if (lastName !== null && lastName !== undefined) {
      params = params.set('lastName', lastName);
    }

    return this.http.get<UserResponse[]>(`${this.apiUrl}/search`, { params });
  }

  /**
   * Find users by role.
   * Returns users with the specified role.
   */
  findByRole(role: UserRole): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/role/${role}`);
  }

  /**
   * Update user information.
   * Admin only operation.
   */
  update(id: number, userData: UserResponse): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, userData);
  }

  /**
   * Get total count of all users.
   * Returns the number as a simple number.
   */
  countAll(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/count`);
  }
}
