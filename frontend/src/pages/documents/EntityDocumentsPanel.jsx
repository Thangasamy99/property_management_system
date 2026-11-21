import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { docFetch, fmtSize, canPreview, downloadDocument, previewDocument } from './docShared'
import UploadModal from './UploadModal'
import { Download, Eye, Paperclip, Upload } from 'lucide-react'

/**
 * Embeddable panel listing documents associated with a business record.
 * Usage: <EntityDocumentsPanel entityType="PROPERTY" entityRef={property.id} domain="PROPERTY" />
 */
export default function EntityDocumentsPanel({ entityType, entityRef, domain }) {
  const { hasPermission } = useAuth()
  const [docs, setDocs] = useState([])
  const [uploadOpen, setUploadOpen] = useState(false)

  const load = useCallback(async () => {
    if (!entityRef) return
    const res = await docFetch(`/documents/for-entity?type=${entityType}&ref=${entityRef}`)
    if (res.ok) setDocs(await res.json())
  }, [entityType, entityRef])

  useEffect(() => { load() }, [load])

  if (!hasPermission('DOCUMENT_VIEW')) return null

  return (
    <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-700">
          <Paperclip size={14} className="text-pcc-500" /> Documents
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-500">{docs.length}</span>
        </h3>
        {hasPermission('DOCUMENT_UPLOAD') && hasPermission('DOCUMENT_ASSOCIATE') && (
          <button onClick={() => setUploadOpen(true)}
                  className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-50">
            <Upload size={13} /> Upload
          </button>)}
      </div>
      <div className="divide-y divide-slate-100">
        {docs.map((d) => (
          <div key={d.id} className="flex items-center justify-between py-2">
            <div className="min-w-0">
              <Link to={`/documents/${d.id}`} className="block truncate text-sm font-medium text-pcc-700 hover:underline">
                {d.originalFilename}
              </Link>
              <p className="text-[11px] text-slate-400">{d.documentRef} · {d.categoryName} · {fmtSize(d.fileSize)}</p>
            </div>
            <div className="flex items-center gap-1">
              {canPreview(d.mimeType) && hasPermission('DOCUMENT_PREVIEW') && d.status === 'ACTIVE' && (
                <button onClick={() => previewDocument(d.id)} className="rounded p-1.5 text-slate-400 hover:bg-pcc-50 hover:text-pcc-700"><Eye size={14} /></button>)}
              {hasPermission('DOCUMENT_DOWNLOAD') && (
                <button onClick={() => downloadDocument(d.id, d.originalFilename)} className="rounded p-1.5 text-slate-400 hover:bg-pcc-50 hover:text-pcc-700"><Download size={14} /></button>)}
            </div>
          </div>))}
        {docs.length === 0 && <p className="py-4 text-center text-xs text-slate-400">No documents linked to this record</p>}
      </div>
      <UploadModal open={uploadOpen} onClose={() => setUploadOpen(false)} onUploaded={load}
                   defaultDomain={domain}
                   defaultEntity={{ type: entityType, ref: String(entityRef) }} />
    </div>
  )
}
