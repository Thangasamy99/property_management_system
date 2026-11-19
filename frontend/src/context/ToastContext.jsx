import { createContext, useCallback, useContext, useRef, useState } from 'react'
import { CheckCircle2, AlertTriangle, XCircle, X, Info } from 'lucide-react'

const ToastContext = createContext(null)

const STYLES = {
  success: { icon: CheckCircle2, cls: 'border-emerald-200 bg-emerald-50 text-emerald-800', iconCls: 'text-emerald-600' },
  error: { icon: XCircle, cls: 'border-red-200 bg-red-50 text-red-800', iconCls: 'text-red-600' },
  warning: { icon: AlertTriangle, cls: 'border-amber-200 bg-amber-50 text-amber-800', iconCls: 'text-amber-600' },
  info: { icon: Info, cls: 'border-pcc-200 bg-pcc-50 text-pcc-800', iconCls: 'text-pcc-600' },
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const idRef = useRef(0)

  const dismiss = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), [])

  const toast = useCallback((message, type = 'success', ms = 4500) => {
    const id = ++idRef.current
    setToasts((t) => [...t, { id, message, type }])
    setTimeout(() => dismiss(id), ms)
  }, [dismiss])

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="pointer-events-none fixed bottom-5 right-5 z-[100] flex w-80 flex-col gap-2">
        {toasts.map((t) => {
          const { icon: Icon, cls, iconCls } = STYLES[t.type] ?? STYLES.info
          return (
            <div key={t.id} className={`pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 shadow-lg animate-slide-right ${cls}`}>
              <Icon className={`mt-0.5 h-5 w-5 shrink-0 ${iconCls}`} />
              <p className="flex-1 text-sm font-medium leading-snug">{t.message}</p>
              <button onClick={() => dismiss(t.id)} className="shrink-0 opacity-50 transition-opacity hover:opacity-100">
                <X className="h-4 w-4" />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
