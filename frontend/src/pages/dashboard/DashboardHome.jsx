import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { apiFetch } from '../../services/api'
import useCountUp from '../../hooks/useCountUp'
import ActivityChart from '../../components/ActivityChart'
import propertyImg from '../../assets/property.jpg'

const ROLE_TITLES = {
  ADMIN: 'Admin Dashboard',
  MANAGER: 'Manager Dashboard',
  ACCOUNTANT: 'Accountant Dashboard',
  PROPERTY_OFFICER: 'Property Dashboard',
  LAND_OFFICER: 'Land Dashboard',
}

const MODULE_LABELS = {
  AUTH: 'Authentication', USER: 'Users', ROLE: 'Roles', PROPERTY: 'Property',
  LAND: 'Land', FINANCE: 'Finance', REPORT: 'Reports', SYSTEM: 'System',
}

const MODULE_COLORS = ['#9333ea', '#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ec4899', '#14b8a6', '#64748b']

const EVENT_STYLES = {
  LOGIN_SUCCESS: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  LOGIN_FAILED: 'bg-red-50 text-red-600 ring-red-200',
  ACCOUNT_LOCKED: 'bg-amber-50 text-amber-700 ring-amber-200',
  LOGOUT: 'bg-slate-100 text-slate-600 ring-slate-200',
  TOKEN_REFRESH: 'bg-sky-50 text-sky-700 ring-sky-200',
  PASSWORD_CHANGED: 'bg-pcc-50 text-pcc-700 ring-pcc-200',
  PASSWORD_RESET: 'bg-pcc-50 text-pcc-700 ring-pcc-200',
}

