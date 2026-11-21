import { wfFetch } from '../../services/api'
export { wfFetch }

export const fmtDate = (v) => (v ? new Date(v).toLocaleString('en-GB', {
  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
}) : '—')

export const STATUS_TONES = {
  DRAFT: 'bg-slate-100 text-slate-600',
  SUBMITTED: 'bg-sky-50 text-sky-700',
  PENDING_APPROVAL: 'bg-amber-50 text-amber-700',
  APPROVED: 'bg-emerald-50 text-emerald-700',
  REJECTED: 'bg-red-50 text-red-600',
  RETURNED_FOR_CORRECTION: 'bg-orange-50 text-orange-700',
  CANCELLED: 'bg-slate-100 text-slate-500',
  COMPLETED: 'bg-emerald-50 text-emerald-700',
  INTEGRATION_PENDING: 'bg-indigo-50 text-indigo-700',
  INTEGRATION_FAILED: 'bg-red-50 text-red-600',
  PENDING: 'bg-amber-50 text-amber-700',
  RETURNED: 'bg-orange-50 text-orange-700',
  SKIPPED: 'bg-slate-100 text-slate-500',
  SENT: 'bg-emerald-50 text-emerald-700',
  FAILED: 'bg-red-50 text-red-600',
}

export function StatusBadge({ value }) {
  const tone = STATUS_TONES[value] ?? 'bg-slate-100 text-slate-600'
  return <span className={`inline-flex rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${tone}`}>
    {(value ?? '').replaceAll('_', ' ')}
  </span>
}

export const MODULES = ['PROPERTY', 'LAND', 'FINANCE', 'DOCUMENT']
export const PRIORITIES = ['LOW', 'NORMAL', 'HIGH', 'URGENT']
export const ROLES = ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'PROPERTY_OFFICER', 'LAND_OFFICER']
