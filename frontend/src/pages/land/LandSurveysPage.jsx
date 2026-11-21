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
import { Plus, Ruler } from 'lucide-react'

const EMPTY = {
  parcelId: '', surveyDate: '', surveyorName: '', surveyorOrganization: '', surveyedArea: '',
  boundaryNorth: '', boundarySouth: '', boundaryEast: '', boundaryWest: '',
  description: '', documentReference: '',
}

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-slate-600">{label}</span>
      {children}
    </label>
  )
}

export default function LandSurveysPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [editSurvey, setEditSurvey] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [searchParams, setSearchParams] = useSearchParams()

  const canCreate = hasPermission('LAND_SURVEY_CREATE')
  const canUpdate = hasPermission('LAND_SURVEY_UPDATE')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    const q = new URLSearchParams({ page, size: 10 })
    if (search) q.set('search', search)
    const res = await landFetch(`/land-surveys?${q}`).catch(() => null)
    if (res?.ok) setData(await res.json())
    else setError('Could not load surveys — is the Land Service running on :8083?')
    setLoading(false)
  }, [page, search])

  useEffect(() => { load() }, [load])
  useEffect(() => { const t = setTimeout(() => { setPage(0); load() }, 350); return () => clearTimeout(t) }, [search]) // eslint-disable-line

  useEffect(() => {
    if (searchParams.get('new') === '1' && canCreate) {
      setEditSurvey(null); setFormError(''); setForm(EMPTY); setOpen(true)
      setSearchParams({}, { replace: true })
    }
  }, [searchParams, canCreate, setSearchParams])

  const openForm = (survey) => {
    setEditSurvey(survey ?? null)
    setFormError('')
    setForm(survey ? {
      parcelId: survey.parcelId, surveyDate: survey.surveyDate ?? '',
      surveyorName: survey.surveyorName ?? '', surveyorOrganization: survey.surveyorOrganization ?? '',
      surveyedArea: survey.surveyedArea ?? '', boundaryNorth: survey.boundaryNorth ?? '',
      boundarySouth: survey.boundarySouth ?? '', boundaryEast: survey.boundaryEast ?? '',
      boundaryWest: survey.boundaryWest ?? '', description: survey.description ?? '',
      documentReference: survey.documentReference ?? '',
    } : EMPTY)
    setOpen(true)
  }

  const submit = async (e) => {
    e.preventDefault()
    if (!form.parcelId || !form.surveyDate) { setFormError('Parcel and survey date are required'); return }
    setSaving(true); setFormError('')
    const payload = { ...form, parcelId: Number(form.parcelId), surveyedArea: form.surveyedArea === '' ? null : Number(form.surveyedArea) }
    const res = await landFetch(editSurvey ? `/land-surveys/${editSurvey.id}` : '/land-surveys', {
      method: editSurvey ? 'PUT' : 'POST', body: JSON.stringify(payload),
    })
    setSaving(false)
    if (!res.ok) { setFormError((await res.json().catch(() => ({}))).message ?? 'Save failed'); return }
    setOpen(false); toast(editSurvey ? 'Survey updated' : 'Survey recorded', 'success'); load()
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const columns = [
    { label: 'Reference' }, { label: 'Parcel' }, { label: 'Date' },
    { label: 'Surveyor' }, { label: 'Boundaries', className: 'hidden xl:table-cell' }, { label: '', className: 'w-12' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink">Land Surveys</h2>
          <p className="text-sm text-slate-500">{data ? `${data.totalElements} survey records` : 'Survey and boundary records'}</p>
        </div>
        {canCreate && (
          <button onClick={() => openForm(null)}
            className="flex h-10 items-center gap-2 rounded-xl bg-pcc-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-pcc-900">
            <Plus className="h-4 w-4" /> Record Survey
          </button>
        )}
      </div>

      <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search reference, surveyor or parcel…"
        className={`${inputCls} max-w-xs`} />

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <DataTable columns={columns} rows={data?.content} loading={loading} page={page}
        totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No surveys recorded yet">
        {data?.content.map((s) => (
          <tr key={s.id} className="border-b border-slate-50 transition-colors hover:bg-pcc-50/40">
            <td className="px-5 py-3.5 font-mono text-xs font-semibold text-pcc-700">{s.surveyReference}</td>
            <td className="px-5 py-3.5">
              <span className="text-ink">{s.parcelName}</span>
              <span className="ml-2 font-mono text-xs text-slate-400">{s.parcelReference}</span>
            </td>
            <td className="px-5 py-3.5 text-slate-600">{s.surveyDate}</td>
            <td className="px-5 py-3.5 text-slate-600">{s.surveyorName ?? '—'}{s.surveyorOrganization ? ` · ${s.surveyorOrganization}` : ''}</td>
            <td className="hidden px-5 py-3.5 text-xs text-slate-500 xl:table-cell">
              {[s.boundaryNorth && `N: ${s.boundaryNorth}`, s.boundarySouth && `S: ${s.boundarySouth}`].filter(Boolean).join(' · ') || '—'}
            </td>
            <td className="px-3 py-3.5">
              {canUpdate && <RowActionsMenu items={[{ label: 'Edit', onClick: () => openForm(s) }]} />}
            </td>
          </tr>
        ))}
      </DataTable>

      <Modal open={open} onClose={() => setOpen(false)} title={editSurvey ? `Edit ${editSurvey.surveyReference}` : 'Record Survey'} width="max-w-2xl">
        {formError && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{formError}</div>}
        <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Field label="Parcel *">
              <ParcelSelect value={form.parcelId} onChange={(v) => setForm((f) => ({ ...f, parcelId: v }))} />
            </Field>
          </div>
          <Field label="Survey date *"><input type="date" required className={inputCls} value={form.surveyDate} onChange={set('surveyDate')} /></Field>
          <Field label="Surveyor name"><input className={inputCls} value={form.surveyorName} onChange={set('surveyorName')} placeholder="J. Manga" /></Field>
          <Field label="Surveyor organization"><input className={inputCls} value={form.surveyorOrganization} onChange={set('surveyorOrganization')} placeholder="CamSurv Ltd" /></Field>
          <Field label="Surveyed area (m²)"><input type="number" step="any" className={inputCls} value={form.surveyedArea} onChange={set('surveyedArea')} /></Field>
          <Field label="North boundary"><input className={inputCls} value={form.boundaryNorth} onChange={set('boundaryNorth')} /></Field>
          <Field label="South boundary"><input className={inputCls} value={form.boundarySouth} onChange={set('boundarySouth')} /></Field>
          <Field label="East boundary"><input className={inputCls} value={form.boundaryEast} onChange={set('boundaryEast')} /></Field>
          <Field label="West boundary"><input className={inputCls} value={form.boundaryWest} onChange={set('boundaryWest')} /></Field>
          <div className="sm:col-span-2"><Field label="Document reference"><input className={inputCls} value={form.documentReference} onChange={set('documentReference')} /></Field></div>
          <div className="sm:col-span-2"><Field label="Description"><textarea className={`${inputCls} h-16 py-2.5`} value={form.description} onChange={set('description')} /></Field></div>
          <div className="sm:col-span-2">
            <button disabled={saving} className="h-11 w-full rounded-xl bg-pcc-700 text-sm font-semibold text-white transition-colors hover:bg-pcc-900 disabled:opacity-60">
              <span className="inline-flex items-center gap-2"><Ruler className="h-4 w-4" />{saving ? 'Saving…' : editSurvey ? 'Save changes' : 'Save survey'}</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
