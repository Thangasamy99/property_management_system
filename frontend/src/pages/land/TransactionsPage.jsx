import { useCallback, useEffect, useState } from 'react'
import { landFetch } from '../../services/api'
import DataTable from '../../components/DataTable'
import { inputCls } from '../property/PropertiesPage'
import { fmtXaf, label } from './landShared'

const TYPES = [
  'PARCEL_REGISTERED', 'STATUS_CHANGED', 'SURVEY_RECORDED', 'COORDINATES_UPDATED',
  'OWNERSHIP_RECORDED', 'OWNERSHIP_ENDED',
  'ACQUISITION_CREATED', 'ACQUISITION_APPROVED', 'ACQUISITION_COMPLETED', 'ACQUISITION_CANCELLED',
  'SALE_CREATED', 'SALE_SUBMITTED', 'SALE_APPROVED', 'SALE_REJECTED', 'SALE_PROGRESSED', 'SALE_COMPLETED', 'SALE_CANCELLED',
  'TRANSFER_CREATED', 'TRANSFER_SUBMITTED', 'TRANSFER_APPROVED', 'TRANSFER_REJECTED', 'TRANSFER_COMPLETED', 'TRANSFER_CANCELLED',
]

export default function TransactionsPage() {
  const [data, setData] = useState(null)
  const [typeFilter, setTypeFilter] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 15 })
    if (typeFilter) q.set('type', typeFilter)
    if (search) q.set('search', search)
    const res = await landFetch(`/land-transactions?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load transactions — is the Land Service running on :8083?')
    setLoading(false)
  }, [page, typeFilter, search])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  const columns = [
    { label: 'Event' }, { label: 'Reference' }, { label: 'Parcel' },
    { label: 'Amount', className: 'hidden lg:table-cell' }, { label: 'Status' },
    { label: 'Actor' }, { label: 'Date', className: 'hidden xl:table-cell' },
  ]

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-ink">Land Transactions</h2>
        <p className="text-sm text-slate-500">{data ? `${data.totalElements} ledger events` : 'Immutable land ledger'}</p>
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search reference or description…" className={`${inputCls} max-w-xs`} />
        <select value={typeFilter} onChange={(e) => { setTypeFilter(e.target.value); setPage(0) }} className={`${inputCls} w-56`}>
          <option value="">All event types</option>
          {TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No land transactions recorded yet">
        {data?.content.map((t) => (
          <tr key={t.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5">
              <span className="font-medium text-ink">{label(t.transactionType)}</span>
              {t.description && <p className="text-xs text-slate-400">{t.description}</p>}
            </td>
            <td className="px-5 py-3.5 font-mono text-xs text-slate-500">{t.referenceCode ?? '—'}</td>
            <td className="px-5 py-3.5">
              <span className="text-slate-600">{t.parcelName}</span>
              <span className="ml-2 font-mono text-xs text-slate-400">{t.parcelReference}</span>
            </td>
            <td className="hidden px-5 py-3.5 text-slate-600 lg:table-cell">{t.amount != null ? fmtXaf(t.amount) : '—'}</td>
            <td className="px-5 py-3.5 text-xs text-slate-500">{label(t.status)}</td>
            <td className="px-5 py-3.5 text-slate-600">{t.actorName ?? '—'}</td>
            <td className="hidden px-5 py-3.5 text-xs text-slate-400 xl:table-cell">
              {t.createdAt ? new Date(t.createdAt).toLocaleString() : '—'}
            </td>
          </tr>
        ))}
      </DataTable>
    </div>
  )
}
