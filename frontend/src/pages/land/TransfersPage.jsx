import { useCallback, useEffect, useState } from 'react'
import { landFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import ParcelSelect from './ParcelSelect'
import { inputCls } from '../property/PropertiesPage'
import { StatusBadge, label } from './landShared'
import { Plus } from 'lucide-react'

const STATUSES = ['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED']
const EMPTY = { parcelId: '', newOwnerName: '', newOwnerReference: '', saleId: '', titleReference: '', documentReference: '' }

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-slate-600">{label}</span>
      {children}
    </label>
  )
}

function CompleteModal({ row, onClose, onDone }) {
  const [date, setDate] = useState('')
  const [busy, setBusy] = useState(false)
  const toast = useToast()

  const run = async () => {
    if (!date) { toast('Legal completion date is required', 'error'); return }
    setBusy(true)
    const res = await landFetch(`/land-transfers/${row.id}/complete`, {
      method: 'POST', body: JSON.stringify({ legalCompletionDate: date }),
    })
    setBusy(false)
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Completion failed', 'error'); return }
    toast('Ownership transferred', 'success'); onDone()
  }

  return (
    <Modal open={!!row} onClose={onClose} title={`Legal completion — ${row?.transferReference}`} width="max-w-sm">
      <p className="mb-3 text-xs text-slate-500">
        Completes the legal handoff: current ownership ends and a new record opens for <b>{row?.newOwnerName}</b>.
        The previous record is preserved in history.
      </p>
      <Field label="Legal completion date *">
        <input type="date" className={inputCls} value={date} onChange={(e) => setDate(e.target.value)} />
      </Field>
      <button onClick={run} disabled={busy}
        className="mt-4 h-10 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white hover:bg-pcc-900 disabled:opacity-60">
        {busy ? 'Completing…' : 'Complete transfer'}
      </button>
    </Modal>
  )
}

function DecisionModal({ action, onClose, onDone }) {
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const toast = useToast()
  const titles = { approve: 'Approve transfer', reject: 'Reject transfer', cancel: 'Cancel transfer' }

  const run = async () => {
    setBusy(true)
    const res = await landFetch(`/land-transfers/${action.row.id}/${action.type}`, {
      method: 'POST', body: JSON.stringify({ note: note || null }),
    })
    setBusy(false)
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Action failed', 'error'); return }
    toast('Done', 'success'); onDone()
  }

  const danger = action?.type === 'reject' || action?.type === 'cancel'

  return (
    <Modal open={!!action} onClose={onClose} title={action ? `${titles[action.type]} — ${action.row.transferReference}` : ''} width="max-w-sm">
      <Field label="Note">
        <textarea className={`${inputCls} h-20 py-2.5`} value={note} onChange={(e) => setNote(e.target.value)} />
      </Field>
      <button onClick={run} disabled={busy}
        className={`mt-4 h-10 w-full rounded-xl text-sm font-semibold text-white disabled:opacity-60 ${danger ? 'bg-red-500 hover:bg-red-600' : 'bg-pcc-700 hover:bg-pcc-900'}`}>
        {busy ? 'Working…' : titles[action?.type] ?? 'Confirm'}
      </button>
    </Modal>
  )
}

