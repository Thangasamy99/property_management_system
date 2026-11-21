import { useEffect, useState } from 'react'
import { financeFetch } from '../../services/api'
import Modal from '../../components/Modal'
import { inputCls } from '../property/PropertiesPage'

export const FIN_STATUS_STYLES = {
  RECORDED: 'bg-amber-50 text-amber-700',
  POSTED: 'bg-emerald-50 text-emerald-700',
  REQUESTED: 'bg-sky-50 text-sky-700',
  PENDING_APPROVAL: 'bg-amber-50 text-amber-700',
  APPROVED: 'bg-emerald-50 text-emerald-700',
  REJECTED: 'bg-red-50 text-red-600',
  PAID: 'bg-emerald-50 text-emerald-700',
  PARTIALLY_REFUNDED: 'bg-violet-50 text-violet-700',
  REFUNDED: 'bg-slate-100 text-slate-500',
  REVERSED: 'bg-red-50 text-red-600',
  CANCELLED: 'bg-slate-100 text-slate-400',
  VOID: 'bg-slate-100 text-slate-400',
}

export const PAYMENT_TYPE_LABELS = {
  RENT: 'Rent',
  LAND_ACQUISITION: 'Land acquisition',
  LAND_SALE: 'Land sale',
  OTHER_INCOME: 'Other income',
  EXPENSE_PAYOUT: 'Expense payout',
}

export const METHOD_LABELS = {
  CASH: 'Cash',
  BANK_TRANSFER: 'Bank Transfer',
  CHEQUE: 'Cheque',
  MOBILE_MONEY: 'Mobile Money',
  OTHER: 'Other',
}

export const fmtXaf = (n) => (n == null ? '—' : `${Number(n).toLocaleString('en-US')} XAF`)
export const label = (s) => (s ?? '—').replaceAll('_', ' ')
export const fmtDate = (d) => (d ? new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '—')

