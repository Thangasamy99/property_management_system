const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8081/api/v1'

// Access token lives in memory only (never localStorage).
// Session restore happens through the HttpOnly refresh cookie.
let accessToken = null

export const tokenStore = {
  get: () => accessToken,
  set: (t) => { accessToken = t },
  clear: () => { accessToken = null },
}

async function rawFetch(path, options = {}) {
  return fetch(`${API_BASE}${path}`, {
    credentials: 'include', // send the HttpOnly refresh cookie
    headers: {
      'Content-Type': 'application/json',
      ...(tokenStore.get() ? { Authorization: `Bearer ${tokenStore.get()}` } : {}),
      ...(options.headers ?? {}),
    },
    ...options,
  })
}

export async function apiFetch(path, options = {}, retry = true) {
  const res = await rawFetch(path, options)
  if (res.status === 401 && retry) {
    const restored = await refreshSession()
    if (restored) return apiFetch(path, options, false)
  }
  return res
}

export async function loginApi(username, password) {
  const res = await rawFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new ApiError(data.message ?? 'Login failed', res.status)
  tokenStore.set(data.accessToken)
  return data
}

export async function refreshSession() {
  const res = await rawFetch('/auth/refresh', { method: 'POST', body: '{}' })
  if (!res.ok) {
    tokenStore.clear()
    return false
  }
  const data = await res.json()
  tokenStore.set(data.accessToken)
  return data.user
}

export async function logoutApi() {
  await rawFetch('/auth/logout', { method: 'POST', body: '{}' }).catch(() => {})
  tokenStore.clear()
}

export async function meApi() {
  const res = await apiFetch('/auth/me')
  if (!res.ok) throw new ApiError('Failed to load profile', res.status)
  return res.json()
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
}

export { API_BASE }
