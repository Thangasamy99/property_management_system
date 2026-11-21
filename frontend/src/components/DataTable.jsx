/** Shared data table: header, skeleton rows, empty state, pagination. */
export default function DataTable({ columns, rows, loading, empty, page, totalPages, onPage, children }) {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50/60 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
              {columns.map((c) => (
                <th key={c.label} className={`px-5 py-3.5 ${c.className ?? ''}`}>{c.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading
              ? Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-slate-50">
                    {columns.map((c, j) => (
                      <td key={j} className="px-5 py-4">
                        <div className="h-4 rounded bg-slate-100 animate-pulse" style={{ width: `${55 + ((i + j) % 3) * 15}%` }} />
                      </td>
                    ))}
                  </tr>
                ))
              : rows?.length
                ? children
                : (
                  <tr>
                    <td colSpan={columns.length} className="px-5 py-14 text-center text-sm text-slate-400">
                      {empty ?? 'No records found'}
                    </td>
                  </tr>
                )}
          </tbody>
        </table>
      </div>
      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3 text-xs text-slate-500">
          <span>Page {page + 1} of {totalPages}</span>
          <div className="flex gap-2">
            <button
              disabled={page === 0}
              onClick={() => onPage(page - 1)}
              className="rounded-lg border border-slate-200 px-3 py-1.5 font-medium transition-colors hover:bg-slate-50 disabled:opacity-40"
            >
              Previous
            </button>
            <button
              disabled={page + 1 >= totalPages}
              onClick={() => onPage(page + 1)}
              className="rounded-lg border border-slate-200 px-3 py-1.5 font-medium transition-colors hover:bg-slate-50 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