export function StatusBadge({ value }) {
  return (
    <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${FIN_STATUS_STYLES[value] ?? 'bg-slate-100 text-slate-500'}`}>
      {label(value)}
    </span>
  )
}

export function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-slate-600">{label}</span>
      {children}
    </label>
  )
}

/** Loads payment methods + finance accounts once for payment forms. */
export function usePaymentFormData() {
  const [methods, setMethods] = useState([])
  const [accounts, setAccounts] = useState([])
  useEffect(() => {
    financeFetch('/payment-methods').then(async (r) => r.ok && setMethods(await r.json())).catch(() => {})
    Promise.all([
      financeFetch('/cash-accounts').then((r) => (r.ok ? r.json() : [])).catch(() => []),
      financeFetch('/bank-accounts').then((r) => (r.ok ? r.json() : [])).catch(() => []),
    ]).then(([c, b]) => setAccounts([...c, ...b]))
  }, [])
  return { methods, accounts }
}

/**
 * Shared "record payment" modal used by rent collection, land acquisition
 * payments and land sale receipts.
 */
export function RecordPaymentModal({ endpoint, context, onClose, onDone, toast }) {
  const { methods, accounts } = usePaymentFormData()
  const [form, setForm] = useState({
    payerName: context?.payerHint ?? '',
    amount: context?.outstanding ?? '',
    paymentDate: new Date().toISOString().slice(0, 10),
    methodCode: 'CASH',
    paymentReference: '',
    verificationDetail: '',
    notes: '',
    accountId: '',
  })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (context) {
      setForm((f) => ({
        ...f,
        payerName: context.payerHint ?? f.payerName,
        amount: context.outstanding ?? f.amount,
      }))
    }
  }, [context])

  if (!context) return null
  const cashLike = form.methodCode === 'CASH'

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    const res = await financeFetch(endpoint, {
      method: 'POST',
      body: JSON.stringify({
        externalId: context.externalId,
        parentId: context.parentId ?? null,
        payerName: form.payerName,
        amount: Number(form.amount),
        paymentDate: form.paymentDate,
        methodCode: form.methodCode,
        paymentReference: form.paymentReference || null,
        verificationDetail: form.verificationDetail || null,
        notes: form.notes || null,
        accountId: form.accountId ? Number(form.accountId) : null,
        idempotencyKey: crypto.randomUUID(),
      }),
    })
    setBusy(false)
    const data = await res.json().catch(() => ({}))
    if (!res.ok) { setError(data.message ?? 'Payment failed'); return }
    toast(`Payment ${data.paymentRef} recorded (${data.status})`, 'success')
    onDone(data)
  }

  return (
    <Modal open onClose={onClose} title={`Record payment — ${context.title}`} width="max-w-lg">
      <div className="mb-4 rounded-xl bg-slate-50 px-4 py-3 text-sm">
        <div className="flex justify-between"><span className="text-slate-500">Total</span><span className="font-semibold">{fmtXaf(context.total)}</span></div>
        <div className="flex justify-between"><span className="text-slate-500">Already paid</span><span className="font-semibold">{fmtXaf(context.paid ?? 0)}</span></div>
        <div className="flex justify-between border-t border-slate-200 pt-1 mt-1"><span className="text-slate-600 font-medium">Outstanding</span><span className="font-bold text-pcc-700">{fmtXaf(context.outstanding)}</span></div>
      </div>
      <form onSubmit={submit} className="grid grid-cols-2 gap-3">
        <Field label="Payer name"><input required className={inputCls} value={form.payerName} onChange={(e) => setForm({ ...form, payerName: e.target.value })} /></Field>
        <Field label={`Amount (max ${Number(context.outstanding).toLocaleString()})`}>
          <input required type="number" min="1" max={context.outstanding} className={inputCls} value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
        </Field>
        <Field label="Payment date"><input required type="date" className={inputCls} value={form.paymentDate} onChange={(e) => setForm({ ...form, paymentDate: e.target.value })} /></Field>
        <Field label="Payment method">
          <select className={inputCls} value={form.methodCode} onChange={(e) => setForm({ ...form, methodCode: e.target.value })}>
            {methods.filter((m) => m.active).map((m) => <option key={m.code} value={m.code}>{m.name}</option>)}
          </select>
        </Field>
        <Field label={form.methodCode === 'CASH' ? 'Reference (optional)' : 'Transaction / cheque / transfer reference'}>
          <input className={inputCls} value={form.paymentReference} onChange={(e) => setForm({ ...form, paymentReference: e.target.value })} />
        </Field>
        <Field label="Account">
          <select className={inputCls} value={form.accountId} onChange={(e) => setForm({ ...form, accountId: e.target.value })}>
            <option value="">— select account —</option>
            {accounts.map((a) => <option key={a.id} value={a.id}>{a.name} ({a.accountType})</option>)}
          </select>
        </Field>
        {!cashLike && (
          <div className="col-span-2">
            <Field label="Verification detail (bank statement line, MoMo confirmation, etc.)">
              <input className={inputCls} value={form.verificationDetail} onChange={(e) => setForm({ ...form, verificationDetail: e.target.value })} />
            </Field>
            <p className="mt-1 text-[11px] text-slate-400">Non-cash payments stay RECORDED until an approver posts them.</p>
          </div>
        )}
        <div className="col-span-2"><Field label="Notes"><input className={inputCls} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field></div>
        {error && <p className="col-span-2 rounded-lg bg-red-50 px-3 py-2 text-xs text-red-600">{error}</p>}
        <button disabled={busy} className="col-span-2 h-11 rounded-xl bg-pcc-700 text-sm font-semibold text-white hover:bg-pcc-900 disabled:opacity-60">
          {busy ? 'Recording…' : `Record ${fmtXaf(Number(form.amount) || 0)}`}
        </button>
      </form>
    </Modal>
  )
}
