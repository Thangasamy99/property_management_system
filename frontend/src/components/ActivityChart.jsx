import { useMemo, useState } from 'react'

/**
 * Stacked-bar activity chart (pure SVG, no deps).
 * data: [{ date, successfulLogins, failedLogins, registrations }]
 */
export default function ActivityChart({ data, height = 200 }) {
  const [hover, setHover] = useState(null)

  const { max, ticks } = useMemo(() => {
    const m = Math.max(1, ...data.map((d) => d.successfulLogins + d.failedLogins))
    return { max: m, ticks: [0, Math.ceil(m / 2), m] }
  }, [data])

  const W = 100 / data.length

  return (
    <div className="relative">
      <div className="flex items-end gap-[3px]" style={{ height }}>
        {data.map((d, i) => {
          const total = d.successfulLogins + d.failedLogins
          const hTotal = (total / max) * 100
          const hFail = total > 0 ? (d.failedLogins / total) * hTotal : 0
          return (
            <div
              key={d.date}
              className="group relative flex flex-1 cursor-pointer flex-col justify-end"
              style={{ height: '100%' }}
              onMouseEnter={() => setHover(i)}
              onMouseLeave={() => setHover(null)}
            >
              <div className="w-full overflow-hidden rounded-t-md transition-all duration-300 group-hover:opacity-80" style={{ height: `${hTotal}%`, minHeight: total > 0 ? 4 : 0 }}>
                <div className="w-full bg-pcc-500" style={{ height: `${hTotal - hFail > 0 ? ((hTotal - hFail) / hTotal) * 100 : 0}%` }} />
                <div className="w-full bg-red-400" style={{ height: `${hFail > 0 ? (hFail / hTotal) * 100 : 0}%` }} />
              </div>
              {hover === i && (
                <div className="pointer-events-none absolute -top-16 left-1/2 z-10 w-32 -translate-x-1/2 rounded-lg border border-slate-200 bg-white p-2.5 text-center shadow-lg animate-scale-in">
                  <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">{d.date}</p>
                  <p className="mt-1 text-xs text-emerald-600">● {d.successfulLogins} success</p>
                  <p className="text-xs text-red-500">● {d.failedLogins} failed</p>
                  <p className="text-xs text-pcc-600">● {d.registrations} new users</p>
                </div>
              )}
            </div>
          )
        })}
      </div>
      {/* baseline + x labels */}
      <div className="mt-1 border-t border-slate-200" />
      <div className="mt-1.5 flex justify-between text-[10px] text-slate-400">
        <span>{data[0]?.date?.slice(5)}</span>
        <span>{data[Math.floor(data.length / 2)]?.date?.slice(5)}</span>
        <span>{data[data.length - 1]?.date?.slice(5)}</span>
      </div>
      <div className="mt-3 flex items-center gap-4 text-[11px] text-slate-500">
        <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm bg-pcc-500" /> Successful logins</span>
        <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm bg-red-400" /> Failed logins</span>
      </div>
    </div>
  )
}
