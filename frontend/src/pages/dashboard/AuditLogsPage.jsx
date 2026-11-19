import { useEffect, useState } from 'react'
import { apiFetch } from '../../services/api'
import Modal from '../../components/Modal'

const ACTION_STYLES = {
  USER_CREATE: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  USER_UPDATE: 'bg-sky-50 text-sky-700 ring-sky-200',
  USER_STATUS_ACTIVE: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  USER_STATUS_INACTIVE: 'bg-amber-50 text-amber-700 ring-amber-200',
  USER_ASSIGN_ROLE: 'bg-pcc-50 text-pcc-700 ring-pcc-200',
  USER_PASSWORD_RESET: 'bg-orange-50 text-orange-700 ring-orange-200',
  ROLE_CREATE: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  ROLE_UPDATE: 'bg-sky-50 text-sky-700 ring-sky-200',
  ROLE_ASSIGN_PERMISSION: 'bg-pcc-50 text-pcc-700 ring-pcc-200',
  PASSWORD_CHANGE: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  PASSWORD_RESET: 'bg-orange-50 text-orange-700 ring-orange-200',
}

const FILTERS = [
  ['', 'All actions'],
  ['USER_CREATE', 'User created'],
  ['USER_UPDATE', 'User updated'],
  ['USER_STATUS_ACTIVE', 'Activated'],
  ['USER_STATUS_INACTIVE', 'Deactivated'],
  ['USER_ASSIGN_ROLE', 'Role assignment'],
  ['USER_PASSWORD_RESET', 'Password reset'],
  ['ROLE_CREATE', 'Role created'],
  ['ROLE_UPDATE', 'Role updated'],
  ['ROLE_ASSIGN_PERMISSION', 'Permission change'],
  ['PASSWORD_CHANGE', 'Password change'],
]

export default function AuditLogsPage() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [action, setAction] = useState('')
  const [entityType, setEntityType] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [detail, setDetail] = useState(null)

  useEffect(() => {
    setLoading(true)
    const params = new URLSearchParams({ page, size: 15 })
    if (action) params.set('action', action)
    if (entityType) params.set('entityType', entityType)
    apiFetch(`/audit-logs?${params}`)
      .then(async (res) => {
        if (!res.ok) throw new Error(`Request failed (${res.status})`)
        setData(await res.json())
        setError('')
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [page, action, entityType])

  return (
    <div className="mx-auto max-w-6xl animate-slide-up">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Audit Logs</h2>
          <p className="text-sm text-slate-500">Every administrative change recorded by the Identity Service.</p>
        </div>
        <div className="flex gap-2.5">
          <select
            value={action}
            onChange={(e) => { setAction(e.target.value); setPage(0) }}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-600 outline-none transition-all focus:border-pcc-500 focus:ring-4 focus:ring-pcc-500/15"
          >
            {FILTERS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
          <select
            value={entityType}
            onChange={(e) => { setEntityType(e.target.value); setPage(0) }}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-600 outline-none transition-all focus:border-pcc-500 focus:ring-4 focus:ring-pcc-500/15"
          >
            <option value="">All entities</option>
            <option value="USER">User</option>
            <option value="ROLE">Role</option>
          </select>
        </div>
      </div>

      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-sm text-red-600">{error}</p>}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th className="px-5 py-3.5">Actor</th>
                <th className="px-5 py-3.5">Action</th>
                <th className="hidden px-5 py-3.5 sm:table-cell">Entity</th>
                <th className="hidden px-5 py-3.5 md:table-cell">Details</th>
                <th className="px-5 py-3.5">Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading
                ? Array.from({ length: 8 }).map((_, i) => (
                    <tr key={i}>
                      {Array.from({ length: 5 }).map((__, j) => (
                        <td key={j} className="px-5 py-4">
                          <div className="h-4 rounded-md bg-slate-100 animate-shimmer" style={{ backgroundImage: 'linear-gradient(90deg,#f1f5f9 25%,#e2e8f0 50%,#f1f5f9 75%)', backgroundSize: '400px 100%' }} />
                        </td>
                      ))}
                    </tr>
                  ))
                : (data?.content ?? []).map((e, i) => (
                    <tr key={e.id} onClick={() => setDetail(e)} className="cursor-pointer transition-colors hover:bg-pcc-50/50 animate-fade-in" style={{ animationDelay: `${i * 25}ms` }}>
                      <td className="px-5 py-3.5 font-medium text-slate-700">{e.actorUsername ?? 'system'}</td>
                      <td className="px-5 py-3.5">
                        <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ring-1 ${ACTION_STYLES[e.action] ?? 'bg-slate-100 text-slate-600 ring-slate-200'}`}>
                          {e.action.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="hidden px-5 py-3.5 text-xs text-slate-500 sm:table-cell">{e.entityType} #{e.entityId}</td>
                      <td className="hidden max-w-xs truncate px-5 py-3.5 text-xs text-slate-500 md:table-cell" title={e.details}>{e.details}</td>
                      <td className="px-5 py-3.5 text-xs text-slate-500">{new Date(e.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
              {!loading && !data?.content?.length && (
                <tr><td colSpan="5" className="px-5 py-10 text-center text-sm text-slate-400">No audit events found</td></tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="flex items-center justify-between border-t border-slate-200 px-5 py-3 text-xs text-slate-500">
          <span>{data?.totalElements ?? 0} events · page {(data?.page ?? 0) + 1} of {Math.max(data?.totalPages ?? 1, 1)}</span>
          <div className="flex gap-2">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}
              className="rounded-lg border border-slate-200 px-3 py-1.5 font-medium transition-colors hover:bg-slate-50 disabled:opacity-40">Prev</button>
            <button disabled={page >= (data?.totalPages ?? 1) - 1} onClick={() => setPage((p) => p + 1)}
              className="rounded-lg border border-slate-200 px-3 py-1.5 font-medium transition-colors hover:bg-slate-50 disabled:opacity-40">Next</button>
          </div>
        </div>
      </div>

      {detail && (
        <Modal open onClose={() => setDetail(null)} title="Audit Event Details">
          <dl className="space-y-3 text-sm">
            {[
              ['Actor', detail.actorUsername ?? 'system'],
              ['Action', detail.action],
              ['Module', detail.entityType],
              ['Record', `#${detail.entityId}`],
              ['Timestamp', new Date(detail.createdAt).toLocaleString()],
            ].map(([k, v]) => (
              <div key={k} className="flex justify-between gap-4 border-b border-slate-50 pb-2.5 last:border-0">
                <dt className="text-slate-400">{k}</dt>
                <dd className="font-medium text-slate-700">{v}</dd>
              </div>
            ))}
          </dl>
          <div className="mt-4">
            <p className="mb-1.5 text-xs font-semibold uppercase tracking-wider text-slate-400">Description</p>
            <p className="whitespace-pre-wrap rounded-xl bg-slate-50 p-4 font-mono text-xs leading-relaxed text-slate-600 ring-1 ring-slate-200">
              {detail.details ?? '—'}
            </p>
          </div>
        </Modal>
      )}
    </div>
  )
}
