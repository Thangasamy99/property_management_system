import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import DataTable from '../../components/DataTable'
import Modal from '../../components/Modal'
import { inputCls } from '../property/PropertiesPage'
import { wfFetch, StatusBadge, fmtDate, MODULES, PRIORITIES } from './wfShared'
import { useAuth } from '../../context/AuthContext'
import { Search, ChevronLeft, ChevronRight, Plus } from 'lucide-react'

function SubmitModal({ open, onClose, onSaved }) {
  const [defs, setDefs] = useState([])
  const [form, setForm] = useState({ definitionId: '', entityRef: '', description: '', priority: 'NORMAL', amount: '', documentRefs: '' })
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) wfFetch('/workflow-definitions').then((r) => (r.ok ? r.json() : []))
      .then((d) => setDefs(d.filter((x) => x.active)))
  }, [open])

  const save = async () => {
    const res = await wfFetch('/workflows', {
      method: 'POST',
      body: JSON.stringify({
        definitionId: Number(form.definitionId), entityRef: form.entityRef,
        description: form.description || undefined,
        priority: form.priority,
        amount: form.amount === '' ? undefined : Number(form.amount),
        documentRefs: form.documentRefs ? form.documentRefs.split(',').map((s) => s.trim()).filter(Boolean) : undefined,
      }),
    })
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? `Failed (${res.status})`); return }
    setError(''); onClose(); onSaved()
  }

  return (
    <Modal open={open} title="Submit workflow request" onClose={onClose}>
      <div className="space-y-3">
        <select className={inputCls} value={form.definitionId}
                onChange={(e) => setForm({ ...form, definitionId: e.target.value })}>
          <option value="">Select workflow…</option>
          {defs.map((d) => <option key={d.id} value={d.id}>{d.name} ({d.module})</option>)}
        </select>
        <div className="grid grid-cols-2 gap-3">
          <input className={inputCls} placeholder="Entity reference (ID)" value={form.entityRef}
                 onChange={(e) => setForm({ ...form, entityRef: e.target.value })} />
          <input className={inputCls} placeholder="Amount (optional)" type="number" value={form.amount}
                 onChange={(e) => setForm({ ...form, amount: e.target.value })} />
          <select className={inputCls} value={form.priority}
                  onChange={(e) => setForm({ ...form, priority: e.target.value })}>
            {PRIORITIES.map((p) => <option key={p}>{p}</option>)}
          </select>
          <input className={inputCls} placeholder="Doc refs (comma-separated)" value={form.documentRefs}
                 onChange={(e) => setForm({ ...form, documentRefs: e.target.value })} />
        </div>
        <textarea className={`${inputCls} h-20 py-2`} placeholder="Description" value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })} />
        {error && <p className="text-xs text-red-600">{error}</p>}
        <div className="flex justify-end gap-2">
          <button onClick={onClose} className="rounded-xl border border-slate-200 px-4 py-2 text-sm text-slate-600">Cancel</button>
          <button onClick={save} disabled={!form.definitionId || !form.entityRef}
                  className="rounded-xl bg-pcc-600 px-4 py-2 text-sm font-medium text-white hover:bg-pcc-700 disabled:opacity-50">Submit</button>
        </div>
      </div>
    </Modal>
  )
}

