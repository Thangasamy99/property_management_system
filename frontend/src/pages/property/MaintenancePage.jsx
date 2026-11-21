import { useCallback, useEffect, useState } from 'react'
import { propertyFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { inputCls } from './PropertiesPage'
import { Plus } from 'lucide-react'

const CATEGORIES = ['PLUMBING', 'ELECTRICAL', 'STRUCTURAL', 'PAINTING', 'SECURITY', 'CLEANING', 'OTHER']
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
const STATUSES = ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED']
const STATUS_STYLES = {
  OPEN: 'bg-sky-50 text-sky-700',
  IN_PROGRESS: 'bg-amber-50 text-amber-700',
  ON_HOLD: 'bg-slate-100 text-slate-500',
  COMPLETED: 'bg-emerald-50 text-emerald-700',
  CANCELLED: 'bg-red-50 text-red-600',
}
const PRIORITY_STYLES = { LOW: 'text-slate-400', MEDIUM: 'text-sky-600', HIGH: 'text-amber-600', URGENT: 'text-red-600' }

function RequestForm({ open, onClose, onSaved, apartments, buildings, saving, setSaving }) {
  const [form, setForm] = useState({ target: '', apartmentId: '', buildingId: '', category: 'PLUMBING', description: '', priority: 'MEDIUM' })
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) { setError(''); setForm({ target: 'apartment', apartmentId: '', buildingId: '', category: 'PLUMBING', description: '', priority: 'MEDIUM' }) }
  }, [open])

  const submit = async (e) => {
    e.preventDefault()
    const payload = {
      category: form.category, description: form.description, priority: form.priority,
      apartmentId: form.target === 'apartment' && form.apartmentId ? Number(form.apartmentId) : null,
      buildingId: form.target === 'building' && form.buildingId ? Number(form.buildingId) : null,
    }
    if (!payload.apartmentId && !payload.buildingId) { setError('Pick an apartment or a building'); return }
    if (!form.description.trim()) { setError('Description is required'); return }
    setSaving(true); setError('')
    const res = await propertyFetch('/maintenance', { method: 'POST', body: JSON.stringify(payload) })
    setSaving(false)
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    onSaved()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  return (
    <Modal open={open} onClose={onClose} title="New Maintenance Request">
      {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
      <form onSubmit={submit} className="space-y-4">
        <div className="flex gap-2">
          {['apartment', 'building'].map((t) => (
            <button type="button" key={t} onClick={() => set('target')({ target: { value: t } })}
              className={`flex-1 rounded-xl border px-3 py-2.5 text-sm font-medium capitalize transition-colors ${form.target === t ? 'border-pcc-400 bg-pcc-50 text-pcc-700' : 'border-slate-200 text-slate-500'}`}>
              {t}
            </button>
          ))}
        </div>
        {form.target === 'apartment' ? (
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-slate-600">Apartment *</span>
            <select className={inputCls} value={form.apartmentId} onChange={set('apartmentId')}>
              <option value="">Select…</option>
              {apartments.map((a) => <option key={a.id} value={a.id}>{a.buildingName} · {a.apartmentNumber}</option>)}
            </select>
          </label>
        ) : (
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-slate-600">Building *</span>
            <select className={inputCls} value={form.buildingId} onChange={set('buildingId')}>
              <option value="">Select…</option>
              {buildings.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
            </select>
          </label>
        )}
        <div className="grid grid-cols-2 gap-4">
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-slate-600">Category *</span>
            <select className={inputCls} value={form.category} onChange={set('category')}>
              {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
            </select>
          </label>
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-slate-600">Priority</span>
            <select className={inputCls} value={form.priority} onChange={set('priority')}>
              {PRIORITIES.map((p) => <option key={p}>{p}</option>)}
            </select>
          </label>
        </div>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Issue description *</span>
          <textarea className={`${inputCls} h-24 py-2.5`} value={form.description} onChange={set('description')} placeholder="Describe the problem…" />
        </label>
        <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
          {saving ? 'Creating…' : 'Create request'}
        </button>
      </form>
    </Modal>
  )
}

export default function MaintenancePage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [apartments, setApartments] = useState([])
  const [buildings, setBuildings] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [histReq, setHistReq] = useState(null)
  const [history, setHistory] = useState(null)
  const [saving, setSaving] = useState(false)

  const canManage = hasPermission('MAINTENANCE_MANAGE')

  useEffect(() => {
    if (hasPermission('APARTMENT_VIEW')) propertyFetch('/apartments?size=100').then(async (r) => r.ok && setApartments((await r.json()).content)).catch(() => {})
    if (hasPermission('BUILDING_VIEW')) propertyFetch('/buildings?size=100').then(async (r) => r.ok && setBuildings((await r.json()).content)).catch(() => {})
  }, [hasPermission])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (statusFilter) q.set('status', statusFilter)
    const res = await propertyFetch(`/maintenance?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load maintenance requests')
    setLoading(false)
  }, [page, statusFilter])

  useEffect(() => { load() }, [load])

  const setStatus = async (m, status) => {
    const res = await propertyFetch(`/maintenance/${m.id}`, { method: 'PATCH', body: JSON.stringify({ status }) })
    if (res.ok) { toast(`Marked ${status.replaceAll('_', ' ').toLowerCase()}`, 'success'); load() }
    else toast('Update failed', 'error')
  }

  const openHistory = async (m) => {
    setHistReq(m); setHistory(null)
    const r = await propertyFetch(`/maintenance/${m.id}/history`)
    if (r.ok) setHistory(await r.json())
  }

  const columns = [
    { label: 'Request' }, { label: 'Location' }, { label: 'Category' },
    { label: 'Priority' }, { label: 'Status' }, { label: 'Created' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Maintenance</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} requests` : 'Repairs and upkeep'}</p>
        </div>
        {canManage && (
          <button onClick={() => setFormOpen(true)}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> New Request
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-44`}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage} empty="No maintenance requests">
        {data?.content.map((m) => (
          <tr key={m.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5">
              <button onClick={() => openHistory(m)} className="font-mono text-xs font-semibold text-pcc-700 hover:underline">{m.requestNumber}</button>
              <p className="max-w-xs truncate text-xs text-slate-400">{m.description}</p>
            </td>
            <td className="px-5 py-3.5 text-slate-600">{m.buildingName ?? '—'}{m.apartmentNumber ? ` · ${m.apartmentNumber}` : ''}</td>
            <td className="px-5 py-3.5 text-slate-600">{m.category}</td>
            <td className={`px-5 py-3.5 text-xs font-bold ${PRIORITY_STYLES[m.priority]}`}>{m.priority}</td>
            <td className="px-5 py-3.5">
              <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${STATUS_STYLES[m.status]}`}>{m.status.replaceAll('_', ' ')}</span>
            </td>
            <td className="px-5 py-3.5 text-xs text-slate-400">{new Date(m.createdAt).toLocaleDateString()}</td>
            <td className="px-3 py-3.5">
              <RowActionsMenu items={[
                { label: 'History', onClick: () => openHistory(m) },
                canManage && m.status === 'OPEN' && { label: 'Start work', onClick: () => setStatus(m, 'IN_PROGRESS') },
                canManage && m.status === 'IN_PROGRESS' && { label: 'Mark completed', onClick: () => setStatus(m, 'COMPLETED') },
                canManage && (m.status === 'OPEN' || m.status === 'IN_PROGRESS') && { label: 'Cancel', danger: true, onClick: () => setStatus(m, 'CANCELLED') },
              ].filter(Boolean)} />
            </td>
          </tr>
        ))}
      </DataTable>

      <RequestForm open={formOpen} onClose={() => setFormOpen(false)} apartments={apartments} buildings={buildings}
        saving={saving} setSaving={setSaving}
        onSaved={() => { setFormOpen(false); toast('Maintenance request created', 'success'); load() }} />

      <Modal open={!!histReq} onClose={() => setHistReq(null)} title={histReq?.requestNumber ?? ''} width="max-w-md">
        {history == null ? <p className="py-8 text-center text-sm text-slate-400">Loading…</p> : (
          <ul className="space-y-3">
            {history.map((h, i) => (
              <li key={i} className="flex gap-3 text-sm">
                <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-pcc-500" />
                <div>
                  <p className="font-medium text-ink">{h.event}</p>
                  <p className="text-xs text-slate-400">{h.note} · {h.actorUsername} · {new Date(h.createdAt).toLocaleString()}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Modal>
    </div>
  )
}
