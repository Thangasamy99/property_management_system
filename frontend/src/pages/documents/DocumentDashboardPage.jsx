import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { docFetch, fmtDate, StatusBadge } from './docShared'
import { useAuth } from '../../context/AuthContext'
import { FileText, Archive, Clock4, AlertTriangle, FolderOpen, Upload } from 'lucide-react'

const DOMAIN_TILES = [
  { domain: 'PROPERTY', label: 'Property Documents', to: '/documents/property', color: 'bg-pcc-50 text-pcc-700' },
  { domain: 'LAND', label: 'Land Documents', to: '/documents/land', color: 'bg-emerald-50 text-emerald-700' },
  { domain: 'FINANCE', label: 'Finance Documents', to: '/documents/finance', color: 'bg-amber-50 text-amber-700' },
]

export default function DocumentDashboardPage() {
  const { hasPermission } = useAuth()
  const [summary, setSummary] = useState(null)
  const [recent, setRecent] = useState([])
  const [expiring, setExpiring] = useState([])

  useEffect(() => {
    docFetch('/dashboard').then((r) => (r.ok ? r.json() : null)).then(setSummary)
    docFetch('/documents?size=8&status=ACTIVE').then((r) => (r.ok ? r.json() : { content: [] }))
      .then((d) => setRecent(d.content ?? []))
    if (hasPermission('DOCUMENT_EXPIRY_VIEW'))
      docFetch('/documents/expiry?bucket=30').then((r) => (r.ok ? r.json() : [])).then(setExpiring)
  }, [])

  const card = 'rounded-2xl border border-slate-200 bg-white p-5 shadow-sm'
  const stat = (icon, label, value, tone) => (
    <div className={card}>
      <div className="flex items-center gap-3">
        <div className={`rounded-xl p-2.5 ${tone}`}>{icon}</div>
        <div>
          <p className="text-2xl font-bold text-slate-800">{value ?? '—'}</p>
          <p className="text-xs text-slate-500">{label}</p>
        </div>
      </div>
    </div>
  )

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {stat(<FileText size={20} />, 'Active documents', summary?.activeDocuments, 'bg-pcc-50 text-pcc-700')}
        {stat(<AlertTriangle size={20} />, 'Expired', summary?.expiry?.expired, 'bg-red-50 text-red-600')}
        {stat(<Clock4 size={20} />, 'Expiring ≤ 30 days', summary?.expiry?.expiringWithin30, 'bg-amber-50 text-amber-700')}
        {stat(<Archive size={20} />, 'Archived', summary?.archivedDocuments, 'bg-slate-100 text-slate-500')}
      </div>

      <div className="grid grid-cols-3 gap-4">
        {DOMAIN_TILES.map((t) => (
          <Link key={t.domain} to={t.to}
                className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:border-pcc-300">
            <div className={`rounded-xl p-2.5 ${t.color}`}><FolderOpen size={18} /></div>
            <span className="text-sm font-semibold text-slate-700">{t.label}</span>
          </Link>
        ))}
      </div>

      {expiring.length > 0 && (
        <div className={card}>
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-slate-700">Expiring within 30 days</h3>
            <Link to="/documents/expiring" className="text-xs font-medium text-pcc-700 hover:underline">View all</Link>
          </div>
          <div className="space-y-1.5">
            {expiring.slice(0, 5).map((d) => (
              <Link key={d.id} to={`/documents/${d.id}`}
                    className="flex items-center justify-between rounded-lg px-3 py-2 text-sm hover:bg-slate-50">
                <span className="font-medium text-slate-700">{d.documentRef} <span className="font-normal text-slate-400">· {d.originalFilename}</span></span>
                <span className="text-xs font-semibold text-amber-700">{fmtDate(d.expiryDate)}</span>
              </Link>
            ))}
          </div>
        </div>
      )}

      <div className={card}>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-700">Recent uploads</h3>
          <Link to="/documents/all" className="text-xs font-medium text-pcc-700 hover:underline">View all</Link>
        </div>
        <div className="divide-y divide-slate-100">
          {recent.map((d) => (
            <Link key={d.id} to={`/documents/${d.id}`} className="flex items-center justify-between py-2.5 text-sm hover:bg-slate-50">
              <div className="min-w-0">
                <p className="truncate font-medium text-slate-700">{d.originalFilename}</p>
                <p className="text-[11px] text-slate-400">{d.documentRef} · {d.categoryName} · {d.uploadedByName}</p>
              </div>
              <StatusBadge value={d.status} />
            </Link>
          ))}
          {recent.length === 0 && <p className="py-6 text-center text-sm text-slate-400">No documents uploaded yet</p>}
        </div>
      </div>
    </div>
  )
}
