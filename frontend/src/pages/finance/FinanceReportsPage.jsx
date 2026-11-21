import { useCallback, useEffect, useState } from 'react'
import { financeFetch } from '../../services/api'
import { useToast } from '../../context/ToastContext'
import { inputCls } from '../property/PropertiesPage'
import { StatusBadge, fmtXaf, fmtDate, label, PAYMENT_TYPE_LABELS } from './financeShared'
import { Download } from 'lucide-react'

const REPORTS = [
  { key: 'collections', label: 'Collections', desc: 'Posted receipts by type and method, date range' },
  { key: 'rent-arrears', label: 'Rent arrears', desc: 'Open rent schedules with outstanding balances', noDates: true },
  { key: 'expenses', label: 'Expense report', desc: 'Paid expenses by category, date range' },
  { key: 'income-vs-expense', label: 'Income vs expense', desc: 'Totals comparison for the period' },
  { key: 'payment-method-summary', label: 'Payment methods', desc: 'Volume and value by payment channel' },
]

const SummaryCard = ({ k, v }) => (
  <div className="rounded-xl border border-slate-200 bg-white px-4 py-3">
    <p className="text-xs uppercase tracking-wide text-slate-400">{k}</p>
    <p className="mt-1 text-lg font-bold text-slate-800">{v}</p>
  </div>
)

function Table({ cols, rows, empty = 'No data for this period' }) {
  if (!rows?.length) return <div className="rounded-2xl border border-slate-200 bg-white py-10 text-center text-sm text-slate-400">{empty}</div>
  return (
    <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead><tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">
          {cols.map((c) => <th key={c.label} className="px-5 py-3 font-medium">{c.label}</th>)}
        </tr></thead>
        <tbody>{rows.map((r, i) => <tr key={i} className="border-b border-slate-50 hover:bg-slate-50/60">{cols.map((c) => <td key={c.label} className="px-5 py-3.5 text-slate-600">{c.render(r)}</td>)}</tr>)}</tbody>
      </table>
    </div>
  )
}

const toCsv = (name, cols, rows) => {
  const a = document.createElement('a')
  a.href = URL.createObjectURL(new Blob([[cols.map((c) => c.label).join(','), ...rows.map((r) => cols.map((c) => `"${String(c.csv ? c.csv(r) : c.render(r) ?? '').replaceAll('"', '""')}"`).join(','))].join('\n')], { type: 'text/csv' }))
  a.download = name; a.click()
}

