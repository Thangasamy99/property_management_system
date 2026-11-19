import { useEffect, useRef, useState } from 'react'
import { MoreHorizontal } from 'lucide-react'

/**
 * Overflow menu for table row actions.
 * items: [{ label, icon: LucideIcon, onClick, danger?, disabled? }]
 */
export default function RowActionsMenu({ items }) {
  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    const onClick = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false)
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  return (
    <div className="relative inline-block" ref={ref}>
      <button
        onClick={() => setOpen((v) => !v)}
        aria-label="Row actions"
        className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
      >
        <MoreHorizontal className="h-5 w-5" />
      </button>
      {open && (
        <div className="absolute right-0 z-20 mt-1 w-48 overflow-hidden rounded-xl border border-slate-100 bg-white py-1 shadow-xl animate-scale-in">
          {items.filter(Boolean).map((item) => (
            <button
              key={item.label}
              disabled={item.disabled}
              onClick={() => { setOpen(false); item.onClick() }}
              className={`flex w-full items-center gap-2.5 px-4 py-2.5 text-left text-sm transition-colors disabled:opacity-40 ${
                item.danger
                  ? 'text-red-600 hover:bg-red-50'
                  : 'text-slate-600 hover:bg-pcc-50 hover:text-pcc-700'
              }`}
            >
              {item.icon && <item.icon className="h-4 w-4 shrink-0" strokeWidth={1.8} />}
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
