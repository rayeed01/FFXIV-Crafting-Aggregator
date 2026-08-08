import type {
  AddRecipeRequest,
  AuthResponse,
  CraftCostNode,
  CreateSavedCraftRequest,
  DataCenterDto,
  ErrorResponse,
  FieldError,
  GameServerSyncResult,
  ItemDto,
  LoginRequest,
  Quality,
  RecipeDto,
  RecipeSummaryDto,
  RegisterRequest,
  RemoveRecipeRequest,
  SavedCraftCostDto,
  SavedCraftDto,
  SavedCraftSummaryDto,
  SyncStatus,
  UpdateSavedCraftRequest,
  UpdateUserRequest,
  UserDto,
  Uuid,
  WorldDto,
} from '@/types/api'

/**
 * Relative by default so the Vite dev proxy handles it and requests stay same-origin.
 * Override with VITE_API_BASE_URL when the API is deployed to a different host.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export const TOKEN_STORAGE_KEY = 'ffxiv-token'

/**
 * A failed request, carrying the backend's ErrorResponse detail.
 *
 * `fieldErrors` is populated for 400s from bean validation, letting forms mark the exact
 * offending input instead of dumping "Validation Failed" into a toast.
 */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldError[]

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }

  /** Message for a specific form field, if the backend rejected that field. */
  fieldError(field: string): string | undefined {
    return this.fieldErrors.find((e) => e.field === field)?.message
  }

  /** 503 from GameServerDataNotSyncedException - worlds have never been synced. */
  get isNotSynced(): boolean {
    return this.status === 503
  }
}

// The token lives here rather than being read from localStorage per request, so an expired
// session cleared in memory cannot be resurrected by a stale storage entry mid-flight.
let authToken: string | null = null
let onUnauthorized: (() => void) | null = null

export function setAuthToken(token: string | null): void {
  authToken = token
}

/** Registered by AuthProvider so a 401 anywhere can clear the session exactly once. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler
}

export function readStoredToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY)
  } catch {
    return null // private browsing
  }
}

export function writeStoredToken(token: string | null): void {
  try {
    if (token === null) localStorage.removeItem(TOKEN_STORAGE_KEY)
    else localStorage.setItem(TOKEN_STORAGE_KEY, token)
  } catch {
    /* non-fatal: the session just will not survive a reload */
  }
}

interface RequestOptions {
  method?: string
  body?: unknown
  signal?: AbortSignal
  /** Skip the 401 session-clear, so the login screen's own failures do not loop. */
  skipAuthRedirect?: boolean
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, signal, skipAuthRedirect } = options

  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (authToken) headers.Authorization = `Bearer ${authToken}`

  const response = await fetch(`${BASE_URL}/api/v1${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  })

  if (response.ok) {
    // 204 from DELETE, and any empty body, would throw on .json().
    if (response.status === 204) return undefined as T
    const text = await response.text()
    return (text ? JSON.parse(text) : undefined) as T
  }

  // JwtAuthFilter failures bypass GlobalExceptionHandler (a documented gap in that class), so
  // an error body is not guaranteed to be our ErrorResponse shape - fall back to the status.
  let message = `Request failed (${response.status})`
  let fieldErrors: FieldError[] = []
  try {
    const text = await response.text()
    if (text) {
      const parsed = JSON.parse(text) as Partial<ErrorResponse>
      if (typeof parsed.message === 'string' && parsed.message) message = parsed.message
      if (Array.isArray(parsed.errors)) fieldErrors = parsed.errors
    }
  } catch {
    /* non-JSON error body - keep the status-derived message */
  }

  if (response.status === 401 && !skipAuthRedirect) {
    onUnauthorized?.()
    if (message.startsWith('Request failed')) message = 'Your session has expired. Please sign in again.'
  }

  throw new ApiError(response.status, message, fieldErrors)
}

export const api = {
  auth: {
    register: (payload: RegisterRequest) =>
      request<AuthResponse>('/auth/register', { method: 'POST', body: payload, skipAuthRedirect: true }),
    login: (payload: LoginRequest) =>
      request<AuthResponse>('/auth/login', { method: 'POST', body: payload, skipAuthRedirect: true }),
  },

  users: {
    me: (signal?: AbortSignal) => request<UserDto>('/users/me', { signal }),
    updateDefaults: (payload: UpdateUserRequest) =>
      request<UserDto>('/users/me/defaults', { method: 'PATCH', body: payload }),
  },

  items: {
    search: (query: string, signal?: AbortSignal) =>
      request<ItemDto[]>(`/items?search=${encodeURIComponent(query)}`, { signal }),
    byId: (id: Uuid, signal?: AbortSignal) => request<ItemDto>(`/items/${id}`, { signal }),
  },

  recipes: {
    search: (query: string, signal?: AbortSignal) =>
      request<RecipeSummaryDto[]>(`/recipes?search=${encodeURIComponent(query)}`, { signal }),
    byJob: (job: string, signal?: AbortSignal) =>
      request<RecipeSummaryDto[]>(`/recipes?job=${encodeURIComponent(job)}`, { signal }),
    byId: (id: Uuid, signal?: AbortSignal) => request<RecipeDto>(`/recipes/${id}`, { signal }),
  },

  craftCost: {
    /**
     * `scope` accepts either a world or a data center name; the backend canonicalises it.
     * `quality` applies to the requested item only, not to its ingredients.
     */
    calculate: (
      itemXivapiId: number,
      quantity: number,
      scope: string,
      quality: Quality = 'CHEAPEST',
      signal?: AbortSignal,
    ) =>
      request<CraftCostNode>(
        `/craft-cost/${itemXivapiId}?quantity=${quantity}&scope=${encodeURIComponent(scope)}&quality=${quality}`,
        { signal },
      ),
  },

  worlds: {
    list: (signal?: AbortSignal) => request<WorldDto[]>('/worlds', { signal }),
    dataCenters: (signal?: AbortSignal) => request<DataCenterDto[]>('/data-centers', { signal }),
  },

  savedCrafts: {
    list: (signal?: AbortSignal) => request<SavedCraftSummaryDto[]>('/saved-crafts', { signal }),
    byId: (id: Uuid, signal?: AbortSignal) => request<SavedCraftDto>(`/saved-crafts/${id}`, { signal }),
    create: (payload: CreateSavedCraftRequest) =>
      request<SavedCraftDto>('/saved-crafts', { method: 'POST', body: payload }),
    update: (id: Uuid, payload: UpdateSavedCraftRequest) =>
      request<SavedCraftDto>(`/saved-crafts/${id}`, { method: 'PATCH', body: payload }),
    remove: (id: Uuid) => request<void>(`/saved-crafts/${id}`, { method: 'DELETE' }),
    addRecipes: (id: Uuid, payload: AddRecipeRequest) =>
      request<SavedCraftDto>(`/saved-crafts/${id}/recipes`, { method: 'POST', body: payload }),
    removeRecipes: (id: Uuid, payload: RemoveRecipeRequest) =>
      request<SavedCraftDto>(`/saved-crafts/${id}/recipes`, { method: 'DELETE', body: payload }),
    cost: (id: Uuid, signal?: AbortSignal) =>
      request<SavedCraftCostDto>(`/saved-crafts/${id}/cost`, { signal }),
  },

  admin: {
    syncStatus: (signal?: AbortSignal) => request<SyncStatus>('/admin/sync/recipe', { signal }),
    startRecipeSync: () => request<SyncStatus>('/admin/sync/recipe', { method: 'POST' }),
    syncWorlds: () => request<GameServerSyncResult>('/admin/sync/worlds', { method: 'POST' }),
  },
}
