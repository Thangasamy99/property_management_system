import { useCallback, useEffect, useState } from 'react'
import { propertyFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { inputCls } from './PropertiesPage'
import { Plus } from 'lucide-react'

const STATUSES = ['AVAILABLE', 'OCCUPIED', 'RESERVED', 'UNDER_MAINTENANCE', 'INACTIVE']
const STATUS_STYLES = {
  AVAILABLE: 'bg-emerald-50 text-emerald-700',
  OCCUPIED: 'bg-pcc-50 text-pcc-700',
  RESERVED: 'bg-amber-50 text-amber-700',
  UNDER_MAINTENANCE: 'bg-orange-50 text-orange-600',
  INACTIVE: 'bg-slate-100 text-slate-500',
}
const EMPTY = { buildingId: '', apartmentNumber: '', floorNumber: '', bedrooms: '', bathrooms: '', areaSqm: '', monthlyRent: '', description: '', status: 'AVAILABLE' }

const fmt = (n) => n == null ? '—' : `${Number(n).toLocaleString('en-US')} XAF`

function ApartmentForm({ open, onClose, onSaved, apartment, buildings, saving, setSaving }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setError('')
      setForm(apartment ? {
        buildingId: apartment.buildingId, apartmentNumber: apartment.apartmentNumber,
        floorNumber: apartment.floorNumber ?? '', bedrooms: apartment.bedrooms ?? '',
        bathrooms: apartment.bathrooms ?? '', areaSqm: apartment.areaSqm ?? '',
        monthlyRent: apartment.monthlyRent ?? '', description: apartment.description ?? '',
        status: apartment.status,
      } : { ...EMPTY, buildingId: buildings[0]?.id ?? '' })
    }
  }, [open, apartment, buildings])

  const submit = async (e) => {
    e.preventDefault()
    if (!form.buildingId) { setError('Select a building'); return }
    if (!form.apartmentNumber.trim()) { setError('Apartment number is required'); return }
    setSaving(true); setError('')
    const num = (v) => (v === '' ? null : Number(v))
    const payload = { ...form, buildingId: Number(form.buildingId), floorNumber: num(form.floorNumber), bedrooms: num(form.bedrooms), bathrooms: num(form.bathrooms), areaSqm: num(form.areaSqm), monthlyRent: num(form.monthlyRent) }
    const res = await propertyFetch(apartment ? `/apartments/${apartment.id}` : '/apartments', {
      method: apartment ? 'PUT' : 'POST', body: JSON.stringify(payload),
    })
    setSaving(false)
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    onSaved()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  return (
    <Modal open={open} onClose={onClose} title={apartment ? `Edit ${apartment.apartmentNumber}` : 'New Apartment'} width="max-w-2xl">
      {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
      <form onSubmit={submit} className="grid grid-cols-2 gap-4">
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Building *</span>
          <select className={inputCls} value={form.buildingId} onChange={set('buildingId')}>
            <option value="">Select building…</option>
            {buildings.map((b) => <option key={b.id} value={b.id}>{b.code} — {b.name}</option>)}
          </select>
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Apartment # *</span>
          <input className={inputCls} value={form.apartmentNumber} onChange={set('apartmentNumber')} placeholder="A-01" />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Floor</span>
          <input type="number" min="0" className={inputCls} value={form.floorNumber} onChange={set('floorNumber')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Bedrooms</span>
          <input type="number" min="0" className={inputCls} value={form.bedrooms} onChange={set('bedrooms')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Bathrooms</span>
          <input type="number" min="0" className={inputCls} value={form.bathrooms} onChange={set('bathrooms')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Area (m²)</span>
          <input type="number" min="0" step="0.1" className={inputCls} value={form.areaSqm} onChange={set('areaSqm')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Monthly rent (XAF)</span>
          <input type="number" min="0" className={inputCls} value={form.monthlyRent} onChange={set('monthlyRent')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Status</span>
          <select className={inputCls} value={form.status} onChange={set('status')}>
            {STATUSES.map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}
          </select>
        </label>
        <div className="col-span-2">
          <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
            {saving ? 'Saving…' : apartment ? 'Save changes' : 'Create apartment'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default function ApartmentsPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [buildings, setBuildings] = useState([])
  const [buildingFilter, setBuildingFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editA, setEditA] = useState(null)
  const [saving, setSaving] = useState(false)

  const canCreate = hasPermission('APARTMENT_CREATE')
  const canUpdate = hasPermission('APARTMENT_UPDATE')

  useEffect(() => {
    if (hasPermission('BUILDING_VIEW')) {
      propertyFetch('/buildings?size=100').then(async (r) => r.ok && setBuildings((await r.json()).content)).catch(() => {})
    }
  }, [hasPermission])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (buildingFilter) q.set('buildingId', buildingFilter)
    if (statusFilter) q.set('status', statusFilter)
    if (search) q.set('search', search)
    const res = await propertyFetch(`/apartments?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load apartments')
    setLoading(false)
  }, [page, buildingFilter, statusFilter, search])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  const columns = [
    { label: 'Apt #' }, { label: 'Building' }, { label: 'Floor' }, { label: 'Beds' },
    { label: 'Baths', className: 'hidden md:table-cell' }, { label: 'Area', className: 'hidden md:table-cell' },
    { label: 'Rent/mo' }, { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Apartments</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} units` : 'Rentable units across buildings'}</p>
        </div>
        {canCreate && buildings.length > 0 && (
          <button onClick={() => { setEditA(null); setFormOpen(true) }}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> New Apartment
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search apartment #…" className={`${inputCls} max-w-xs`} />
        <select value={buildingFilter} onChange={(e) => { setBuildingFilter(e.target.value); setPage(0) }} className={`${inputCls} w-52`}>
          <option value="">All buildings</option>
          {buildings.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </select>
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-48`}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage} empty="No apartments yet">
        {data?.content.map((a) => (
          <tr key={a.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5 font-medium text-ink">{a.apartmentNumber}</td>
            <td className="px-5 py-3.5 text-slate-600">{a.buildingName}<span className="block text-xs text-slate-400">{a.propertyName}</span></td>
            <td className="px-5 py-3.5 text-slate-600">{a.floorNumber ?? '—'}</td>
            <td className="px-5 py-3.5 text-slate-600">{a.bedrooms ?? '—'}</td>
            <td className="hidden px-5 py-3.5 text-slate-600 md:table-cell">{a.bathrooms ?? '—'}</td>
            <td className="hidden px-5 py-3.5 text-slate-600 md:table-cell">{a.areaSqm ? `${a.areaSqm} m²` : '—'}</td>
            <td className="px-5 py-3.5 text-slate-600">{fmt(a.monthlyRent)}</td>
            <td className="px-5 py-3.5">
              <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${STATUS_STYLES[a.status]}`}>{a.status.replaceAll('_', ' ')}</span>
            </td>
            <td className="px-3 py-3.5">
              {canUpdate && <RowActionsMenu items={[{ label: 'Edit', onClick: () => { setEditA(a); setFormOpen(true) } }]} />}
            </td>
          </tr>
        ))}
      </DataTable>

      <ApartmentForm open={formOpen} onClose={() => setFormOpen(false)} apartment={editA} buildings={buildings}
        saving={saving} setSaving={setSaving}
        onSaved={() => { setFormOpen(false); toast(editA ? 'Apartment updated' : 'Apartment created', 'success'); load() }} />
    </div>
  )
}