/** Generic workflow list — modes: all | mine | pending | overdue | returned | history */
export default function RequestsPage({ mode = 'all' }) {
  const { hasPermission } = useAuth()
  const [params] = useSearchParams()
  const [rows, setRows] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [q, setQ] = useState('')
  const [module, setModule] = useState('')
  const [status, setStatus] = useState(params.get('status') ?? '')
  const [priority, setPriority] = useState('')
  const [submitOpen, setSubmitOpen] = useState(params.get('new') === '1')
  const canSubmit = hasPermission('WORKFLOW_SUBMIT') && (mode === 'all' || mode === 'mine')

  const load = useCallback(async () => {
    setLoading(true)
    const p = new URLSearchParams({ page, size: '15' })
    let url
    if (mode === 'mine') { url = '/workflows/mine'; if (status) p.set('status', status) }
    else if (mode === 'pending') url = '/workflows/pending-approvals'
    else if (mode === 'overdue') url = '/workflows/overdue'
    else {
      url = '/workflows'
      if (mode === 'returned') p.set('status', 'RETURNED_FOR_CORRECTION')
      else if (mode === 'history') p.set('status', 'COMPLETED')
      else if (status) p.set('status', status)
      if (module) p.set('module', module)
      if (priority) p.set('priority', priority)
      if (q) p.set('q', q)
    }
    const res = await wfFetch(`${url}?${p}`)
    if (res.ok) {
      const d = await res.json()
      setRows(d.content)
      setTotalPages(d.totalPages)
    }
    setLoading(false)
  }, [mode, page, q, module, status, priority])

  useEffect(() => { load() }, [load])

  const isTask = mode === 'pending'
  const columns = [
    { key: 'ref', label: 'Reference', render: (r) => (
        <Link to={`/workflows/detail/${isTask ? r.instanceId : r.id}`}
              className="font-semibold text-pcc-700 hover:underline">{r.workflowRef}</Link>) },
    { key: 'type', label: 'Type', render: (r) => (
        <div>
          <p className="text-sm text-slate-700">{r.workflowType}</p>
          <p className="text-[11px] text-slate-400">{r.module} · {r.entityType}:{r.entityRef}</p>
        </div>) },
    ...(isTask ? [{ key: 'lvl', label: 'Level', render: (r) => <span className="text-xs text-slate-600">L{r.levelOrder} · {r.assigneeRole ?? `#${r.assigneeId}`}</span> }]
              : [{ key: 'lvl', label: 'Level', render: (r) => <span className="text-xs text-slate-600">L{r.currentLevel}</span> },
                 { key: 'sub', label: 'Submitted by', render: (r) => (
                     <div>
                       <p className="text-xs text-slate-600">{r.submittedByName}</p>
                       <p className="text-[11px] text-slate-400">{fmtDate(r.submittedAt)}</p>
                     </div>) }]),
    { key: 'status', label: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: 'due', label: 'Due', render: (r) => (
        <span className={`text-xs ${r.overdue ? 'font-semibold text-red-600' : 'text-slate-500'}`}>
          {fmtDate(r.dueAt)}{r.overdue ? ' · overdue' : ''}
        </span>) },
  ]

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        {(mode === 'all' || mode === 'history') && (
          <div className="relative">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input className={`${inputCls} !h-10 w-64 !pl-9`} placeholder="Search ref, entity…"
                   value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
          </div>)}
        {(mode === 'all' || mode === 'history') && (
          <select className={`${inputCls} !h-10 w-44`} value={module}
                  onChange={(e) => { setModule(e.target.value); setPage(0) }}>
            <option value="">All modules</option>
            {MODULES.map((m) => <option key={m} value={m}>{m}</option>)}
          </select>)}
        {(mode === 'all' || mode === 'mine') && (
          <select className={`${inputCls} !h-10 w-52`} value={status}
                  onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
            <option value="">All statuses</option>
            {['SUBMITTED','PENDING_APPROVAL','APPROVED','REJECTED','RETURNED_FOR_CORRECTION',
              'CANCELLED','COMPLETED','INTEGRATION_PENDING','INTEGRATION_FAILED']
              .map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}
          </select>)}
        {mode === 'all' && (
          <select className={`${inputCls} !h-10 w-36`} value={priority}
                  onChange={(e) => { setPriority(e.target.value); setPage(0) }}>
            <option value="">Priority</option>
            {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>)}
        <button onClick={load} className="rounded-xl bg-pcc-600 px-4 py-2 text-sm font-medium text-white hover:bg-pcc-700">
          Refresh
        </button>
        {canSubmit && (
          <button onClick={() => setSubmitOpen(true)} className="ml-auto flex items-center gap-1.5 rounded-xl bg-pcc-600 px-4 py-2 text-sm font-medium text-white hover:bg-pcc-700">
            <Plus size={15} /> New request</button>)}
      </div>

      <DataTable columns={columns} rows={rows} loading={loading}
                 emptyMessage="No workflow requests found." />

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2">
          <button disabled={page === 0} onClick={() => setPage(page - 1)}
                  className="rounded-lg border border-slate-200 p-1.5 disabled:opacity-40"><ChevronLeft size={15} /></button>
          <span className="text-xs text-slate-500">Page {page + 1} of {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}
                  className="rounded-lg border border-slate-200 p-1.5 disabled:opacity-40"><ChevronRight size={15} /></button>
        </div>)}
      <SubmitModal open={submitOpen} onClose={() => setSubmitOpen(false)} onSaved={load} />
    </div>
  )
}
