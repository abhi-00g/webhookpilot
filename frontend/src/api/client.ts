const API_BASE = '/api'

let token: string | null = localStorage.getItem('whp_token')

export function setToken(t: string | null) {
  token = t
  if (t) localStorage.setItem('whp_token', t)
  else localStorage.removeItem('whp_token')
}

export function getToken() {
  return token
}

async function request<T>(path: string, opts: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(opts.headers as Record<string, string>),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${API_BASE}${path}`, { ...opts, headers })

  if (res.status === 401) {
    setToken(null)
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(err.error || `HTTP ${res.status}`)
  }

  if (res.status === 204) return undefined as T
  return res.json()
}

// Auth
export const auth = {
  register: (email: string, password: string) =>
    request<{ token: string; email: string }>('/auth/register', {
      method: 'POST', body: JSON.stringify({ email, password }),
    }),
  login: (email: string, password: string) =>
    request<{ token: string; email: string }>('/auth/login', {
      method: 'POST', body: JSON.stringify({ email, password }),
    }),
}

// Applications
export const apps = {
  list: () => request<App[]>('/applications'),
  get: (id: string) => request<App>(`/applications/${id}`),
  create: (name: string) =>
    request<App>('/applications', { method: 'POST', body: JSON.stringify({ name }) }),
  delete: (id: string) =>
    request<void>(`/applications/${id}`, { method: 'DELETE' }),
}

// Event Types
export const eventTypes = {
  list: (appId: string) => request<EventType[]>(`/applications/${appId}/event-types`),
  create: (appId: string, name: string, description?: string) =>
    request<EventType>(`/applications/${appId}/event-types`, {
      method: 'POST', body: JSON.stringify({ name, description }),
    }),
  delete: (appId: string, id: string) =>
    request<void>(`/applications/${appId}/event-types/${id}`, { method: 'DELETE' }),
}

// Endpoints
export const endpoints = {
  list: (appId: string) => request<Endpoint[]>(`/applications/${appId}/endpoints`),
  create: (appId: string, url: string, description?: string) =>
    request<Endpoint>(`/applications/${appId}/endpoints`, {
      method: 'POST', body: JSON.stringify({ url, description }),
    }),
  update: (appId: string, id: string, data: Partial<{ url: string; description: string; status: string }>) =>
    request<Endpoint>(`/applications/${appId}/endpoints/${id}`, {
      method: 'PATCH', body: JSON.stringify(data),
    }),
  delete: (appId: string, id: string) =>
    request<void>(`/applications/${appId}/endpoints/${id}`, { method: 'DELETE' }),
  setSubscriptions: (appId: string, endpointId: string, eventTypeIds: string[]) =>
    request<Endpoint>(`/applications/${appId}/endpoints/${endpointId}/event-types`, {
      method: 'PUT', body: JSON.stringify({ eventTypeIds }),
    }),
}

// Events
export const events = {
  list: (appId: string, page = 0, size = 20) =>
    request<PageResponse<Event>>(`/applications/${appId}/events?page=${page}&size=${size}`),
  get: (appId: string, id: string) =>
    request<Event>(`/applications/${appId}/events/${id}`),
}

// Types
export interface App {
  id: string; name: string; apiKey: string;
  createdAt: string; updatedAt: string;
}
export interface EventType {
  id: string; name: string; description: string | null; createdAt: string;
}
export interface Endpoint {
  id: string; url: string; secret: string; description: string | null;
  status: 'ACTIVE' | 'PAUSED' | 'DISABLED';
  subscribedEventTypes: EventType[]; createdAt: string; updatedAt: string;
}
export interface DeliveryAttempt {
  id: string; endpointId: string; attemptNumber: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'EXHAUSTED';
  httpStatusCode: number | null; responseBody: string | null;
  errorMessage: string | null; nextRetryAt: string | null; createdAt: string;
}
export interface Event {
  id: string; eventType: string; payload: Record<string, unknown>;
  idempotencyKey: string | null; createdAt: string;
  deliveryAttempts: DeliveryAttempt[];
}
export interface PageResponse<T> {
  content: T[]; totalElements: number; totalPages: number;
  number: number; size: number;
}