export default function TransfersPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [action, setAction] = useState(null)
  const [completeRow, setCompleteRow] = useState(null)

  const canRequest = hasPermission('LAND_SALE_REQUEST')
  const canApprove = hasPermission('LAND_SALE_APPROVE')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (statusFilter) q.set('status', statusFilter)
    if (search) q.set('search', search)
    const res = await landFetch(`/land-transfers?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load transfers — is the Land Service running on :8083?')
    setLoading(false)
  }, [page, statusFilter, search])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  const quick = async (id, step) => {
    const res = await landFetch(`/land-transfers/${id}/${step}`, { method: 'POST', body: '{}' })
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Action failed', 'error'); return }
    toast('Done', 'success'); load()
  }

  const submit = async (e) => {
    e.preventDefault()
    if (!form.parcelId || !form.newOwnerName.trim()) { setFormError('Parcel and new owner are required'); return }
    setSaving(true); setFormError('')
    const payload = {
      ...form, parcelId: Number(form.parcelId),
      saleId: form.saleId === '' ? null : Number(form.saleId),
    }
    const res = await landFetch('/land-transfers', { method: 'POST', body: JSON.stringify(payload) })
    setSaving(false)
    if (!res.ok) { setFormError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    setOpen(false); setForm(EMPTY); toast('Transfer request created', 'success'); load()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const columns = [
    { label: 'Reference' }, { label: 'Parcel' }, { label: 'From → To' },
    { label: 'Legal completion', className: 'hidden lg:table-cell' }, { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  const actionsFor = (t) => [
    canRequest && t.status === 'DRAFT' && { label: 'Submit', onClick: () => quick(t.id, 'submit') },
    canApprove && t.status === 'SUBMITTED' && { label: 'Approve', onClick: () => setAction({ type: 'approve', row: t }) },
    canApprove && t.status === 'SUBMITTED' && { label: 'Reject', danger: true, onClick: () => setAction({ type: 'reject', row: t }) },
    canApprove && t.status === 'APPROVED' && { label: 'Legal completion', onClick: () => setCompleteRow(t) },
    canRequest && (t.status === 'DRAFT' || t.status === 'SUBMITTED') && { label: 'Cancel', danger: true, onClick: () => setAction({ type: 'cancel', row: t }) },
  ].filter(Boolean)

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Ownership Transfers</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} transfer records` : 'Legal ownership handoffs'}</p>
        </div>
        {canRequest && (
          <button onClick={() => { setForm(EMPTY); setFormError(''); setOpen(true) }}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> New Transfer
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search reference, owner or parcel…" className={`${inputCls} max-w-xs`} />
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-44`}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No transfer requests yet">
        {data?.content.map((t) => (
          <tr key={t.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5 font-mono text-xs font-semibold text-pcc-700">{t.transferReference}</td>
            <td className="px-5 py-3.5">
              <span className="text-ink">{t.parcelName}</span>
              <span className="ml-2 font-mono text-xs text-slate-400">{t.parcelReference}</span>
            </td>
            <td className="px-5 py-3.5 text-slate-600">
              <span className="text-slate-400">{t.previousOwnerName ?? '—'}</span>
              <span className="mx-1.5 text-slate-300">→</span>
              <span className="font-medium">{t.newOwnerName}</span>
            </td>
            <td className="hidden px-5 py-3.5 text-slate-600 lg:table-cell">{t.legalCompletionDate ?? '—'}</td>
            <td className="px-5 py-3.5"><StatusBadge value={t.status} /></td>
            <td className="px-3 py-3.5">
              {actionsFor(t).length > 0 && <RowActionsMenu items={actionsFor(t)} />}
            </td>
          </tr>
        ))}
      </DataTable>

      <Modal open={open} onClose={() => setOpen(false)} title="New Ownership Transfer" width="max-w-xl">
        {formError && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{formError}</div>}
        <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Field label="Parcel *"><ParcelSelect value={form.parcelId} onChange={(v) => setForm((f) => ({ ...f, parcelId: v }))} /></Field>
            <p className="mt-1 text-xs text-slate-400">The parcel's current ownership record becomes the previous owner.</p>
          </div>
          <Field label="New owner name *"><input required className={inputCls} value={form.newOwnerName} onChange={set('newOwnerName')} placeholder="Douala Estates SARL" /></Field>
          <Field label="New owner reference"><input className={inputCls} value={form.newOwnerReference} onChange={set('newOwnerReference')} /></Field>
          <Field label="Linked sale ID (optional)"><input type="number" min="1" className={inputCls} value={form.saleId} onChange={set('saleId')} placeholder="Sale id from Land Sales" /></Field>
          <Field label="Title reference"><input className={inputCls} value={form.titleReference} onChange={set('titleReference')} placeholder="TC-9988/DLA" /></Field>
          <div className="sm:col-span-2"><Field label="Document reference"><input className={inputCls} value={form.documentReference} onChange={set('documentReference')} /></Field></div>
          <div className="sm:col-span-2">
            <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
              {saving ? 'Saving…' : 'Create transfer request'}
            </button>
          </div>
        </form>
      </Modal>

      <DecisionModal action={action} onClose={() => setAction(null)} onDone={() => { setAction(null); load() }} />
      <CompleteModal row={completeRow} onClose={() => setCompleteRow(null)} onDone={() => { setCompleteRow(null); load() }} />
    </div>
  )
}
