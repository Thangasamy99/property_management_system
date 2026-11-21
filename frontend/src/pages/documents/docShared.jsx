import { docFetch, docUpload, tokenStore } from '../../services/api'

export const DOC_STATUS_STYLES = {
  ACTIVE: 'bg-emerald-50 text-emerald-700',
  ARCHIVED: 'bg-slate-100 text-slate-500',
}

export const DOMAIN_LABELS = {
  PROPERTY: 'Property',
  LAND: 'Land',
  FINANCE: 'Finance',
  ADMINISTRATION: 'Administration',
}

export const ENTITY_TYPE_LABELS = {
  PROPERTY: 'Property',
  BUILDING: 'Building',
  APARTMENT: 'Apartment',
  RENTAL_AGREEMENT: 'Rental agreement',
  TENANT: 'Tenant',
  LAND_PARCEL: 'Land parcel',
  LAND_SURVEY: 'Land survey',
  LAND_ACQUISITION: 'Land acquisition',
  LAND_SALE: 'Land sale',
  LAND_TRANSFER: 'Land transfer',
  FINANCE_PAYMENT: 'Finance payment',
  EXPENSE: 'Expense',
  RECEIPT: 'Receipt',
  OTHER: 'Other',
}

export const fmtSize = (n) => {
  if (n == null) return '—'
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / (1024 * 1024)).toFixed(2)} MB`
}
export const fmtDate = (d) => (d ? new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '—')
export const label = (s) => (s ?? '—').replaceAll('_', ' ')

export function StatusBadge({ value }) {
  return (
    <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${DOC_STATUS_STYLES[value] ?? 'bg-slate-100 text-slate-500'}`}>
      {label(value)}
    </span>
  )
}

export function ExpiryBadge({ doc }) {
  if (!doc.expiryDate) return <span className="text-xs text-slate-400">—</span>
  const days = Math.ceil((new Date(doc.expiryDate) - Date.now()) / 86400000)
  if (doc.expired || days < 0)
    return <span className="rounded-full bg-red-50 px-2.5 py-1 text-[11px] font-semibold text-red-600">Expired</span>
  if (days <= 30)
    return <span className="rounded-full bg-amber-50 px-2.5 py-1 text-[11px] font-semibold text-amber-700">{days}d left</span>
  if (days <= 60)
    return <span className="rounded-full bg-sky-50 px-2.5 py-1 text-[11px] font-semibold text-sky-700">{days}d left</span>
  return <span className="text-xs text-slate-500">{fmtDate(doc.expiryDate)}</span>
}

export function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-slate-600">{label}</span>
      {children}
    </label>
  )
}

/** Fetches a binary with the bearer token — browser open/download flows. */
export async function docFileUrl(path) {
  const res = await fetch(`http://localhost:8085/api/v1${path}`, {
    credentials: 'include',
    headers: { Authorization: `Bearer ${tokenStore.get()}` },
  })
  if (!res.ok) throw new Error(`File request failed: ${res.status}`)
  return URL.createObjectURL(await res.blob())
}

export async function downloadDocument(id, filename, version) {
  const v = version ? `?version=${version}` : ''
  const url = await docFileUrl(`/documents/${id}/download${v}`)
  const a = document.createElement('a')
  a.href = url
  a.download = filename ?? 'document'
  a.click()
  URL.revokeObjectURL(url)
}

export async function previewDocument(id, version) {
  const v = version ? `?version=${version}` : ''
  const url = await docFileUrl(`/documents/${id}/preview${v}`)
  window.open(url, '_blank')
}

export async function fetchCategories() {
  const res = await docFetch('/categories')
  return res.ok ? res.json() : []
}

export function canPreview(mime) {
  return mime === 'application/pdf' || (mime ?? '').startsWith('image/')
}

export { docFetch, docUpload }
