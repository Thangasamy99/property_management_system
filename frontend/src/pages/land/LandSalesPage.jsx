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

const STATUSES = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'PAYMENT_PENDING', 'LEGAL_PROCESSING', 'COMPLETED', 'CANCELLED']
const EMPTY = { parcelId: '', buyerName: '', buyerReference: '', buyerContact: '', proposedPrice: '', agreementReference: '', agreementDate: '', documentReference: '' }
const PROGRESS_LABELS = { APPROVED: 'Mark payment pending', PAYMENT_PENDING: 'Enter legal processing', LEGAL_PROCESSING: 'Complete sale' }

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
  const [agreedPrice, setAgreedPrice] = useState('')
  const [busy, setBusy] = useState(false)
  const toast = useToast()

  useEffect(() => {
    setNote(''); setAgreedPrice(action?.row?.agreedPrice ?? action?.row?.proposedPrice ?? '')
  }, [action])

  const titles = { approve: 'Approve sale', reject: 'Reject sale', cancel: 'Cancel sale' }

  const run = async () => {
    setBusy(true)
    const body = action.type === 'approve'
      ? { note: note || null, agreedPrice: agreedPrice === '' ? null : Number(agreedPrice) }
      : { note: note || null }
    const res = await landFetch(`/land-sales/${action.row.id}/${action.type}`, {
      method: 'POST', body: JSON.stringify(body),
    })
    setBusy(false)
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Action failed', 'error'); return }
    toast('Done', 'success'); onDone()
  }

  const danger = action?.type === 'reject' || action?.type === 'cancel'

  return (
    <Modal open={!!action} onClose={onClose} title={action ? `${titles[action.type]} — ${action.row.saleReference}` : ''} width="max-w-sm">
      {action?.type === 'approve' && (
        <Field label="Agreed price (XAF)">
          <input type="number" min="0" className={`${inputCls} mb-3`} value={agreedPrice} onChange={(e) => setAgreedPrice(e.target.value)} />
        </Field>
      )}
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

export default function LandSalesPage() {
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

  const canRequest = hasPermission('LAND_SALE_REQUEST')
  const canApprove = hasPermission('LAND_SALE_APPROVE')

  useEffect(() => {
    if (searchParams.get('new') === '1' && canRequest) {
      setForm(EMPTY); setFormError(''); setOpen(true)
      setSearchParams({}, { replace: true })
    }
  }, [searchParams, canRequest, setSearchParams])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (statusFilter) q.set('status', statusFilter)
    if (search) q.set('search', search)
    const res = await landFetch(`/land-sales?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load sales — is the Land Service running on :8083?')
    setLoading(false)
  }, [page, statusFilter, search])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  const quick = async (id, step) => {
    const res = await landFetch(`/land-sales/${id}/${step}`, { method: 'POST', body: '{}' })
    if (!res.ok) { toast((await res.json().catch(() => ({}))).message ?? 'Action failed', 'error'); return }
    toast('Done', 'success'); load()
  }

  const submit = async (e) => {
    e.preventDefault()
    if (!form.parcelId || !form.buyerName.trim() || !form.proposedPrice) {
      setFormError('Parcel, buyer name and proposed price are required'); return
    }
    setSaving(true); setFormError('')
    const payload = {
      ...form, parcelId: Number(form.parcelId), proposedPrice: Number(form.proposedPrice),
      agreementDate: form.agreementDate || null,
    }
    const res = await landFetch('/land-sales', { method: 'POST', body: JSON.stringify(payload) })
    setSaving(false)
    if (!res.ok) { setFormError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    setOpen(false); setForm(EMPTY); toast('Sale request created', 'success'); load()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const columns = [
    { label: 'Reference' }, { label: 'Parcel' }, { label: 'Buyer' },
    { label: 'Price (XAF)', className: 'hidden lg:table-cell' }, { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  const actionsFor = (s) => [
    canRequest && s.status === 'DRAFT' && { label: 'Submit for review', onClick: () => quick(s.id, 'submit') },
    canApprove && s.status === 'SUBMITTED' && { label: 'Start review', onClick: () => quick(s.id, 'review') },
    canApprove && (s.status === 'SUBMITTED' || s.status === 'UNDER_REVIEW') && { label: 'Approve', onClick: () => setAction({ type: 'approve', row: s }) },
    canApprove && (s.status === 'SUBMITTED' || s.status === 'UNDER_REVIEW') && { label: 'Reject', danger: true, onClick: () => setAction({ type: 'reject', row: s }) },
    canApprove && PROGRESS_LABELS[s.status] && { label: PROGRESS_LABELS[s.status], onClick: () => quick(s.id, 'progress') },
    canRequest && ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'PAYMENT_PENDING'].includes(s.status)
      && { label: 'Cancel', danger: true, onClick: () => setAction({ type: 'cancel', row: s }) },
  ].filter(Boolean)

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Land Sales</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} sale records` : 'Sale request pipeline'}</p>
        </div>
        {canRequest && (
          <button onClick={() => { setForm(EMPTY); setFormError(''); setOpen(true) }}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> New Sale Request
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search reference, buyer or parcel…" className={`${inputCls} max-w-xs`} />
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-48`}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No sale requests yet">
        {data?.content.map((s) => (
          <tr key={s.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5 font-mono text-xs font-semibold text-pcc-700">{s.saleReference}</td>
            <td className="px-5 py-3.5">
              <span className="text-ink">{s.parcelName}</span>
              <span className="ml-2 font-mono text-xs text-slate-400">{s.parcelReference}</span>
            </td>
            <td className="px-5 py-3.5 text-slate-600">{s.buyerName}</td>
            <td className="hidden px-5 py-3.5 text-slate-600 lg:table-cell">
              {s.agreedPrice != null ? <span title="Agreed">{fmtXaf(s.agreedPrice)}</span> : fmtXaf(s.proposedPrice)}
            </td>
            <td className="px-5 py-3.5"><StatusBadge value={s.status} /></td>
            <td className="px-3 py-3.5">
              {actionsFor(s).length > 0 && <RowActionsMenu items={actionsFor(s)} />}
            </td>
          </tr>
        ))}
      </DataTable>

      <Modal open={open} onClose={() => setOpen(false)} title="New Sale Request" width="max-w-xl">
        {formError && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{formError}</div>}
        <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2"><Field label="Parcel *"><ParcelSelect value={form.parcelId} onChange={(v) => setForm((f) => ({ ...f, parcelId: v }))} /></Field></div>
          <Field label="Buyer name *"><input required className={inputCls} value={form.buyerName} onChange={set('buyerName')} placeholder="Douala Estates SARL" /></Field>
          <Field label="Buyer reference"><input className={inputCls} value={form.buyerReference} onChange={set('buyerReference')} /></Field>
          <Field label="Buyer contact"><input className={inputCls} value={form.buyerContact} onChange={set('buyerContact')} placeholder="+237 …" /></Field>
          <Field label="Proposed price (XAF) *"><input type="number" required min="1" className={inputCls} value={form.proposedPrice} onChange={set('proposedPrice')} placeholder="60000000" /></Field>
          <Field label="Agreement reference"><input className={inputCls} value={form.agreementReference} onChange={set('agreementReference')} /></Field>
          <Field label="Agreement date"><input type="date" className={inputCls} value={form.agreementDate} onChange={set('agreementDate')} /></Field>
          <div className="sm:col-span-2"><Field label="Document reference"><input className={inputCls} value={form.documentReference} onChange={set('documentReference')} /></Field></div>
          <div className="sm:col-span-2">
            <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
              {saving ? 'Saving…' : 'Create sale request'}
            </button>
          </div>
        </form>
      </Modal>

      <DecisionModal action={action} onClose={() => setAction(null)} onDone={() => { setAction(null); load() }} />
    </div>
  )
}