const QUICK_LINKS = [
  { to: '/users', permission: 'USER_VIEW', title: 'Employees', text: 'Manage employee accounts, roles and access.', icon: 'M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m9-4.13a4 4 0 11-8 0 4 4 0 018 0z' },
  { to: '/roles', permission: 'ROLE_VIEW', title: 'Roles & Permissions', text: 'Configure roles and their permissions.', icon: 'M9 12l2 2 4-4m5.6 2A9 9 0 1112 3a9 9 0 018.6 9z' },
  { to: '/login-history', title: 'Login History', text: 'Review your recent sign-in activity.', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
]

function timeAgo(iso) {
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return 'just now'
  if (m < 60) return `${m}m ago`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}h ago`
  return `${Math.floor(h / 24)}d ago`
}

function StatCard({ label, value, icon, tint, delay }) {
  const count = useCountUp(value)
  return (
    <div
      className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-lg animate-slide-up"
      style={{ animationDelay: `${delay}ms` }}
    >
      <div className={`absolute -right-6 -top-6 h-20 w-20 rounded-full opacity-10 transition-transform duration-500 group-hover:scale-150 ${tint}`} />
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-slate-400">{label}</p>
          <p className="mt-2 text-3xl font-bold tabular-nums text-slate-800">{value == null ? '—' : count}</p>
        </div>
        <span className={`flex h-11 w-11 items-center justify-center rounded-xl text-white shadow-md ${tint}`}>
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
            <path strokeLinecap="round" strokeLinejoin="round" d={icon} />
          </svg>
        </span>
      </div>
    </div>
  )
}

export default function DashboardHome() {
  const { user, hasPermission } = useAuth()
  const [stats, setStats] = useState(null)
  const [chart, setChart] = useState([])
  const [activity, setActivity] = useState([])
  const [auditFeed, setAuditFeed] = useState([])

  const primaryRole = user?.roles?.[0]?.code
  const title = ROLE_TITLES[primaryRole] ?? 'Dashboard'

  const permissionGroups = useMemo(() => {
    const groups = (user?.permissions ?? []).reduce((acc, code) => {
      const key = MODULE_LABELS[code.split('_')[0]] ?? code.split('_')[0]
      ;(acc[key] ??= []).push(code)
      return acc
    }, {})
    return Object.entries(groups)
  }, [user])

  const donut = useMemo(() => {
    const total = user?.permissions?.length ?? 0
    if (!total) return { gradient: 'conic-gradient(#e2e8f0 0 100%)', slices: [] }
    let acc = 0
    const slices = permissionGroups.map(([label, codes], i) => {
      const pct = (codes.length / total) * 100
      const slice = { label, count: codes.length, pct, color: MODULE_COLORS[i % MODULE_COLORS.length], from: acc, to: acc + pct }
      acc += pct
      return slice
    })
    const gradient = `conic-gradient(${slices.map((s) => `${s.color} ${s.from}% ${s.to}%`).join(', ')})`
    return { gradient, slices, total }
  }, [permissionGroups, user])

  useEffect(() => {
    const load = async () => {
      const [dash, series, history, audit] = await Promise.allSettled([
        hasPermission('USER_VIEW') ? apiFetch('/stats/dashboard').then((r) => (r.ok ? r.json() : Promise.reject())) : Promise.reject(),
        hasPermission('USER_VIEW') ? apiFetch('/stats/activity?days=14').then((r) => (r.ok ? r.json() : Promise.reject())) : Promise.reject(),
        apiFetch('/auth/login-history?page=0&size=6').then((r) => r.json()),
        hasPermission('AUDIT_LOG_VIEW') ? apiFetch('/audit-logs?size=5').then((r) => (r.ok ? r.json() : Promise.reject())) : Promise.reject(),
      ])
      setStats(dash.status === 'fulfilled' ? dash.value : null)
      if (series.status === 'fulfilled') setChart(series.value)
      if (history.status === 'fulfilled') setActivity(history.value.content)
      if (audit.status === 'fulfilled') setAuditFeed(audit.value.content)
    }
    load()
  }, [hasPermission])

  const statCards = [
    stats && { label: 'Employees', value: stats.totalEmployees, tint: 'bg-pcc-600', icon: 'M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m9-4.13a4 4 0 11-8 0 4 4 0 018 0z' },
    stats && { label: 'Active', value: stats.activeEmployees, tint: 'bg-emerald-500', icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' },
    stats && { label: 'Inactive', value: stats.inactiveEmployees, tint: 'bg-slate-500', icon: 'M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636' },
    stats && { label: 'Roles', value: stats.totalRoles, tint: 'bg-indigo-500', icon: 'M9 12l2 2 4-4m5.6 2A9 9 0 1112 3a9 9 0 018.6 9z' },
    stats && { label: 'Logins', value: stats.successfulLogins, tint: 'bg-teal-500', icon: 'M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1' },
    stats && { label: 'Failed Logins', value: stats.failedLogins, tint: 'bg-red-500', icon: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z' },
    stats && stats.lockedAccounts > 0 && { label: 'Lockouts', value: stats.lockedAccounts, tint: 'bg-amber-500', icon: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z' },
    { label: 'My Permissions', value: user?.permissions?.length ?? 0, tint: 'bg-pcc-500', icon: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z' },
  ].filter(Boolean)

  const links = QUICK_LINKS.filter((l) => !l.permission || hasPermission(l.permission))

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* welcome banner */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-pcc-950 via-pcc-800 to-pcc-500 p-7 text-white shadow-xl shadow-pcc-500/25 sm:p-9 animate-slide-up">
        <div className="pointer-events-none absolute -right-20 -top-24 h-72 w-72 rounded-full bg-pcc-400/20 blur-3xl animate-blob" />
        <img
          src={propertyImg}
          alt=""
          className="pointer-events-none absolute -right-4 bottom-0 hidden h-48 w-auto rounded-tl-3xl object-cover opacity-25 mix-blend-luminosity md:block"
        />
        <p className="text-sm font-medium uppercase tracking-widest text-pcc-300">
          {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
        </p>
        <h2 className="mt-1.5 text-3xl font-bold sm:text-4xl">
          Welcome back, {user?.firstName}
        </h2>
        <p className="mt-2 max-w-xl text-pcc-100/90">
          {title} — manage PCC Cameroon properties, buildings, apartments and land assets from one place.
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          {user?.roles?.map((r) => (
            <span key={r.code} className="rounded-full bg-white/15 px-3 py-1 text-xs font-semibold ring-1 ring-white/30 backdrop-blur transition-transform hover:scale-105">
              {r.name}
            </span>
          ))}
          {user?.mustChangePassword && (
            <span className="rounded-full bg-amber-400/20 px-3 py-1 text-xs font-semibold text-amber-200 ring-1 ring-amber-300/40">
              Password change required
            </span>
          )}
        </div>
      </section>

      {/* stat cards */}
      <section className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {statCards.map((c, i) => (
          <StatCard key={c.label} {...c} delay={120 + i * 90} />
        ))}
      </section>

      {/* chart + donut */}
      <section className="grid gap-5 lg:grid-cols-5">
        {chart.length > 0 && (
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:col-span-3 animate-slide-up" style={{ animationDelay: '300ms' }}>
            <div className="mb-5">
              <h3 className="font-semibold text-ink">Login Activity</h3>
              <p className="text-xs text-slate-400">Sign-ins over the last 14 days</p>
            </div>
            <ActivityChart data={chart} />
          </div>
        )}

        <div className={`rounded-2xl border border-slate-200 bg-white p-6 shadow-sm animate-slide-up ${chart.length ? 'lg:col-span-2' : 'lg:col-span-5'}`} style={{ animationDelay: '380ms' }}>
          <h3 className="font-semibold text-slate-800">Permissions by Module</h3>
          <p className="text-xs text-slate-400">What your roles grant you</p>
          <div className="mt-5 flex items-center justify-center">
            <div className="relative h-40 w-40">
              <div className="h-full w-full rounded-full transition-transform duration-700 hover:scale-105" style={{ background: donut.gradient }} />
              <div className="absolute inset-5 flex flex-col items-center justify-center rounded-full bg-white shadow-inner">
                <span className="text-2xl font-bold text-slate-800">{donut.total}</span>
                <span className="text-[10px] font-medium uppercase tracking-wider text-slate-400">permissions</span>
              </div>
            </div>
          </div>
          <ul className="mt-5 space-y-1.5">
            {donut.slices.map((s) => (
              <li key={s.label} className="flex items-center gap-2 text-xs">
                <span className="h-2.5 w-2.5 rounded-sm" style={{ background: s.color }} />
                <span className="flex-1 text-slate-600">{s.label}</span>
                <span className="font-semibold text-slate-700">{s.count}</span>
              </li>
            ))}
          </ul>
        </div>
      </section>

      {/* recent activity + admin activity */}
      <section className={`grid gap-5 ${auditFeed.length ? 'lg:grid-cols-2' : ''}`}>
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm animate-slide-up" style={{ animationDelay: '420ms' }}>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-ink">Recent Activity</h3>
              <p className="text-xs text-slate-400">Your latest security events</p>
            </div>
            <Link to="/login-history" className="text-xs font-semibold text-pcc-600 transition-colors hover:text-pcc-800">
              View all →
            </Link>
          </div>
          <ul className="space-y-2.5">
            {activity.length === 0 && <li className="py-6 text-center text-sm text-slate-400">No activity yet</li>}
            {activity.map((e, i) => (
              <li
                key={e.id}
                className="flex items-center gap-3 rounded-xl border border-slate-100 px-4 py-3 transition-all duration-300 hover:border-pcc-200 hover:bg-pcc-50/50 animate-fade-in"
                style={{ animationDelay: `${i * 40}ms` }}
              >
                <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ring-1 ${EVENT_STYLES[e.eventType] ?? 'bg-slate-100 text-slate-600 ring-slate-200'}`}>
                  {e.eventType.replace(/_/g, ' ')}
                </span>
                <span className="min-w-0 flex-1 truncate text-xs text-slate-500">
                  {e.failureReason ?? e.ipAddress ?? ''}
                </span>
                <span className="shrink-0 text-[11px] text-slate-400">{timeAgo(e.createdAt)}</span>
              </li>
            ))}
          </ul>
        </div>

        {auditFeed.length > 0 && (
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm animate-slide-up" style={{ animationDelay: '460ms' }}>
            <div className="mb-4 flex items-center justify-between">
              <div>
                <h3 className="font-semibold text-ink">Administrative Activity</h3>
                <p className="text-xs text-slate-400">Latest changes across the system</p>
              </div>
              <Link to="/audit-logs" className="text-xs font-semibold text-pcc-600 transition-colors hover:text-pcc-800">
                Audit logs →
              </Link>
            </div>
            <ul className="space-y-2.5">
              {auditFeed.map((e, i) => (
                <li key={e.id} className="flex items-start gap-3 rounded-xl border border-slate-100 px-4 py-3 animate-fade-in" style={{ animationDelay: `${i * 40}ms` }}>
                  <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-pcc-100 text-[10px] font-bold text-pcc-700">
                    {e.actorUsername?.[0]?.toUpperCase() ?? 'S'}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-medium text-slate-700">{e.action.replace(/_/g, ' ')}</p>
                    <p className="truncate text-[11px] text-slate-400">{e.actorUsername ?? 'system'} · {e.details}</p>
                  </div>
                  <span className="shrink-0 text-[11px] text-slate-400">{timeAgo(e.createdAt)}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>

      {/* recent registrations */}
      {stats?.recentRegistrations?.length > 0 && (
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm animate-slide-up" style={{ animationDelay: '430ms' }}>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-slate-800">Recent Registrations</h3>
              <p className="text-xs text-slate-400">Newest employee accounts</p>
            </div>
            <Link to="/users" className="text-xs font-semibold text-pcc-600 transition-colors hover:text-pcc-800">
              Manage →
            </Link>
          </div>
          <ul className="grid gap-2.5 sm:grid-cols-2 lg:grid-cols-5">
            {stats.recentRegistrations.map((u, i) => (
              <li
                key={u.id}
                className="flex items-center gap-3 rounded-xl border border-slate-100 px-3.5 py-3 transition-all duration-300 hover:border-pcc-200 hover:bg-pcc-50/50 animate-fade-in"
                style={{ animationDelay: `${450 + i * 60}ms` }}
              >
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-pcc-500 to-pcc-300 text-xs font-bold text-white">
                  {u.firstName?.[0]}{u.lastName?.[0]}
                </span>
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-slate-700">{u.firstName} {u.lastName}</p>
                  <p className="truncate text-[11px] text-slate-400">{u.employeeCode} · {timeAgo(u.createdAt)}</p>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* quick links */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {links.map((l, i) => (
          <Link
            key={l.to}
            to={l.to}
            className="group rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-pcc-300 hover:shadow-lg hover:shadow-pcc-500/10 animate-slide-up"
            style={{ animationDelay: `${450 + i * 80}ms` }}
          >
            <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-pcc-100 text-pcc-600 transition-all duration-300 group-hover:scale-110 group-hover:bg-pcc-600 group-hover:text-white">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
                <path strokeLinecap="round" strokeLinejoin="round" d={l.icon} />
              </svg>
            </span>
            <h3 className="mt-4 font-semibold text-slate-800">{l.title}</h3>
            <p className="mt-1 text-sm text-slate-500">{l.text}</p>
          </Link>
        ))}
      </section>

      {/* permissions detail */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm animate-slide-up" style={{ animationDelay: '520ms' }}>
        <h3 className="font-semibold text-slate-800">Your Permissions</h3>
        <p className="mt-1 text-sm text-slate-500">Granted by your assigned roles — enforced by the Identity Service.</p>
        <div className="mt-5 space-y-4">
          {permissionGroups.map(([module, codes]) => (
            <div key={module}>
              <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-400">{module}</p>
              <div className="flex flex-wrap gap-2">
                {codes.sort().map((c) => (
                  <span key={c} className="rounded-lg bg-pcc-50 px-2.5 py-1 text-xs font-medium text-pcc-700 ring-1 ring-pcc-200 transition-colors hover:bg-pcc-100">
                    {c}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
