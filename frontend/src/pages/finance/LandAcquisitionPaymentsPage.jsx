import { useCallback, useEffect, useState } from 'react'
import { financeFetch, landFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import DataTable from '../../components/DataTable'
import { inputCls } from '../property/PropertiesPage'
import { StatusBadge, fmtXaf, fmtDate, label, RecordPaymentModal } from './financeShared'
import { Search } from 'lucide-react'

const FIN_STYLES = { UNPAID: 'bg-amber-50 text-amber-700', PARTIALLY_PAID: 'bg-sky-50 text-sky-700', PAID: 'bg-emerald-50 text-emerald-700' }

export default function LandAcquisitionPaymentsPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const canCreate = hasPermission('LAND_PAYMENT_CREATE')
  const canPost = hasPermission('LAND_PAYMENT_APPROVE')

  const [q, setQ] = useState('')
  const [acqs, setAcqs] = useState([])
  const [targets, setTargets] = useState({})
  const [payCtx, setPayCtx] = useState(null)
  const [payments, setPayments] = useState(null)
  const [page, setPage] = useState(0)
  const [paySearch, setPaySearch] = useState('')

  const payable = (a) => ['APPROVED', 'COMPLETED'].includes(a.status) && a.financeStatus !== 'PAID'

  const search = async (e) => {
    e?.preventDefault()
    const res = await landFetch(`/land-acquisitions?size=15${q ? `&search=${encodeURIComponent(q)}` : ''}`)
    if (res.ok) {
      const d = await res.json()
      const list = d.content ?? []
      setAcqs(list)
      list.forEach(loadTarget)
    } else toast('Land Service search failed', 'error')
  }

  const loadTarget = async (a) => {
    const res = await financeFetch(`/land-acquisition-payments/targets/${a.id}`)
    if (res.ok) { const t = await res.json(); setTargets((prev) => ({ ...prev, [a.id]: t })) }
  }

  const loadPayments = useCallback(async () => {
    const res = await financeFetch(`/land-acquisition-payments/payments?page=${page}&size=10${paySearch ? `&search=${encodeURIComponent(paySearch)}` : ''}`)
    if (res.ok) setPayments(await res.json())
  }, [page, paySearch])

  useEffect(() => { loadPayments() }, [loadPayments])
  useEffect(() => { search() }, []) // eslint-disable-line

  const outstanding = (a) => {
    const t = targets[a.id]
    return t ? Number(t.outstanding) : Number(a.purchasePrice ?? 0) - Number(a.paidAmount ?? 0)
  }

  const postPayment = async (p) => {
    const res = await financeFetch(`/land-acquisition-payments/payments/${p.id}/post`, { method: 'POST', body: '{}' })
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Post failed', 'error'); return }
    toast('Payment posted', 'success'); loadPayments(); acqs.forEach(loadTarget)
  }

  return (
    <div className="grid gap-4 xl:grid-cols-5">
      <div className="xl:col-span-2 rounded-2xl border border-slate-200 bg-white p-5">
        <h3 className="mb-3 text-sm font-semibold text-slate-700">Approved acquisitions</h3>
        <form onSubmit={search} className="mb-3 flex gap-2">
          <input className={inputCls} placeholder="Reference or seller…" value={q} onChange={(e) => setQ(e.target.value)} />
          <button className="rounded-xl bg-pcc-700 px-3 text-white hover:bg-pcc-900"><Search size={16} /></button>
        </form>
        <div className="max-h-[28rem] space-y-1.5 overflow-y-auto">
          {acqs.map((a) => {
            const out = outstanding(a)
            return (
              <div key={a.id} className="rounded-xl border border-slate-100 px-3 py-2.5 text-sm">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-slate-700">{a.acquisitionReference}</span>
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${FIN_STYLES[a.financeStatus] ?? 'bg-slate-100 text-slate-500'}`}>{label(a.financeStatus ?? 'UNPAID')}</span>
                </div>
                <p className="mt-0.5 text-xs text-slate-400">{a.parcelName} · {a.sellerName} · total {fmtXaf(a.purchasePrice)} · outstanding <span className="font-medium text-pcc-700">{fmtXaf(out)}</span></p>
                {payable(a) && out > 0 && canCreate && (
                  <button onClick={() => setPayCtx({
                    externalId: a.id, title: `${a.acquisitionReference} — ${a.parcelName}`,
                    payerHint: 'PCC Cameroon', total: a.purchasePrice,
                    paid: Number(a.purchasePrice ?? 0) - out, outstanding: out,
                  })} className="mt-2 rounded-lg bg-pcc-700 px-3 py-1.5 text-xs font-semibold text-white hover:bg-pcc-900">
                    Record payment
                  </button>
                )}
              </div>
            )
          })}
          {acqs.length === 0 && <p className="py-6 text-center text-xs text-slate-400">No acquisitions found</p>}
        </div>
      </div>

      <div className="xl:col-span-3">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-700">Acquisition payments</h3>
          <input className={`${inputCls} !h-9 w-56`} placeholder="Search…" value={paySearch}
            onChange={(e) => { setPaySearch(e.target.value); setPage(0) }} />
        </div>
        <DataTable
          columns={[{ label: 'Reference' }, { label: 'Payer' }, { label: 'Amount' }, { label: 'Date' }, { label: 'Method' }, { label: 'Status' }, { label: 'Receipt' }, { label: '' }]}
          rows={payments?.content} loading={!payments}
          page={page} totalPages={payments?.totalPages ?? 0} onPage={setPage}
          empty="No acquisition payments recorded">
          {payments?.content.map((p) => (
            <tr key={p.id} className="border-b border-slate-50 hover:bg-slate-50/60">
              <td className="px-5 py-3.5 font-medium text-slate-700">{p.paymentRef}</td>
              <td className="px-5 py-3.5 text-slate-600">{p.payerName}</td>
              <td className="px-5 py-3.5 font-semibold">{fmtXaf(p.amount)}</td>
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

      {payCtx && (
        <RecordPaymentModal endpoint="/land-acquisition-payments/payments" context={payCtx} toast={toast}
          onClose={() => setPayCtx(null)}
          onDone={() => { setPayCtx(null); loadPayments(); acqs.forEach(loadTarget) }} />
      )}
    </div>
  )
}
