import { useCallback, useEffect, useState } from 'react'
import DataTable from '../../components/DataTable'
import Modal from '../../components/Modal'
import { inputCls } from '../property/PropertiesPage'
import { wfFetch, StatusBadge, MODULES, ROLES } from './wfShared'
import { useAuth } from '../../context/AuthContext'
import { Plus, Pencil, Trash2 } from 'lucide-react'

const EMPTY = { code: '', name: '', description: '', module: 'PROPERTY', workflowType: '',
  entityType: '', integrationMethod: '', integrationPath: '', escalationDays: '', active: true,
  levels: [{ levelOrder: 1, name: 'Level 1', approverRole: 'MANAGER', approverId: '', minAmount: '', mandatory: true }] }

export default function DefinitionsPage() {
  const { hasPermission } = useAuth()
  const [rows, setRows] = useState([])
  const [module, setModule] = useState('')
  const [editing, setEditing] = useState(null)   // null | {form, id}
  const [error, setError] = useState('')
  const canConfig = hasPermission('WORKFLOW_CONFIGURE')

  const load = useCallback(async () => {
    const res = await wfFetch(`/workflow-definitions${module ? `?module=${module}` : ''}`)
    if (res.ok) setRows(await res.json())
  }, [module])
  useEffect(() => { load() }, [load])

  const openNew = () => setEditing({ id: null, form: structuredClone(EMPTY) })
  const openEdit = (d) => setEditing({
    id: d.id,
    form: { ...d, escalationDays: d.escalationDays ?? '', integrationMethod: d.integrationMethod ?? '',
      integrationPath: d.integrationPath ?? '',
      levels: d.levels.map((l) => ({ ...l, approverId: l.approverId ?? '', minAmount: l.minAmount ?? '' })) },
  })

  const setF = (k) => (e) => setEditing((s) => ({ ...s, form: { ...s.form, [k]: e.target?.value ?? e } }))
  const setL = (idx, k) => (e) => setEditing((s) => {
    const lv = s.form.levels.slice(); lv[idx] = { ...lv[idx], [k]: e.target?.value ?? e }
    return { ...s, form: { ...s.form, levels: lv } }
  })

  const save = async () => {
    const f = editing.form
    const payload = {
      code: f.code, name: f.name, description: f.description || undefined,
      module: f.module, workflowType: f.workflowType, entityType: f.entityType,
      integrationMethod: f.integrationMethod || undefined,
      integrationPath: f.integrationPath || undefined,
      escalationDays: f.escalationDays === '' ? undefined : Number(f.escalationDays),
      active: f.active,
      levels: f.levels.map((l) => ({
        levelOrder: Number(l.levelOrder), name: l.name,
        approverRole: l.approverRole || undefined,
        approverId: l.approverId === '' ? undefined : Number(l.approverId),
        minAmount: l.minAmount === '' ? undefined : Number(l.minAmount),
        mandatory: l.mandatory,
      })),
    }
    const res = await wfFetch(editing.id ? `/workflow-definitions/${editing.id}` : '/workflow-definitions', {
      method: editing.id ? 'PUT' : 'POST', body: JSON.stringify(payload),
    })
    if (!res.ok) { setError((await res.json().catch(() => ({}))).message ?? `Failed (${res.status})`); return }
    setEditing(null); setError(''); load()
  }

  const columns = [
    { key: 'code', label: 'Code', render: (d) => <span className="font-mono text-xs font-semibold text-pcc-700">{d.code}</span> },
    { key: 'name', label: 'Name', render: (d) => (
        <div><p className="text-sm text-slate-700">{d.name}</p>
        <p className="text-[11px] text-slate-400">{d.workflowType} · {d.entityType}</p></div>) },
    { key: 'module', label: 'Module', render: (d) => <span className="text-xs text-slate-600">{d.module}</span> },
    { key: 'levels', label: 'Levels', render: (d) => (
        <span className="text-xs text-slate-600">{d.levels.map((l) => `L${l.levelOrder}:${l.approverRole ?? `#${l.approverId}`}`).join(' → ')}</span>) },
    { key: 'int', label: 'Integration', render: (d) => (
        <span className="text-xs text-slate-500">{d.integrationMethod ? `${d.integrationMethod} ${d.integrationPath}` : '—'}</span>) },
    { key: 'active', label: 'Status', render: (d) => <StatusBadge value={d.active ? 'APPROVED' : 'CANCELLED'} /> },
    ...(canConfig ? [{ key: 'act', label: '', render: (d) => (
        <button onClick={() => openEdit(d)} className="rounded p-1.5 text-slate-400 hover:bg-pcc-50 hover:text-pcc-700"><Pencil size={15} /></button>) }] : []),
  ]

  return (
    <div>
      <div className="mb-4 flex items-center gap-3">
        <select className={`${inputCls} !h-10 w-44`} value={module} onChange={(e) => setModule(e.target.value)}>
          <option value="">All modules</option>
          {MODULES.map((m) => <option key={m} value={m}>{m}</option>)}
        </select>
        {canConfig && (
          <button onClick={openNew} className="ml-auto flex items-center gap-1.5 rounded-xl bg-pcc-600 px-4 py-2 text-sm font-medium text-white hover:bg-pcc-700">
            <Plus size={15} /> New definition</button>)}
      </div>
      <DataTable columns={columns} rows={rows} emptyMessage="No workflow definitions." />

      <Modal open={!!editing} title={editing?.id ? 'Edit definition' : 'New workflow definition'}
             onClose={() => setEditing(null)} width="max-w-2xl">
        {editing && (
          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <input className={inputCls} placeholder="CODE (A-Z0-9_)" value={editing.form.code} onChange={setF('code')} disabled={!!editing.id} />
              <input className={inputCls} placeholder="Name" value={editing.form.name} onChange={setF('name')} />
              <select className={inputCls} value={editing.form.module} onChange={setF('module')}>
                {MODULES.map((m) => <option key={m}>{m}</option>)}</select>
              <input className={inputCls} placeholder="Workflow type (e.g. EXPENSE)" value={editing.form.workflowType} onChange={setF('workflowType')} />
              <input className={inputCls} placeholder="Entity type (e.g. EXPENSE)" value={editing.form.entityType} onChange={setF('entityType')} />
              <input className={inputCls} placeholder="Escalation days" type="number" value={editing.form.escalationDays} onChange={setF('escalationDays')} />
              <select className={inputCls} value={editing.form.integrationMethod} onChange={setF('integrationMethod')}>
                <option value="">No integration</option><option>POST</option><option>PATCH</option><option>PUT</option></select>
              <input className={inputCls} placeholder="Integration path (e.g. /api/v1/expenses/{id}/approve)" value={editing.form.integrationPath} onChange={setF('integrationPath')} />
            </div>
            <textarea className={`${inputCls} h-16 py-2`} placeholder="Description" value={editing.form.description ?? ''} onChange={setF('description')} />
            <label className="flex items-center gap-2 text-sm text-slate-600">
              <input type="checkbox" checked={editing.form.active} onChange={(e) => setF('active')(e.target.checked)} /> Active</label>

            <h4 className="pt-2 text-xs font-semibold uppercase text-slate-400">Approval levels (sequential)</h4>
            {editing.form.levels.map((l, idx) => (
              <div key={idx} className="flex flex-wrap items-center gap-2 rounded-xl border border-slate-100 p-2">
                <span className="w-8 text-xs font-bold text-slate-500">L{l.levelOrder}</span>
                <input className={`${inputCls} !h-9 w-32`} placeholder="Level name" value={l.name} onChange={setL(idx, 'name')} />
                <select className={`${inputCls} !h-9 w-44`} value={l.approverRole ?? ''} onChange={setL(idx, 'approverRole')}>
                  <option value="">Specific employee</option>
                  {ROLES.map((r) => <option key={r}>{r}</option>)}</select>
                <input className={`${inputCls} !h-9 w-28`} placeholder="Emp. ID" value={l.approverId} onChange={setL(idx, 'approverId')} />
                <input className={`${inputCls} !h-9 w-28`} placeholder="Min amount" type="number" value={l.minAmount} onChange={setL(idx, 'minAmount')} />
                <label className="flex items-center gap-1 text-xs text-slate-500">
                  <input type="checkbox" checked={l.mandatory} onChange={(e) => setL(idx, 'mandatory')(e.target.checked)} /> Mandatory</label>
                {editing.form.levels.length > 1 && (
                  <button onClick={() => setEditing((s) => ({ ...s, form: { ...s.form, levels: s.form.levels.filter((_, i) => i !== idx).map((x, i) => ({ ...x, levelOrder: i + 1 })) } }))}
                          className="rounded p-1 text-red-400 hover:bg-red-50"><Trash2 size={14} /></button>)}
              </div>))}
            <button onClick={() => setEditing((s) => ({ ...s, form: { ...s.form, levels: [...s.form.levels, { levelOrder: s.form.levels.length + 1, name: `Level ${s.form.levels.length + 1}`, approverRole: 'MANAGER', approverId: '', minAmount: '', mandatory: true }] } }))}
                    className="text-xs font-medium text-pcc-700 hover:underline">+ Add level</button>
            {error && <p className="text-xs text-red-600">{error}</p>}
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setEditing(null)} className="rounded-xl border border-slate-200 px-4 py-2 text-sm text-slate-600">Cancel</button>
              <button onClick={save} className="rounded-xl bg-pcc-600 px-4 py-2 text-sm font-medium text-white hover:bg-pcc-700">Save</button>
            </div>
          </div>)}
      </Modal>
    </div>
  )
}
