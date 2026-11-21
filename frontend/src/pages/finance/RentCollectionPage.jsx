import { useCallback, useEffect, useState } from 'react'
import { financeFetch, propertyFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { inputCls } from '../property/PropertiesPage'
import { StatusBadge, fmtXaf, fmtDate, label, RecordPaymentModal } from './financeShared'
import { Search, HandCoins } from 'lucide-react'

const SCHED_STYLES = { PENDING: 'bg-amber-50 text-amber-700', PARTIAL: 'bg-sky-50 text-sky-700', PAID: 'bg-emerald-50 text-emerald-700', OVERDUE: 'bg-red-50 text-red-600', CANCELLED: 'bg-slate-100 text-slate-400' }

export default function RentCollectionPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const canCollect = hasPermission('RENT_COLLECTION_CREATE')
  const canPost = hasPermission('RENT_COLLECTION_APPROVE')

  // agreement search (Property Service)
  const [q, setQ] = useState('')
  const [agreements, setAgreements] = useState([])
  const [searching, setSearching] = useState(false)
  const [agreement, setAgreement] = useState(null)
  const [schedules, setSchedules] = useState([])
  const [targets, setTargets] = useState({})
  const [payCtx, setPayCtx] = useState(null)

  // payments list (Finance Service)
  const [payments, setPayments] = useState(null)
  const [page, setPage] = useState(0)
  const [paySearch, setPaySearch] = useState('')

  const searchAgreements = async (e) => {
    e?.preventDefault()
    setSearching(true)
    const res = await propertyFetch(`/rental-agreements?size=10${q ? `&search=${encodeURIComponent(q)}` : ''}`)
    setSearching(false)
    if (res.ok) {
      const d = await res.json()
      setAgreements(d.content ?? d)
    } else toast('Property Service search failed', 'error')
  }

  const loadTarget = async (scheduleId) => {
    const res = await financeFetch(`/rent-collections/targets/${scheduleId}`)
    if (res.ok) {
      const t = await res.json()
      setTargets((prev) => ({ ...prev, [scheduleId]: t }))
    }
  }

  const pick = async (a) => {
    setAgreement(a)
    const res = await propertyFetch(`/rental-agreements/${a.id}/schedules`)
    if (res.ok) {
      const list = await res.json()
      setSchedules(list)
      list.forEach((s) => loadTarget(s.id))
    }
  }

  const loadPayments = useCallback(async () => {
    const res = await financeFetch(`/rent-collections/payments?page=${page}&size=10${paySearch ? `&search=${encodeURIComponent(paySearch)}` : ''}`)
    if (res.ok) setPayments(await res.json())
  }, [page, paySearch])

  useEffect(() => { loadPayments() }, [loadPayments])
  useEffect(() => { searchAgreements() }, []) // eslint-disable-line

  const outstanding = (s) => {
    const t = targets[s.id]
    if (t) return Number(t.outstanding)
    return Number(s.amount) - Number(s.paidAmount ?? 0)
  }

  const postPayment = async (p) => {
    const res = await financeFetch(`/rent-collections/payments/${p.id}/post`, { method: 'POST', body: '{}' })
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Post failed', 'error'); return }
    toast('Payment posted', 'success'); loadPayments()
    schedules.forEach((s) => loadTarget(s.id))
  }

  return (
    <div className="space-y-6">
      <div className="grid gap-4 xl:grid-cols-5">
        {/* Agreement picker */}
        <div className="xl:col-span-2 rounded-2xl border border-slate-200 bg-white p-5">
          <h3 className="mb-3 text-sm font-semibold text-slate-700">Find rental agreement</h3>
          <form onSubmit={searchAgreements} className="mb-3 flex gap-2">
            <input className={inputCls} placeholder="Agreement no. or tenant…" value={q} onChange={(e) => setQ(e.target.value)} />
            <button className="rounded-xl bg-pcc-700 px-3 text-white hover:bg-pcc-900"><Search size={16} /></button>
          </form>
          <div className="max-h-40 space-y-1 overflow-y-auto">
            {searching && <p className="py-3 text-center text-xs text-slate-400">Searching…</p>}
            {agreements.map((a) => (
              <button key={a.id} onClick={() => pick(a)}
                className={`w-full rounded-lg px-3 py-2 text-left text-sm transition-colors ${agreement?.id === a.id ? 'bg-pcc-50 ring-1 ring-pcc-200' : 'hover:bg-slate-50'}`}>
                <span className="font-medium text-slate-700">{a.agreementNumber}</span>
                <span className="block text-xs text-slate-400">{a.tenantName} · {a.propertyName} · {a.status}</span>
              </button>
            ))}
          </div>

          {agreement && (
            <div className="mt-4 border-t border-slate-100 pt-4">
              <h4 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Rent schedule — {agreement.agreementNumber}</h4>
              <div className="max-h-72 space-y-1.5 overflow-y-auto">
                {schedules.map((s) => {
                  const out = outstanding(s)
                  const payable = !['CANCELLED', 'PAID'].includes(s.status) && out > 0
                  return (
                    <div key={s.id} className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-sm">
                      <div>
                        <p className="font-medium text-slate-700">{fmtDate(s.periodStart)} — {fmtDate(s.periodEnd)}</p>
                        <p className="text-xs text-slate-400">Due {fmtDate(s.dueDate)} · {fmtXaf(s.amount)}
                          {out < Number(s.amount) && <span className="text-pcc-600"> · outstanding {fmtXaf(out)}</span>}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${SCHED_STYLES[s.status]}`}>{label(s.status)}</span>
                        {payable && canCollect && (
                          <button onClick={() => setPayCtx({
                            externalId: s.id, parentId: agreement.id,
                            title: `Rent ${fmtDate(s.periodStart)}—${fmtDate(s.periodEnd)}`,
                            payerHint: agreement.tenantName,
                            total: s.amount, paid: Number(s.amount) - out, outstanding: out,
                          })} className="rounded-lg bg-pcc-700 px-2.5 py-1.5 text-xs font-semibold text-white hover:bg-pcc-900">
                            Pay
                          </button>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </div>

        {/* Payment history */}
        <div className="xl:col-span-3">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-700"><HandCoins size={16} className="text-pcc-700" /> Rent payments</h3>
            <input className={`${inputCls} !h-9 w-56`} placeholder="Search ref / payer…" value={paySearch}
              onChange={(e) => { setPaySearch(e.target.value); setPage(0) }} />
          </div>
          <DataTable
            columns={[
              { label: 'Reference' }, { label: 'Payer' }, { label: 'Amount' },
              { label: 'Date' }, { label: 'Method' }, { label: 'Status' }, { label: 'Receipt' }, { label: '' },
            ]}
            rows={payments?.content} loading={!payments}
            page={page} totalPages={payments?.totalPages ?? 0} onPage={setPage}
            empty="No rent payments recorded yet">
            {payments?.content.map((p) => (
              <tr key={p.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                <td className="px-5 py-3.5 font-medium text-slate-700">{p.paymentRef}</td>
                <td className="px-5 py-3.5 text-slate-600">{p.payerName}</td>
                <td className="px-5 py-3.5 font-semibold text-slate-800">{fmtXaf(p.amount)}</td>
                <td className="px-5 py-3.5 text-slate-500">{fmtDate(p.paymentDate)}</td>
                <td className="px-5 py-3.5 text-slate-500">{label(p.methodCode)}</td>
                <td className="px-5 py-3.5"><StatusBadge value={p.status} /></td>
                <td className="px-5 py-3.5 text-xs text-slate-500">{p.receiptNo ?? '—'}</td>
                <td className="px-5 py-3.5 text-right">
                  {p.status === 'RECORDED' && canPost && (
                    <button onClick={() => postPayment(p)} className="rounded-lg bg-emerald-50 px-2.5 py-1.5 text-xs font-semibold text-emerald-700 hover:bg-emerald-100">Post</button>
                  )}
                </td>
              </tr>
            ))}
          </DataTable>
        </div>
      </div>

      {payCtx && (
        <RecordPaymentModal
          endpoint="/rent-collections/payments" context={payCtx} toast={toast}
          onClose={() => setPayCtx(null)}
          onDone={() => { setPayCtx(null); loadPayments(); schedules.forEach((s) => loadTarget(s.id)) }} />
      )}
    </div>
  )
}
