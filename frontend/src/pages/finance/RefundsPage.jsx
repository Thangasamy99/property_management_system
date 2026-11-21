import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { financeFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { inputCls } from '../property/PropertiesPage'
import { StatusBadge, Field, fmtXaf, fmtDate, label } from './financeShared'
import { Plus } from 'lucide-react'

export default function RefundsPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({ paymentRef: '', amount: '', reason: '' })
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [params, setParams] = useSearchParams()

  const canCreate = hasPermission('REFUND_CREATE')
  const canApprove = hasPermission('REFUND_APPROVE')

  const load = useCallback(async () => {
    const res = await financeFetch(`/refunds?page=${page}&size=10`)
    if (res.ok) setData(await res.json())
  }, [page]) // eslint-disable-line
  useEffect(() => { load() }, [load])
  useEffect(() => {
    if (params.get('paymentId') || (params.get('new') === '1' && canCreate)) {
      setForm((f) => ({ ...f, paymentRef: params.get('paymentRef') ?? '' }))
      setOpen(true); setParams({}, { replace: true })
    }
  }, [params]) // eslint-disable-line

  const submit = async (e) => {
    e.preventDefault()
    setErr(''); setBusy(true)
    const lookup = await financeFetch(`/receipts?search=${encodeURIComponent(form.paymentRef)}&size=5`)
    const found = lookup.ok ? (await lookup.json()).content?.find((r) => r.paymentRef === form.paymentRef || r.receiptNo === form.paymentRef) : null
    if (!found?.paymentId) { setErr(`No receipted payment found for ${form.paymentRef}`); setBusy(false); return }
    const res = await financeFetch('/refunds', {
      method: 'POST',
      body: JSON.stringify({ paymentId: found.paymentId, amount: Number(form.amount), reason: form.reason }),
    })
    setBusy(false)
    const d = await res.json().catch(() => ({}))
    if (!res.ok) { setErr(d.message ?? 'Refund failed'); return }
    toast(`Refund ${d.refundRef} submitted for approval`, 'success')
    setOpen(false); setForm({ ...form, paymentRef: '', amount: '', reason: '' }); load()
  }

  const act = async (row, action) => {
    const res = await financeFetch(`/refunds/${row.id}/${action}`, { method: 'POST', body: '{}' })
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Action failed', 'error'); return }
    toast(`Refund ${action}d`, 'success'); load()
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <p className="text-sm text-slate-500">Refunds are linked to original posted payments and require approval.</p>
        {canCreate && (
          <button onClick={() => setOpen(true)} className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white hover:bg-pcc-900">
            <Plus size={16} /> New refund
          </button>
        )}
      </div>
      <DataTable
        columns={[{ label: 'Refund Ref' }, { label: 'Payment Ref' }, { label: 'Payer' }, { label: 'Amount' }, { label: 'Reason' }, { label: 'Requested' }, { label: 'Status' }, { label: '' }]}
        rows={data?.content} loading={!data}
        page={page} totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No refunds recorded">
        {data?.content.map((r) => (
          <tr key={r.id} className="border-b border-slate-50 hover:bg-slate-50/60">
            <td className="px-5 py-3.5 font-medium text-slate-700">{r.refundRef}</td>
            <td className="px-5 py-3.5 text-slate-600">{r.paymentRef}</td>
            <td className="px-5 py-3.5 text-slate-500">{r.payerName}</td>
            <td className="px-5 py-3.5 font-semibold">{fmtXaf(r.amount)}</td>
            <td className="px-5 py-3.5 max-w-48 truncate text-slate-500">{r.reason}</td>
            <td className="px-5 py-3.5 text-slate-500">{fmtDate(r.requestedAt)}</td>
            <td className="px-5 py-3.5"><StatusBadge value={r.status} /></td>
            <td className="px-5 py-3.5 text-right">
              {r.status === 'REQUESTED' && canApprove && (
                <RowActionsMenu items={[
                  { label: 'Approve & post', onClick: () => act(r, 'approve') },
                  { label: 'Reject', danger: true, onClick: () => act(r, 'reject') },
                ]} />
              )}
            </td>
          </tr>
        ))}
      </DataTable>

      <Modal open={open} onClose={() => setOpen(false)} title="New refund" width="max-w-sm">
        <form onSubmit={submit} className="space-y-3">
          <Field label="Original payment reference"><input required className={inputCls} value={form.paymentRef} onChange={(e) => setForm({ ...form, paymentRef: e.target.value })} /></Field>
          <Field label="Refund amount (XAF)"><input required type="number" min="1" className={inputCls} value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></Field>
          <Field label="Reason"><input required className={inputCls} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></Field>
          {err && <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-600">{err}</p>}
          <button disabled={busy} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white hover:bg-pcc-900 disabled:opacity-60">{busy ? 'Submitting…' : 'Submit refund'}</button>
        </form>
      </Modal>
    </div>
  )
}