export default function FinanceReportsPage() {
  const toast = useToast()
  const [report, setReport] = useState('collections')
  const [from, setFrom] = useState(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10))
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10))
  const [r, setR] = useState(null)
  const [busy, setBusy] = useState(false)

  const def = REPORTS.find((x) => x.key === report)
  const run = useCallback(async () => {
    setBusy(true)
    const url = def.noDates ? `/reports/${report}` : `/reports/${report}?from=${from}&to=${to}`
    const res = await financeFetch(url)
    setBusy(false)
    if (res.ok) setR(await res.json())
    else { setR(null); toast('Report failed', 'error') }
  }, [report, from, to]) // eslint-disable-line
  useEffect(() => { run() }, [run])

  let summary = null, table = null, csvCols = null, csvRows = null

  if (r && report === 'collections') {
    summary = <div className="mb-4 grid gap-3 sm:grid-cols-3">
      <SummaryCard k="Total collected" v={fmtXaf(r.total)} /><SummaryCard k="Transactions" v={r.count} />
      <SummaryCard k="Top method" v={Object.entries(r.byMethod ?? {}).sort((a, b) => b[1] - a[1])[0]?.map(([k]) => label(k))[0] ?? '—'} />
    </div>
    csvCols = [{ label: 'Ref', render: (p) => p.paymentRef }, { label: 'Type', render: (p) => label(p.paymentType) }, { label: 'Payer', render: (p) => p.payerName }, { label: 'Amount', render: (p) => fmtXaf(p.amount), csv: (p) => p.amount }, { label: 'Date', render: (p) => fmtDate(p.paymentDate) }, { label: 'Method', render: (p) => label(p.methodCode) }, { label: 'Status', render: (p) => <StatusBadge value={p.status} />, csv: (p) => p.status }]
    table = <Table cols={csvCols} rows={r.payments} />; csvRows = r.payments
  } else if (r && report === 'rent-arrears') {
    summary = <div className="mb-4 grid gap-3 sm:grid-cols-2"><SummaryCard k="Outstanding total" v={fmtXaf(r.outstandingTotal)} /><SummaryCard k="Open schedules" v={r.count} /></div>
    csvCols = [{ label: 'Target', render: (t) => t.externalReference }, { label: 'Agreement', render: (t) => t.parentReference }, { label: 'Description', render: (t) => t.description }, { label: 'Total', render: (t) => fmtXaf(t.totalAmount), csv: (t) => t.totalAmount }, { label: 'Paid', render: (t) => fmtXaf(t.paidAmount), csv: (t) => t.paidAmount }, { label: 'Outstanding', render: (t) => <span className="font-semibold text-red-600">{fmtXaf(t.outstanding)}</span>, csv: (t) => t.outstanding }]
    table = <Table cols={csvCols} rows={r.targets} empty="No outstanding rent — all schedules settled" />; csvRows = r.targets
  } else if (r && report === 'expenses') {
    summary = <div className="mb-4 grid gap-3 sm:grid-cols-3">
      <SummaryCard k="Total paid" v={fmtXaf(r.total)} />
      {Object.entries(r.byCategory ?? {}).slice(0, 2).map(([k, v]) => <SummaryCard key={k} k={label(k)} v={fmtXaf(v)} />)}
    </div>
    csvCols = [{ label: 'Ref', render: (e) => e.expenseRef }, { label: 'Category', render: (e) => label(e.category) }, { label: 'Vendor', render: (e) => e.vendorName ?? '—' }, { label: 'Amount', render: (e) => fmtXaf(e.amount), csv: (e) => e.amount }, { label: 'Paid', render: (e) => fmtDate(e.paidDate) }, { label: 'Status', render: (e) => <StatusBadge value={e.status} />, csv: (e) => e.status }]
    table = <Table cols={csvCols} rows={r.expenses?.content} />; csvRows = r.expenses?.content
  } else if (r && report === 'income-vs-expense') {
    summary = <div className="mb-4 grid gap-3 sm:grid-cols-3">
      <SummaryCard k="Income" v={fmtXaf(r.income)} /><SummaryCard k="Expenses" v={fmtXaf(r.expenses)} /><SummaryCard k="Net" v={fmtXaf(r.net)} />
    </div>
    csvCols = [{ label: 'Bucket', render: (x) => x.bucket }, { label: 'Amount', render: (x) => fmtXaf(x.amount), csv: (x) => x.amount }]
    const rows = [...Object.entries(r.incomeByType ?? {}).map(([k, v]) => ({ bucket: `Income — ${PAYMENT_TYPE_LABELS[k] ?? label(k)}`, amount: v })), ...Object.entries(r.expenseByCategory ?? {}).map(([k, v]) => ({ bucket: `Expense — ${label(k)}`, amount: v }))]
    table = <Table cols={csvCols} rows={rows} />; csvRows = rows
  } else if (r && report === 'payment-method-summary') {
    csvCols = [{ label: 'Method', render: (x) => x.method }, { label: 'Transactions', render: (x) => x.count }, { label: 'Total', render: (x) => fmtXaf(x.amount), csv: (x) => x.amount }]
    const rows = Object.entries(r.amountsByMethod ?? {}).map(([k, v]) => ({ method: label(k), amount: v, count: r.countByMethod?.[k] ?? 0 }))
    table = <Table cols={csvCols} rows={rows} />; csvRows = rows
  }

  return (
    <div>
      <div className="mb-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {REPORTS.map((x) => (
          <button key={x.key} onClick={() => setReport(x.key)}
            className={`rounded-2xl border p-4 text-left transition-colors ${report === x.key ? 'border-pcc-300 bg-pcc-50 ring-1 ring-pcc-200' : 'border-slate-200 bg-white hover:bg-slate-50'}`}>
            <p className="text-sm font-semibold text-slate-800">{x.label}</p>
            <p className="mt-0.5 text-xs text-slate-400">{x.desc}</p>
          </button>
        ))}
      </div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        {!def.noDates && (
          <>
            <input type="date" className={`${inputCls} !h-10 w-44`} value={from} onChange={(e) => setFrom(e.target.value)} />
            <span className="text-xs text-slate-400">to</span>
            <input type="date" className={`${inputCls} !h-10 w-44`} value={to} onChange={(e) => setTo(e.target.value)} />
          </>
        )}
        <button onClick={run} className="h-10 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white hover:bg-pcc-900">{busy ? 'Running…' : 'Run report'}</button>
        {csvRows?.length > 0 && (
          <button onClick={() => toCsv(`${report}-${from}-${to}.csv`, csvCols, csvRows)} className="flex h-10 items-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-medium text-slate-600 hover:bg-slate-50">
            <Download size={14} /> CSV
          </button>
        )}
      </div>
      {busy ? <div className="h-40 animate-pulse rounded-2xl bg-slate-100" /> : <>{summary}{table}</>}
    </div>
  )
}
