import { useEffect, useState } from 'react'
import { apiFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'

const EVENT_META = {
  LOGIN_SUCCESS: { style: 'bg-emerald-50 text-emerald-700 ring-emerald-200', icon: 'M5 13l4 4L19 7' },
  LOGIN_FAILED: { style: 'bg-red-50 text-red-600 ring-red-200', icon: 'M6 18L18 6M6 6l12 12' },
  ACCOUNT_LOCKED: { style: 'bg-amber-50 text-amber-700 ring-amber-200', icon: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z' },
  LOGOUT: { style: 'bg-slate-100 text-slate-600 ring-slate-200', icon: 'M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1' },
  TOKEN_REFRESH: { style: 'bg-sky-50 text-sky-700 ring-sky-200', icon: 'M4 4v5h5M20 20v-5h-5M5 9a7 7 0 0114-1M19 15a7 7 0 01-14 1' },
  PASSWORD_CHANGED: { style: 'bg-pcc-50 text-pcc-700 ring-pcc-200', icon: 'M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z' },
  PASSWORD_RESET: { style: 'bg-pcc-50 text-pcc-700 ring-pcc-200', icon: 'M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z' },
}

const EVENT_TYPES = ['LOGIN_SUCCESS', 'LOGIN_FAILED', 'ACCOUNT_LOCKED', 'LOGOUT', 'TOKEN_REFRESH', 'PASSWORD_CHANGED', 'PASSWORD_RESET']

// Best-effort browser/OS label from a User-Agent string
function deviceLabel(ua) {
  if (!ua) return '—'
  const browser = ua.includes('Edg') ? 'Edge' : ua.includes('OPR') || ua.includes('Opera') ? 'Opera'
    : ua.includes('Chrome') ? 'Chrome' : ua.includes('Safari') ? 'Safari'
    : ua.includes('Firefox') ? 'Firefox' : 'Other'
  const os = ua.includes('Windows') ? 'Windows' : ua.includes('Mac') ? 'macOS'
    : ua.includes('Android') ? 'Android' : ua.includes('iPhone') || ua.includes('iPad') ? 'iOS'
    : ua.includes('Linux') ? 'Linux' : ''
  return os ? `${browser} · ${os}` : browser
}

export default function LoginHistoryPage() {
  const { hasPermission } = useAuth()
  const isAdmin = hasPermission('AUDIT_LOG_VIEW')

  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [eventType, setEventType] = useState('')
  const [username, setUsername] = useState('')
  const [search, setSearch] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const t = setTimeout(() => { setUsername(search); setPage(0) }, 400)
    return () => clearTimeout(t)
  }, [search])

  useEffect(() => {
    setLoading(true)
    const params = new URLSearchParams({ page, size: 15, ...(eventType && { eventType }) })
    if (isAdmin) {
      if (username) params.set('username', username)
      if (from) params.set('from', from)
      if (to) params.set('to', to)
    }
    apiFetch(`${isAdmin ? '/login-history' : '/auth/login-history'}?${params}`)
      .then(async (res) => {
        if (!res.ok) throw new Error(`Request failed (${res.status})`)
        let json = await res.json()
        if (!isAdmin) {
          let content = json.content
          if (eventType) content = content.filter((e) => e.eventType === eventType)
          if (from) content = content.filter((e) => e.createdAt >= `${from}T00:00:00`)
          if (to) content = content.filter((e) => e.createdAt < `${to}T23:59:59`)
          json = { ...json, content }
        }
        setData(json)
        setError('')
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [page, eventType, username, from, to, isAdmin])

  return (
    <div className="mx-auto max-w-6xl animate-slide-up">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Login History</h2>
          <p className="text-sm text-slate-500">
            {isAdmin ? 'Security events for all employees.' : 'Security events recorded for your account.'}
          </p>
        </div>
        <div className="flex flex-wrap gap-2.5">
          {isAdmin && (
            <input
              type="search"
              placeholder="Filter by username…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full max-w-[160px] rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm outline-none transition-all focus:border-pcc-500 focus:ring-4 focus:ring-pcc-500/15"
            />
          )}
          <input
            type="date"
            value={from}
            onChange={(e) => { setFrom(e.target.value); setPage(0) }}
            title="From date"
            className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-600 outline-none transition-all focus:border-pcc-500 focus:ring-4 focus:ring-pcc-500/15"
          />
          <input
            type="date"
            value={to}
            onChange={(e) => { setTo(e.target.value); setPage(0) }}
            title="To date"
            className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-600 outline-none transition-all focus:border-pcc-500 focus:ring-4 focus:ring-pcc-500/15"
          />
          <select
            value={eventType}
            onChange={(e) => { setEventType(e.target.value); setPage(0) }}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-600 outline-none transition-all focus:border-pcc-500 focus:ring-4 focus:ring-pcc-500/15"
          >
            <option value="">All events</option>
            {EVENT_TYPES.map((t) => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
          </select>
        </div>
      </div>

      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-sm text-red-600">{error}</p>}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wider text-slate-500">
              <tr>
                {isAdmin && <th className="px-5 py-3.5">Employee</th>}
                <th className="px-5 py-3.5">Event</th>
                <th className="hidden px-5 py-3.5 sm:table-cell">IP Address</th>
                <th className="hidden px-5 py-3.5 lg:table-cell">Device</th>
                <th className="hidden px-5 py-3.5 md:table-cell">Details</th>
                <th className="px-5 py-3.5">Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading
                ? Array.from({ length: 8 }).map((_, i) => (
                    <tr key={i}>
                      {Array.from({ length: isAdmin ? 6 : 5 }).map((__, j) => (
                        <td key={j} className="px-5 py-4">
                          <div className="h-4 rounded-md bg-slate-100 animate-shimmer" style={{ backgroundImage: 'linear-gradient(90deg,#f1f5f9 25%,#e2e8f0 50%,#f1f5f9 75%)', backgroundSize: '400px 100%' }} />
                        </td>
                      ))}
                    </tr>
                  ))
                : (data?.content ?? []).map((e, i) => {
                    const meta = EVENT_META[e.eventType] ?? { style: 'bg-slate-100 text-slate-600 ring-slate-200', icon: 'M13 16h-1v-4h-1m1-4h.01' }
                    return (
                      <tr key={e.id} className="transition-colors hover:bg-pcc-50/50 animate-fade-in" style={{ animationDelay: `${i * 25}ms` }}>
                        {isAdmin && (
                          <td className="px-5 py-3.5">
                            <p className="font-medium text-slate-700">{e.employeeName ?? e.username}</p>
                            <p className="text-xs text-slate-400">@{e.username}</p>
                          </td>
                        )}
                        <td className="px-5 py-3.5">
                          <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${meta.style}`}>
                            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                              <path strokeLinecap="round" strokeLinejoin="round" d={meta.icon} />
                            </svg>
                            {e.eventType.replace(/_/g, ' ')}
                          </span>
                        </td>
                        <td className="hidden px-5 py-3.5 font-mono text-xs text-slate-600 sm:table-cell">{e.ipAddress ?? '—'}</td>
                        <td className="hidden max-w-[160px] truncate px-5 py-3.5 text-xs text-slate-500 lg:table-cell" title={e.userAgent}>
                          {deviceLabel(e.userAgent)}
                        </td>
                        <td className="hidden px-5 py-3.5 text-xs text-slate-500 md:table-cell">{e.failureReason ?? '—'}</td>
                        <td className="px-5 py-3.5 text-xs text-slate-500">{new Date(e.createdAt).toLocaleString()}</td>
                      </tr>
                    )
                  })}
              {!loading && !data?.content?.length && (
                <tr><td colSpan={isAdmin ? 6 : 5} className="px-5 py-10 text-center text-sm text-slate-400">No events found</td></tr>
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
    </div>
  )
}
