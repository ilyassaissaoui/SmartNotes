import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserResponse {
  id: number;
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
}

export interface TagResponse {
  id: number;
  name: string;
}

export interface NoteResponse {
  id: number;
  title: string;
  content: string;
  category: CategoryResponse;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  hasCachedSummary: boolean;
  hasCachedKeyPoints: boolean;
  hasCachedQuizQuestions: boolean;
}

export interface NoteRequest {
  title: string;
  content: string;
  categoryId: number | null;
  tags: string;
}

export interface AiStatusResponse {
  configured: boolean;
  hint: string;
}

export interface AiTextResponse {
  title: string;
  text: string;
  fromCache: boolean;
}

export interface AiListResponse {
  title: string;
  items: string[];
  fromCache: boolean;
}

export interface DashboardResponse {
  noteCount: number;
  latestNotes: NoteResponse[];
}

@Injectable({ providedIn: 'root' })
export class SmartNotesApiService {
  private readonly apiBaseUrl = '/api';
  private readonly tokenKey = 'smart-notes-token';
  private readonly userKey = 'smart-notes-user';

  constructor(private readonly http: HttpClient) {}

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiBaseUrl}/auth/register`, request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiBaseUrl}/auth/login`, request);
  }

  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiBaseUrl}/auth/me`, this.authOptions());
  }

  saveSession(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.token);
    localStorage.setItem(this.userKey, JSON.stringify(response.user));
  }

  clearSession(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  }

  hasToken(): boolean {
    return this.getToken() !== null;
  }

  getStoredUser(): UserResponse | null {
    const rawUser = localStorage.getItem(this.userKey);
    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as UserResponse;
    } catch {
      this.clearSession();
      return null;
    }
  }

  getDashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(`${this.apiBaseUrl}/dashboard`, this.authOptions());
  }

  getAiStatus(): Observable<AiStatusResponse> {
    return this.http.get<AiStatusResponse>(`${this.apiBaseUrl}/ai/status`, this.authOptions());
  }

  getCategories(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(`${this.apiBaseUrl}/categories`, this.authOptions());
  }

  getTags(): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(`${this.apiBaseUrl}/tags`, this.authOptions());
  }

  getNotes(query = '', categoryId: number | null = null, tagId: number | null = null): Observable<NoteResponse[]> {
    let params = new HttpParams();

    if (query.trim().length > 0) {
      params = params.set('query', query.trim());
    }

    if (categoryId !== null) {
      params = params.set('categoryId', categoryId);
    }

    if (tagId !== null) {
      params = params.set('tagId', tagId);
    }

    return this.http.get<NoteResponse[]>(`${this.apiBaseUrl}/notes`, {
      ...this.authOptions(),
      params
    });
  }

  getNote(id: number): Observable<NoteResponse> {
    return this.http.get<NoteResponse>(`${this.apiBaseUrl}/notes/${id}`, this.authOptions());
  }

  createNote(request: NoteRequest): Observable<NoteResponse> {
    return this.http.post<NoteResponse>(`${this.apiBaseUrl}/notes`, request, this.authOptions());
  }

  updateNote(id: number, request: NoteRequest): Observable<NoteResponse> {
    return this.http.put<NoteResponse>(`${this.apiBaseUrl}/notes/${id}`, request, this.authOptions());
  }

  deleteNote(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/notes/${id}`, this.authOptions());
  }

  summarizeNote(id: number): Observable<AiTextResponse> {
    return this.http.post<AiTextResponse>(`${this.apiBaseUrl}/notes/${id}/ai/summary`, {}, this.authOptions());
  }

  extractKeyPoints(id: number): Observable<AiListResponse> {
    return this.http.post<AiListResponse>(`${this.apiBaseUrl}/notes/${id}/ai/key-points`, {}, this.authOptions());
  }

  generateQuiz(id: number): Observable<AiListResponse> {
    return this.http.post<AiListResponse>(`${this.apiBaseUrl}/notes/${id}/ai/quiz`, {}, this.authOptions());
  }

  generateQuizAnswers(id: number): Observable<AiListResponse> {
    return this.http.post<AiListResponse>(`${this.apiBaseUrl}/notes/${id}/ai/quiz-answers`, {}, this.authOptions());
  }

  private getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  private authOptions(): { headers?: HttpHeaders } {
    const token = this.getToken();
    if (!token) {
      return {};
    }

    return {
      headers: new HttpHeaders({
        Authorization: `Bearer ${token}`
      })
    };
  }
}
