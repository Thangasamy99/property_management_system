import { useMemo, useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import ChangePasswordModal from '../../components/ChangePasswordModal'

const MODULE_LABELS = {
  AUTH: 'Authentication', USER: 'Users', ROLE: 'Roles', PROPERTY: 'Property',
  LAND: 'Land', FINANCE: 'Finance', PAYMENT: 'Finance', EXPENSE: 'Finance',
  FINANCIAL: 'Finance', REPORT: 'Reports', DOCUMENT: 'Documents', WORKFLOW: 'Workflow',
  SYSTEM: 'Administration', SETTINGS: 'Administration', AUDIT: 'Administration',
}

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [pwdOpen, setPwdOpen] = useState(false)

  const groups = useMemo(() => {
    const g = (user?.permissions ?? []).reduce((acc, code) => {
      const key = MODULE_LABELS[code.split('_')[0]] ?? code.split('_')[0]
      ;(acc[key] ??= []).push(code)
      return acc
    }, {})
    return Object.entries(g)
  }, [user])

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-pcc-950 via-pcc-800 to-pcc-500 p-7 text-white shadow-xl shadow-pcc-500/25 sm:p-9 animate-slide-up">
        <div className="pointer-events-none absolute -right-20 -top-24 h-72 w-72 rounded-full bg-pcc-400/20 blur-3xl animate-blob" />
        <div className="flex flex-col items-start gap-5 sm:flex-row sm:items-center">
          <span className="flex h-20 w-20 items-center justify-center rounded-2xl bg-white/15 text-3xl font-bold ring-2 ring-white/30 backdrop-blur">
            {user?.firstName?.[0]}{user?.lastName?.[0]}
          </span>
          <div className="min-w-0">
            <h2 className="text-2xl font-bold sm:text-3xl">{user?.firstName} {user?.lastName}</h2>
            <p className="mt-1 text-pcc-100/80">@{user?.username} · {user?.employeeCode}</p>
            <div className="mt-3 flex flex-wrap gap-2">
              {user?.roles?.map((r) => (
                <span key={r.code} className="rounded-full bg-white/15 px-3 py-1 text-xs font-semibold ring-1 ring-white/30">
                  {r.name}
                </span>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 animate-slide-up" style={{ animationDelay: '150ms' }}>
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h3 className="mb-4 font-semibold text-slate-800">Account Details</h3>
          <dl className="space-y-3 text-sm">
            {[
              ['Email', user?.email],
              ['Phone', user?.phone ?? '—'],
              ['Employee code', user?.employeeCode],
              ['Status', user?.accountStatus],
              ['Last login', user?.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : '—'],
              ['Member since', user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'],
            ].map(([k, v]) => (
              <div key={k} className="flex justify-between gap-4 border-b border-slate-50 pb-2.5 last:border-0">
                <dt className="text-slate-400">{k}</dt>
                <dd className="truncate font-medium text-slate-700">{v}</dd>
              </div>
            ))}
          </dl>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h3 className="mb-4 font-semibold text-slate-800">Security</h3>
          <p className="text-sm text-slate-500">
            {user?.mustChangePassword
              ? 'Your password must be changed before continuing.'
              : 'Your password is up to date.'}
          </p>
          {user?.mustChangePassword && (
            <p className="mt-2 rounded-xl bg-amber-50 px-3 py-2 text-xs font-medium text-amber-700 ring-1 ring-amber-200">
              Password change required
            </p>
          )}
          <button
            onClick={() => setPwdOpen(true)}
            className="mt-4 w-full rounded-xl bg-gradient-to-r from-pcc-700 to-pcc-500 py-2.5 text-sm font-semibold text-white shadow-lg shadow-pcc-500/25 transition-all hover:-translate-y-0.5 hover:shadow-xl"
          >
            Change Password
          </button>
          <button
            onClick={handleLogout}
            className="mt-2.5 w-full rounded-xl border border-slate-200 py-2.5 text-sm font-semibold text-red-500 transition-all hover:border-red-200 hover:bg-red-50"
          >
            Logout
          </button>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm animate-slide-up" style={{ animationDelay: '250ms' }}>
        <h3 className="font-semibold text-slate-800">Your Permissions</h3>
        <p className="mt-1 text-sm text-slate-500">{user?.permissions?.length ?? 0} permissions granted by your roles.</p>
        <div className="mt-5 space-y-4">
          {groups.map(([module, codes]) => (
            <div key={module}>
              <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-400">{module}</p>
              <div className="flex flex-wrap gap-2">
                {codes.sort().map((c) => (
                  <span key={c} className="rounded-lg bg-pcc-50 px-2.5 py-1 text-xs font-medium text-pcc-700 ring-1 ring-pcc-200">
                    {c}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>

      <ChangePasswordModal open={pwdOpen} onClose={() => setPwdOpen(false)} />
    </div>
  )
}
