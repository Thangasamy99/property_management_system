import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Modal from '../../components/Modal'
import { useAuth } from '../../context/AuthContext'
import { inputCls } from '../property/PropertiesPage'
import {
  ENTITY_TYPE_LABELS, ExpiryBadge, Field, StatusBadge, canPreview, docFetch, docUpload,
  downloadDocument, fetchCategories, fmtDate, fmtSize, previewDocument,
} from './docShared'
import { Archive, Download, Eye, Link2, Pencil, Plus, RotateCcw, Trash2, Upload } from 'lucide-react'

const TABS = ['Details', 'Versions', 'Associations', 'Audit history']

export default function DocumentDetailPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const { hasPermission } = useAuth()
  const [doc, setDoc] = useState(null)
  const [tab, setTab] = useState('Details')
  const [versions, setVersions] = useState([])
  const [associations, setAssociations] = useState([])
  const [audit, setAudit] = useState(null)
  const [editOpen, setEditOpen] = useState(false)
  const [assocOpen, setAssocOpen] = useState(false)
  const [editForm, setEditForm] = useState(null)
  const [assocForm, setAssocForm] = useState({ entityType: '', entityRef: '' })
  const [error, setError] = useState(null)
  const [categories, setCategories] = useState([])
  const versionInput = useRef(null)

  const load = useCallback(async () => {
    const res = await docFetch(`/documents/${id}`)
    if (res.ok) {
      const d = await res.json()
      setDoc(d)
      setEditForm({ originalFilename: d.originalFilename, description: d.description ?? '',
        categoryId: d.categoryId, documentDate: d.documentDate ?? '', expiryDate: d.expiryDate ?? '' })
    } else if (res.status === 404) nav('/documents/all')
  }, [id, nav])

  useEffect(() => { load(); fetchCategories().then(setCategories) }, [load])
  useEffect(() => {
    if (!doc) return
    if (tab === 'Versions') docFetch(`/documents/${id}/versions`).then((r) => (r.ok ? r.json() : [])).then(setVersions)
    if (tab === 'Associations') docFetch(`/documents/${id}/associations`).then((r) => (r.ok ? r.json() : [])).then(setAssociations)
    if (tab === 'Audit history') docFetch(`/documents/${id}/audit-history?size=50`).then((r) => (r.ok ? r.json() : null)).then(setAudit)
  }, [tab, doc?.id])

  const saveMeta = async () => {
    const res = await docFetch(`/documents/${id}`, { method: 'PUT', body: JSON.stringify({
      ...editForm, documentDate: editForm.documentDate || null, expiryDate: editForm.expiryDate || null }) })
    if (!res.ok) { const d = await res.json().catch(() => ({})); return setError(d.message ?? 'Update failed') }
    setEditOpen(false); setError(null); load()
  }

  const uploadVersion = async (file) => {
    if (!file) return
    const fd = new FormData(); fd.append('file', file)
    const res = await docUpload(`/documents/${id}/versions`, fd)
    if (!res.ok) { const d = await res.json().catch(() => ({})); setError(d.message ?? 'Version upload failed'); return }
    setError(null); load(); docFetch(`/documents/${id}/versions`).then((r) => (r.ok ? r.json() : [])).then(setVersions)
  }

  const archive = async () => { await docFetch(`/documents/${id}/archive`, { method: 'POST' }); load() }
  const restore = async () => { await docFetch(`/documents/${id}/restore`, { method: 'POST' }); load() }

  const addAssoc = async () => {
    const res = await docFetch(`/documents/${id}/associations`, { method: 'POST', body: JSON.stringify(assocForm) })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) return setError(data.message ?? 'Association failed')
    setAssocOpen(false); setAssocForm({ entityType: '', entityRef: '' }); setError(null)
    docFetch(`/documents/${id}/associations`).then((r) => (r.ok ? r.json() : [])).then(setAssociations)
  }

  const removeAssoc = async (assocId) => {
    await docFetch(`/documents/${id}/associations/${assocId}`, { method: 'DELETE' })
    docFetch(`/documents/${id}/associations`).then((r) => (r.ok ? r.json() : [])).then(setAssociations)
  }

  if (!doc) return <p className="py-10 text-center text-sm text-slate-400">Loading…</p>
  const active = doc.status === 'ACTIVE'

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-bold text-slate-800">{doc.originalFilename}</h2>
            <StatusBadge value={doc.status} />
          </div>
          <p className="mt-0.5 text-sm text-slate-400">{doc.documentRef} · {doc.categoryName} · v{doc.currentVersion} · {fmtSize(doc.fileSize)}</p>
        </div>
        <div className="flex items-center gap-2">
          {canPreview(doc.mimeType) && hasPermission('DOCUMENT_PREVIEW') && (
            <button onClick={() => previewDocument(doc.id)} className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"><Eye size={14} /> Preview</button>)}
          {hasPermission('DOCUMENT_DOWNLOAD') && (
            <button onClick={() => downloadDocument(doc.id, doc.originalFilename)} className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"><Download size={14} /> Download</button>)}
          {active && hasPermission('DOCUMENT_VERSION_UPLOAD') && (
            <button onClick={() => versionInput.current?.click()} className="flex items-center gap-1.5 rounded-lg bg-pcc-700 px-3 py-2 text-sm font-semibold text-white hover:bg-pcc-800"><Upload size={14} /> New version</button>)}
          {active && hasPermission('DOCUMENT_UPDATE') && (
            <button onClick={() => setEditOpen(true)} className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"><Pencil size={14} /> Edit</button>)}
          {active && hasPermission('DOCUMENT_ARCHIVE') && (
            <button onClick={archive} className="flex items-center gap-1.5 rounded-lg border border-red-100 px-3 py-2 text-sm text-red-600 hover:bg-red-50"><Archive size={14} /> Archive</button>)}
          {!active && hasPermission('DOCUMENT_RESTORE') && (
            <button onClick={restore} className="flex items-center gap-1.5 rounded-lg border border-emerald-200 px-3 py-2 text-sm text-emerald-700 hover:bg-emerald-50"><RotateCcw size={14} /> Restore</button>)}
          <input ref={versionInput} type="file" className="hidden" accept=".pdf,.jpg,.jpeg,.png,.doc,.docx,.xls,.xlsx"
                 onChange={(e) => { uploadVersion(e.target.files?.[0]); e.target.value = '' }} />
        </div>
      </div>
      {error && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}

      <div className="flex gap-1 border-b border-slate-200">
        {TABS.map((t) => (
          <button key={t} onClick={() => setTab(t)}
                  className={`px-4 py-2 text-sm font-medium ${tab === t ? 'border-b-2 border-pcc-700 text-pcc-700' : 'text-slate-500 hover:text-slate-700'}`}>{t}</button>))}
      </div>

      {tab === 'Details' && (
        <div className="grid grid-cols-2 gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm lg:grid-cols-3">
          {[['Reference', doc.documentRef], ['Category', doc.categoryName], ['Domain', doc.categoryDomain],
            ['MIME type', doc.mimeType], ['Size', fmtSize(doc.fileSize)], ['Version', `v${doc.currentVersion}`],
            ['Document date', fmtDate(doc.documentDate)], ['Uploaded by', doc.uploadedByName], ['Uploaded at', fmtDate(doc.createdAt)],
            ['Description', doc.description ?? '—']].map(([k, v]) => (
            <div key={k}><p className="text-[11px] uppercase tracking-wide text-slate-400">{k}</p><p className="mt-0.5 text-sm font-medium text-slate-700">{v}</p></div>))}
          <div><p className="text-[11px] uppercase tracking-wide text-slate-400">Expiry</p><ExpiryBadge doc={doc} /></div>
        </div>)}

      {tab === 'Versions' && (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
              <tr><th className="px-4 py-3">Version</th><th className="px-4 py-3">Size</th><th className="px-4 py-3">Type</th><th className="px-4 py-3">Uploaded by</th><th className="px-4 py-3">Date</th><th className="px-4 py-3"></th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {versions.map((v) => (
                <tr key={v.id}>
                  <td className="px-4 py-3 font-semibold text-slate-700">v{v.versionNumber}{v.current && <span className="ml-2 rounded-full bg-pcc-50 px-2 py-0.5 text-[10px] font-semibold text-pcc-700">current</span>}</td>
                  <td className="px-4 py-3 text-slate-600">{fmtSize(v.fileSize)}</td>
                  <td className="px-4 py-3 text-slate-600">{v.mimeType?.split('/').pop()}</td>
                  <td className="px-4 py-3 text-slate-600">{v.uploadedByName}</td>
                  <td className="px-4 py-3 text-slate-600">{fmtDate(v.createdAt)}</td>
                  <td className="px-4 py-3">
                    {hasPermission('DOCUMENT_VERSION_VIEW') && (
                      <div className="flex gap-1">
                        {canPreview(v.mimeType) && <button onClick={() => previewDocument(doc.id, v.versionNumber)} className="rounded p-1.5 text-slate-400 hover:bg-pcc-50 hover:text-pcc-700"><Eye size={14} /></button>}
                        <button onClick={() => downloadDocument(doc.id, doc.originalFilename, v.versionNumber)} className="rounded p-1.5 text-slate-400 hover:bg-pcc-50 hover:text-pcc-700"><Download size={14} /></button>
                      </div>)}
                  </td>
                </tr>))}
            </tbody>
          </table>
        </div>)}

      {tab === 'Associations' && (
        <div className="space-y-3">
          {active && hasPermission('DOCUMENT_ASSOCIATE') && (
            <button onClick={() => setAssocOpen(true)} className="flex items-center gap-1.5 rounded-lg bg-pcc-700 px-3 py-2 text-sm font-semibold text-white hover:bg-pcc-800"><Plus size={14} /> Link record</button>)}
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr><th className="px-4 py-3">Entity type</th><th className="px-4 py-3">Reference</th><th className="px-4 py-3">Linked by</th><th className="px-4 py-3">Date</th><th className="px-4 py-3"></th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {associations.map((a) => (
                  <tr key={a.id}>
                    <td className="px-4 py-3"><span className="flex items-center gap-1.5 text-slate-700"><Link2 size={13} className="text-pcc-500" />{ENTITY_TYPE_LABELS[a.entityType] ?? a.entityType}</span></td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-600">{a.entityRef}</td>
                    <td className="px-4 py-3 text-slate-600">{a.associatedByName}</td>
                    <td className="px-4 py-3 text-slate-600">{fmtDate(a.createdAt)}</td>
                    <td className="px-4 py-3">{active && hasPermission('DOCUMENT_ASSOCIATE') && (
                      <button onClick={() => removeAssoc(a.id)} className="rounded p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600"><Trash2 size={14} /></button>)}</td>
                  </tr>))}
                {associations.length === 0 && <tr><td colSpan={5} className="px-4 py-8 text-center text-slate-400">No linked records</td></tr>}
              </tbody>
            </table>
          </div>
        </div>)}

      {tab === 'Audit history' && (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          {!hasPermission('DOCUMENT_AUDIT_VIEW') ? (
            <p className="px-4 py-8 text-center text-sm text-slate-400">You do not have permission to view audit history.</p>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr><th className="px-4 py-3">Action</th><th className="px-4 py-3">Actor</th><th className="px-4 py-3">Detail</th><th className="px-4 py-3">Time</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {(audit?.content ?? []).map((a) => (
                  <tr key={a.id}>
                    <td className="px-4 py-3"><span className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-semibold text-slate-600">{a.action?.replaceAll('_', ' ')}</span></td>
                    <td className="px-4 py-3 text-slate-600">{a.actorName}</td>
                    <td className="px-4 py-3 text-xs text-slate-500">{a.detail ?? '—'}</td>
                    <td className="px-4 py-3 text-xs text-slate-500">{new Date(a.createdAt).toLocaleString()}</td>
                  </tr>))}
              </tbody>
            </table>)}
        </div>)}

      <Modal open={editOpen} onClose={() => setEditOpen(false)} title="Edit metadata">
        {editForm && (
          <div className="space-y-3">
            <Field label="Filename"><input className={inputCls} value={editForm.originalFilename} onChange={(e) => setEditForm({ ...editForm, originalFilename: e.target.value })} /></Field>
            <Field label="Category">
              <select className={inputCls} value={editForm.categoryId} onChange={(e) => setEditForm({ ...editForm, categoryId: Number(e.target.value) })}>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Document date"><input type="date" className={inputCls} value={editForm.documentDate} onChange={(e) => setEditForm({ ...editForm, documentDate: e.target.value })} /></Field>
              <Field label="Expiry date"><input type="date" className={inputCls} value={editForm.expiryDate} onChange={(e) => setEditForm({ ...editForm, expiryDate: e.target.value })} /></Field>
            </div>
            <Field label="Description"><input className={inputCls} value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} /></Field>
            <div className="flex justify-end gap-2">
              <button onClick={() => setEditOpen(false)} className="rounded-lg px-4 py-2 text-sm text-slate-600 hover:bg-slate-100">Cancel</button>
              <button onClick={saveMeta} className="rounded-lg bg-pcc-700 px-4 py-2 text-sm font-semibold text-white hover:bg-pcc-800">Save</button>
            </div>
          </div>)}
      </Modal>

      <Modal open={assocOpen} onClose={() => setAssocOpen(false)} title="Link business record">
        <div className="space-y-3">
          <Field label="Entity type">
            <select className={inputCls} value={assocForm.entityType} onChange={(e) => setAssocForm({ ...assocForm, entityType: e.target.value })}>
              <option value="">Select…</option>
              {Object.entries(ENTITY_TYPE_LABELS).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </select>
          </Field>
          <Field label="Entity ID / reference">
            <input className={inputCls} value={assocForm.entityRef} onChange={(e) => setAssocForm({ ...assocForm, entityRef: e.target.value })} placeholder="e.g. 12" />
          </Field>
          <p className="text-[11px] text-slate-400">References are validated against the owning service where a lookup is available.</p>
          <div className="flex justify-end gap-2">
            <button onClick={() => setAssocOpen(false)} className="rounded-lg px-4 py-2 text-sm text-slate-600 hover:bg-slate-100">Cancel</button>
            <button onClick={addAssoc} disabled={!assocForm.entityType || !assocForm.entityRef}
                    className="rounded-lg bg-pcc-700 px-4 py-2 text-sm font-semibold text-white hover:bg-pcc-800 disabled:opacity-50">Link</button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
