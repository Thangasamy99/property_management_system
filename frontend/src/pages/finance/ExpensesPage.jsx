import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { financeFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { inputCls } from '../property/PropertiesPage'
import { StatusBadge, Field, fmtXaf, fmtDate, label, usePaymentFormData } from './financeShared'
import { Plus } from 'lucide-react'

const EMPTY = { category: '', description: '', amount: '', expenseDate: new Date().toISOString().slice(0, 10), department: '', vendorName: '', vendorReference: '', documentReference: '' }
const CATEGORIES = ['UTILITIES', 'SALARIES', 'MAINTENANCE', 'SUPPLIES', 'TRANSPORT', 'LEGAL', 'ADMIN', 'OTHER']

export default function ExpensesPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const { methods, accounts } = usePaymentFormData()
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [open, setOpen] = useState(false)
  const [payRow, setPayRow] = useState(null)
  const [payForm, setPayForm] = useState({ methodCode: 'CASH', paymentReference: '', paidDate: new Date().toISOString().slice(0, 10), accountId: '' })
  const [form, setForm] = useState(EMPTY)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [params, setParams] = useSearchParams()

  const canCreate = hasPermission('EXPENSE_CREATE')
  const canApprove = hasPermission('EXPENSE_APPROVE')
  const canPay = hasPermission('EXPENSE_PAY')

  const load = useCallback(async () => {
    const res = await financeFetch(`/expenses?page=${page}&size=10${status ? `&status=${status}` : ''}${q ? `&search=${encodeURIComponent(q)}` : ''}`)
    if (res.ok) setData(await res.json())
  }, [page, status, q]) // eslint-disable-line

  useEffect(() => { load() }, [load])
  useEffect(() => { if (params.get('new') === '1' && canCreate) { setOpen(true); params.delete('new'); setParams(params, { replace: true }) } }, [params]) // eslint-disable-line

  const submit = async (e) => {
    e.preventDefault()
    setErr(''); setBusy(true)
    const res = await financeFetch('/expenses', {
      method: 'POST',
      body: JSON.stringify({ ...form, amount: Number(form.amount), department: form.department || null, vendorName: form.vendorName || null, vendorReference: form.vendorReference || null, documentReference: form.documentReference || null }),
    })
    setBusy(false)
    const d = await res.json().catch(() => ({}))
    if (!res.ok) { setErr(d.message ?? 'Save failed'); return }
    toast(`Expense ${d.expenseRef} submitted for approval`, 'success')
    setOpen(false); setForm(EMPTY); load()
  }

  const act = async (row, action, body = {}) => {
    const res = await financeFetch(`/expenses/${row.id}/${action}`, { method: 'POST', body: JSON.stringify(body) })
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Action failed', 'error'); return }
    toast(`Expense ${action}d`, 'success'); load()
  }

  const pay = async (e) => {
    e.preventDefault()
    setBusy(true)
    const res = await financeFetch(`/expenses/${payRow.id}/pay`, {
      method: 'POST',
      body: JSON.stringify({ ...payForm, accountId: payForm.accountId ? Number(payForm.accountId) : null }),
    })
    setBusy(false)
    const d = await res.json().catch(() => ({}))
    if (!res.ok) { toast(d.message ?? 'Payment failed', 'error'); return }
    toast(`Expense paid — ${d.paymentRef}`, 'success'); setPayRow(null); load()
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <input className={`${inputCls} !h-10 w-64`} placeholder="Search ref / vendor / dept…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <select className={`${inputCls} !h-10 w-48`} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
          <option value="">All statuses</option>
          {['PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'PAID', 'CANCELLED'].map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        {canCreate && (
          <button onClick={() => setOpen(true)} className="ml-auto flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white hover:bg-pcc-900">
            <Plus size={16} /> New expense
          </button>
        )}
      </div>

      <DataTable
        columns={[{ label: 'Reference' }, { label: 'Category' }, { label: 'Description' }, { label: 'Vendor / Dept' }, { label: 'Amount' }, { label: 'Date' }, { label: 'Status' }, { label: 'Paid' }, { label: '' }]}
        rows={data?.content} loading={!data}
        page={page} totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No expenses recorded">
        {data?.content.map((r) => (
          <tr key={r.id} className="border-b border-slate-50 hover:bg-slate-50/60">
            <td className="px-5 py-3.5 font-medium text-slate-700">{r.expenseRef}</td>
            <td className="px-5 py-3.5 text-slate-600">{label(r.category)}</td>
            <td className="px-5 py-3.5 max-w-48 truncate text-slate-500">{r.description}</td>
            <td className="px-5 py-3.5 text-slate-500">{r.vendorName ?? '—'}{r.department ? ` · ${r.department}` : ''}</td>
            <td className="px-5 py-3.5 font-semibold">{fmtXaf(r.amount)}</td>
            <td className="px-5 py-3.5 text-slate-500">{fmtDate(r.expenseDate)}</td>
            <td className="px-5 py-3.5"><StatusBadge value={r.status} /></td>
            <td className="px-5 py-3.5 text-xs text-slate-500">{r.paidDate ? `${fmtDate(r.paidDate)} ${r.paymentRef ? `· ${r.paymentRef}` : ''}` : '—'}</td>
            <td className="px-5 py-3.5 text-right">
              <RowActionsMenu items={[
                ...(r.status === 'PENDING_APPROVAL' && canApprove ? [
                  { label: 'Approve', onClick: () => act(r, 'approve') },
                  { label: 'Reject', danger: true, onClick: () => act(r, 'reject', { note: 'Rejected' }) },
                ] : []),
                ...(r.status === 'APPROVED' && canPay ? [
                  { label: 'Record payment', onClick: () => { setPayForm({ ...payForm, accountId: '' }); setPayRow(r) } },
                ] : []),
                ...(['PENDING_APPROVAL', 'APPROVED', 'REJECTED'].includes(r.status) && canCreate ? [
                  { label: 'Cancel expense', danger: true, onClick: () => act(r, 'cancel') },
                ] : []),
              ]} />
            </td>
          </tr>
        ))}
      </DataTable>

      <Modal open={open} onClose={() => setOpen(false)} title="New expense" width="max-w-lg">
        <form onSubmit={submit} className="grid grid-cols-2 gap-3">
          <Field label="Category">
            <select required className={inputCls} value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              <option value="">— select —</option>
              {CATEGORIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}
            </select>
          </Field>
          <Field label="Amount (XAF)"><input required type="number" min="1" className={inputCls} value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></Field>
          <div className="col-span-2"><Field label="Description"><input required className={inputCls} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field></div>
          <Field label="Expense date"><input required type="date" className={inputCls} value={form.expenseDate} onChange={(e) => setForm({ ...form, expenseDate: e.target.value })} /></Field>
          <Field label="Department / cost centre"><input className={inputCls} value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} /></Field>
          <Field label="Vendor / payee"><input className={inputCls} value={form.vendorName} onChange={(e) => setForm({ ...form, vendorName: e.target.value })} /></Field>
          <Field label="Vendor reference"><input className={inputCls} value={form.vendorReference} onChange={(e) => setForm({ ...form, vendorReference: e.target.value })} /></Field>
          <div className="col-span-2"><Field label="Supporting document reference"><input className={inputCls} value={form.documentReference} onChange={(e) => setForm({ ...form, documentReference: e.target.value })} /></Field></div>
          {err && <p className="col-span-2 rounded-lg bg-red-50 px-3 py-2 text-xs text-red-600">{err}</p>}
          <button disabled={busy} className="col-span-2 h-11 rounded-xl bg-pcc-700 text-sm font-semibold text-white hover:bg-pcc-900 disabled:opacity-60">{busy ? 'Saving…' : 'Submit for approval'}</button>
        </form>
      </Modal>

      <Modal open={!!payRow} onClose={() => setPayRow(null)} title={`Pay expense — ${payRow?.expenseRef}`} width="max-w-sm">
        <p className="mb-4 text-sm text-slate-600">Pay <strong>{fmtXaf(payRow?.amount)}</strong>{payRow?.vendorName ? ` to ${payRow.vendorName}` : ''}.</p>
        <form onSubmit={pay} className="space-y-3">
          <Field label="Payment method">
            <select className={inputCls} value={payForm.methodCode} onChange={(e) => setPayForm({ ...payForm, methodCode: e.target.value })}>
              {methods.filter((m) => m.active).map((m) => <option key={m.code} value={m.code}>{m.name}</option>)}
            </select>
          </Field>
          <Field label="Payment reference"><input className={inputCls} value={payForm.paymentReference} onChange={(e) => setPayForm({ ...payForm, paymentReference: e.target.value })} /></Field>
          <Field label="Paid date"><input required type="date" className={inputCls} value={payForm.paidDate} onChange={(e) => setPayForm({ ...payForm, paidDate: e.target.value })} /></Field>
          <Field label="Debit account">
            <select className={inputCls} value={payForm.accountId} onChange={(e) => setPayForm({ ...payForm, accountId: e.target.value })}>
              <option value="">— select account —</option>
              {accounts.map((a) => <option key={a.id} value={a.id}>{a.name} ({a.accountType})</option>)}
            </select>
          </Field>
          <button disabled={busy} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white hover:bg-pcc-900 disabled:opacity-60">{busy ? 'Processing…' : 'Confirm payment'}</button>
        </form>
      </Modal>
    </div>
  )
}
