import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { docFetch, fmtDate, fmtSize, ExpiryBadge } from './docShared'

const BUCKETS = [
  { key: 'expired', label: 'Expired' },
  { key: '30', label: 'Within 30 days' },
  { key: '60', label: '31–60 days' },
  { key: 'none', label: 'No expiry set' },
]

export default function ExpiringDocumentsPage() {
  const [bucket, setBucket] = useState('30')
  const [rows, setRows] = useState([])
  const [summary, setSummary] = useState(null)

  useEffect(() => {
    docFetch('/documents/expiry/summary').then((r) => (r.ok ? r.json() : null)).then(setSummary)
  }, [])
  useEffect(() => {
    docFetch(`/documents/expiry?bucket=${bucket}`).then((r) => (r.ok ? r.json() : [])).then(setRows)
  }, [bucket])

  const counts = { expired: summary?.expired, 30: summary?.expiringWithin30, 60: summary?.expiringWithin60, none: summary?.noExpiry }

  return (
    <div>
      <div className="mb-4 grid grid-cols-4 gap-3">
        {BUCKETS.map((b) => (
          <button key={b.key} onClick={() => setBucket(b.key)}
                  className={`rounded-2xl border p-4 text-left shadow-sm transition ${bucket === b.key ? 'border-pcc-400 bg-pcc-50' : 'border-slate-200 bg-white hover:border-pcc-200'}`}>
            <p className="text-xl font-bold text-slate-800">{counts[b.key] ?? '—'}</p>
            <p className="text-xs text-slate-500">{b.label}</p>
          </button>
        ))}
      </div>
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
            <tr><th className="px-4 py-3">Reference</th><th className="px-4 py-3">Filename</th><th className="px-4 py-3">Category</th><th className="px-4 py-3">Expiry</th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {rows.map((d) => (
              <tr key={d.id} className="hover:bg-slate-50">
                <td className="px-4 py-3"><Link to={`/documents/${d.id}`} className="font-semibold text-pcc-700 hover:underline">{d.documentRef}</Link></td>
                <td className="px-4 py-3"><p className="max-w-[280px] truncate text-slate-700">{d.originalFilename}</p><p className="text-[11px] text-slate-400">{fmtSize(d.fileSize)}</p></td>
                <td className="px-4 py-3 text-xs text-slate-600">{d.categoryName}</td>
                <td className="px-4 py-3"><ExpiryBadge doc={d} /> <span className="ml-2 text-xs text-slate-400">{fmtDate(d.expiryDate)}</span></td>
              </tr>
            ))}
            {rows.length === 0 && <tr><td colSpan={4} className="px-4 py-10 text-center text-slate-400">No documents in this bucket</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}
