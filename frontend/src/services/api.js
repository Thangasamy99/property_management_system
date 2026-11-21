const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8081/api/v1'
const PROPERTY_API_BASE = import.meta.env.VITE_PROPERTY_API_URL ?? 'http://localhost:8082/api/v1'
const LAND_API_BASE = import.meta.env.VITE_LAND_API_URL ?? 'http://localhost:8083/api/v1'
const FINANCE_API_BASE = import.meta.env.VITE_FINANCE_API_URL ?? 'http://localhost:8084/api/v1'
const DOCUMENT_API_BASE = import.meta.env.VITE_DOCUMENT_API_URL ?? 'http://localhost:8085/api/v1'
const WORKFLOW_API_BASE = import.meta.env.VITE_WORKFLOW_API_URL ?? 'http://localhost:8086/api/v1'
const REPORTING_API_BASE = import.meta.env.VITE_REPORTING_API_URL ?? 'http://localhost:8087/api/v1'

// Access token lives in memory only (never localStorage).
// Session restore happens through the HttpOnly refresh cookie.
let accessToken = null

export const tokenStore = {
  get: () => accessToken,
  set: (t) => { accessToken = t },
  clear: () => { accessToken = null },
}

async function rawFetch(path, options = {}) {
  return baseFetch(API_BASE, path, options)
}

async function baseFetch(base, path, options = {}) {
  return fetch(`${base}${path}`, {
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

/** Same auth contract against the Property Service (:8082). */
export async function propertyFetch(path, options = {}, retry = true) {
  const res = await baseFetch(PROPERTY_API_BASE, path, options)
  if (res.status === 401 && retry) {
    const restored = await refreshSession()
    if (restored) return propertyFetch(path, options, false)
  }
  return res
}

/** Same auth contract against the Land Service (:8083). */
export async function landFetch(path, options = {}, retry = true) {
  const res = await baseFetch(LAND_API_BASE, path, options)
  if (res.status === 401 && retry) {
    const restored = await refreshSession()
    if (restored) return landFetch(path, options, false)
  }
  return res
}

/** Same auth contract against the Finance Service (:8084). */
export async function financeFetch(path, options = {}, retry = true) {
  const res = await baseFetch(FINANCE_API_BASE, path, options)
  if (res.status === 401 && retry) {
    const restored = await refreshSession()
    if (restored) return financeFetch(path, options, false)
  }
  return res
}

/** Same auth contract against the Document Service (:8085). */
export async function docFetch(path, options = {}, retry = true) {
  const res = await baseFetch(DOCUMENT_API_BASE, path, options)
  if (res.status === 401 && retry) {
    const restored = await refreshSession()
    if (restored) return docFetch(path, options, false)
  }
  return res
}

/** Same auth contract against the Workflow Service (:8086). */
export async function wfFetch(path, options = {}, retry = true) {
  const res = await baseFetch(WORKFLOW_API_BASE, path, options)
  if (res.status === 401 && retry) {
    const restored = await refreshSession()
    if (restored) return wfFetch(path, options, false)
  }
  return res
}

/** Same auth contract against the Reporting Service (:8087). */
export async function rpFetch(path, options = {}, retry = true) {
  const res = await baseFetch(REPORTING_API_BASE, path, options)
  if (res.status === 401 && retry) {
    const restored = await refreshSession()
    if (restored) return rpFetch(path, options, false)
  }
  return res
}

/** Binary download (exports) against the Reporting Service. */
export async function rpDownload(path, retry = true) {
  const res = await fetch(`${REPORTING_API_BASE}${path}`, {
    credentials: 'include',
    headers: tokenStore.get() ? { Authorization: `Bearer ${tokenStore.get()}` } : {},
  })
  if (res.status === 401 && retry) {
    const restored = await refreshSession()
    if (restored) return rpDownload(path, false)
  }
  return res
}

/** Multipart upload - lets fetch set the boundary Content-Type itself. */
export async function docUpload(path, formData, retry = true) {
  const res = await fetch(`${DOCUMENT_API_BASE}${path}`, {
    method: 'POST',
    credentials: 'include',
    headers: { ...(tokenStore.get() ? { Authorization: `Bearer ${tokenStore.get()}` } : {}) },
    body: formData,
  })
  if (res.status === 401 && retry) {
    const restored = await refreshSession()
    if (restored) return docUpload(path, formData, false)
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
