import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { wfFetch, StatusBadge, fmtDate } from './wfShared'
import { useAuth } from '../../context/AuthContext'
import { ClipboardCheck, Inbox, Send, AlertTriangle, CheckCircle2, XCircle, RotateCcw, Bell } from 'lucide-react'

export default function WorkflowDashboardPage() {
  const { hasPermission } = useAuth()
  const [summary, setSummary] = useState(null)
  const [pending, setPending] = useState([])
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    wfFetch('/workflows/dashboard').then((r) => (r.ok ? r.json() : null)).then(setSummary)
    wfFetch('/workflows/pending-approvals?size=6').then((r) => (r.ok ? r.json() : { content: [] }))
      .then((d) => setPending(d.content ?? []))
    if (hasPermission('NOTIFICATION_VIEW'))
      wfFetch('/notifications/unread-count').then((r) => (r.ok ? r.json() : { unread: 0 }))
        .then((d) => setUnread(d.unread))
  }, [])

  const card = 'rounded-2xl border border-slate-200 bg-white p-5 shadow-sm'
  const stat = (icon, label, value, tone, to) => (
    <Link to={to ?? '#'} className={`${card} block transition hover:border-pcc-200`}>
      <div className="flex items-center gap-3">
        <div className={`rounded-xl p-2.5 ${tone}`}>{icon}</div>
        <div>
          <p className="text-2xl font-bold text-slate-800">{value ?? '—'}</p>
          <p className="text-xs text-slate-500">{label}</p>
        </div>
      </div>
    </Link>
  )

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {stat(<ClipboardCheck size={20} />, 'My pending approvals', summary?.pendingMyApprovals, 'bg-amber-50 text-amber-700', '/workflows/pending')}
        {stat(<Send size={20} />, 'My submitted requests', summary?.mySubmitted, 'bg-pcc-50 text-pcc-700', '/workflows/mine')}
        {stat(<AlertTriangle size={20} />, 'Overdue', summary?.overdue, 'bg-red-50 text-red-600', '/workflows/overdue')}
        {stat(<Bell size={20} />, 'Unread notifications', unread, 'bg-indigo-50 text-indigo-700', '/workflows/notifications')}
      </div>
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        {stat(<Inbox size={20} />, 'Pending approval', summary?.pendingApproval, 'bg-amber-50 text-amber-700', '/workflows/all?status=PENDING_APPROVAL')}
        {stat(<CheckCircle2 size={20} />, 'Completed', summary?.completed, 'bg-emerald-50 text-emerald-700', '/workflows/history')}
        {stat(<XCircle size={20} />, 'Rejected', summary?.rejected, 'bg-red-50 text-red-600', '/workflows/all?status=REJECTED')}
        {stat(<RotateCcw size={20} />, 'Returned', summary?.returned, 'bg-orange-50 text-orange-700', '/workflows/returned')}
        {stat(<AlertTriangle size={20} />, 'Integration failed', summary?.integrationFailed, 'bg-red-50 text-red-600', '/workflows/all?status=INTEGRATION_FAILED')}
      </div>

      <div className={card}>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-800">My Pending Approvals</h3>
          <Link to="/workflows/pending" className="text-xs font-medium text-pcc-700 hover:underline">View all</Link>
        </div>
        {pending.length === 0 && <p className="py-6 text-center text-sm text-slate-400">No pending approvals.</p>}
        <div className="divide-y divide-slate-100">
          {pending.map((t) => (
            <Link key={t.id} to={`/workflows/detail/${t.instanceId}`}
                  className="flex items-center justify-between py-2.5 hover:bg-slate-50">
              <div>
                <p className="text-sm font-medium text-slate-700">{t.workflowRef} · {t.workflowType}</p>
                <p className="text-xs text-slate-400">{t.module} · {t.entityType}:{t.entityRef} · level {t.levelOrder}</p>
              </div>
              <div className="text-right">
                <StatusBadge value={t.status} />
                <p className={`mt-0.5 text-[11px] ${t.overdue ? 'font-semibold text-red-600' : 'text-slate-400'}`}>
                  Due {fmtDate(t.dueAt)}
                </p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}
