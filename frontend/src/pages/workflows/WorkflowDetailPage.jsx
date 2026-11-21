import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Modal from '../../components/Modal'
import { inputCls } from '../property/PropertiesPage'
import { wfFetch, StatusBadge, fmtDate } from './wfShared'
import { useAuth } from '../../context/AuthContext'
import { CheckCircle2, XCircle, RotateCcw, MessageSquare, RefreshCw, Send, Ban } from 'lucide-react'

export default function WorkflowDetailPage() {
  const { id } = useParams()
  const { hasPermission, user } = useAuth()
  const [wf, setWf] = useState(null)
  const [tasks, setTasks] = useState([])
  const [history, setHistory] = useState([])
  const [integrations, setIntegrations] = useState([])
  const [action, setAction] = useState(null) // 'approve'|'reject'|'return'|'comment'
  const [comment, setComment] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    const r = await wfFetch(`/workflows/${id}`)
    if (r.ok) setWf(await r.json())
    wfFetch(`/workflows/${id}/tasks`).then((r) => (r.ok ? r.json() : [])).then(setTasks)
    wfFetch(`/workflows/${id}/history`).then((r) => (r.ok ? r.json() : [])).then(setHistory)
    wfFetch(`/workflows/${id}/integrations`).then((r) => (r.ok ? r.json() : [])).then(setIntegrations)
  }, [id])

  useEffect(() => { load() }, [load])

  const act = async () => {
    setBusy(true); setError('')
    const res = await wfFetch(`/workflows/${id}/${action}`, {
      method: 'POST',
      body: JSON.stringify({ comment: comment || undefined }),
    })
    if (!res.ok) setError((await res.json().catch(() => ({}))).message ?? `Failed (${res.status})`)
    else { setAction(null); setComment('') }
    setBusy(false); load()
  }

  const resubmit = async () => {
    const res = await wfFetch(`/workflows/${id}/resubmit`, { method: 'POST', body: '{}' })
    if (res.ok) load()
  }

  const cancel = async () => {
    const res = await wfFetch(`/workflows/${id}/cancel`, { method: 'POST' })
    if (res.ok) load()
  }

  const retry = async (outId) => {
    await wfFetch(`/workflows/integrations/${outId}/retry`, { method: 'POST' })
    load()
  }

  if (!wf) return <p className="py-10 text-center text-sm text-slate-400">Loading…</p>

  const pending = wf.status === 'PENDING_APPROVAL'
  const isSubmitter = user?.id === wf.submittedById

  return (
    <div className="space-y-5">
      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-bold text-slate-800">{wf.workflowRef}
              <span className="ml-3"><StatusBadge value={wf.status} /></span></h2>
            <p className="mt-1 text-sm text-slate-500">{wf.definitionName} · {wf.module} · {wf.entityType}:{wf.entityRef}</p>
            {wf.description && <p className="mt-2 text-sm text-slate-600">{wf.description}</p>}
            <p className="mt-2 text-xs text-slate-400">
              Submitted by {wf.submittedByName} on {fmtDate(wf.submittedAt)} · level {wf.currentLevel}
              {wf.amount != null && ` · amount ${wf.amount}`}
              {wf.dueAt && <> · due {fmtDate(wf.dueAt)}{wf.overdue && <span className="font-semibold text-red-600"> (overdue)</span>}</>}
            </p>
            {wf.documentRefs?.length > 0 && (
              <p className="mt-1 text-xs text-slate-500">Documents: {wf.documentRefs.join(', ')}</p>)}
          </div>
          <div className="flex flex-wrap gap-2">
            {pending && hasPermission('WORKFLOW_APPROVE') && (
              <button onClick={() => setAction('approve')} className="flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-emerald-700">
                <CheckCircle2 size={15} /> Approve</button>)}
            {pending && hasPermission('WORKFLOW_REJECT') && (
              <button onClick={() => setAction('reject')} className="flex items-center gap-1.5 rounded-xl bg-red-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-red-700">
                <XCircle size={15} /> Reject</button>)}
            {pending && hasPermission('WORKFLOW_RETURN') && (
              <button onClick={() => setAction('return')} className="flex items-center gap-1.5 rounded-xl bg-orange-500 px-3.5 py-2 text-sm font-medium text-white hover:bg-orange-600">
                <RotateCcw size={15} /> Return</button>)}
            {wf.status === 'RETURNED_FOR_CORRECTION' && isSubmitter && (
              <button onClick={resubmit} className="flex items-center gap-1.5 rounded-xl bg-pcc-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-pcc-700">
                <Send size={15} /> Resubmit</button>)}
            {(isSubmitter || hasPermission('WORKFLOW_CANCEL')) && !['COMPLETED','CANCELLED'].includes(wf.status) && hasPermission('WORKFLOW_CANCEL') && (
              <button onClick={cancel} className="flex items-center gap-1.5 rounded-xl border border-slate-200 px-3.5 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50">
                <Ban size={15} /> Cancel</button>)}
            <button onClick={() => setAction('comment')} className="flex items-center gap-1.5 rounded-xl border border-slate-200 px-3.5 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50">
              <MessageSquare size={15} /> Comment</button>
          </div>
        </div>
      </div>

      <div className="grid gap-5 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 className="mb-3 text-sm font-semibold text-slate-800">Approval Timeline</h3>
          <div className="space-y-3">
            {tasks.map((t) => (
              <div key={t.id} className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-medium text-slate-700">Level {t.levelOrder} — {t.assigneeRole ?? `Employee #${t.assigneeId}`}</p>
                  <p className="text-xs text-slate-400">
                    {t.actedByName ? `${t.actedByName} · ${fmtDate(t.actedAt)}` : `due ${fmtDate(t.dueAt)}`}
                  </p>
                  {t.comment && <p className="mt-0.5 text-xs italic text-slate-500">“{t.comment}”</p>}
                </div>
                <StatusBadge value={t.status} />
              </div>))}
          </div>
          {integrations.length > 0 && (
            <div className="mt-4 border-t border-slate-100 pt-3">
              <h4 className="mb-2 text-xs font-semibold uppercase text-slate-400">Integration</h4>
              {integrations.map((o) => (
                <div key={o.id} className="mb-2 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-slate-600">{o.method} {o.service}{o.path}</p>
                    {o.lastError && <p className="text-[11px] text-red-500">{o.lastError}</p>}
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusBadge value={o.status} />
                    {o.status === 'FAILED' && hasPermission('WORKFLOW_APPROVE') && (
                      <button onClick={() => retry(o.id)} title="Retry"
                              className="rounded p-1 text-slate-400 hover:bg-pcc-50 hover:text-pcc-700"><RefreshCw size={14} /></button>)}
                  </div>
                </div>))}
            </div>)}
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 className="mb-3 text-sm font-semibold text-slate-800">Action History</h3>
          <div className="space-y-2.5">
            {history.map((a) => (
              <div key={a.id} className="flex items-start gap-3">
                <span className="mt-1 h-2 w-2 shrink-0 rounded-full bg-pcc-400" />
                <div>
                  <p className="text-sm text-slate-700">
                    <span className="font-medium">{a.action}</span>
                    {a.levelOrder > 0 && <span className="text-slate-400"> · level {a.levelOrder}</span>}
                  </p>
                  <p className="text-xs text-slate-400">{a.actorName} · {fmtDate(a.createdAt)}</p>
                  {a.comment && <p className="text-xs italic text-slate-500">“{a.comment}”</p>}
                </div>
              </div>))}
          </div>
        </div>
      </div>

      <Modal open={!!action}
             title={`${action === 'return' ? 'Return for correction' : (action ?? '').charAt(0).toUpperCase() + (action ?? '').slice(1)} ${wf.workflowRef}`}
             onClose={() => { setAction(null); setError('') }}>
          <textarea className={`${inputCls} h-24 py-2.5`} placeholder={
              action === 'approve' ? 'Comment (optional)' : action === 'comment' ? 'Comment' : 'Reason (required)'}
            value={comment} onChange={(e) => setComment(e.target.value)} />
          {error && <p className="mt-2 text-xs text-red-600">{error}</p>}
          <div className="mt-4 flex justify-end gap-2">
            <button onClick={() => setAction(null)} className="rounded-xl border border-slate-200 px-4 py-2 text-sm text-slate-600">Cancel</button>
            <button onClick={act} disabled={busy || ((action === 'reject' || action === 'return' || action === 'comment') && !comment.trim())}
                    className="rounded-xl bg-pcc-600 px-4 py-2 text-sm font-medium text-white hover:bg-pcc-700 disabled:opacity-50">
              {busy ? 'Sending…' : 'Confirm'}</button>
          </div>
        </Modal>
    </div>
  )
}
