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
import { StatusBadge, label } from './landShared'
import { Plus, History } from 'lucide-react'

const EMPTY = { parcelId: '', ownerName: 'PCC Cameroon', ownerReference: '', titleReference: '', sharePercent: '', acquisitionDate: '', effectiveDate: '', documentReference: '' }

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-slate-600">{label}</span>
      {children}
    </label>
  )
}

export default function OwnershipPage() {
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
  const [historyParcel, setHistoryParcel] = useState(null)
  const [history, setHistory] = useState(null)

  const [searchParams, setSearchParams] = useSearchParams()

  const canCreate = hasPermission('LAND_OWNERSHIP_CREATE')

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
    const res = await landFetch(`/land-ownership?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load ownership records — is the Land Service running on :8083?')
    setLoading(false)
  }, [page, statusFilter, search])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  useEffect(() => {
    if (!historyParcel) { setHistory(null); return }
    landFetch(`/land-ownership/history/${historyParcel.parcelId}`).then(async (r) => setHistory(r?.ok ? await r.json() : []))
  }, [historyParcel])

  const submit = async (e) => {
    e.preventDefault()
    if (!form.parcelId || !form.ownerName.trim() || !form.effectiveDate) {
      setFormError('Parcel, owner name and effective date are required'); return
    }
    setSaving(true); setFormError('')
    const payload = {
      ...form, parcelId: Number(form.parcelId),
      sharePercent: form.sharePercent === '' ? null : Number(form.sharePercent),
      acquisitionDate: form.acquisitionDate || null,
    }
    const res = await landFetch('/land-ownership', { method: 'POST', body: JSON.stringify(payload) })
    setSaving(false)
    if (!res.ok) { setFormError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    setOpen(false); setForm(EMPTY); toast('Ownership registered', 'success'); load()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const columns = [
    { label: 'Reference' }, { label: 'Parcel' }, { label: 'Owner' },
    { label: 'Title', className: 'hidden lg:table-cell' }, { label: 'Share' },
    { label: 'Effective' }, { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Land Ownership</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} ownership records` : 'Company ownership register'}</p>
        </div>
        {canCreate && (
          <button onClick={() => { setForm(EMPTY); setFormError(''); setOpen(true) }}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> Register Ownership
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search owner or parcel…" className={`${inputCls} max-w-xs`} />
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-40`}>
          <option value="">All statuses</option>
          <option value="CURRENT">Current</option>
          <option value="ENDED">Ended</option>
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No ownership records yet">
        {data?.content.map((o) => (
          <tr key={o.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5 font-mono text-xs font-semibold text-pcc-700">{o.ownershipReference}</td>
            <td className="px-5 py-3.5">
              <span className="text-ink">{o.parcelName}</span>
              <span className="ml-2 font-mono text-xs text-slate-400">{o.parcelReference}</span>
            </td>
            <td className="px-5 py-3.5 text-slate-600">{o.ownerName}</td>
            <td className="hidden px-5 py-3.5 font-mono text-xs text-slate-500 lg:table-cell">{o.titleReference ?? '—'}</td>
            <td className="px-5 py-3.5 text-slate-600">{o.sharePercent}%</td>
            <td className="px-5 py-3.5 text-slate-600">{o.effectiveDate}{o.endDate ? ` → ${o.endDate}` : ''}</td>
            <td className="px-5 py-3.5"><StatusBadge value={o.status} /></td>
            <td className="px-3 py-3.5">
              <RowActionsMenu items={[{ label: 'Ownership history', icon: History, onClick: () => setHistoryParcel(o) }]} />
            </td>
          </tr>
        ))}
      </DataTable>

      <Modal open={open} onClose={() => setOpen(false)} title="Register Ownership" width="max-w-xl">
        {formError && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{formError}</div>}
        <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2"><Field label="Parcel *"><ParcelSelect value={form.parcelId} onChange={(v) => setForm((f) => ({ ...f, parcelId: v }))} /></Field></div>
          <Field label="Owner name *"><input required className={inputCls} value={form.ownerName} onChange={set('ownerName')} /></Field>
          <Field label="Owner reference"><input className={inputCls} value={form.ownerReference} onChange={set('ownerReference')} placeholder="RC/DLA/2019/B/1234" /></Field>
          <Field label="Title reference"><input className={inputCls} value={form.titleReference} onChange={set('titleReference')} placeholder="TC-4421/BUEA" /></Field>
          <Field label="Share %"><input type="number" step="0.01" min="0.01" max="100" className={inputCls} value={form.sharePercent} onChange={set('sharePercent')} placeholder="100" /></Field>
          <Field label="Acquisition date"><input type="date" className={inputCls} value={form.acquisitionDate} onChange={set('acquisitionDate')} /></Field>
          <Field label="Effective date *"><input type="date" required className={inputCls} value={form.effectiveDate} onChange={set('effectiveDate')} /></Field>
          <div className="sm:col-span-2"><Field label="Document reference"><input className={inputCls} value={form.documentReference} onChange={set('documentReference')} /></Field></div>
          <div className="sm:col-span-2">
            <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
              {saving ? 'Saving…' : 'Register ownership'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={!!historyParcel} onClose={() => setHistoryParcel(null)}
        title={`Ownership history — ${historyParcel?.parcelReference}`} width="max-w-lg">
        {history === null ? <p className="text-sm text-slate-400">Loading…</p>
          : history.length === 0 ? <p className="text-sm text-slate-400">No ownership events recorded.</p>
          : <ul className="space-y-2">
              {history.map((h) => (
                <li key={h.id} className="rounded-xl border border-slate-100 px-4 py-3 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-pcc-700">{label(h.eventType)}</span>
                    <span className="text-xs text-slate-400">{h.effectiveDate ?? ''}</span>
                  </div>
                  <p className="mt-0.5 text-slate-600">
                    {h.previousOwner ? `${h.previousOwner} → ` : ''}{h.newOwner ?? '—'}
                  </p>
                  <p className="mt-0.5 text-xs text-slate-400">{h.performedByName ?? 'system'}{h.note ? ` · ${h.note}` : ''}</p>
                </li>
              ))}
            </ul>}
      </Modal>
    </div>
  )
}
