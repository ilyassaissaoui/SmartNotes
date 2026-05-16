import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AiListResponse,
  AiStatusResponse,
  AiTextResponse,
  CategoryResponse,
  DashboardResponse,
  NoteRequest,
  NoteResponse,
  SmartNotesApiService,
  TagResponse,
  UserResponse
} from './smart-notes-api.service';

type AiAction = 'summary' | 'key-points' | 'quiz' | 'quiz-answers';
type AuthMode = 'login' | 'register';

interface AiPanelResult {
  title: string;
  text?: string;
  items?: string[];
  fromCache: boolean;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'Smart Notes AI';

  currentUser: UserResponse | null = null;
  authMode: AuthMode = 'login';
  authForm = {
    name: '',
    email: '',
    password: ''
  };
  authenticating = false;

  dashboard: DashboardResponse = { noteCount: 0, latestNotes: [] };
  aiStatus: AiStatusResponse = { configured: false, hint: 'Loading AI status...' };
  categories: CategoryResponse[] = [];
  tags: TagResponse[] = [];
  notes: NoteResponse[] = [];
  selectedNote: NoteResponse | null = null;
  activeTab: 'notes' | 'dashboard' = 'notes';
  editingNote = false;

  searchQuery = '';
  selectedCategoryId: number | null = null;
  selectedTagId: number | null = null;

  formMode: 'create' | 'edit' = 'create';
  form: NoteRequest = this.emptyForm();

  loadingNotes = false;
  saving = false;
  deleting = false;
  aiLoadingAction: AiAction | null = null;
  errorMessage = '';
  successMessage = '';
  aiResult: AiPanelResult | null = null;

  constructor(private readonly api: SmartNotesApiService) {}

  ngOnInit(): void {
    const storedUser = this.api.getStoredUser();
    if (this.api.hasToken() && storedUser) {
      this.currentUser = storedUser;
      this.api.getMe().subscribe({
        next: (user) => {
          this.currentUser = user;
          this.loadInitialData();
        },
        error: () => {
          this.api.clearSession();
          this.currentUser = null;
          this.errorMessage = 'Your session expired. Please log in again.';
        }
      });
    }
  }

  submitAuth(): void {
    this.clearMessages();

    const email = this.authForm.email.trim();
    const password = this.authForm.password;
    const name = this.authForm.name.trim();

    if (!email || !password || (this.authMode === 'register' && !name)) {
      this.errorMessage = this.authMode === 'register'
        ? 'Name, email, and password are required.'
        : 'Email and password are required.';
      return;
    }

    if (this.authMode === 'register' && password.length < 6) {
      this.errorMessage = 'Password must contain at least 6 characters.';
      return;
    }

    this.authenticating = true;
    const request = this.authMode === 'register'
      ? this.api.register({ name, email, password })
      : this.api.login({ email, password });

    request.subscribe({
      next: (response) => {
        this.authenticating = false;
        this.api.saveSession(response);
        this.currentUser = response.user;
        this.authForm.password = '';
        this.successMessage = this.authMode === 'register' ? 'Account created successfully.' : 'Logged in successfully.';
        this.loadInitialData();
      },
      error: (error) => {
        this.authenticating = false;
        this.showError(error);
      }
    });
  }

  switchAuthMode(mode: AuthMode): void {
    this.authMode = mode;
    this.clearMessages();
  }

  logout(): void {
    this.api.clearSession();
    this.currentUser = null;
    this.resetWorkspace();
    this.successMessage = 'Logged out successfully.';
  }

