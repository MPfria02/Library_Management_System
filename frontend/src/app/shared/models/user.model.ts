/**
 * Enum representing the available user roles.
 * Values must match the backend enum exactly.
 */
export enum UserRole {
  MEMBER = 'MEMBER',
  ADMIN = 'ADMIN'
}

/**
 * DTO for a user as returned by the backend API.
 * Contains user information for admin operations.
 */
export interface UserResponse {
  /** Database identifier */
  id: number;
  /** User's email address */
  email: string;
  /** User's first name */
  firstName: string;
  /** User's last name */
  lastName: string;
  /** User's phone number */
  phone: string;
  /** User's role in the system */
  role: UserRole;
}
