import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { landFetch } from '../../services/api'
import { fmtXaf, label } from './landShared'
import { Map, HandCoins, ArrowLeftRight, LandPlot, LoaderPinwheel } from 'lucide-react'

const CARDS = [
  { key: 'totalParcels', label: 'Registered Parcels', icon: Map, to: '/land-parcels' },
  { key: 'totalAreaSqm', label: 'Total Land Area (m²)', icon: LandPlot },
  { key: 'totalSales', label: 'Sale Records', icon: HandCoins, to: '/land-sales' },
  { key: 'pendingTransfers', label: 'Pending Transfers', icon: ArrowLeftRight, to: '/land-transfers' },
]

export default function LandDashboardPage() {
  const [summary, setSummary] = useState(null)
  const [recent, setRecent] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    landFetch('/land-parcels/summary').then(async (r) => {
      if (r?.ok) setSummary(await r.json())
      else setError('Could not load the land summary — is the Land Service running on :8083?')
    }).catch(() => setError('Could not load the land summary — is the Land Service running on :8083?'))
    landFetch('/land-transactions?page=0&size=6').then(async (r) => {
      if (r?.ok) setRecent((await r.json()).content ?? [])
    }).catch(() => {})
  }, [])

  const value = (key) => {
    if (!summary) return '—'
    if (key === 'totalAreaSqm') return Number(summary.totalAreaSqm ?? 0).toLocaleString()
    return summary[key] ?? 0
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-bold text-ink">Land Management</h2>
        <p className="text-sm text-slate-500">Registry, surveys, ownership and transactions</p>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      {!summary && !error && (
        <div className="flex items-center gap-2 text-sm text-slate-400"><LoaderPinwheel className="h-4 w-4 animate-spin" /> Loading…</div>
      )}

      {summary && (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {CARDS.map(({ key, label: l, icon: Icon, to }) => {
              const card = (
                <div className="rounded-2xl border border-slate-200 bg-white p-5 transition-all hover:-translate-y-0.5 hover:shadow-md hover:shadow-pcc-100">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-pcc-50 text-pcc-700">
                    <Icon className="h-4.5 w-4.5" />
                  </div>
                  <p className="mt-3 text-2xl font-bold text-ink">{value(key)}</p>
                  <p className="text-xs font-medium text-slate-500">{l}</p>
                </div>
              )
              return to ? <Link key={key} to={to}>{card}</Link> : <div key={key}>{card}</div>
            })}
          </div>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            <div className="rounded-2xl border border-slate-200 bg-white p-5">
              <h3 className="text-sm font-semibold text-ink">Parcels by status</h3>
              <div className="mt-3 flex flex-wrap gap-2">
                {Object.entries(summary.parcelsByStatus ?? {}).map(([s, n]) => (
                  <span key={s} className="rounded-full bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600">
                    {label(s)} <b className="ml-1 text-pcc-700">{n}</b>
                  </span>
                ))}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5">
              <h3 className="text-sm font-semibold text-ink">Recent activity</h3>
              <ul className="mt-3 space-y-2.5">
                {(recent ?? []).map((t) => (
                  <li key={t.id} className="flex items-center justify-between text-sm">
                    <div className="min-w-0">
                      <span className="font-medium text-slate-700">{label(t.transactionType)}</span>
                      <span className="ml-2 truncate font-mono text-xs text-slate-400">{t.referenceCode}</span>
                    </div>
                    <span className="shrink-0 text-xs text-slate-400">
                      {t.amount != null ? fmtXaf(t.amount) : (t.createdAt ? new Date(t.createdAt).toLocaleDateString() : '')}
                    </span>
                  </li>
                ))}
                {recent?.length === 0 && <li className="text-sm text-slate-400">No activity yet — register a parcel to get started.</li>}
                {recent === null && <li className="text-sm text-slate-400">Loading…</li>}
              </ul>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