  loadInitialData(): void {
    this.errorMessage = '';
    this.api.getDashboard().subscribe({
      next: (dashboard) => this.dashboard = dashboard,
      error: (error) => this.showError(error)
    });

    this.api.getAiStatus().subscribe({
      next: (status) => this.aiStatus = status,
      error: (error) => this.showError(error)
    });

    this.api.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        if (this.form.categoryId === null && categories.length > 0) {
          this.form.categoryId = categories[0].id;
        }
      },
      error: (error) => this.showError(error)
    });

    this.api.getTags().subscribe({
      next: (tags) => this.tags = tags,
      error: (error) => this.showError(error)
    });

    this.loadNotes();
  }

  loadNotes(): void {
    if (!this.currentUser) {
      return;
    }

    this.loadingNotes = true;
    this.api.getNotes(this.searchQuery, this.selectedCategoryId, this.selectedTagId).subscribe({
      next: (notes) => {
        this.notes = notes;
        this.loadingNotes = false;

        if (this.selectedNote && !notes.some((note) => note.id === this.selectedNote?.id)) {
          this.selectedNote = null;
          this.startCreate();
        }
      },
      error: (error) => {
        this.loadingNotes = false;
        this.showError(error);
      }
    });
  }

  selectNote(note: NoteResponse): void {
    this.api.getNote(note.id).subscribe({
      next: (freshNote) => {
        this.selectedNote = freshNote;
        this.formMode = 'edit';
        this.editingNote = false;
        this.form = {
          title: freshNote.title,
          content: freshNote.content,
          categoryId: freshNote.category.id,
          tags: freshNote.tags.join(', ')
        };
        this.aiResult = null;
        this.successMessage = '';
        this.errorMessage = '';
      },
      error: (error) => this.showError(error)
    });
  }

  startCreate(): void {
    this.formMode = 'create';
    this.selectedNote = null;
    this.form = this.emptyForm();
    this.aiResult = null;
    this.editingNote = true;
    this.successMessage = '';
    this.errorMessage = '';
  }

  saveNote(): void {
    this.clearMessages();

    if (!this.form.title.trim() || !this.form.content.trim() || this.form.categoryId === null) {
      this.errorMessage = 'Title, content, and category are required.';
      return;
    }

    this.saving = true;

    const request: NoteRequest = {
      title: this.form.title.trim(),
      content: this.form.content.trim(),
      categoryId: this.form.categoryId,
      tags: this.form.tags.trim()
    };

    const saveCall = this.formMode === 'create' || this.selectedNote === null
      ? this.api.createNote(request)
      : this.api.updateNote(this.selectedNote.id, request);

    saveCall.subscribe({
      next: (note) => {
        this.saving = false;
        this.successMessage = this.formMode === 'create' ? 'Note created successfully.' : 'Note updated successfully.';
        this.selectedNote = note;
        this.formMode = 'edit';
        this.editingNote = false;
        this.aiResult = null;
        this.reloadAfterChange(note.id);
      },
      error: (error) => {
        this.saving = false;
        this.showError(error);
      }
    });
  }

  deleteSelectedNote(): void {
    if (!this.selectedNote || this.deleting) {
      return;
    }

    const noteId = this.selectedNote.id;
    this.deleting = true;
    this.clearMessages();

    this.api.deleteNote(noteId).subscribe({
      next: () => {
        this.deleting = false;
        this.successMessage = 'Note deleted successfully.';
        this.startCreate();
        this.loadNotes();
        this.refreshDashboard();
      },
      error: (error) => {
        this.deleting = false;
        this.showError(error);
      }
    });
  }

  runAi(action: AiAction): void {
    if (!this.selectedNote || !this.aiStatus.configured || this.aiLoadingAction !== null) {
      return;
    }

    this.aiLoadingAction = action;
    this.aiResult = null;
    this.clearMessages();

    const noteId = this.selectedNote.id;
    const request: Observable<AiTextResponse | AiListResponse> = action === 'summary'
      ? this.api.summarizeNote(noteId)
      : action === 'key-points'
        ? this.api.extractKeyPoints(noteId)
        : action === 'quiz'
          ? this.api.generateQuiz(noteId)
          : this.api.generateQuizAnswers(noteId);

    request.subscribe({
      next: (result: AiTextResponse | AiListResponse) => {
        this.aiLoadingAction = null;
        this.aiResult = this.toAiPanelResult(result);
        this.api.getNote(noteId).subscribe((freshNote: NoteResponse) => this.selectedNote = freshNote);
      },
      error: (error: unknown) => {
        this.aiLoadingAction = null;
        this.showError(error);
      }
    });
  }

  isAiButtonLoading(action: AiAction): boolean {
    return this.aiLoadingAction === action;
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  private emptyForm(): NoteRequest {
    return {
      title: '',
      content: '',
      categoryId: this.categories.length > 0 ? this.categories[0].id : null,
      tags: ''
    };
  }

  private reloadAfterChange(selectedNoteId: number): void {
    this.loadNotes();
    this.refreshDashboard();
    this.api.getTags().subscribe((tags) => this.tags = tags);
    this.api.getNote(selectedNoteId).subscribe((note) => this.selectedNote = note);
  }

  private refreshDashboard(): void {
    this.api.getDashboard().subscribe((dashboard) => this.dashboard = dashboard);
  }

  private toAiPanelResult(result: AiTextResponse | AiListResponse): AiPanelResult {
    if ('text' in result) {
      return {
        title: result.title,
        text: result.text,
        fromCache: result.fromCache
      };
    }

    return {
      title: result.title,
      items: result.items,
      fromCache: result.fromCache
    };
  }

  private resetWorkspace(): void {
    this.dashboard = { noteCount: 0, latestNotes: [] };
    this.aiStatus = { configured: false, hint: 'Log in to check AI status.' };
    this.categories = [];
    this.tags = [];
    this.notes = [];
    this.selectedNote = null;
    this.searchQuery = '';
    this.selectedCategoryId = null;
    this.selectedTagId = null;
    this.formMode = 'create';
    this.form = this.emptyForm();
    this.loadingNotes = false;
    this.saving = false;
    this.deleting = false;
    this.aiLoadingAction = null;
    this.aiResult = null;
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  private showError(error: unknown): void {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401 && this.currentUser) {
        this.api.clearSession();
        this.currentUser = null;
        this.resetWorkspace();
        this.errorMessage = 'Your session expired. Please log in again.';
        return;
      }

      if (error.status === 0) {
        this.errorMessage = 'Cannot reach the backend. Make sure the Spring Boot app is running on port 8080.';
        return;
      }

      const body = error.error as { message?: string; errors?: string[] } | string | null;
      if (typeof body === 'string') {
        try {
          const parsed = JSON.parse(body) as { message?: string; errors?: string[] };
          if (parsed.errors && parsed.errors.length > 0) {
            this.errorMessage = parsed.errors.join(' ');
            return;
          }
          if (parsed.message) {
            this.errorMessage = parsed.message;
            return;
          }
        } catch {
          this.errorMessage = `Request failed with status ${error.status}.`;
          return;
        }
      }

      if (body && typeof body !== 'string' && body.errors && body.errors.length > 0) {
        this.errorMessage = body.errors.join(' ');
        return;
      }
      if (body && typeof body !== 'string' && body.message) {
        this.errorMessage = body.message;
        return;
      }

      if ([502, 503, 504].includes(error.status)) {
        this.errorMessage = 'The backend or AI service did not answer in time. Check docker logs smart-notes-app.';
        return;
      }

      if (error.status >= 400) {
        this.errorMessage = `Request failed with status ${error.status}. Check the backend logs for details.`;
        return;
      }
    }

    this.errorMessage = 'Something went wrong. Please try again.';
  }
}
