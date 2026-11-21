import { useEffect, useRef, useState } from 'react'
import Modal from '../../components/Modal'
import { inputCls } from '../property/PropertiesPage'
import { ENTITY_TYPE_LABELS, Field, docUpload, fetchCategories } from './docShared'
import { UploadCloud } from 'lucide-react'

const ACCEPT = '.pdf,.jpg,.jpeg,.png,.doc,.docx,.xls,.xlsx'

export default function UploadModal({ open, onClose, onUploaded, defaultDomain, defaultEntity }) {
  const [categories, setCategories] = useState([])
  const [file, setFile] = useState(null)
  const [categoryId, setCategoryId] = useState('')
  const [description, setDescription] = useState('')
  const [documentDate, setDocumentDate] = useState('')
  const [expiryDate, setExpiryDate] = useState('')
  const [entityType, setEntityType] = useState(defaultEntity?.type ?? '')
  const [entityRef, setEntityRef] = useState(defaultEntity?.ref ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [drag, setDrag] = useState(false)
  const inputRef = useRef(null)

  useEffect(() => {
    if (!open) return
    fetchCategories().then(setCategories)
    setEntityType(defaultEntity?.type ?? '')
    setEntityRef(defaultEntity?.ref ?? '')
  }, [open, defaultEntity])

  const visible = defaultDomain ? categories.filter((c) => c.domain === defaultDomain || c.domain === 'ADMINISTRATION') : categories
  const selected = categories.find((c) => String(c.id) === categoryId)

  const submit = async () => {
    if (!file || !categoryId) return
    setBusy(true); setError(null)
    const fd = new FormData()
    fd.append('file', file)
    fd.append('categoryId', categoryId)
    if (description) fd.append('description', description)
    if (documentDate) fd.append('documentDate', documentDate)
    if (expiryDate) fd.append('expiryDate', expiryDate)
    if (entityType && entityRef)
      fd.append('associations', JSON.stringify([{ entityType, entityRef }]))
    try {
      const res = await docUpload('/documents', fd)
      const data = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(data.message ?? `Upload failed (${res.status})`)
      onUploaded?.(data)
      onClose()
      setFile(null); setDescription(''); setDocumentDate(''); setExpiryDate('')
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Upload document">
      <div className="space-y-4">
        <div
          onDragOver={(e) => { e.preventDefault(); setDrag(true) }}
          onDragLeave={() => setDrag(false)}
          onDrop={(e) => { e.preventDefault(); setDrag(false); const f = e.dataTransfer.files?.[0]; if (f) setFile(f) }}
          onClick={() => inputRef.current?.click()}
          className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-4 py-8 text-center transition ${drag ? 'border-pcc-400 bg-pcc-50' : 'border-slate-200 hover:border-pcc-300'}`}
        >
          <UploadCloud size={28} className="mb-2 text-pcc-500" />
          {file
            ? <p className="text-sm font-medium text-slate-700">{file.name} <span className="text-slate-400">({(file.size / 1024).toFixed(0)} KB)</span></p>
            : <p className="text-sm text-slate-500">Drag a file here or <span className="font-semibold text-pcc-700">browse</span></p>}
          <p className="mt-1 text-[11px] text-slate-400">PDF, JPG, PNG, DOC/DOCX, XLS/XLSX</p>
          <input ref={inputRef} type="file" accept={ACCEPT} className="hidden" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Category">
            <select className={inputCls} value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
              <option value="">Select category…</option>
              {visible.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <Field label="Document date">
            <input type="date" className={inputCls} value={documentDate} onChange={(e) => setDocumentDate(e.target.value)} />
          </Field>
          <Field label={`Expiry date${selected?.requiresExpiry ? ' (required)' : ''}`}>
            <input type="date" className={inputCls} value={expiryDate} onChange={(e) => setExpiryDate(e.target.value)} />
          </Field>
          <Field label="Description">
            <input className={inputCls} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Optional" />
          </Field>
        </div>

        <div className="rounded-xl bg-slate-50 p-3">
          <p className="mb-2 text-xs font-semibold text-slate-600">Link to a business record (optional)</p>
          <div className="grid grid-cols-2 gap-3">
            <select className={inputCls} value={entityType} onChange={(e) => setEntityType(e.target.value)}>
              <option value="">No association</option>
              {Object.entries(ENTITY_TYPE_LABELS).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </select>
            <input className={inputCls} value={entityRef} onChange={(e) => setEntityRef(e.target.value)}
                   placeholder="Entity ID (e.g. 12)" disabled={!entityType} />
          </div>
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex justify-end gap-2">
          <button onClick={onClose} className="rounded-lg px-4 py-2 text-sm text-slate-600 hover:bg-slate-100">Cancel</button>
          <button onClick={submit} disabled={!file || !categoryId || busy || (selected?.requiresExpiry && !expiryDate)}
                  className="rounded-lg bg-pcc-700 px-4 py-2 text-sm font-semibold text-white hover:bg-pcc-800 disabled:opacity-50">
            {busy ? 'Uploading…' : 'Upload'}
          </button>
        </div>
      </div>
    </Modal>
  )
}
