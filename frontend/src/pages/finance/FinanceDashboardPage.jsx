import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { financeFetch } from '../../services/api'
import { fmtXaf, fmtDate, label, PAYMENT_TYPE_LABELS } from './financeShared'
import { Banknote, TrendingUp, TrendingDown, Landmark, Wallet, ReceiptText, ArrowRight } from 'lucide-react'

function StatCard({ icon: Icon, label: l, value, tone = 'text-slate-800', sub }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5">
      <div className="flex items-center gap-3">
        <div className="rounded-xl bg-pcc-50 p-2.5 text-pcc-700"><Icon size={18} /></div>
        <span className="text-xs font-medium uppercase tracking-wide text-slate-500">{l}</span>
      </div>
      <p className={`mt-3 text-xl font-bold ${tone}`}>{value}</p>
      {sub && <p className="mt-0.5 text-xs text-slate-400">{sub}</p>}
    </div>
  )
}

export default function FinanceDashboardPage() {
  const [d, setD] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    financeFetch('/dashboard/summary')
      .then(async (r) => (r.ok ? setD(await r.json()) : setError('Failed to load dashboard')))
      .catch(() => setError('Finance Service unavailable'))
  }, [])

  if (error) return <div className="rounded-2xl border border-red-100 bg-red-50 p-6 text-sm text-red-600">{error}</div>
  if (!d) return <div className="space-y-4">{[...Array(3)].map((_, i) => <div key={i} className="h-28 animate-pulse rounded-2xl bg-slate-100" />)}</div>

  const maxBar = Math.max(1, ...d.monthlySeries.map((m) => Math.max(Number(m.income), Number(m.expenses))))

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Banknote} label="Collections today" value={fmtXaf(d.collectionsToday)} />
        <StatCard icon={TrendingUp} label="Collections this month" value={fmtXaf(d.collectionsThisMonth)} />
        <StatCard icon={TrendingDown} label="Expenses this month" value={fmtXaf(d.expensesThisMonth)} sub={`Today: ${fmtXaf(d.expensesToday)}`} />
        <StatCard icon={ReceiptText} label="Outstanding rent" value={fmtXaf(d.outstandingRent)} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Landmark} label="Land acquisition paid" value={fmtXaf(d.landAcquisitionPaid)} />
        <StatCard icon={Landmark} label="Land sale receipts" value={fmtXaf(d.landSaleReceipts)} />
        <StatCard icon={Wallet} label="Cash balance" value={fmtXaf(d.cashBalance)} />
        <StatCard icon={Wallet} label="Bank balance" value={fmtXaf(d.bankBalance)} />
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h3 className="mb-4 text-sm font-semibold text-slate-700">Income vs expenses — last 6 months</h3>
          <div className="space-y-3">
            {d.monthlySeries.map((m) => (
              <div key={m.month}>
                <div className="mb-1 flex justify-between text-xs text-slate-500">
                  <span>{m.month}</span>
                  <span>{fmtXaf(m.income)} / {fmtXaf(m.expenses)}</span>
                </div>
                <div className="flex gap-1">
                  <div className="h-2.5 rounded bg-pcc-600" style={{ width: `${(Number(m.income) / maxBar) * 100}%` }} />
                  <div className="h-2.5 rounded bg-rose-300" style={{ width: `${(Number(m.expenses) / maxBar) * 100}%` }} />
                </div>
              </div>
            ))}
          </div>
          <div className="mt-3 flex gap-4 text-[11px] text-slate-400">
            <span className="flex items-center gap-1"><i className="h-2 w-2 rounded-full bg-pcc-600" /> Income</span>
            <span className="flex items-center gap-1"><i className="h-2 w-2 rounded-full bg-rose-300" /> Expenses</span>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-slate-700">Recent posted payments</h3>
            <Link to="/finance/receipts" className="flex items-center gap-1 text-xs font-medium text-pcc-700 hover:underline">Receipts <ArrowRight size={12} /></Link>
          </div>
          <div className="divide-y divide-slate-50">
            {d.recentTransactions.length === 0 && <p className="py-8 text-center text-sm text-slate-400">No payments posted yet</p>}
            {d.recentTransactions.map((t) => (
              <div key={t.ref} className="flex items-center justify-between py-2.5 text-sm">
                <div>
                  <p className="font-medium text-slate-700">{t.payer}</p>
                  <p className="text-xs text-slate-400">{t.ref} · {PAYMENT_TYPE_LABELS[t.type] ?? label(t.type)} · {fmtDate(t.date)}</p>
                </div>
                <span className="font-semibold text-slate-800">{fmtXaf(t.amount)}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
