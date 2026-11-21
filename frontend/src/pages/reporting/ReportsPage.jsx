import { useEffect, useMemo, useState } from 'react'
import { rpFetch, rpDownload, fmtDate, xaf, num, SourceHealth } from './rpShared.jsx'

const DOMAIN_LABELS = {
  PROPERTY: 'Property', LAND: 'Land', FINANCE: 'Finance',
  WORKFLOW: 'Workflow', DOCUMENT: 'Document', IDENTITY: 'Identity',
}

/** Generic report runner: pick a registered report, apply filters, view table, export. */
export default function ReportsPage({ domain }) {
  const [defs, setDefs] = useState(null)
  const [defErr, setDefErr] = useState(null)
  const [key, setKey] = useState('')
  const [from, setFrom] = useState(''); const [to, setTo] = useState('')
  const [status, setStatus] = useState('')
  const [state, setState] = useState({ loading: false, error: null, result: null })
  const [page, setPage] = useState(0)
  const PAGE = 25

  useEffect(() => {
    (async () => {
      const res = await rpFetch(`/reports/definitions${domain ? `?domain=${domain}` : ''}`)
      if (!res.ok) { setDefErr(res.status === 403 ? 'No report permissions for this domain.' : `HTTP ${res.status}`); return }
      const list = await res.json()
      setDefs(list); if (list.length) setKey(list[0].key)
    })()
  }, [domain])

  const params = useMemo(() => {
    const q = new URLSearchParams()
    if (from) q.set('from', from); if (to) q.set('to', to); if (status) q.set('status', status)
    return q.toString()
  }, [from, to, status])

  const run = async () => {
    if (!key) return
    setState({ loading: true, error: null, result: null }); setPage(0)
    const res = await rpFetch(`/reports/${domain?.toLowerCase() ?? 'run'}/${key}${params ? `?${params}` : ''}`)
    if (!res.ok) {
      const j = await res.json().catch(() => ({}))
      setState({ loading: false, error: j.message ?? `HTTP ${res.status}`, result: null }); return
    }
    setState({ loading: false, error: null, result: await res.json() })
  }

  const doExport = async (fmt) => {
    const res = await rpDownload(`/reports/${key}/export?format=${fmt}${params ? `&${params}` : ''}`)
    if (!res.ok) { setState((s) => ({ ...s, error: `Export failed (HTTP ${res.status})` })); return }
    const blob = await res.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `pcc-${key}.${fmt === 'XLSX' ? 'xlsx' : fmt.toLowerCase()}`
    a.click(); URL.revokeObjectURL(a.href)
  }

  const r = state.result
  const rows = r?.rows ?? []
  const pageRows = rows.slice(page * PAGE, (page + 1) * PAGE)

  if (defErr) return <p className="p-6 text-sm text-red-600">{defErr}</p>
  if (!defs) return <p className="p-6 text-sm text-slate-500">Loading reports…</p>
  if (!defs.length) return <p className="p-6 text-sm text-slate-500">No reports available for your role in this domain.</p>

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <div className="min-w-56 flex-1">
          <label className="mb-1 block text-xs font-medium text-slate-500">Report</label>
          <select value={key} onChange={(e) => setKey(e.target.value)}
            className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm">
            {defs.map((d) => <option key={d.key} value={d.key}>{d.name}</option>)}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">From</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)}
            className="rounded-lg border border-slate-200 px-2 py-2 text-sm" />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">To</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)}
            className="rounded-lg border border-slate-200 px-2 py-2 text-sm" />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">Status</label>
          <input type="text" value={status} onChange={(e) => setStatus(e.target.value)} placeholder="e.g. ACTIVE"
            className="w-28 rounded-lg border border-slate-200 px-2 py-2 text-sm" />
        </div>
        <button onClick={run} disabled={state.loading}
          className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-semibold text-white hover:bg-violet-700 disabled:opacity-50">
          {state.loading ? 'Running…' : 'Run report'}
        </button>
        <div className="flex gap-1">
          {['CSV', 'XLSX', 'PDF'].map((f) => (
            <button key={f} onClick={() => doExport(f)} disabled={!r}
              className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-600 hover:border-violet-300 disabled:opacity-40">
              {f}
            </button>
          ))}
        </div>
      </div>

      {state.error && <p className="text-sm text-red-600">{state.error}</p>}
      {r && (
        <>
          <SourceHealth sources={r.sources} generatedAt={r.generatedAt} />
          <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 bg-violet-50/60">
                  {r.columns.map((c) => <th key={c.key} className="px-3 py-2 font-semibold text-slate-700">{c.label}</th>)}
                </tr>
              </thead>
              <tbody>
                {pageRows.map((row, i) => (
                  <tr key={i} className="border-b border-slate-100 hover:bg-slate-50">
                    {r.columns.map((c) => (
                      <td key={c.key} className="px-3 py-2 text-slate-700">
                        {c.type === 'money' ? xaf(row[c.key])
                          : c.type === 'date' || c.type === 'datetime' ? fmtDate(row[c.key])
                          : c.type === 'int' ? num(row[c.key])
                          : (row[c.key] ?? '—')}
                      </td>
                    ))}
                  </tr>
                ))}
                {!pageRows.length && (
                  <tr><td colSpan={r.columns.length} className="px-3 py-8 text-center text-slate-400">
                    {r.sources?.every(s => s.status !== 'OK') ? 'Source unavailable — no data.' : 'No rows for these filters.'}
                  </td></tr>
                )}
              </tbody>
              {Object.keys(r.totals ?? {}).length > 0 && (
                <tfoot>
                  <tr className="bg-slate-50 font-semibold">
                    {r.columns.map((c, i) => (
                      <td key={c.key} className="px-3 py-2">
                        {i === 0 ? 'TOTAL' : r.totals[c.key] != null
                          ? (c.type === 'money' ? xaf(r.totals[c.key]) : num(r.totals[c.key])) : ''}
                      </td>
                    ))}
                  </tr>
                </tfoot>
              )}
            </table>
          </div>
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>{rows.length} rows{r.truncated ? ' (truncated)' : ''}</span>
            {rows.length > PAGE && (
              <div className="flex gap-2">
                <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={!page}
                  className="rounded border border-slate-200 px-2 py-1 disabled:opacity-40">Prev</button>
                <span className="py-1">Page {page + 1} / {Math.ceil(rows.length / PAGE)}</span>
                <button onClick={() => setPage((p) => p + 1)} disabled={(page + 1) * PAGE >= rows.length}
                  className="rounded border border-slate-200 px-2 py-1 disabled:opacity-40">Next</button>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
