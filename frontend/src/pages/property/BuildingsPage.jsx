import { useCallback, useEffect, useState } from 'react'
import { propertyFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { inputCls } from './PropertiesPage'
import { Plus } from 'lucide-react'

const STATUSES = ['ACTIVE', 'UNDER_MAINTENANCE', 'INACTIVE']
const EMPTY = { propertyId: '', name: '', numberOfFloors: '', constructionYear: '', description: '', status: 'ACTIVE' }

function BuildingForm({ open, onClose, onSaved, building, properties, saving, setSaving }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setError('')
      setForm(building ? {
        propertyId: building.propertyId, name: building.name,
        numberOfFloors: building.numberOfFloors ?? '', constructionYear: building.constructionYear ?? '',
        description: building.description ?? '', status: building.status,
      } : { ...EMPTY, propertyId: properties[0]?.id ?? '' })
    }
  }, [open, building, properties])

  const submit = async (e) => {
    e.preventDefault()
    if (!form.propertyId) { setError('Select a property'); return }
    if (!form.name.trim()) { setError('Name is required'); return }
    setSaving(true); setError('')
    const payload = {
      ...form,
      propertyId: Number(form.propertyId),
      numberOfFloors: form.numberOfFloors === '' ? null : Number(form.numberOfFloors),
      constructionYear: form.constructionYear === '' ? null : Number(form.constructionYear),
    }
    const res = await propertyFetch(building ? `/buildings/${building.id}` : '/buildings', {
      method: building ? 'PUT' : 'POST', body: JSON.stringify(payload),
    })
    setSaving(false)
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    onSaved()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  return (
    <Modal open={open} onClose={onClose} title={building ? `Edit ${building.code}` : 'New Building'}>
      {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
      <form onSubmit={submit} className="space-y-4">
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Property *</span>
          <select className={inputCls} value={form.propertyId} onChange={set('propertyId')}>
            <option value="">Select property…</option>
            {properties.map((p) => <option key={p.id} value={p.id}>{p.code} — {p.name}</option>)}
          </select>
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Building name *</span>
          <input className={inputCls} value={form.name} onChange={set('name')} placeholder="Tower A" />
        </label>
        <div className="grid grid-cols-2 gap-4">
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-slate-600">Floors</span>
            <input type="number" min="1" className={inputCls} value={form.numberOfFloors} onChange={set('numberOfFloors')} />
          </label>
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-slate-600">Construction year</span>
            <input type="number" min="1800" max="2100" className={inputCls} value={form.constructionYear} onChange={set('constructionYear')} />
          </label>
        </div>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Status</span>
          <select className={inputCls} value={form.status} onChange={set('status')}>
            {STATUSES.map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}
          </select>
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Description</span>
          <textarea className={`${inputCls} h-20 py-2.5`} value={form.description} onChange={set('description')} />
        </label>
        <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
          {saving ? 'Saving…' : building ? 'Save changes' : 'Create building'}
        </button>
      </form>
    </Modal>
  )
}

export default function BuildingsPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [properties, setProperties] = useState([])
  const [propertyFilter, setPropertyFilter] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editB, setEditB] = useState(null)
  const [saving, setSaving] = useState(false)

  const canCreate = hasPermission('BUILDING_CREATE')
  const canUpdate = hasPermission('BUILDING_UPDATE')

  useEffect(() => {
    if (hasPermission('PROPERTY_VIEW')) {
      propertyFetch('/properties?size=100').then(async (r) => r.ok && setProperties((await r.json()).content)).catch(() => {})
    }
  }, [hasPermission])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (propertyFilter) q.set('propertyId', propertyFilter)
    if (search) q.set('search', search)
    const res = await propertyFetch(`/buildings?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load buildings')
    setLoading(false)
  }, [page, propertyFilter, search])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  const columns = [
    { label: 'Building' }, { label: 'Code' }, { label: 'Property' },
    { label: 'Floors' }, { label: 'Year' }, { label: 'Apartments' }, { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Buildings</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} buildings` : 'Structures on company properties'}</p>
        </div>
        {canCreate && properties.length > 0 && (
          <button onClick={() => { setEditB(null); setFormOpen(true) }}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> New Building
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search name or code…" className={`${inputCls} max-w-xs`} />
        <select value={propertyFilter} onChange={(e) => { setPropertyFilter(e.target.value); setPage(0) }} className={`${inputCls} w-56`}>
          <option value="">All properties</option>
          {properties.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage} empty="No buildings yet">
        {data?.content.map((b) => (
          <tr key={b.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5 font-medium text-ink">{b.name}</td>
            <td className="px-5 py-3.5 font-mono text-xs text-slate-500">{b.code}</td>
            <td className="px-5 py-3.5 text-slate-600">{b.propertyName}</td>
            <td className="px-5 py-3.5 text-slate-600">{b.numberOfFloors ?? '—'}</td>
            <td className="px-5 py-3.5 text-slate-600">{b.constructionYear ?? '—'}</td>
            <td className="px-5 py-3.5 text-slate-600">{b.apartmentCount ?? '—'}</td>
            <td className="px-5 py-3.5">
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-semibold text-slate-600">{b.status.replaceAll('_', ' ')}</span>
            </td>
            <td className="px-3 py-3.5">
              {canUpdate && <RowActionsMenu items={[{ label: 'Edit', onClick: () => { setEditB(b); setFormOpen(true) } }]} />}
            </td>
          </tr>
        ))}
      </DataTable>

      <BuildingForm open={formOpen} onClose={() => setFormOpen(false)} building={editB} properties={properties}
        saving={saving} setSaving={setSaving}
        onSaved={() => { setFormOpen(false); toast(editB ? 'Building updated' : 'Building created', 'success'); load() }} />
    </div>
  )
}
