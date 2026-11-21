import { useCallback, useEffect, useState } from 'react'
import { wfFetch, fmtDate } from './wfShared'
import { useAuth } from '../../context/AuthContext'
import { CheckCheck, ChevronLeft, ChevronRight } from 'lucide-react'

export default function NotificationsPage() {
  const { user } = useAuth()
  const [rows, setRows] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const load = useCallback(async () => {
    const res = await wfFetch(`/notifications?page=${page}&size=20`)
    if (res.ok) {
      const d = await res.json()
      setRows(d.content); setTotalPages(d.totalPages)
    }
  }, [page])
  useEffect(() => { load() }, [load])

  const markRead = async (id) => { await wfFetch(`/notifications/${id}/read`, { method: 'PATCH' }); load() }
  const markAll = async () => { await wfFetch('/notifications/read-all', { method: 'PATCH' }); load() }

  const tones = {
    TASK_ASSIGNED: 'bg-amber-50 text-amber-700', REJECTED: 'bg-red-50 text-red-600',
    RETURNED: 'bg-orange-50 text-orange-700', COMPLETED: 'bg-emerald-50 text-emerald-700',
    DELEGATED: 'bg-indigo-50 text-indigo-700', CANCELLED: 'bg-slate-100 text-slate-500',
  }

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-800">Notifications</h3>
        <button onClick={markAll} className="flex items-center gap-1.5 text-xs font-medium text-pcc-700 hover:underline">
          <CheckCheck size={14} /> Mark all read</button>
      </div>
      <div className="divide-y divide-slate-100">
        {rows.length === 0 && <p className="py-8 text-center text-sm text-slate-400">No notifications.</p>}
        {rows.map((n) => (
          <button key={n.id} onClick={() => !n.readFlag && markRead(n.id)}
                  className={`flex w-full items-start gap-3 py-3 text-left ${n.readFlag ? 'opacity-60' : ''}`}>
            <span className={`mt-0.5 rounded-full px-2 py-0.5 text-[10px] font-bold ${tones[n.type] ?? 'bg-slate-100 text-slate-500'}`}>
              {n.type}</span>
            <div className="flex-1">
              <p className={`text-sm ${n.readFlag ? 'text-slate-500' : 'font-medium text-slate-800'}`}>{n.title}</p>
              <p className="text-xs text-slate-400">{n.message}</p>
            </div>
            <div className="text-right">
              <p className="text-[11px] text-slate-400">{fmtDate(n.createdAt)}</p>
              {!n.readFlag && <span className="mt-1 inline-block h-2 w-2 rounded-full bg-pcc-500" />}
            </div>
          </button>))}
      </div>
      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2">
          <button disabled={page === 0} onClick={() => setPage(page - 1)} className="rounded-lg border border-slate-200 p-1.5 disabled:opacity-40"><ChevronLeft size={15} /></button>
          <span className="text-xs text-slate-500">Page {page + 1} of {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)} className="rounded-lg border border-slate-200 p-1.5 disabled:opacity-40"><ChevronRight size={15} /></button>
        </div>)}
    </div>
  )
}
