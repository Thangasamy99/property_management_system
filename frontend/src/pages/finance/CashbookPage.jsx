import { useCallback, useEffect, useState } from 'react'
import { financeFetch } from '../../services/api'
import DataTable from '../../components/DataTable'
import { inputCls } from '../property/PropertiesPage'
import { fmtXaf, fmtDate, label } from './financeShared'
import { Wallet, Landmark } from 'lucide-react'

const DIR = { CREDIT: 'text-emerald-600', DEBIT: 'text-red-500' }

export default function CashbookPage() {
  const [accounts, setAccounts] = useState([])
  const [accountId, setAccountId] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [result, setResult] = useState(null)
  const [page, setPage] = useState(0)

  useEffect(() => {
    Promise.all([
      financeFetch('/cash-accounts').then((r) => (r.ok ? r.json() : [])).catch(() => []),
      financeFetch('/bank-accounts').then((r) => (r.ok ? r.json() : [])).catch(() => []),
    ]).then(([c, b]) => {
      const list = [...c, ...b]
      setAccounts(list)
      if (list.length) setAccountId(String(list[0].id))
    })
  }, [])

  const load = useCallback(async () => {
    if (!accountId) return
    const p = new URLSearchParams({ accountId, page, size: '15' })
    if (from) p.set('from', from)
    if (to) p.set('to', to)
    const res = await financeFetch(`/reports/cashbook?${p}`)
    if (res.ok) setResult(await res.json())
  }, [accountId, from, to, page])

  useEffect(() => { load() }, [load])

  const account = result?.account ?? accounts.find((a) => String(a.id) === accountId)
  const Icon = account?.accountType === 'CASH' ? Wallet : Landmark
  const entries = result?.entries

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select className={`${inputCls} !h-10 w-64`} value={accountId} onChange={(e) => { setAccountId(e.target.value); setPage(0) }}>
          {accounts.map((a) => <option key={a.id} value={a.id}>{a.name} — {a.accountType}</option>)}
        </select>
        <input type="date" className={`${inputCls} !h-10 w-40`} value={from} onChange={(e) => { setFrom(e.target.value); setPage(0) }} />
        <input type="date" className={`${inputCls} !h-10 w-40`} value={to} onChange={(e) => { setTo(e.target.value); setPage(0) }} />
        {account && (
          <div className="ml-auto flex items-center gap-3">
            <span className="rounded-xl bg-slate-100 px-4 py-2 text-sm text-slate-600">Opening: {fmtXaf(result?.openingBalance ?? account.openingBalance)}</span>
            <span className="flex items-center gap-2 rounded-xl bg-pcc-50 px-4 py-2 text-sm font-semibold text-pcc-800">
              <Icon size={15} /> Balance: {fmtXaf(account.balance)}
            </span>
          </div>
        )}
      </div>
      <DataTable
        columns={[{ label: 'Entry' }, { label: 'Type' }, { label: 'Money in' }, { label: 'Money out' }, { label: 'Date' }, { label: 'Source' }, { label: 'Method' }, { label: 'Description' }]}
        rows={entries?.content} loading={accountId && !result}
        page={page} totalPages={entries?.totalPages ?? 0} onPage={setPage}
        empty="No account movements">
        {entries?.content.map((r) => (
          <tr key={r.id} className="border-b border-slate-50 hover:bg-slate-50/60">
            <td className="px-5 py-3.5 font-medium text-slate-700">{r.entryRef}</td>
            <td className="px-5 py-3.5 text-slate-600">{label(r.transactionType)}</td>
            <td className={`px-5 py-3.5 font-semibold ${DIR.CREDIT}`}>{r.direction === 'CREDIT' ? fmtXaf(r.amount) : '—'}</td>
            <td className={`px-5 py-3.5 font-semibold ${DIR.DEBIT}`}>{r.direction === 'DEBIT' ? fmtXaf(r.amount) : '—'}</td>
            <td className="px-5 py-3.5 text-slate-500">{fmtDate(r.transactionDate)}</td>
            <td className="px-5 py-3.5 text-xs text-slate-500">{r.sourceRef ?? '—'}</td>
            <td className="px-5 py-3.5 text-slate-500">{label(r.paymentMethod)}</td>
            <td className="px-5 py-3.5 max-w-56 truncate text-xs text-slate-400">{r.description}</td>
          </tr>
        ))}
      </DataTable>
    </div>
  )
}
