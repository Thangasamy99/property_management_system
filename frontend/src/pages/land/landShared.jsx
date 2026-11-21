export const LAND_STATUS_STYLES = {
  AVAILABLE: 'bg-emerald-50 text-emerald-700',
  RESERVED: 'bg-sky-50 text-sky-700',
  UNDER_SALE: 'bg-amber-50 text-amber-700',
  SOLD: 'bg-slate-100 text-slate-500',
  UNDER_TRANSFER: 'bg-violet-50 text-violet-700',
  DISPUTED: 'bg-red-50 text-red-600',
  INACTIVE: 'bg-slate-100 text-slate-400',
}

export const WORKFLOW_STATUS_STYLES = {
  DRAFT: 'bg-slate-100 text-slate-600',
  PENDING: 'bg-amber-50 text-amber-700',
  SUBMITTED: 'bg-sky-50 text-sky-700',
  UNDER_REVIEW: 'bg-sky-50 text-sky-700',
  APPROVED: 'bg-emerald-50 text-emerald-700',
  REJECTED: 'bg-red-50 text-red-600',
  PAYMENT_PENDING: 'bg-amber-50 text-amber-700',
  LEGAL_PROCESSING: 'bg-violet-50 text-violet-700',
  COMPLETED: 'bg-emerald-50 text-emerald-700',
  CANCELLED: 'bg-slate-100 text-slate-400',
  CURRENT: 'bg-emerald-50 text-emerald-700',
  ENDED: 'bg-slate-100 text-slate-500',
}

export const fmtXaf = (n) => (n == null ? '—' : `${Number(n).toLocaleString('en-US')} XAF`)
export const fmtArea = (sqm, orig, unit) =>
  orig != null && unit ? `${Number(orig).toLocaleString()} ${unit === 'SQUARE_METRE' ? 'm²' : unit === 'HECTARE' ? 'ha' : 'ac'} (${Number(sqm).toLocaleString()} m²)` : `${Number(sqm ?? 0).toLocaleString()} m²`
export const label = (s) => (s ?? '—').replaceAll('_', ' ')

export function StatusBadge({ value, styles = WORKFLOW_STATUS_STYLES }) {
  return (
    <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${styles[value] ?? 'bg-slate-100 text-slate-500'}`}>
      {label(value)}
    </span>
  )
}
