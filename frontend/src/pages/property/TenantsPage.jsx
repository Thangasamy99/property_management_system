import { useCallback, useEffect, useState } from 'react'
import { propertyFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { inputCls } from './PropertiesPage'
import { Plus } from 'lucide-react'

const EMPTY = { tenantType: 'INDIVIDUAL', name: '', email: '', phone: '', address: '', identificationType: '', identificationNumber: '', emergencyContactName: '', emergencyContactPhone: '', status: 'ACTIVE' }

function TenantForm({ open, onClose, onSaved, tenant, saving, setSaving }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setError('')
      setForm(tenant ? {
        tenantType: tenant.tenantType, name: tenant.name, email: tenant.email ?? '', phone: tenant.phone ?? '',
        address: tenant.address ?? '', identificationType: tenant.identificationType ?? '',
        identificationNumber: tenant.identificationNumber ?? '', emergencyContactName: tenant.emergencyContactName ?? '',
        emergencyContactPhone: tenant.emergencyContactPhone ?? '', status: tenant.status,
      } : EMPTY)
    }
  }, [open, tenant])

  const submit = async (e) => {
    e.preventDefault()
    if (!form.name.trim()) { setError('Name is required'); return }
    setSaving(true); setError('')
    const res = await propertyFetch(tenant ? `/tenants/${tenant.id}` : '/tenants', {
      method: tenant ? 'PUT' : 'POST', body: JSON.stringify(form),
    })
    setSaving(false)
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    onSaved()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  return (
    <Modal open={open} onClose={onClose} title={tenant ? `Edit ${tenant.code}` : 'New Tenant'} width="max-w-2xl">
      {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
      <form onSubmit={submit} className="grid grid-cols-2 gap-4">
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Type *</span>
          <select className={inputCls} value={form.tenantType} onChange={set('tenantType')}>
            <option value="INDIVIDUAL">Individual</option>
            <option value="ORGANIZATION">Organization</option>
          </select>
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">{form.tenantType === 'ORGANIZATION' ? 'Organization name *' : 'Full name *'}</span>
          <input className={inputCls} value={form.name} onChange={set('name')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Email</span>
          <input type="email" className={inputCls} value={form.email} onChange={set('email')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Phone</span>
          <input className={inputCls} value={form.phone} onChange={set('phone')} placeholder="+2376XXXXXXXX" />
        </label>
        <label className="col-span-2 block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Address</span>
          <input className={inputCls} value={form.address} onChange={set('address')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">ID type</span>
          <select className={inputCls} value={form.identificationType} onChange={set('identificationType')}>
            <option value="">—</option>
            <option>NATIONAL_ID</option><option>PASSPORT</option><option>DRIVERS_LICENSE</option><option>BUSINESS_REGISTRATION</option>
          </select>
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">ID number</span>
          <input className={inputCls} value={form.identificationNumber} onChange={set('identificationNumber')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Emergency contact</span>
          <input className={inputCls} value={form.emergencyContactName} onChange={set('emergencyContactName')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Emergency phone</span>
          <input className={inputCls} value={form.emergencyContactPhone} onChange={set('emergencyContactPhone')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Status</span>
          <select className={inputCls} value={form.status} onChange={set('status')}>
            <option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option>
          </select>
        </label>
        <div className="col-span-2">
          <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
            {saving ? 'Saving…' : tenant ? 'Save changes' : 'Register tenant'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default function TenantsPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editT, setEditT] = useState(null)
  const [saving, setSaving] = useState(false)

  const canManage = hasPermission('TENANT_MANAGE')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (search) q.set('search', search)
    if (statusFilter) q.set('status', statusFilter)
    const res = await propertyFetch(`/tenants?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load tenants')
    setLoading(false)
  }, [page, search, statusFilter])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  const columns = [
    { label: 'Tenant' }, { label: 'Code' }, { label: 'Type' }, { label: 'Contact' },
    { label: 'ID', className: 'hidden lg:table-cell' }, { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Tenants</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} registered` : 'Individual and organization tenants'}</p>
        </div>
        {canManage && (
          <button onClick={() => { setEditT(null); setFormOpen(true) }}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> New Tenant
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search name, code or email…" className={`${inputCls} max-w-xs`} />
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-40`}>
          <option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option>
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage} empty="No tenants yet">
        {data?.content.map((t) => (
          <tr key={t.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5 font-medium text-ink">{t.name}</td>
            <td className="px-5 py-3.5 font-mono text-xs text-slate-500">{t.code}</td>
            <td className="px-5 py-3.5 text-slate-600">{t.tenantType === 'ORGANIZATION' ? 'Organization' : 'Individual'}</td>
            <td className="px-5 py-3.5 text-slate-600">{t.email ?? '—'}<span className="block text-xs text-slate-400">{t.phone ?? ''}</span></td>
            <td className="hidden px-5 py-3.5 text-slate-600 lg:table-cell">{t.identificationType ? `${t.identificationType.replaceAll('_', ' ')} · ${t.identificationNumber}` : '—'}</td>
            <td className="px-5 py-3.5">
              <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${t.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>{t.status}</span>
            </td>
            <td className="px-3 py-3.5">
              {canManage && <RowActionsMenu items={[{ label: 'Edit', onClick: () => { setEditT(t); setFormOpen(true) } }]} />}
            </td>
          </tr>
        ))}
      </DataTable>

      <TenantForm open={formOpen} onClose={() => setFormOpen(false)} tenant={editT}
        saving={saving} setSaving={setSaving}
        onSaved={() => { setFormOpen(false); toast(editT ? 'Tenant updated' : 'Tenant registered', 'success'); load() }} />
    </div>
  )
}
