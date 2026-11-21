import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { propertyFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import EntityDocumentsPanel from '../documents/EntityDocumentsPanel'
import ConfirmDialog from '../../components/ConfirmDialog'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { Plus, Building2 } from 'lucide-react'

const TYPES = ['RESIDENTIAL', 'COMMERCIAL', 'MIXED', 'INDUSTRIAL', 'LAND_ONLY']
const STATUSES = ['ACTIVE', 'UNDER_MAINTENANCE', 'DISPOSED', 'INACTIVE']

const STATUS_STYLES = {
  ACTIVE: 'bg-emerald-50 text-emerald-700',
  UNDER_MAINTENANCE: 'bg-amber-50 text-amber-700',
  DISPOSED: 'bg-slate-100 text-slate-500',
  INACTIVE: 'bg-red-50 text-red-600',
}

const EMPTY = { name: '', propertyType: 'COMMERCIAL', description: '', acquisitionDate: '', acquisitionCost: '', addressLine1: '', city: '', region: '', country: 'Cameroon' }

const fmt = (n) => n == null ? '—' : `${Number(n).toLocaleString('en-US')} XAF`

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-slate-600">{label}</span>
      {children}
    </label>
  )
}

export const inputCls = 'h-10 w-full rounded-xl border border-slate-200 px-3.5 text-sm text-ink placeholder:text-slate-400 focus:border-pcc-400 focus:outline-none focus:ring-2 focus:ring-pcc-100'

