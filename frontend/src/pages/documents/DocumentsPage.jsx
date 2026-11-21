import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import DataTable from '../../components/DataTable'
import { useAuth } from '../../context/AuthContext'
import { inputCls } from '../property/PropertiesPage'
import UploadModal from './UploadModal'
import {
  DOMAIN_LABELS, ExpiryBadge, StatusBadge, canPreview, docFetch, downloadDocument,
  fetchCategories, fmtDate, fmtSize, previewDocument,
} from './docShared'
import { Download, Eye, Plus, Search } from 'lucide-react'

/** Generic document list — reused for All/Property/Land/Finance/Archived views. */
export default function DocumentsPage({ domain, archived = false }) {
  const { hasPermission } = useAuth()
  const [params, setParams] = useSearchParams()
  const [rows, setRows] = useState([])
  const [categories, setCategories] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [q, setQ] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [uploadOpen, setUploadOpen] = useState(params.get('new') === '1')

  useEffect(() => { fetchCategories().then(setCategories) }, [])

  const load = useCallback(async () => {
    setLoading(true)
    const p = new URLSearchParams({ page, size: '15' })
    p.set('status', archived ? 'ARCHIVED' : 'ACTIVE')
    if (domain) p.set('domain', domain)
    if (categoryId) p.set('categoryId', categoryId)
    if (q) p.set('q', q)
    const res = await docFetch(`/documents?${p}`)
    if (res.ok) {
      const data = await res.json()
      setRows(data.content)
      setTotalPages(data.totalPages)
    }
    setLoading(false)
  }, [page, domain, categoryId, q, archived])

  useEffect(() => { load() }, [load])

  const restore = async (id) => {
    await docFetch(`/documents/${id}/restore`, { method: 'POST' })
    load()
  }

  const columns = [
    { key: 'ref', label: 'Reference', render: (d) => (
        <Link to={`/documents/${d.id}`} className="font-semibold text-pcc-700 hover:underline">{d.documentRef}</Link>) },
    { key: 'file', label: 'Filename', render: (d) => (
        <div>
          <p className="max-w-[220px] truncate text-sm text-slate-700" title={d.originalFilename}>{d.originalFilename}</p>
          <p className="text-[11px] text-slate-400">{d.mimeType?.split('/').pop()} · {fmtSize(d.fileSize)}</p>
        </div>) },
    { key: 'cat', label: 'Category', render: (d) => <span className="text-xs text-slate-600">{d.categoryName}</span> },
    { key: 'ver', label: 'Ver', render: (d) => <span className="text-xs text-slate-500">v{d.currentVersion}</span> },
    { key: 'expiry', label: 'Expiry', render: (d) => <ExpiryBadge doc={d} /> },
    { key: 'up', label: 'Uploaded', render: (d) => (
        <div>
          <p className="text-xs text-slate-600">{d.uploadedByName}</p>
          <p className="text-[11px] text-slate-400">{fmtDate(d.createdAt)}</p>
        </div>) },
    { key: 'status', label: 'Status', render: (d) => <StatusBadge value={d.status} /> },
    { key: 'act', label: '', render: (d) => (
        <div className="flex items-center gap-1">
          {canPreview(d.mimeType) && hasPermission('DOCUMENT_PREVIEW') && !archived && (
            <button onClick={() => previewDocument(d.id)} title="Preview"
                    className="rounded p-1.5 text-slate-400 hover:bg-pcc-50 hover:text-pcc-700"><Eye size={15} /></button>)}
          {hasPermission('DOCUMENT_DOWNLOAD') && (
            <button onClick={() => downloadDocument(d.id, d.originalFilename)} title="Download"
                    className="rounded p-1.5 text-slate-400 hover:bg-pcc-50 hover:text-pcc-700"><Download size={15} /></button>)}
          {archived && hasPermission('DOCUMENT_RESTORE') && (
            <button onClick={() => restore(d.id)}
                    className="rounded px-2 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-50">Restore</button>)}
        </div>) },
  ]

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input className={`${inputCls} !h-10 w-72 !pl-9`} placeholder="Search ref, filename…"
                 value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        </div>
        <select className={`${inputCls} !h-10 w-52`} value={categoryId}
                onChange={(e) => { setCategoryId(e.target.value); setPage(0) }}>
          <option value="">All categories</option>
          {(domain ? categories.filter((c) => c.domain === domain || c.domain === 'ADMINISTRATION') : categories)
            .map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        {!archived && hasPermission('DOCUMENT_UPLOAD') && (
          <button onClick={() => setUploadOpen(true)}
                  className="ml-auto flex items-center gap-2 rounded-lg bg-pcc-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-pcc-800">
            <Plus size={15} /> Upload
          </button>)}
      </div>

      <DataTable columns={columns} rows={rows} loading={loading}
                 empty={archived ? 'No archived documents' : `No ${DOMAIN_LABELS[domain]?.toLowerCase() ?? ''} documents yet`}
                 page={page} totalPages={totalPages} onPage={setPage} />

      <UploadModal open={uploadOpen} onClose={() => { setUploadOpen(false); params.delete('new'); setParams(params, { replace: true }) }}
                   onUploaded={load} defaultDomain={domain} />
    </div>
  )
}
