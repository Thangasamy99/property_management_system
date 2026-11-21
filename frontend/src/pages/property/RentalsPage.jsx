import { useCallback, useEffect, useState } from 'react'
import { propertyFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import EntityDocumentsPanel from '../documents/EntityDocumentsPanel'
import ConfirmDialog from '../../components/ConfirmDialog'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { inputCls } from './PropertiesPage'
import { Plus } from 'lucide-react'

const STATUS_STYLES = {
  ACTIVE: 'bg-emerald-50 text-emerald-700',
  DRAFT: 'bg-slate-100 text-slate-600',
  EXPIRED: 'bg-amber-50 text-amber-700',
  TERMINATED: 'bg-red-50 text-red-600',
}
const fmt = (n) => n == null ? '—' : `${Number(n).toLocaleString('en-US')} XAF`

function AgreementForm({ open, onClose, onSaved, apartments, tenants, saving, setSaving }) {
  const [form, setForm] = useState({ apartmentId: '', tenantId: '', startDate: '', endDate: '', monthlyRent: '', deposit: '', paymentFrequency: 'MONTHLY', rentDueDay: 5, terms: '' })
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setError('')
      setForm({ apartmentId: '', tenantId: '', startDate: '', endDate: '', monthlyRent: '', deposit: '', paymentFrequency: 'MONTHLY', rentDueDay: 5, terms: '' })
    }
  }, [open])

  const submit = async (e) => {
    e.preventDefault()
    if (!form.apartmentId || !form.tenantId || !form.startDate || !form.endDate || !form.monthlyRent) {
      setError('Apartment, tenant, dates and rent are required'); return
    }
    setSaving(true); setError('')
    const payload = {
      apartmentId: Number(form.apartmentId), tenantId: Number(form.tenantId),
      startDate: form.startDate, endDate: form.endDate,
      monthlyRent: Number(form.monthlyRent), deposit: form.deposit === '' ? null : Number(form.deposit),
      paymentFrequency: form.paymentFrequency, rentDueDay: Number(form.rentDueDay),
      terms: form.terms.split('\n').map((t) => t.trim()).filter(Boolean),
    }
    const res = await propertyFetch('/rental-agreements', { method: 'POST', body: JSON.stringify(payload) })
    setSaving(false)
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? 'Creation failed'); return }
    onSaved()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const pickApt = (id) => {
    const apt = apartments.find((a) => String(a.id) === String(id))
    setForm((f) => ({ ...f, apartmentId: id, monthlyRent: apt?.monthlyRent ?? f.monthlyRent }))
  }

  return (
    <Modal open={open} onClose={onClose} title="New Rental Agreement" width="max-w-2xl">
      {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
      <form onSubmit={submit} className="grid grid-cols-2 gap-4">
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Apartment *</span>
          <select className={inputCls} value={form.apartmentId} onChange={(e) => pickApt(e.target.value)}>
            <option value="">Select apartment…</option>
            {apartments.map((a) => (
              <option key={a.id} value={a.id} disabled={a.status !== 'AVAILABLE'}>
                {a.buildingName} · {a.apartmentNumber}{a.status !== 'AVAILABLE' ? ` (${a.status.replaceAll('_', ' ')})` : ''}
              </option>
            ))}
          </select>
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Tenant *</span>
          <select className={inputCls} value={form.tenantId} onChange={set('tenantId')}>
            <option value="">Select tenant…</option>
            {tenants.map((t) => <option key={t.id} value={t.id}>{t.code} — {t.name}</option>)}
          </select>
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Start date *</span>
          <input type="date" className={inputCls} value={form.startDate} onChange={set('startDate')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">End date *</span>
          <input type="date" className={inputCls} value={form.endDate} onChange={set('endDate')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Monthly rent (XAF) *</span>
          <input type="number" min="1" className={inputCls} value={form.monthlyRent} onChange={set('monthlyRent')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Deposit (XAF)</span>
          <input type="number" min="0" className={inputCls} value={form.deposit} onChange={set('deposit')} />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Payment frequency</span>
          <select className={inputCls} value={form.paymentFrequency} onChange={set('paymentFrequency')}>
            <option value="MONTHLY">Monthly</option><option value="QUARTERLY">Quarterly</option><option value="ANNUALLY">Annually</option>
          </select>
        </label>
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Rent due day</span>
          <input type="number" min="1" max="28" className={inputCls} value={form.rentDueDay} onChange={set('rentDueDay')} />
        </label>
        <label className="col-span-2 block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">Terms (one per line)</span>
          <textarea className={`${inputCls} h-20 py-2.5`} value={form.terms} onChange={set('terms')} placeholder="Quiet hours 22h–6h&#10;No subletting" />
        </label>
        <div className="col-span-2">
          <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
            {saving ? 'Creating…' : 'Create agreement'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

function AgreementDetails({ agreement, onClose }) {
  const [tab, setTab] = useState('details')
  const [schedules, setSchedules] = useState(null)
  const [history, setHistory] = useState(null)

  useEffect(() => {
    if (!agreement) return
    setTab('details'); setSchedules(null); setHistory(null)
    propertyFetch(`/rental-agreements/${agreement.id}/schedules`).then(async (r) => r.ok && setSchedules(await r.json()))
    propertyFetch(`/rental-agreements/${agreement.id}/history`).then(async (r) => r.ok && setHistory(await r.json()))
  }, [agreement])

  return (
    <Modal open={!!agreement} onClose={onClose} title={agreement?.agreementNumber ?? ''} width="max-w-2xl">
      {agreement && (
        <div>
          <div className="mb-4 flex gap-2">
            {['details', 'schedule', 'history'].map((t) => (
              <button key={t} onClick={() => setTab(t)}
                className={`rounded-lg px-3.5 py-2 text-sm font-medium capitalize transition-colors ${tab === t ? 'bg-pcc-100 text-pcc-700' : 'text-slate-500 hover:bg-slate-100'}`}>
                {t === 'schedule' ? `Rent schedule${schedules ? ` (${schedules.length})` : ''}` : t}
              </button>
            ))}
          </div>

          {tab === 'details' && (
            <>
            <dl className="grid grid-cols-2 gap-x-6 gap-y-4 text-sm">
              <div><dt className="text-xs text-slate-400">Tenant</dt><dd className="font-medium">{agreement.tenantName}</dd></div>
              <div><dt className="text-xs text-slate-400">Unit</dt><dd>{agreement.buildingName} · {agreement.apartmentNumber}</dd></div>
              <div><dt className="text-xs text-slate-400">Period</dt><dd>{agreement.startDate} → {agreement.endDate}</dd></div>
              <div><dt className="text-xs text-slate-400">Monthly rent</dt><dd>{fmt(agreement.monthlyRent)}</dd></div>
              <div><dt className="text-xs text-slate-400">Deposit</dt><dd>{fmt(agreement.deposit)}</dd></div>
              <div><dt className="text-xs text-slate-400">Billing</dt><dd>{agreement.paymentFrequency} · due day {agreement.rentDueDay}</dd></div>
              <div><dt className="text-xs text-slate-400">Status</dt><dd>{agreement.status}</dd></div>
              <div><dt className="text-xs text-slate-400">Created by</dt><dd>{agreement.createdByName ?? '—'}</dd></div>
              {agreement.terms?.length > 0 && (
                <div className="col-span-2"><dt className="text-xs text-slate-400">Terms</dt>
                  <dd><ul className="mt-1 list-inside list-disc space-y-0.5">{agreement.terms.map((t, i) => <li key={i}>{t}</li>)}</ul></dd></div>
              )}
              {agreement.terminationReason && <div className="col-span-2"><dt className="text-xs text-slate-400">Termination reason</dt><dd>{agreement.terminationReason}</dd></div>}
            </dl>
            <EntityDocumentsPanel entityType="RENTAL_AGREEMENT" entityRef={agreement.id} domain="PROPERTY" />
            </>
          )}

          {tab === 'schedule' && (
            schedules == null ? <p className="py-8 text-center text-sm text-slate-400">Loading…</p> :
            schedules.length === 0 ? <p className="py-8 text-center text-sm text-slate-400">No schedule rows</p> : (
              <div className="max-h-80 overflow-y-auto">
                <table className="w-full text-sm">
                  <thead className="sticky top-0 bg-white"><tr className="border-b text-left text-xs text-slate-400">
                    <th className="py-2">Period</th><th>Due</th><th className="text-right">Amount</th><th className="text-right">Status</th>
                  </tr></thead>
                  <tbody>
                    {schedules.map((s) => (
                      <tr key={s.id} className="border-b border-slate-50">
                        <td className="py-2">{s.periodStart} → {s.periodEnd}</td>
                        <td>{s.dueDate}</td>
                        <td className="text-right">{fmt(s.amount)}</td>
                        <td className="text-right"><span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-600">{s.status}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}

          {tab === 'history' && (
            history == null ? <p className="py-8 text-center text-sm text-slate-400">Loading…</p> :
            history.length === 0 ? <p className="py-8 text-center text-sm text-slate-400">No events</p> : (
              <ul className="space-y-3">
                {history.map((h, i) => (
                  <li key={i} className="flex gap-3 text-sm">
                    <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-pcc-500" />
                    <div>
                      <p className="font-medium text-ink">{h.event}{h.fromStatus ? ` · ${h.fromStatus} → ${h.toStatus}` : ''}</p>
                      <p className="text-xs text-slate-400">{h.note} · {h.actorUsername} · {new Date(h.createdAt).toLocaleString()}</p>
                    </div>
                  </li>
                ))}
              </ul>
            )
          )}
        </div>
      )}
    </Modal>
  )
}

export default function RentalsPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [apartments, setApartments] = useState([])
  const [tenants, setTenants] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [viewA, setViewA] = useState(null)
  const [confirmTerm, setConfirmTerm] = useState(null)
  const [renewA, setRenewA] = useState(null)
  const [renewDate, setRenewDate] = useState('')
  const [busy, setBusy] = useState(false)
  const [saving, setSaving] = useState(false)

  const canManage = hasPermission('RENTAL_MANAGE')

  useEffect(() => {
    if (hasPermission('APARTMENT_VIEW')) propertyFetch('/apartments?size=100').then(async (r) => r.ok && setApartments((await r.json()).content)).catch(() => {})
    if (hasPermission('TENANT_VIEW')) propertyFetch('/tenants?size=100').then(async (r) => r.ok && setTenants((await r.json()).content)).catch(() => {})
  }, [hasPermission])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (statusFilter) q.set('status', statusFilter)
    const res = await propertyFetch(`/rental-agreements?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load rental agreements')
    setLoading(false)
  }, [page, statusFilter])

  useEffect(() => { load() }, [load])

  const terminate = async () => {
    setBusy(true)
    const res = await propertyFetch(`/rental-agreements/${confirmTerm.id}/terminate`, {
      method: 'POST', body: JSON.stringify({ reason: 'Terminated from admin panel' }),
    })
    setBusy(false); setConfirmTerm(null)
    if (res.ok) { toast('Agreement terminated', 'success'); load() }
    else toast((await res.json().catch(() => ({}))).message ?? 'Termination failed', 'error')
  }

  const renew = async () => {
    if (!renewDate) return
    setBusy(true)
    const res = await propertyFetch(`/rental-agreements/${renewA.id}/renew`, {
      method: 'POST', body: JSON.stringify({ newEndDate: renewDate }),
    })
    setBusy(false); setRenewA(null)
    if (res.ok) { toast('Agreement renewed', 'success'); load() }
    else toast((await res.json().catch(() => ({}))).message ?? 'Renewal failed', 'error')
  }

  const columns = [
    { label: 'Agreement' }, { label: 'Tenant' }, { label: 'Unit' },
    { label: 'Period' }, { label: 'Rent/mo' }, { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Rental Agreements</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} agreements` : 'Tenancy contracts and rent schedules'}</p>
        </div>
        {canManage && (
          <button onClick={() => setFormOpen(true)} disabled={!apartments.length || !tenants.length}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-50">
            <Plus className="h-4 w-4" /> New Agreement
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-44`}>
          <option value="">All statuses</option>
          {['DRAFT', 'ACTIVE', 'EXPIRED', 'TERMINATED'].map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage} empty="No agreements yet">
        {data?.content.map((a) => (
          <tr key={a.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5">
              <button onClick={() => setViewA(a)} className="font-mono text-xs font-semibold text-pcc-700 hover:underline">{a.agreementNumber}</button>
            </td>
            <td className="px-5 py-3.5 font-medium text-ink">{a.tenantName}</td>
            <td className="px-5 py-3.5 text-slate-600">{a.buildingName} · {a.apartmentNumber}</td>
            <td className="px-5 py-3.5 text-xs text-slate-500">{a.startDate} → {a.endDate}</td>
            <td className="px-5 py-3.5 text-slate-600">{fmt(a.monthlyRent)}</td>
            <td className="px-5 py-3.5">
              <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${STATUS_STYLES[a.status]}`}>{a.status}</span>
            </td>
            <td className="px-3 py-3.5">
              <RowActionsMenu items={[
                { label: 'View details', onClick: () => setViewA(a) },
                canManage && a.status === 'ACTIVE' && { label: 'Renew', onClick: () => { setRenewA(a); setRenewDate('') } },
                canManage && (a.status === 'ACTIVE' || a.status === 'DRAFT') && { label: 'Terminate', danger: true, onClick: () => setConfirmTerm(a) },
              ].filter(Boolean)} />
            </td>
          </tr>
        ))}
      </DataTable>

      <AgreementForm open={formOpen} onClose={() => setFormOpen(false)} apartments={apartments} tenants={tenants}
        saving={saving} setSaving={setSaving}
        onSaved={() => { setFormOpen(false); toast('Agreement created — apartment marked occupied', 'success'); load() }} />

      <AgreementDetails agreement={viewA} onClose={() => setViewA(null)} />

      <Modal open={!!renewA} onClose={() => setRenewA(null)} title={`Renew ${renewA?.agreementNumber}`} width="max-w-sm">
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium text-slate-600">New end date (after {renewA?.endDate})</span>
          <input type="date" className={inputCls} value={renewDate} onChange={(e) => setRenewDate(e.target.value)} />
        </label>
        <button onClick={renew} disabled={busy || !renewDate}
          className="mt-4 h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
          {busy ? 'Renewing…' : 'Renew agreement'}
        </button>
      </Modal>

      <ConfirmDialog open={!!confirmTerm} onClose={() => setConfirmTerm(null)} onConfirm={terminate} loading={busy}
        title="Terminate agreement" confirmLabel="Terminate"
        message={`Terminate ${confirmTerm?.agreementNumber}? The apartment becomes available and future pending rent schedules are cancelled.`} />
    </div>
  )
}
