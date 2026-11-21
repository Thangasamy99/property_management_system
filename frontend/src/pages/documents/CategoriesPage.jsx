import { useEffect, useState } from 'react'
import DataTable from '../../components/DataTable'
import Modal from '../../components/Modal'
import { useAuth } from '../../context/AuthContext'
import { inputCls } from '../property/PropertiesPage'
import { DOMAIN_LABELS, Field, docFetch } from './docShared'
import { Pencil, Plus } from 'lucide-react'

const DOMAINS = Object.keys(DOMAIN_LABELS)

export default function CategoriesPage() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('DOCUMENT_CATEGORY_MANAGE')
  const [rows, setRows] = useState([])
  const [domain, setDomain] = useState('')
  const [editing, setEditing] = useState(null)   // null | {id?, code, name, domain, description, requiresExpiry, active}
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const load = async () => {
    const res = await docFetch(`/categories${domain ? `?domain=${domain}` : ''}`)
    if (res.ok) setRows(await res.json())
  }
  useEffect(() => { load() }, [domain])

  const save = async () => {
    setBusy(true); setError(null)
    const res = await docFetch(editing.id ? `/categories/${editing.id}` : '/categories', {
      method: editing.id ? 'PUT' : 'POST',
      body: JSON.stringify({
        code: editing.code, name: editing.name, domain: editing.domain,
        description: editing.description, requiresExpiry: editing.requiresExpiry,
        active: editing.active,
      }),
    })
    const data = await res.json().catch(() => ({}))
    setBusy(false)
    if (!res.ok) return setError(data.message ?? `Save failed (${res.status})`)
    setEditing(null); load()
  }

  const columns = [
    { key: 'code', label: 'Code', render: (c) => <span className="font-mono text-xs font-semibold text-slate-700">{c.code}</span> },
    { key: 'name', label: 'Name', render: (c) => <span className="text-sm text-slate-700">{c.name}</span> },
    { key: 'domain', label: 'Domain', render: (c) => <span className="rounded-full bg-pcc-50 px-2.5 py-1 text-[11px] font-semibold text-pcc-700">{DOMAIN_LABELS[c.domain] ?? c.domain}</span> },
    { key: 'exp', label: 'Requires expiry', render: (c) => <span className="text-xs text-slate-500">{c.requiresExpiry ? 'Yes' : 'No'}</span> },
    { key: 'desc', label: 'Description', render: (c) => <span className="text-xs text-slate-500">{c.description ?? '—'}</span> },
    { key: 'act', label: '', render: (c) => canManage && (
        <button onClick={() => setEditing({ ...c })} className="rounded p-1.5 text-slate-400 hover:bg-pcc-50 hover:text-pcc-700"><Pencil size={14} /></button>) },
  ]

  return (
    <div>
      <div className="mb-4 flex items-center gap-3">
        <select className={`${inputCls} !h-10 w-52`} value={domain} onChange={(e) => setDomain(e.target.value)}>
          <option value="">All domains</option>
          {DOMAINS.map((d) => <option key={d} value={d}>{DOMAIN_LABELS[d]}</option>)}
        </select>
        {canManage && (
          <button onClick={() => setEditing({ code: '', name: '', domain: 'PROPERTY', description: '', requiresExpiry: false, active: true })}
                  className="ml-auto flex items-center gap-2 rounded-lg bg-pcc-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-pcc-800">
            <Plus size={15} /> New category
          </button>)}
      </div>
      <DataTable columns={columns} rows={rows} empty="No categories" />

      <Modal open={!!editing} onClose={() => setEditing(null)} title={editing?.id ? 'Edit category' : 'New category'}>
        {editing && (
          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <Field label="Code (A–Z, 0–9, _)">
                <input className={inputCls} value={editing.code} disabled={!!editing.id}
                       onChange={(e) => setEditing({ ...editing, code: e.target.value.toUpperCase() })} />
              </Field>
              <Field label="Domain">
                <select className={inputCls} value={editing.domain}
                        onChange={(e) => setEditing({ ...editing, domain: e.target.value })}>
                  {DOMAINS.map((d) => <option key={d} value={d}>{DOMAIN_LABELS[d]}</option>)}
                </select>
              </Field>
            </div>
            <Field label="Name">
              <input className={inputCls} value={editing.name}
                     onChange={(e) => setEditing({ ...editing, name: e.target.value })} />
            </Field>
            <Field label="Description">
              <input className={inputCls} value={editing.description ?? ''}
                     onChange={(e) => setEditing({ ...editing, description: e.target.value })} />
            </Field>
            <div className="flex gap-6">
              <label className="flex items-center gap-2 text-sm text-slate-600">
                <input type="checkbox" checked={editing.requiresExpiry}
                       onChange={(e) => setEditing({ ...editing, requiresExpiry: e.target.checked })} /> Requires expiry date
              </label>
              <label className="flex items-center gap-2 text-sm text-slate-600">
                <input type="checkbox" checked={editing.active}
                       onChange={(e) => setEditing({ ...editing, active: e.target.checked })} /> Active
              </label>
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <div className="flex justify-end gap-2">
              <button onClick={() => setEditing(null)} className="rounded-lg px-4 py-2 text-sm text-slate-600 hover:bg-slate-100">Cancel</button>
              <button onClick={save} disabled={busy || !editing.code || !editing.name}
                      className="rounded-lg bg-pcc-700 px-4 py-2 text-sm font-semibold text-white hover:bg-pcc-800 disabled:opacity-50">
                {busy ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
