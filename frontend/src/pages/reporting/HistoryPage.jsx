import { useEffect, useState } from 'react'
import { rpFetch, fmtDate } from './rpShared.jsx'

/** Report execution + export audit trail. */
export default function HistoryPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [err, setErr] = useState(null)

  useEffect(() => {
    (async () => {
      const res = await rpFetch(`/reports/execution-history?page=${page}&size=25`)
      if (!res.ok) { setErr(`HTTP ${res.status}`); return }
      setData(await res.json())
    })()
  }, [page])

  if (err) return <p className="p-6 text-sm text-red-600">{err}</p>
  if (!data) return <p className="p-6 text-sm text-slate-500">Loading…</p>
  const rows = data.content ?? []

  return (
    <div className="space-y-3">
      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-slate-200 bg-violet-50/60">
              {['Report', 'Domain', 'Executed by', 'At', 'Rows', 'Source health', 'Duration'].map((h) => (
                <th key={h} className="px-3 py-2 font-semibold text-slate-700">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id} className="border-b border-slate-100 hover:bg-slate-50">
                <td className="px-3 py-2 font-medium">{r.reportName}</td>
                <td className="px-3 py-2">{r.reportKey.split('-')[0].toUpperCase()}</td>
                <td className="px-3 py-2">{r.executedBy}</td>
                <td className="px-3 py-2">{fmtDate(r.createdAt)}</td>
                <td className="px-3 py-2">{r.rowCount}</td>
                <td className="px-3 py-2">
                  <span className={`rounded-full px-2 py-0.5 font-semibold ${
                    r.sourceHealth === 'OK' ? 'bg-emerald-50 text-emerald-700'
                      : r.sourceHealth === 'PARTIAL' ? 'bg-amber-50 text-amber-700'
                      : 'bg-red-50 text-red-700'}`}>{r.sourceHealth}</span>
                </td>
                <td className="px-3 py-2">{r.durationMs} ms</td>
              </tr>
            ))}
            {!rows.length && <tr><td colSpan={7} className="px-3 py-8 text-center text-slate-400">No executions yet.</td></tr>}
          </tbody>
        </table>
      </div>
      <div className="flex gap-2 text-xs">
        <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={!page}
          className="rounded border border-slate-200 px-2 py-1 disabled:opacity-40">Prev</button>
        <span className="py-1 text-slate-500">Page {page + 1}</span>
        <button onClick={() => setPage((p) => p + 1)} disabled={data.last}
          className="rounded border border-slate-200 px-2 py-1 disabled:opacity-40">Next</button>
      </div>
    </div>
  )
}
