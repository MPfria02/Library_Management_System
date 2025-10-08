/**
 * Enum representing the available book genres.
 * Values must match the backend enum exactly.
 */
export enum BookGenre {
    FICTION = 'FICTION',
    TECHNOLOGY = 'TECHNOLOGY',
    SCIENCE = 'SCIENCE',
    HISTORY = 'HISTORY',
    BIOGRAPHY = 'BIOGRAPHY',
    FANTASY = 'FANTASY',
    MYSTERY = 'MYSTERY',
    ROMANCE = 'ROMANCE',
    NON_FICTION = 'NON_FICTION'
  }
  
  /**
   * DTO for a book as returned by the backend API.
   * publicationDate is an ISO date string (not a Date object).
   */
  export interface BookResponse {
    id: number;
    title: string;
    author: string;
    description: string;
    genre: BookGenre;
    availableCopies: number;
    publicationDate: string;
  }
  
  /**
   * Optional filters for searching and filtering the books catalog.
   */
  export interface BookSearchFilters {
    /** Optional search term for title/author matching */
    searchTerm?: string;
    /** Optional genre filter */
    genre?: BookGenre;
    /** Optional flag to return only books with available copies */
    availableOnly?: boolean;
  }
  
  /**
   * Generic Spring Boot page response wrapper used by the API.
   * Contains only fields that are consumed by the frontend.
   */
  export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    /** Current page index (0-based) */
    number: number;
    first: boolean;
    last: boolean;
    empty: boolean;
  }