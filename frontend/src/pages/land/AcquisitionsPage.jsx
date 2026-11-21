import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { landFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import ParcelSelect from './ParcelSelect'
import { inputCls } from '../property/PropertiesPage'
import { StatusBadge, fmtXaf, label } from './landShared'
import { Plus } from 'lucide-react'

const STATUSES = ['PENDING', 'APPROVED', 'COMPLETED', 'CANCELLED']
const EMPTY = { parcelId: '', sellerName: '', sellerReference: '', sellerContact: '', acquisitionDate: '', purchasePrice: '', documentReference: '' }

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-slate-600">{label}</span>
      {children}
    </label>
  )
}

function DecisionModal({ action, onClose, onDone }) {
  const [note, setNote] = useState('')
  const [approvalReference, setApprovalReference] = useState('')
  const [busy, setBusy] = useState(false)
  const toast = useToast()

  const titles = { approve: 'Approve acquisition', complete: 'Complete acquisition', cancel: 'Cancel acquisition' }

  const run = async () => {
    setBusy(true)
    const res = await landFetch(`/land-acquisitions/${action.row.id}/${action.type}`, {
      method: 'POST',
      body: JSON.stringify({ note: note || null, approvalReference: approvalReference || null }),
    })
    setBusy(false)
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Action failed', 'error'); return }
    toast('Done', 'success'); onDone()
  }

  return (
    <Modal open={!!action} onClose={onClose} title={action ? `${titles[action.type]} — ${action.row.acquisitionReference}` : ''} width="max-w-sm">
      {action?.type === 'approve' && (
        <Field label="Approval reference">
          <input className={`${inputCls} mb-3`} value={approvalReference} onChange={(e) => setApprovalReference(e.target.value)} placeholder="BRD-2025-19" />
        </Field>
      )}
      <Field label="Note">
        <textarea className={`${inputCls} h-20 py-2.5`} value={note} onChange={(e) => setNote(e.target.value)} />
      </Field>
      <button onClick={run} disabled={busy}
        className={`mt-4 h-10 w-full rounded-xl text-sm font-semibold text-white disabled:opacity-60 ${action?.type === 'cancel' ? 'bg-red-500 hover:bg-red-600' : 'bg-pcc-700 hover:bg-pcc-900'}`}>
        {busy ? 'Working…' : titles[action?.type] ?? 'Confirm'}
      </button>
    </Modal>
  )
}

export default function AcquisitionsPage() {
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

  const [searchParams, setSearchParams] = useSearchParams()

  const canCreate = hasPermission('LAND_ACQUISITION_CREATE') || hasPermission('LAND_ACQUISITION_MANAGE')
  const canApprove = hasPermission('LAND_ACQUISITION_APPROVE')

  useEffect(() => {
    if (searchParams.get('new') === '1' && canCreate) {
      setForm(EMPTY); setFormError(''); setOpen(true)
      setSearchParams({}, { replace: true })
    }
  }, [searchParams, canCreate, setSearchParams])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (statusFilter) q.set('status', statusFilter)
    if (search) q.set('search', search)
    const res = await landFetch(`/land-acquisitions?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load acquisitions — is the Land Service running on :8083?')
    setLoading(false)
  }, [page, statusFilter, search])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  const submit = async (e) => {
    e.preventDefault()
    if (!form.parcelId || !form.sellerName.trim()) { setFormError('Parcel and seller name are required'); return }
    setSaving(true); setFormError('')
    const payload = {
      ...form, parcelId: Number(form.parcelId),
      purchasePrice: form.purchasePrice === '' ? null : Number(form.purchasePrice),
      acquisitionDate: form.acquisitionDate || null,
    }
    const res = await landFetch('/land-acquisitions', { method: 'POST', body: JSON.stringify(payload) })
    setSaving(false)
    if (!res.ok) { setFormError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    setOpen(false); setForm(EMPTY); toast('Acquisition registered', 'success'); load()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const columns = [
    { label: 'Reference' }, { label: 'Parcel' }, { label: 'Seller' },
    { label: 'Price (XAF)', className: 'hidden lg:table-cell' }, { label: 'Date' },
    { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Land Acquisitions</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} acquisition records` : 'Land purchase pipeline'}</p>
        </div>
        {canCreate && (
          <button onClick={() => { setForm(EMPTY); setFormError(''); setOpen(true) }}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> New Acquisition
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search reference, seller or parcel…" className={`${inputCls} max-w-xs`} />
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-40`}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No acquisitions registered yet">
        {data?.content.map((a) => (
          <tr key={a.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5 font-mono text-xs font-semibold text-pcc-700">{a.acquisitionReference}</td>
            <td className="px-5 py-3.5">
              <span className="text-ink">{a.parcelName}</span>
              <span className="ml-2 font-mono text-xs text-slate-400">{a.parcelReference}</span>
            </td>
            <td className="px-5 py-3.5 text-slate-600">{a.sellerName}</td>
            <td className="hidden px-5 py-3.5 text-slate-600 lg:table-cell">{fmtXaf(a.purchasePrice)}</td>
            <td className="px-5 py-3.5 text-slate-600">{a.acquisitionDate ?? '—'}</td>
            <td className="px-5 py-3.5"><StatusBadge value={a.status} /></td>
            <td className="px-3 py-3.5">
              <RowActionsMenu items={[
                canApprove && a.status === 'PENDING' && { label: 'Approve', onClick: () => setAction({ type: 'approve', row: a }) },
                canApprove && a.status === 'APPROVED' && { label: 'Complete', onClick: () => setAction({ type: 'complete', row: a }) },
                canCreate && (a.status === 'PENDING' || a.status === 'APPROVED') && { label: 'Cancel', danger: true, onClick: () => setAction({ type: 'cancel', row: a }) },
              ].filter(Boolean)} />
            </td>
          </tr>
        ))}
      </DataTable>

      <Modal open={open} onClose={() => setOpen(false)} title="New Acquisition" width="max-w-xl">
        {formError && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{formError}</div>}
        <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2"><Field label="Parcel *"><ParcelSelect value={form.parcelId} onChange={(v) => setForm((f) => ({ ...f, parcelId: v }))} /></Field></div>
          <Field label="Seller name *"><input required className={inputCls} value={form.sellerName} onChange={set('sellerName')} placeholder="Bokwango Family Trust" /></Field>
          <Field label="Seller reference"><input className={inputCls} value={form.sellerReference} onChange={set('sellerReference')} /></Field>
          <Field label="Seller contact"><input className={inputCls} value={form.sellerContact} onChange={set('sellerContact')} placeholder="+237 …" /></Field>
          <Field label="Acquisition date"><input type="date" className={inputCls} value={form.acquisitionDate} onChange={set('acquisitionDate')} /></Field>
          <Field label="Purchase price (XAF)"><input type="number" min="0" className={inputCls} value={form.purchasePrice} onChange={set('purchasePrice')} placeholder="45000000" /></Field>
          <Field label="Document reference"><input className={inputCls} value={form.documentReference} onChange={set('documentReference')} /></Field>
          <div className="sm:col-span-2">
            <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
              {saving ? 'Saving…' : 'Register acquisition'}
            </button>
          </div>
        </form>
      </Modal>

      <DecisionModal action={action} onClose={() => setAction(null)} onDone={() => { setAction(null); load() }} />
    </div>
  )
}
