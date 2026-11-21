import { useCallback, useEffect, useState } from 'react'
import DataTable from '../../components/DataTable'
import Modal from '../../components/Modal'
import { inputCls } from '../property/PropertiesPage'
import { wfFetch, StatusBadge, MODULES } from './wfShared'
import { useAuth } from '../../context/AuthContext'
import { Plus, Ban } from 'lucide-react'

export default function DelegationsPage() {
  const { hasPermission } = useAuth()
  const [rows, setRows] = useState([])
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({ delegateId: '', delegateName: '', module: '', startDate: '', endDate: '', reason: '' })
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    const res = await wfFetch('/delegations/mine')
    if (res.ok) setRows(await res.json())
  }, [])
  useEffect(() => { load() }, [load])

  const save = async () => {
    const res = await wfFetch('/delegations', {
      method: 'POST',
      body: JSON.stringify({
        delegateId: Number(form.delegateId), delegateName: form.delegateName || undefined,
        module: form.module || undefined, startDate: form.startDate, endDate: form.endDate,
        reason: form.reason || undefined,
      }),
    })
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? `Failed (${res.status})`); return }
    setOpen(false); setError(''); load()
  }

  const revoke = async (id) => {
    await wfFetch(`/delegations/${id}`, { method: 'DELETE' })
    load()
  }

  const columns = [
    { key: 'del', label: 'Delegator → Delegate', render: (d) => (
        <span className="text-sm text-slate-700">#{d.delegatorId} → {d.delegateName ?? `#${d.delegateId}`}</span>) },
    { key: 'scope', label: 'Scope', render: (d) => (
        <span className="text-xs text-slate-600">{d.module ?? 'All modules'}{d.definitionId ? ` · def ${d.definitionId}` : ''}</span>) },
    { key: 'dates', label: 'Period', render: (d) => <span className="text-xs text-slate-600">{d.startDate} → {d.endDate}</span> },
    { key: 'reason', label: 'Reason', render: (d) => <span className="text-xs text-slate-500">{d.reason ?? '—'}</span> },
    { key: 'status', label: 'Status', render: (d) => <StatusBadge value={d.status === 'ACTIVE' ? 'PENDING_APPROVAL' : 'CANCELLED'} /> },
    { key: 'act', label: '', render: (d) => (
        d.status === 'ACTIVE' && hasPermission('WORKFLOW_DELEGATE') && (
          <button onClick={() => revoke(d.id)} title="Revoke"
                  className="rounded p-1.5 text-red-400 hover:bg-red-50"><Ban size={15} /></button>)) },
  ]

  return (
    <div>
      <div className="mb-4 flex justify-end">
        {hasPermission('WORKFLOW_DELEGATE') && (
          <button onClick={() => setOpen(true)} className="flex items-center gap-1.5 rounded-xl bg-pcc-600 px-4 py-2 text-sm font-medium text-white hover:bg-pcc-700">
            <Plus size={15} /> New delegation</button>)}
      </div>
      <DataTable columns={columns} rows={rows} emptyMessage="No delegations." />

      <Modal open={open} title="New delegation" onClose={() => setOpen(false)}>
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <input className={inputCls} placeholder="Delegate employee ID" type="number" value={form.delegateId}
                   onChange={(e) => setForm({ ...form, delegateId: e.target.value })} />
            <input className={inputCls} placeholder="Delegate name (optional)" value={form.delegateName}
                   onChange={(e) => setForm({ ...form, delegateName: e.target.value })} />
            <select className={inputCls} value={form.module} onChange={(e) => setForm({ ...form, module: e.target.value })}>
              <option value="">All modules</option>
              {MODULES.map((m) => <option key={m}>{m}</option>)}</select>
            <input className={inputCls} placeholder="Reason" value={form.reason}
                   onChange={(e) => setForm({ ...form, reason: e.target.value })} />
            <label className="text-xs text-slate-500">From
              <input className={inputCls} type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></label>
            <label className="text-xs text-slate-500">To
              <input className={inputCls} type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></label>
          </div>
          {error && <p className="text-xs text-red-600">{error}</p>}
          <div className="flex justify-end gap-2">
            <button onClick={() => setOpen(false)} className="rounded-xl border border-slate-200 px-4 py-2 text-sm text-slate-600">Cancel</button>
            <button onClick={save} disabled={!form.delegateId || !form.startDate || !form.endDate}
                    className="rounded-xl bg-pcc-600 px-4 py-2 text-sm font-medium text-white hover:bg-pcc-700 disabled:opacity-50">Save</button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
