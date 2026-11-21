import { useCallback, useEffect, useState } from 'react'
import { financeFetch } from '../../services/api'
import DataTable from '../../components/DataTable'
import { inputCls } from '../property/PropertiesPage'
import { StatusBadge, fmtXaf, fmtDate, label } from './financeShared'

const DIR_STYLES = { CREDIT: 'text-emerald-600', DEBIT: 'text-red-500' }

export default function LedgerPage() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [filters, setFilters] = useState({ from: '', to: '', type: '', direction: '', search: '' })

  const load = useCallback(async () => {
    const p = new URLSearchParams({ page, size: '15' })
    Object.entries(filters).forEach(([k, v]) => v && p.set(k, v))
    const res = await financeFetch(`/ledger?${p}`)
    if (res.ok) setData(await res.json())
  }, [page, filters])

  useEffect(() => { load() }, [load])

  const set = (k, v) => { setPage(0); setFilters((f) => ({ ...f, [k]: v })) }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <input type="date" className={`${inputCls} !h-10 w-40`} value={filters.from} onChange={(e) => set('from', e.target.value)} />
        <input type="date" className={`${inputCls} !h-10 w-40`} value={filters.to} onChange={(e) => set('to', e.target.value)} />
        <select className={`${inputCls} !h-10 w-44`} value={filters.direction} onChange={(e) => set('direction', e.target.value)}>
          <option value="">All directions</option><option value="CREDIT">Credit</option><option value="DEBIT">Debit</option>
        </select>
        <select className={`${inputCls} !h-10 w-52`} value={filters.type} onChange={(e) => set('type', e.target.value)}>
          <option value="">All types</option>
          {['RECEIPT', 'REFUND', 'REVERSAL', 'EXPENSE_PAYOUT', 'ADJUSTMENT'].map((t) => <option key={t}>{label(t)}</option>)}
        </select>
        <input className={`${inputCls} !h-10 w-56`} placeholder="Search ref…" value={filters.search} onChange={(e) => set('search', e.target.value)} />
      </div>
      <DataTable
        columns={[{ label: 'Ledger Ref' }, { label: 'Type' }, { label: 'Direction' }, { label: 'Amount' }, { label: 'Date' }, { label: 'Source' }, { label: 'Method' }, { label: 'Status' }, { label: 'Posted' }]}
        rows={data?.content} loading={!data}
        page={page} totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No ledger entries">
        {data?.content.map((r) => (
          <tr key={r.id} className="border-b border-slate-50 hover:bg-slate-50/60">
            <td className="px-5 py-3.5 font-medium text-slate-700">{r.entryRef}</td>
            <td className="px-5 py-3.5 text-slate-600">{label(r.transactionType)}</td>
            <td className={`px-5 py-3.5 font-semibold ${DIR_STYLES[r.direction]}`}>{r.direction}</td>
            <td className="px-5 py-3.5 font-semibold">{fmtXaf(r.amount)}</td>
            <td className="px-5 py-3.5 text-slate-500">{fmtDate(r.transactionDate)}</td>
            <td className="px-5 py-3.5 text-xs text-slate-500">{r.sourceRef ?? '—'}</td>
            <td className="px-5 py-3.5 text-slate-500">{label(r.paymentMethod)}</td>
            <td className="px-5 py-3.5"><StatusBadge value={r.postingStatus} /></td>
            <td className="px-5 py-3.5 text-xs text-slate-500">{r.postedAt ? fmtDate(r.postedAt) : '—'}</td>
          </tr>
        ))}
      </DataTable>
    </div>
  )
}
