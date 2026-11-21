import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { landFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import EntityDocumentsPanel from '../documents/EntityDocumentsPanel'
import ConfirmDialog from '../../components/ConfirmDialog'
import DataTable from '../../components/DataTable'
import RowActionsMenu from '../../components/RowActionsMenu'
import { inputCls } from '../property/PropertiesPage'
import { LAND_STATUS_STYLES, StatusBadge, fmtArea, fmtXaf, label } from './landShared'
import { Plus, MapPin, Trash2 } from 'lucide-react'

const CLASSIFICATIONS = ['RESIDENTIAL', 'COMMERCIAL', 'AGRICULTURAL', 'INDUSTRIAL', 'MIXED_USE', 'OTHER']
const STATUSES = ['AVAILABLE', 'RESERVED', 'UNDER_SALE', 'SOLD', 'UNDER_TRANSFER', 'DISPUTED', 'INACTIVE']
const UNITS = ['SQUARE_METRE', 'HECTARE', 'ACRE']

const EMPTY = {
  name: '', classification: 'COMMERCIAL', areaValue: '', unit: 'SQUARE_METRE',
  region: '', division: '', subdivision: '', locality: '', address: '',
  acquisitionDate: '', description: '',
}

function Field({ label: l, children, span }) {
  return (
    <label className={`block ${span ? 'sm:col-span-2' : ''}`}>
      <span className="mb-1.5 block text-xs font-medium text-slate-600">{l}</span>
      {children}
    </label>
  )
}

function ParcelForm({ open, onClose, onSaved, parcel, saving, setSaving }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setError('')
      setForm(parcel ? {
        name: parcel.name, classification: parcel.classification,
        areaValue: parcel.originalArea ?? parcel.areaSqm ?? '',
        unit: parcel.originalUnit ?? 'SQUARE_METRE',
        region: parcel.region ?? '', division: parcel.division ?? '',
        subdivision: parcel.subdivision ?? '', locality: parcel.locality ?? '',
        address: parcel.address ?? '', acquisitionDate: parcel.acquisitionDate ?? '',
        description: parcel.description ?? '',
      } : EMPTY)
    }
  }, [open, parcel])

  const submit = async (e) => {
    e.preventDefault()
    if (!form.name.trim()) { setError('Parcel name is required'); return }
    if (!form.areaValue || Number(form.areaValue) <= 0) { setError('A positive land area is required'); return }
    if (!form.region.trim()) { setError('Region is required'); return }
    setSaving(true); setError('')
    const payload = { ...form, areaValue: Number(form.areaValue), acquisitionDate: form.acquisitionDate || null }
    const res = await landFetch(parcel ? `/land-parcels/${parcel.id}` : '/land-parcels', {
      method: parcel ? 'PUT' : 'POST', body: JSON.stringify(payload),
    })
    setSaving(false)
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    onSaved()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  return (
    <Modal open={open} onClose={onClose} title={parcel ? `Edit ${parcel.parcelReference}` : 'Register Land Parcel'} width="max-w-2xl">
      {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
      <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Field label="Land name *" span>
          <input className={inputCls} value={form.name} onChange={set('name')} placeholder="Buea Farm Estate" />
        </Field>
        <Field label="Classification *">
          <select className={inputCls} value={form.classification} onChange={set('classification')}>
            {CLASSIFICATIONS.map((c) => <option key={c} value={c}>{label(c)}</option>)}
          </select>
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Area *">
            <input type="number" min="0" step="any" className={inputCls} value={form.areaValue} onChange={set('areaValue')} placeholder="2500" />
          </Field>
          <Field label="Unit *">
            <select className={inputCls} value={form.unit} onChange={set('unit')}>
              {UNITS.map((u) => <option key={u} value={u}>{label(u)}</option>)}
            </select>
          </Field>
        </div>
        <Field label="Region *">
          <input className={inputCls} value={form.region} onChange={set('region')} placeholder="South West" />
        </Field>
        <Field label="Division">
          <input className={inputCls} value={form.division} onChange={set('division')} placeholder="Fako" />
        </Field>
        <Field label="Subdivision">
          <input className={inputCls} value={form.subdivision} onChange={set('subdivision')} placeholder="Buea" />
        </Field>
        <Field label="Locality">
          <input className={inputCls} value={form.locality} onChange={set('locality')} placeholder="Bokwango" />
        </Field>
        <Field label="Address / location" span>
          <input className={inputCls} value={form.address} onChange={set('address')} placeholder="Mile 17, Buea road" />
        </Field>
        <Field label="Acquisition date">
          <input type="date" className={inputCls} value={form.acquisitionDate} onChange={set('acquisitionDate')} />
        </Field>
        <Field label="Description" span>
          <textarea className={`${inputCls} h-20 py-2.5`} value={form.description} onChange={set('description')} />
        </Field>
        <div className="sm:col-span-2">
          <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
            {saving ? 'Saving…' : parcel ? 'Save changes' : 'Register parcel'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

function SurveyTab({ parcelId, canCreate }) {
  const toast = useToast()
  const [rows, setRows] = useState(null)
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({ surveyDate: '', surveyorName: '', surveyorOrganization: '', surveyedArea: '', boundaryNorth: '', boundarySouth: '', boundaryEast: '', boundaryWest: '', description: '', documentReference: '' })
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    const res = await landFetch(`/land-surveys/by-parcel/${parcelId}`).catch(() => null)
    setRows(res?.ok ? await res.json() : [])
  }, [parcelId])
  useEffect(() => { load() }, [load])

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true)
    const res = await landFetch('/land-surveys', {
      method: 'POST',
      body: JSON.stringify({ ...form, parcelId, surveyedArea: form.surveyedArea === '' ? null : Number(form.surveyedArea), surveyDate: form.surveyDate || null }),
    })
    setSaving(false)
    if (!res.ok) { toast('Survey save failed', 'error'); return }
    setOpen(false); setForm({ surveyDate: '', surveyorName: '', surveyorOrganization: '', surveyedArea: '', boundaryNorth: '', boundarySouth: '', boundaryEast: '', boundaryWest: '', description: '', documentReference: '' })
    toast('Survey recorded', 'success'); load()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  return (
    <div className="space-y-3">
      {canCreate && (
        <button onClick={() => setOpen(true)} className="flex h-9 items-center gap-2 rounded-xl bg-pcc-700 px-3.5 text-xs font-semibold text-white hover:bg-pcc-900">
          <Plus className="h-3.5 w-3.5" /> Record survey
        </button>
      )}
      {rows === null ? <p className="text-sm text-slate-400">Loading…</p>
        : rows.length === 0 ? <p className="text-sm text-slate-400">No surveys recorded for this parcel.</p>
        : rows.map((s) => (
          <div key={s.id} className="rounded-xl border border-slate-100 p-4 text-sm">
            <div className="flex items-center justify-between">
              <span className="font-mono text-xs font-semibold text-pcc-700">{s.surveyReference}</span>
              <span className="text-xs text-slate-400">{s.surveyDate}</span>
            </div>
            <p className="mt-1 text-slate-600">{s.surveyorName ?? '—'}{s.surveyorOrganization ? ` · ${s.surveyorOrganization}` : ''}</p>
            <div className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-xs text-slate-500">
              <span>N: {s.boundaryNorth ?? '—'}</span><span>S: {s.boundarySouth ?? '—'}</span>
              <span>E: {s.boundaryEast ?? '—'}</span><span>W: {s.boundaryWest ?? '—'}</span>
            </div>
            {s.documentReference && <p className="mt-1 text-xs text-slate-400">Doc: {s.documentReference}</p>}
          </div>
        ))}
      <Modal open={open} onClose={() => setOpen(false)} title="Record Survey" width="max-w-xl">
        <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Survey date *"><input type="date" required className={inputCls} value={form.surveyDate} onChange={set('surveyDate')} /></Field>
          <Field label="Surveyor name"><input className={inputCls} value={form.surveyorName} onChange={set('surveyorName')} /></Field>
          <Field label="Organization"><input className={inputCls} value={form.surveyorOrganization} onChange={set('surveyorOrganization')} /></Field>
          <Field label="Surveyed area (m²)"><input type="number" step="any" className={inputCls} value={form.surveyedArea} onChange={set('surveyedArea')} /></Field>
          <Field label="North boundary"><input className={inputCls} value={form.boundaryNorth} onChange={set('boundaryNorth')} /></Field>
          <Field label="South boundary"><input className={inputCls} value={form.boundarySouth} onChange={set('boundarySouth')} /></Field>
          <Field label="East boundary"><input className={inputCls} value={form.boundaryEast} onChange={set('boundaryEast')} /></Field>
          <Field label="West boundary"><input className={inputCls} value={form.boundaryWest} onChange={set('boundaryWest')} /></Field>
          <Field label="Document reference" span><input className={inputCls} value={form.documentReference} onChange={set('documentReference')} /></Field>
          <Field label="Description" span><textarea className={`${inputCls} h-16 py-2.5`} value={form.description} onChange={set('description')} /></Field>
          <div className="sm:col-span-2">
            <button disabled={saving} className="h-10 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white hover:bg-pcc-900 disabled:opacity-60">
              {saving ? 'Saving…' : 'Save survey'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}

function CoordinatesTab({ parcelId, canUpdate }) {
  const toast = useToast()
  const [points, setPoints] = useState(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    const res = await landFetch(`/land-coordinates?parcelId=${parcelId}`).catch(() => null)
    setPoints(res?.ok ? await res.json() : [])
  }, [parcelId])
  useEffect(() => { load() }, [load])

  const setPoint = (i, k, v) => setPoints((p) => p.map((pt, j) => (j === i ? { ...pt, [k]: v } : pt)))
  const addPoint = () => setPoints((p) => [...(p ?? []), { sequence: (p?.length ?? 0) + 1, latitude: '', longitude: '', label: '' }])
  const removePoint = (i) => setPoints((p) => p.filter((_, j) => j !== i).map((pt, j) => ({ ...pt, sequence: j + 1 })))

  const save = async () => {
    const valid = (points ?? []).every((p) => p.latitude !== '' && p.longitude !== '' && Math.abs(Number(p.latitude)) <= 90 && Math.abs(Number(p.longitude)) <= 180)
    if (!valid || (points ?? []).length === 0) { toast('Every point needs a valid latitude (−90…90) and longitude (−180…180)', 'error'); return }
    setSaving(true)
    const res = await landFetch('/land-coordinates', {
      method: 'PUT',
      body: JSON.stringify({ parcelId, points: points.map((p) => ({ sequence: p.sequence, latitude: Number(p.latitude), longitude: Number(p.longitude), label: p.label || null })) }),
    })
    setSaving(false)
    if (!res.ok) { toast('Coordinates save failed', 'error'); return }
    toast('Boundary points saved', 'success'); load()
  }

  if (points === null) return <p className="text-sm text-slate-400">Loading…</p>

  return (
    <div className="space-y-3">
      <p className="text-xs text-slate-400">Boundary points in order. Coordinates do not imply legal boundary verification.</p>
      {points.map((p, i) => (
        <div key={i} className="grid grid-cols-12 items-center gap-2">
          <span className="col-span-1 text-center font-mono text-xs text-slate-400">#{p.sequence}</span>
          <input className={`${inputCls} col-span-3`} placeholder="Latitude" value={p.latitude} onChange={(e) => setPoint(i, 'latitude', e.target.value)} />
          <input className={`${inputCls} col-span-3`} placeholder="Longitude" value={p.longitude} onChange={(e) => setPoint(i, 'longitude', e.target.value)} />
          <input className={`${inputCls} col-span-4`} placeholder="Label (optional)" value={p.label ?? ''} onChange={(e) => setPoint(i, 'label', e.target.value)} />
          {canUpdate && (
            <button type="button" onClick={() => removePoint(i)} className="col-span-1 flex h-8 w-8 items-center justify-center rounded-lg text-slate-300 hover:bg-red-50 hover:text-red-500">
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      ))}
      {points.length === 0 && <p className="text-sm text-slate-400">No coordinates recorded.</p>}
      {canUpdate && (
        <div className="flex gap-2">
          <button type="button" onClick={addPoint} className="flex h-9 items-center gap-1.5 rounded-xl border border-slate-200 px-3.5 text-xs font-semibold text-slate-600 hover:border-pcc-300 hover:text-pcc-700">
            <Plus className="h-3.5 w-3.5" /> Add point
          </button>
          {points.length > 0 && (
            <button onClick={save} disabled={saving} className="h-9 rounded-xl bg-pcc-700 px-3.5 text-xs font-semibold text-white hover:bg-pcc-900 disabled:opacity-60">
              {saving ? 'Saving…' : 'Save coordinates'}
            </button>
          )}
        </div>
      )}
    </div>
  )
}

function ParcelDetail({ parcel, onClose }) {
  const { hasPermission } = useAuth()
  const [tab, setTab] = useState('details')
  const [history, setHistory] = useState(null)
  const [ownership, setOwnership] = useState(null)
  const [transactions, setTransactions] = useState(null)

  useEffect(() => {
    if (!parcel) return
    setTab('details'); setHistory(null); setOwnership(null); setTransactions(null)
  }, [parcel])

  useEffect(() => {
    if (!parcel) return
    if (tab === 'status' && history === null) {
      landFetch(`/land-parcels/${parcel.id}/status-history`).then(async (r) => setHistory(r?.ok ? await r.json() : []))
    }
    if (tab === 'ownership' && ownership === null) {
      landFetch(`/land-ownership/by-parcel/${parcel.id}`).then(async (r) => setOwnership(r?.ok ? await r.json() : []))
    }
    if (tab === 'transactions' && transactions === null) {
      landFetch(`/land-transactions/by-parcel/${parcel.id}`).then(async (r) => setTransactions(r?.ok ? await r.json() : []))
    }
  }, [tab, parcel, history, ownership, transactions])

  const tabs = [
    ['details', 'Details'], ['status', 'Status History'], ['surveys', 'Surveys'],
    ['coordinates', 'Coordinates'], ['ownership', 'Ownership'], ['transactions', 'Transactions'], ['documents', 'Documents'],
  ]

  return (
    <Modal open={!!parcel} onClose={onClose} title={parcel ? `${parcel.parcelReference} — ${parcel.name}` : ''} width="max-w-3xl">
      {parcel && (
        <div>
          <div className="mb-4 flex flex-wrap gap-1.5 border-b border-slate-100 pb-3">
            {tabs.map(([k, l]) => (
              <button key={k} onClick={() => setTab(k)}
                className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${tab === k ? 'bg-pcc-700 text-white' : 'text-slate-500 hover:bg-pcc-50 hover:text-pcc-700'}`}>
                {l}
              </button>
            ))}
          </div>

          {tab === 'details' && (
            <dl className="grid grid-cols-2 gap-x-6 gap-y-4 text-sm">
              <div><dt className="text-xs text-slate-400">Reference</dt><dd className="font-mono">{parcel.parcelReference}</dd></div>
              <div><dt className="text-xs text-slate-400">Classification</dt><dd>{label(parcel.classification)}</dd></div>
              <div><dt className="text-xs text-slate-400">Status</dt><dd><StatusBadge value={parcel.status} styles={LAND_STATUS_STYLES} /></dd></div>
              <div><dt className="text-xs text-slate-400">Area</dt><dd>{fmtArea(parcel.areaSqm, parcel.originalArea, parcel.originalUnit)}</dd></div>
              <div><dt className="text-xs text-slate-400">Region</dt><dd>{parcel.region}</dd></div>
              <div><dt className="text-xs text-slate-400">Division / Subdivision</dt><dd>{[parcel.division, parcel.subdivision].filter(Boolean).join(' / ') || '—'}</dd></div>
              <div><dt className="text-xs text-slate-400">Locality</dt><dd>{parcel.locality ?? '—'}</dd></div>
              <div><dt className="text-xs text-slate-400">Address</dt><dd>{parcel.address ?? '—'}</dd></div>
              <div><dt className="text-xs text-slate-400">Acquisition date</dt><dd>{parcel.acquisitionDate ?? '—'}</dd></div>
              <div><dt className="text-xs text-slate-400">Registered by</dt><dd>{parcel.createdByName ?? '—'}</dd></div>
              {parcel.description && <div className="col-span-2"><dt className="text-xs text-slate-400">Description</dt><dd>{parcel.description}</dd></div>}
            </dl>
          )}

          {tab === 'status' && (
            history === null ? <p className="text-sm text-slate-400">Loading…</p>
              : history.length === 0 ? <p className="text-sm text-slate-400">No status changes recorded.</p>
              : <ul className="space-y-2">
                  {history.map((h) => (
                    <li key={h.id} className="flex items-center justify-between rounded-xl border border-slate-100 px-4 py-2.5 text-sm">
                      <span>{label(h.previousStatus)} → <b>{label(h.newStatus)}</b>{h.reason ? ` — ${h.reason}` : ''}</span>
                      <span className="text-xs text-slate-400">{h.changedByName ?? 'system'}</span>
                    </li>
                  ))}
                </ul>
          )}

          {tab === 'surveys' && <SurveyTab parcelId={parcel.id} canCreate={hasPermission('LAND_SURVEY_CREATE')} />}
          {tab === 'coordinates' && <CoordinatesTab parcelId={parcel.id} canUpdate={hasPermission('LAND_UPDATE')} />}

          {tab === 'ownership' && (
            ownership === null ? <p className="text-sm text-slate-400">Loading…</p>
              : ownership.length === 0 ? <p className="text-sm text-slate-400">No ownership records.</p>
              : <ul className="space-y-2">
                  {ownership.map((o) => (
                    <li key={o.id} className="rounded-xl border border-slate-100 px-4 py-3 text-sm">
                      <div className="flex items-center justify-between">
                        <span className="font-medium">{o.ownerName}</span>
                        <StatusBadge value={o.status} />
                      </div>
                      <p className="mt-0.5 font-mono text-xs text-slate-400">{o.ownershipReference}{o.titleReference ? ` · ${o.titleReference}` : ''}</p>
                      <p className="mt-0.5 text-xs text-slate-500">Effective {o.effectiveDate}{o.endDate ? ` → ended ${o.endDate}` : ''} · share {o.sharePercent}%</p>
                    </li>
                  ))}
                </ul>
          )}

          {tab === 'transactions' && (
            transactions === null ? <p className="text-sm text-slate-400">Loading…</p>
              : transactions.length === 0 ? <p className="text-sm text-slate-400">No transactions recorded.</p>
              : <ul className="space-y-2">
                  {transactions.map((t) => (
                    <li key={t.id} className="flex items-center justify-between rounded-xl border border-slate-100 px-4 py-2.5 text-sm">
                      <div>
                        <span className="font-medium">{label(t.transactionType)}</span>
                        <span className="ml-2 font-mono text-xs text-slate-400">{t.referenceCode}</span>
                        {t.description && <p className="text-xs text-slate-500">{t.description}</p>}
                      </div>
                      <div className="text-right text-xs text-slate-400">
                        <p>{t.amount != null ? fmtXaf(t.amount) : ''}</p>
                        <p>{t.actorName ?? ''}</p>
                      </div>
                    </li>
                  ))}
                </ul>
          )}
        
          {tab === 'documents' && (
            <EntityDocumentsPanel entityType="LAND_PARCEL" entityRef={parcel.id} domain="LAND" />
          )}
</div>
      )}
    </Modal>
  )
}

export default function LandParcelsPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [clsFilter, setClsFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editParcel, setEditParcel] = useState(null)
  const [viewParcel, setViewParcel] = useState(null)
  const [confirmDel, setConfirmDel] = useState(null)
  const [statusParcel, setStatusParcel] = useState(null)
  const [busy, setBusy] = useState(false)
  const [saving, setSaving] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()

  const canCreate = hasPermission('LAND_CREATE')
  const canUpdate = hasPermission('LAND_UPDATE')
  const canDelete = hasPermission('LAND_DELETE')

  useEffect(() => {
    if (searchParams.get('new') === '1' && canCreate) {
      setFormOpen(true); setEditParcel(null)
      setSearchParams({}, { replace: true })
    }
  }, [searchParams, canCreate, setSearchParams])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (search) q.set('search', search)
    if (clsFilter) q.set('classification', clsFilter)
    if (statusFilter) q.set('status', statusFilter)
    const res = await landFetch(`/land-parcels?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load land parcels — is the Land Service running on :8083?')
    setLoading(false)
  }, [page, search, clsFilter, statusFilter])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  const changeStatus = async (p, status) => {
    const res = await landFetch(`/land-parcels/${p.id}/status`, {
      method: 'PATCH', body: JSON.stringify({ status, reason: 'Status updated from land management' }),
    })
    if (res.ok) { toast('Status updated', 'success'); load() }
    else toast('Status update failed', 'error')
  }

  const doDelete = async () => {
    setBusy(true)
    const res = await landFetch(`/land-parcels/${confirmDel.id}`, { method: 'DELETE' })
    setBusy(false); setConfirmDel(null)
    if (res.ok || res.status === 204) { toast('Parcel archived', 'success'); load() }
    else toast('Archive failed', 'error')
  }

  const columns = [
    { label: 'Parcel' }, { label: 'Reference' }, { label: 'Classification' },
    { label: 'Location' }, { label: 'Area', className: 'hidden lg:table-cell' },
    { label: 'Status' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Land Parcels</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} registered parcels` : 'Company land registry'}</p>
        </div>
        {canCreate && (
          <button onClick={() => { setEditParcel(null); setFormOpen(true) }}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> Register Land
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search name, reference or locality…"
          className={`${inputCls} max-w-xs`} />
        <select value={clsFilter} onChange={(e) => { setClsFilter(e.target.value); setPage(0) }} className={`${inputCls} w-44`}>
          <option value="">All classifications</option>
          {CLASSIFICATIONS.map((c) => <option key={c} value={c}>{label(c)}</option>)}
        </select>
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }} className={`${inputCls} w-44`}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No land parcels yet — register the first one">
        {data?.content.map((p) => (
          <tr key={p.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5">
              <button onClick={() => setViewParcel(p)} className="flex items-center gap-2 text-left font-medium text-ink hover:text-pcc-700">
                <MapPin className="h-3.5 w-3.5 text-slate-300" /> {p.name}
              </button>
            </td>
            <td className="px-5 py-3.5 font-mono text-xs text-slate-500">{p.parcelReference}</td>
            <td className="px-5 py-3.5 text-slate-600">{label(p.classification)}</td>
            <td className="px-5 py-3.5 text-slate-600">{p.locality ?? '—'}{p.region ? `, ${p.region}` : ''}</td>
            <td className="hidden px-5 py-3.5 text-slate-600 lg:table-cell">{fmtArea(p.areaSqm, p.originalArea, p.originalUnit)}</td>
            <td className="px-5 py-3.5"><StatusBadge value={p.status} styles={LAND_STATUS_STYLES} /></td>
            <td className="px-3 py-3.5">
              <RowActionsMenu items={[
                { label: 'View details', onClick: () => setViewParcel(p) },
                canUpdate && { label: 'Edit', onClick: () => { setEditParcel(p); setFormOpen(true) } },
                canUpdate && { label: 'Change status', onClick: () => setStatusParcel(p) },
                canDelete && { label: 'Archive', danger: true, onClick: () => setConfirmDel(p) },
              ].filter(Boolean)} />
            </td>
          </tr>
        ))}
      </DataTable>

      <ParcelForm open={formOpen} onClose={() => setFormOpen(false)} parcel={editParcel}
        saving={saving} setSaving={setSaving}
        onSaved={() => { setFormOpen(false); toast(editParcel ? 'Parcel updated' : 'Parcel registered', 'success'); load() }} />

      <ParcelDetail parcel={viewParcel} onClose={() => setViewParcel(null)} />

      <Modal open={!!statusParcel} onClose={() => setStatusParcel(null)} title={`Status — ${statusParcel?.parcelReference}`} width="max-w-sm">
        <div className="space-y-2">
          {STATUSES.filter((s) => s !== statusParcel?.status).map((s) => (
            <button key={s} onClick={() => { changeStatus(statusParcel, s); setStatusParcel(null) }}
              className="flex w-full items-center gap-3 rounded-xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-600 transition-colors hover:border-pcc-300 hover:bg-pcc-50 hover:text-pcc-700">
              <MapPin className="h-4 w-4" /> {label(s)}
            </button>
          ))}
        </div>
      </Modal>

      <ConfirmDialog open={!!confirmDel} onClose={() => setConfirmDel(null)} onConfirm={doDelete} loading={busy}
        title="Archive parcel" confirmLabel="Archive"
        message={`Archive ${confirmDel?.name}? It will be hidden from listings; history is preserved.`} />
    </div>
  )
}
