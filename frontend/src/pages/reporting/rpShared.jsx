import { useState } from 'react'
import { rpFetch, rpDownload } from '../../services/api'
export { rpFetch, rpDownload }

export const fmtDate = (v) => (v ? new Date(v).toLocaleString('en-GB', {
  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
}) : '—')

export const xaf = (v) => v == null ? '—'
  : 'XAF ' + Number(v).toLocaleString('en-US', { maximumFractionDigits: 0 })

export const num = (v) => v == null ? '—' : Number(v).toLocaleString()

export function Kpi({ label, value, tone = 'text-slate-800' }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <p className="text-[11px] font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className={`mt-1 text-2xl font-semibold ${tone}`}>{value ?? '—'}</p>
    </div>
  )
}

export function Section({ title, children, warn }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-800">{title}</h3>
        {warn && <span className="rounded-full bg-amber-50 px-2.5 py-0.5 text-[11px] font-semibold text-amber-700">partial data</span>}
      </div>
      {children}
    </div>
  )
}

/** Per-source freshness/status banner — surfaces DOWN services, never hides them. */
export function SourceHealth({ sources, generatedAt }) {
  if (!sources?.length) return null
  return (
    <div className="mb-4 flex flex-wrap items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs">
      <span className="font-medium text-slate-500">Generated {fmtDate(generatedAt)}</span>
      <span className="text-slate-300">|</span>
      {sources.map((s, i) => (
        <span key={i} title={s.error ?? `fetched ${fmtDate(s.fetchedAt)}`}
          className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 font-semibold ${
            s.status === 'OK' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
          <span className={`h-1.5 w-1.5 rounded-full ${s.status === 'OK' ? 'bg-emerald-500' : 'bg-red-500'}`} />
          {s.service}{s.cached ? ' (cached)' : ''}{s.status !== 'OK' ? ' — unavailable' : ''}
        </span>
      ))}
    </div>
  )
}

const BARS = ['#7c3aed', '#a78bfa', '#c4b5fd', '#8b5cf6', '#6d28d9', '#5b21b6']

/** Simple pure-SVG vertical bar chart for {label, value} pairs. */
export function BarChart({ data, money = false }) {
  const items = (data ?? []).filter((d) => d.value != null && Number(d.value) > 0)
  if (!items.length) return <p className="py-6 text-center text-xs text-slate-400">No data</p>
  const max = Math.max(...items.map((d) => Number(d.value)))
  return (
    <div className="flex items-end gap-2 overflow-x-auto pb-1">
      {items.map((d, i) => (
        <div key={i} className="flex min-w-[52px] flex-1 flex-col items-center gap-1">
          <span className="text-[10px] font-semibold text-slate-600">
            {money ? xaf(d.value) : num(d.value)}
          </span>
          <svg width="100%" height={100} viewBox="0 0 40 100" preserveAspectRatio="none">
            <rect x="4" y={100 - (Number(d.value) / max) * 96} width="32"
              height={(Number(d.value) / max) * 96} rx="4"
              fill={BARS[i % BARS.length]} />
          </svg>
          <span className="max-w-full truncate text-[10px] text-slate-500">{d.label}</span>
        </div>
      ))}
    </div>
  )
}

export const PERIODS = [
  { key: 'today', label: 'Today' }, { key: 'week', label: 'This week' },
  { key: 'month', label: 'This month' }, { key: 'quarter', label: 'This quarter' },
  { key: 'year', label: 'This year' }, { key: 'custom', label: 'Custom' },
]

export function PeriodPicker({ period, setPeriod, from, to, setFrom, setTo }) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      {PERIODS.map((p) => (
        <button key={p.key} onClick={() => setPeriod(p.key)}
          className={`rounded-lg px-3 py-1.5 text-xs font-semibold ${
            period === p.key ? 'bg-violet-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:border-violet-300'}`}>
          {p.label}
        </button>
      ))}
      {period === 'custom' && (
        <>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)}
            className="rounded-lg border border-slate-200 px-2 py-1.5 text-xs" />
          <span className="text-xs text-slate-400">to</span>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)}
            className="rounded-lg border border-slate-200 px-2 py-1.5 text-xs" />
        </>
      )}
    </div>
  )
}

export function useLoaded() {
  const [state, setState] = useState({ loading: true, error: null, data: null })
  const load = async (path) => {
    setState((s) => ({ ...s, loading: true, error: null }))
    const res = await rpFetch(path)
    if (!res.ok) {
      setState({ loading: false, error: res.status === 403
        ? 'You do not have permission to view this.' : `Load failed (HTTP ${res.status})`, data: null })
      return
    }
    setState({ loading: false, error: null, data: await res.json() })
  }
  return [state, load]
}
