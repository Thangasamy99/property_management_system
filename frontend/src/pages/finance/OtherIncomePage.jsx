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

const EMPTY = { category: '', description: '', amount: '', incomeDate: new Date().toISOString().slice(0, 10), methodCode: 'CASH', externalReference: '', documentReference: '', payerName: '' }
const CATEGORIES = ['DONATION', 'GRANT', 'SERVICE_FEE', 'INVESTMENT_RETURN', 'SALE_OF_ASSET', 'OTHER']

export default function OtherIncomePage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const { methods } = usePaymentFormData()
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState('')
  const [params, setParams] = useSearchParams()

  const canCreate = hasPermission('INCOME_CREATE')
  const canApprove = hasPermission('INCOME_APPROVE')

  const load = useCallback(async () => {
    const res = await financeFetch(`/other-income?page=${page}&size=10${status ? `&status=${status}` : ''}${q ? `&search=${encodeURIComponent(q)}` : ''}`)
    if (res.ok) setData(await res.json())
    else toast('Failed to load income records', 'error')
  }, [page, status, q]) // eslint-disable-line

  useEffect(() => { load() }, [load])
  useEffect(() => { if (params.get('new') === '1' && canCreate) { setOpen(true); params.delete('new'); setParams(params, { replace: true }) } }, [params]) // eslint-disable-line

  const submit = async (e) => {
    e.preventDefault()
    setErr(''); setSaving(true)
    const res = await financeFetch('/other-income', {
      method: 'POST',
      body: JSON.stringify({ ...form, amount: Number(form.amount), methodCode: form.methodCode, payerName: form.payerName || null, externalReference: form.externalReference || null, documentReference: form.documentReference || null }),
    })
    setSaving(false)
    const d = await res.json().catch(() => ({}))
    if (!res.ok) { setErr(d.message ?? 'Save failed'); return }
    toast(`Income ${d.incomeRef} recorded — awaiting approval`, 'success')
    setOpen(false); setForm(EMPTY); load()
  }

  const act = async (row, action, body = {}) => {
    const res = await financeFetch(`/other-income/${row.id}/${action}`, { method: 'POST', body: JSON.stringify(body) })
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Action failed', 'error'); return }
    toast(`Income ${action === 'approve' ? 'approved & posted' : 'rejected'}`, 'success'); load()
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <input className={`${inputCls} !h-10 w-64`} placeholder="Search ref / description / payer…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <select className={`${inputCls} !h-10 w-48`} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
          <option value="">All statuses</option>
          {['RECORDED', 'POSTED', 'REJECTED'].map((s) => <option key={s}>{label(s)}</option>)}
        </select>
        {canCreate && (
          <button onClick={() => setOpen(true)} className="ml-auto flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white hover:bg-pcc-900">
            <Plus size={16} /> Record income
          </button>
        )}
      </div>

      <DataTable
        columns={[{ label: 'Reference' }, { label: 'Category' }, { label: 'Description' }, { label: 'Payer' }, { label: 'Amount' }, { label: 'Date' }, { label: 'Status' }, { label: 'Payment' }, { label: '' }]}
        rows={data?.content} loading={!data}
        page={page} totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No income records">
        {data?.content.map((r) => (
          <tr key={r.id} className="border-b border-slate-50 hover:bg-slate-50/60">
            <td className="px-5 py-3.5 font-medium text-slate-700">{r.incomeRef}</td>
            <td className="px-5 py-3.5 text-slate-600">{label(r.category)}</td>
            <td className="px-5 py-3.5 max-w-56 truncate text-slate-500">{r.description}</td>
            <td className="px-5 py-3.5 text-slate-500">{r.payerName ?? '—'}</td>
            <td className="px-5 py-3.5 font-semibold">{fmtXaf(r.amount)}</td>
            <td className="px-5 py-3.5 text-slate-500">{fmtDate(r.incomeDate)}</td>
            <td className="px-5 py-3.5"><StatusBadge value={r.status} /></td>
            <td className="px-5 py-3.5 text-xs text-slate-500">{r.paymentRef ?? '—'}</td>
            <td className="px-5 py-3.5 text-right">
              {r.status === 'RECORDED' && canApprove && (
                <RowActionsMenu items={[
                  { label: 'Approve & post', onClick: () => act(r, 'approve') },
                  { label: 'Reject', danger: true, onClick: () => act(r, 'reject', { note: 'Rejected' }) },
                ]} />
              )}
            </td>
          </tr>
        ))}
      </DataTable>

      <Modal open={open} onClose={() => setOpen(false)} title="Record other income" width="max-w-lg">
        <form onSubmit={submit} className="grid grid-cols-2 gap-3">
          <Field label="Category">
            <select required className={inputCls} value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              <option value="">— select —</option>
              {CATEGORIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}
            </select>
          </Field>
          <Field label="Amount (XAF)"><input required type="number" min="1" className={inputCls} value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></Field>
          <div className="col-span-2"><Field label="Description"><input required className={inputCls} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field></div>
          <Field label="Income date"><input required type="date" className={inputCls} value={form.incomeDate} onChange={(e) => setForm({ ...form, incomeDate: e.target.value })} /></Field>
          <Field label="Payment method">
            <select className={inputCls} value={form.methodCode} onChange={(e) => setForm({ ...form, methodCode: e.target.value })}>
              {methods.filter((m) => m.active).map((m) => <option key={m.code} value={m.code}>{m.name}</option>)}
            </select>
          </Field>
          <Field label="Payer"><input className={inputCls} value={form.payerName} onChange={(e) => setForm({ ...form, payerName: e.target.value })} /></Field>
          <Field label="External reference"><input className={inputCls} value={form.externalReference} onChange={(e) => setForm({ ...form, externalReference: e.target.value })} /></Field>
          <div className="col-span-2"><Field label="Document reference"><input className={inputCls} value={form.documentReference} onChange={(e) => setForm({ ...form, documentReference: e.target.value })} /></Field></div>
          {err && <p className="col-span-2 rounded-lg bg-red-50 px-3 py-2 text-xs text-red-600">{err}</p>}
          <button disabled={saving} className="col-span-2 h-11 rounded-xl bg-pcc-700 text-sm font-semibold text-white hover:bg-pcc-900 disabled:opacity-60">
            {saving ? 'Saving…' : 'Record income'}
          </button>
        </form>
      </Modal>
    </div>
  )
}