function PropertyForm({ open, onClose, onSaved, property, saving, setSaving }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setError('')
      setForm(property ? {
        name: property.name, propertyType: property.propertyType, description: property.description ?? '',
        acquisitionDate: property.acquisitionDate ?? '', acquisitionCost: property.acquisitionCost ?? '',
        addressLine1: property.address?.line1 ?? '', city: property.address?.city ?? '',
        region: property.address?.region ?? '', country: property.address?.country ?? 'Cameroon',
      } : EMPTY)
    }
  }, [open, property])

  const submit = async (e) => {
    e.preventDefault()
    if (!form.name.trim()) { setError('Name is required'); return }
    setSaving(true); setError('')
    const payload = {
      ...form,
      acquisitionDate: form.acquisitionDate || null,
      acquisitionCost: form.acquisitionCost === '' ? null : Number(form.acquisitionCost),
    }
    const res = await propertyFetch(property ? `/properties/${property.id}` : '/properties', {
      method: property ? 'PUT' : 'POST', body: JSON.stringify(payload),
    })
    setSaving(false)
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    onSaved()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  return (
    <Modal open={open} onClose={onClose} title={property ? `Edit ${property.code}` : 'New Property'} width="max-w-2xl">
      {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
      <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Field label="Property name *">
          <input className={inputCls} value={form.name} onChange={set('name')} placeholder="Akwa Office Plaza" />
        </Field>
        <Field label="Type *">
          <select className={inputCls} value={form.propertyType} onChange={set('propertyType')}>
            {TYPES.map((t) => <option key={t} value={t}>{t.replaceAll('_', ' ')}</option>)}
          </select>
        </Field>
        <div className="sm:col-span-2">
          <Field label="Description">
            <textarea className={`${inputCls} h-20 py-2.5`} value={form.description} onChange={set('description')} />
          </Field>
        </div>
        <Field label="Address">
          <input className={inputCls} value={form.addressLine1} onChange={set('addressLine1')} placeholder="Avenue de Gaulle" />
        </Field>
        <Field label="City">
          <input className={inputCls} value={form.city} onChange={set('city')} placeholder="Douala" />
        </Field>
        <Field label="Region">
          <input className={inputCls} value={form.region} onChange={set('region')} placeholder="Littoral" />
        </Field>
        <Field label="Country">
          <input className={inputCls} value={form.country} onChange={set('country')} />
        </Field>
        <Field label="Acquisition date">
          <input type="date" className={inputCls} value={form.acquisitionDate} onChange={set('acquisitionDate')} />
        </Field>
        <Field label="Acquisition cost (XAF)">
          <input type="number" min="0" className={inputCls} value={form.acquisitionCost} onChange={set('acquisitionCost')} placeholder="250000000" />
        </Field>
        <div className="sm:col-span-2">
          <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
            {saving ? 'Saving…' : property ? 'Save changes' : 'Create property'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default function PropertiesPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editProp, setEditProp] = useState(null)
  const [viewProp, setViewProp] = useState(null)
  const [confirmDel, setConfirmDel] = useState(null)
  const [statusProp, setStatusProp] = useState(null)
  const [busy, setBusy] = useState(false)
  const [saving, setSaving] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()

  const canCreate = hasPermission('PROPERTY_CREATE')
  const canUpdate = hasPermission('PROPERTY_UPDATE')
  const canDelete = hasPermission('PROPERTY_DELETE')

  useEffect(() => {
    if (searchParams.get('new') === '1' && canCreate) {
      setFormOpen(true); setEditProp(null)
      setSearchParams({}, { replace: true })
    }
  }, [searchParams, canCreate, setSearchParams])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (search) q.set('search', search)
    if (typeFilter) q.set('type', typeFilter)
    if (statusFilter) q.set('status', statusFilter)
    const res = await propertyFetch(`/properties?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load properties — is the Property Service running on :8082?')
    setLoading(false)
  }, [page, search, typeFilter, statusFilter])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  const changeStatus = async (p, status) => {
    const res = await propertyFetch(`/properties/${p.id}/status`, {
      method: 'PATCH', body: JSON.stringify({ status, reason: 'Status updated from admin panel' }),
    })
    if (res.ok) { toast('Status updated', 'success'); load() }
    else toast('Status update failed', 'error')
  }

  const doDelete = async () => {
    setBusy(true)
    const res = await propertyFetch(`/properties/${confirmDel.id}`, { method: 'DELETE' })
    setBusy(false); setConfirmDel(null)
    if (res.ok || res.status === 204) { toast('Property archived', 'success'); load() }
    else toast('Delete failed', 'error')
  }

  const columns = [
    { label: 'Property' }, { label: 'Code' }, { label: 'Type' }, { label: 'Location' },
    { label: 'Cost', className: 'hidden lg:table-cell' }, { label: 'Buildings' }, { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Properties</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} registered assets` : 'Company property portfolio'}</p>
        </div>
        {canCreate && (
          <button onClick={() => { setEditProp(null); setFormOpen(true) }}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> New Property
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search name or code…"
          className={`${inputCls} max-w-xs`} />
        <select value={typeFilter} onChange={(e) => { setTypeFilter(e.target.value); setPage(0) }} className={`${inputCls} w-44`}>
          <option value="">All types</option>
          {TYPES.map((t) => <option key={t} value={t}>{t.replaceAll('_', ' ')}</option>)}
        </select>
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-44`}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No properties yet — register the first one">
        {data?.content.map((p) => (
          <tr key={p.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5">
              <button onClick={() => setViewProp(p)} className="text-left font-medium text-ink hover:text-pcc-700">{p.name}</button>
            </td>
            <td className="px-5 py-3.5 font-mono text-xs text-slate-500">{p.code}</td>
            <td className="px-5 py-3.5 text-slate-600">{p.propertyType.replaceAll('_', ' ')}</td>
            <td className="px-5 py-3.5 text-slate-600">{p.address?.city ?? '—'}{p.address?.region ? `, ${p.address.region}` : ''}</td>
            <td className="hidden px-5 py-3.5 text-slate-600 lg:table-cell">{fmt(p.acquisitionCost)}</td>
            <td className="px-5 py-3.5 text-slate-600">{p.buildingCount ?? '—'}</td>
            <td className="px-5 py-3.5">
              <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${STATUS_STYLES[p.status]}`}>
                {p.status.replaceAll('_', ' ')}
              </span>
            </td>
            <td className="px-3 py-3.5">
              <RowActionsMenu items={[
                { label: 'View details', onClick: () => setViewProp(p) },
                canUpdate && { label: 'Edit', onClick: () => { setEditProp(p); setFormOpen(true) } },
                canUpdate && { label: 'Change status', onClick: () => setStatusProp(p) },
                canDelete && { label: 'Archive', danger: true, onClick: () => setConfirmDel(p) },
              ].filter(Boolean)} />
            </td>
          </tr>
        ))}
      </DataTable>

      <PropertyForm open={formOpen} onClose={() => setFormOpen(false)} property={editProp}
        saving={saving} setSaving={setSaving}
        onSaved={() => { setFormOpen(false); toast(editProp ? 'Property updated' : 'Property created', 'success'); load() }} />

      <Modal open={!!viewProp} onClose={() => setViewProp(null)} title={viewProp?.name ?? ''} width="max-w-lg">
        {viewProp && (
          <dl className="grid grid-cols-2 gap-x-6 gap-y-4 text-sm">
            <div><dt className="text-xs text-slate-400">Code</dt><dd className="font-mono">{viewProp.code}</dd></div>
            <div><dt className="text-xs text-slate-400">Type</dt><dd>{viewProp.propertyType.replaceAll('_', ' ')}</dd></div>
            <div><dt className="text-xs text-slate-400">Status</dt><dd>{viewProp.status.replaceAll('_', ' ')}</dd></div>
            <div><dt className="text-xs text-slate-400">Acquisition cost</dt><dd>{fmt(viewProp.acquisitionCost)}</dd></div>
            <div><dt className="text-xs text-slate-400">Acquired</dt><dd>{viewProp.acquisitionDate ?? '—'}</dd></div>
            <div><dt className="text-xs text-slate-400">Registered by</dt><dd>{viewProp.createdByName ?? '—'}</dd></div>
            <div className="col-span-2"><dt className="text-xs text-slate-400">Address</dt>
              <dd>{[viewProp.address?.line1, viewProp.address?.city, viewProp.address?.region, viewProp.address?.country].filter(Boolean).join(', ') || '—'}</dd></div>
            {viewProp.description && <div className="col-span-2"><dt className="text-xs text-slate-400">Description</dt><dd>{viewProp.description}</dd></div>}
          </dl>
        )}
        {viewProp && <EntityDocumentsPanel entityType="PROPERTY" entityRef={viewProp.id} domain="PROPERTY" />}
      </Modal>

      <Modal open={!!statusProp} onClose={() => setStatusProp(null)} title={`Status — ${statusProp?.code}`} width="max-w-sm">
        <div className="space-y-2">
          {STATUSES.filter((s) => s !== statusProp?.status).map((s) => (
            <button key={s} onClick={() => { changeStatus(statusProp, s); setStatusProp(null) }}
              className="flex w-full items-center gap-3 rounded-xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-600 transition-colors hover:border-pcc-300 hover:bg-pcc-50 hover:text-pcc-700">
              <Building2 className="h-4 w-4" /> {s.replaceAll('_', ' ')}
            </button>
          ))}
        </div>
      </Modal>

      <ConfirmDialog open={!!confirmDel} onClose={() => setConfirmDel(null)} onConfirm={doDelete} loading={busy}
        title="Archive property" confirmLabel="Archive"
        message={`Archive ${confirmDel?.name}? It will be hidden from listings. Buildings and apartments stay in the database.`} />
    </div>
  )
}
