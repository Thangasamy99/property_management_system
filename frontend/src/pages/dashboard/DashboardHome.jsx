import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { apiFetch } from '../../services/api'
import ActivityChart from '../../components/ActivityChart'
import { UserPlus, ShieldPlus, History, ScrollText } from 'lucide-react'

const ROLE_TITLES = {
  ADMIN: 'Admin Dashboard',
  MANAGER: 'Manager Dashboard',
  ACCOUNTANT: 'Accountant Dashboard',
  PROPERTY_OFFICER: 'Property Dashboard',
  LAND_OFFICER: 'Land Dashboard',
}

const EVENT_STYLES = {
  LOGIN_SUCCESS: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  LOGIN_FAILED: 'bg-red-50 text-red-600 ring-red-200',
  ACCOUNT_LOCKED: 'bg-amber-50 text-amber-700 ring-amber-200',
  LOGOUT: 'bg-slate-100 text-slate-600 ring-slate-200',
  TOKEN_REFRESH: 'bg-sky-50 text-sky-700 ring-sky-200',
  PASSWORD_CHANGED: 'bg-pcc-50 text-pcc-700 ring-pcc-200',
  PASSWORD_RESET: 'bg-pcc-50 text-pcc-700 ring-pcc-200',
}

function timeAgo(iso) {
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return 'just now'
  if (m < 60) return `${m}m ago`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}h ago`
  return `${Math.floor(h / 24)}d ago`
}

const ICONS = {
  users: 'M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m9-4.13a4 4 0 11-8 0 4 4 0 018 0z',
  check: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
  off: 'M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636',
  shield: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z',
  login: 'M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1',
  warn: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
}

function KpiCard({ label, value, icon, tone = 'text-pcc-700 bg-pcc-50' }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium uppercase tracking-wider text-slate-400">{label}</p>
        <span className={`flex h-9 w-9 items-center justify-center rounded-lg ${tone}`}>
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
            <path strokeLinecap="round" strokeLinejoin="round" d={icon} />
          </svg>
        </span>
      </div>
      <p className="mt-2 text-3xl font-bold tabular-nums text-ink">{value ?? '—'}</p>
    </div>
  )
}

function Card({ title, subtitle, linkTo, linkLabel, children, delay = 0, className = '' }) {
  return (
    <div className={`rounded-2xl border border-slate-200 bg-white p-6 animate-fade-in ${className}`} style={{ animationDelay: `${delay}ms` }}>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h3 className="font-semibold text-ink">{title}</h3>
          {subtitle && <p className="text-xs text-slate-400">{subtitle}</p>}
        </div>
        {linkTo && <Link to={linkTo} className="text-xs font-semibold text-pcc-600 transition-colors hover:text-pcc-800">{linkLabel}</Link>}
      </div>
      {children}
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
  const canSeeStats = hasPermission('USER_VIEW')
  const canSeeAudit = hasPermission('AUDIT_LOG_VIEW')

  useEffect(() => {
    const load = async () => {
      const [dash, series, history, audit] = await Promise.allSettled([
        canSeeStats ? apiFetch('/stats/dashboard').then((r) => (r.ok ? r.json() : Promise.reject())) : Promise.reject(),
        canSeeStats ? apiFetch('/stats/activity?days=14').then((r) => (r.ok ? r.json() : Promise.reject())) : Promise.reject(),
        apiFetch('/auth/login-history?page=0&size=5').then((r) => r.json()),
        canSeeAudit ? apiFetch('/audit-logs?size=5').then((r) => (r.ok ? r.json() : Promise.reject())) : Promise.reject(),
      ])
      setStats(dash.status === 'fulfilled' ? dash.value : null)
      if (series.status === 'fulfilled') setChart(series.value)
      if (history.status === 'fulfilled') setActivity(history.value.content)
      if (audit.status === 'fulfilled') setAuditFeed(audit.value.content)
    }
    load()
  }, [canSeeStats, canSeeAudit])

  const kpis = useMemo(() => {
    if (stats) {
      return [
        { label: 'Total Employees', value: stats.totalEmployees, icon: ICONS.users },
        { label: 'Active', value: stats.activeEmployees, icon: ICONS.check, tone: 'text-emerald-700 bg-emerald-50' },
        { label: 'Inactive', value: stats.inactiveEmployees, icon: ICONS.off, tone: 'text-slate-500 bg-slate-100' },
        { label: 'Roles', value: stats.totalRoles, icon: ICONS.shield, tone: 'text-indigo-600 bg-indigo-50' },
      ]
    }
    return [
      { label: 'My Roles', value: user?.roles?.length ?? 0, icon: ICONS.shield },
      { label: 'My Permissions', value: user?.permissions?.length ?? 0, icon: ICONS.check, tone: 'text-emerald-700 bg-emerald-50' },
    ]
  }, [stats, user])

  const quickActions = [
    hasPermission('USER_CREATE') && { to: '/users?new=1', label: 'New Employee', sub: 'Onboard a staff member', icon: UserPlus },
    hasPermission('ROLE_CREATE') && { to: '/roles?new=1', label: 'New Role', sub: 'Define access levels', icon: ShieldPlus },
    { to: '/login-history', label: 'Login History', sub: 'Review sign-in activity', icon: History },
    canSeeAudit && { to: '/audit-logs', label: 'Audit Logs', sub: 'Inspect admin changes', icon: ScrollText },
  ].filter(Boolean)

  const securityKpis = stats && [
    { label: 'Logins (all time)', value: stats.successfulLogins, icon: ICONS.login, tone: 'text-teal-600 bg-teal-50' },
    { label: 'Failed Logins', value: stats.failedLogins, icon: ICONS.warn, tone: 'text-red-600 bg-red-50' },
    { label: 'Account Lockouts', value: stats.lockedAccounts, icon: ICONS.warn, tone: 'text-amber-600 bg-amber-50' },
  ]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* greeting strip */}
      <section className="rounded-2xl border border-slate-200 bg-white px-6 py-5 animate-fade-in">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-ink">Welcome back, {user?.firstName}</h2>
            <p className="mt-0.5 text-sm text-slate-500">
              {title} · {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {user?.roles?.map((r) => (
              <span key={r.code} className="rounded-full bg-pcc-50 px-3 py-1 text-xs font-semibold text-pcc-700 ring-1 ring-pcc-200">
                {r.name}
              </span>
            ))}
            {user?.mustChangePassword && (
              <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700 ring-1 ring-amber-200">
                Password change required
              </span>
            )}
          </div>
        </div>
      </section>

      {/* quick actions */}
      {quickActions.length > 0 && (
        <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {quickActions.map((a) => (
            <Link
              key={a.label}
              to={a.to}
              className="group flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-pcc-300 hover:shadow-md hover:shadow-pcc-500/10"
            >
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-pcc-50 text-pcc-600 transition-colors duration-200 group-hover:bg-pcc-600 group-hover:text-white">
                <a.icon className="h-5 w-5" strokeWidth={1.8} />
              </span>
              <span className="min-w-0">
                <span className="block truncate text-sm font-semibold text-ink">{a.label}</span>
                <span className="block truncate text-[11px] text-slate-400">{a.sub}</span>
              </span>
            </Link>
          ))}
        </section>
      )}

      {/* KPI cards */}
      <section className={`grid gap-4 ${kpis.length > 2 ? 'grid-cols-2 lg:grid-cols-4' : 'grid-cols-2'}`}>
        {kpis.map((k) => <KpiCard key={k.label} {...k} />)}
      </section>

      {securityKpis && (
        <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {securityKpis.map((k) => <KpiCard key={k.label} {...k} />)}
        </section>
      )}

      {/* chart */}
      {chart.length > 0 && (
        <Card title="Login Activity" subtitle="Sign-ins over the last 14 days">
          <ActivityChart data={chart} />
        </Card>
      )}

      {/* feeds */}
      <section className={`grid gap-5 ${auditFeed.length ? 'lg:grid-cols-2' : ''}`}>
        <Card title="Recent Activity" subtitle="Your latest security events" linkTo="/login-history" linkLabel="View all →">
          <ul className="space-y-2">
            {activity.length === 0 && <li className="py-6 text-center text-sm text-slate-400">No activity yet</li>}
            {activity.map((e) => (
              <li key={e.id} className="flex items-center gap-3 rounded-xl border border-slate-100 px-4 py-3 transition-colors hover:border-pcc-200 hover:bg-pcc-50/40">
                <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ring-1 ${EVENT_STYLES[e.eventType] ?? 'bg-slate-100 text-slate-600 ring-slate-200'}`}>
                  {e.eventType.replace(/_/g, ' ')}
                </span>
                <span className="min-w-0 flex-1 truncate text-xs text-slate-500">{e.failureReason ?? e.ipAddress ?? ''}</span>
                <span className="shrink-0 text-[11px] text-slate-400">{timeAgo(e.createdAt)}</span>
              </li>
            ))}
          </ul>
        </Card>

        {auditFeed.length > 0 && (
          <Card title="Administrative Activity" subtitle="Latest changes across the system" linkTo="/audit-logs" linkLabel="Audit logs →">
            <ul className="space-y-2">
              {auditFeed.map((e) => (
                <li key={e.id} className="flex items-start gap-3 rounded-xl border border-slate-100 px-4 py-3">
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
          </Card>
        )}
      </section>

      {/* recent registrations */}
      {stats?.recentRegistrations?.length > 0 && (
        <Card title="Recent Registrations" subtitle="Newest employee accounts" linkTo="/users" linkLabel="Manage →">
          <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-5">
            {stats.recentRegistrations.map((u) => (
              <li key={u.id} className="flex items-center gap-3 rounded-xl border border-slate-100 px-3.5 py-3 transition-colors hover:border-pcc-200 hover:bg-pcc-50/40">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-pcc-100 text-xs font-bold text-pcc-700">
                  {u.firstName?.[0]}{u.lastName?.[0]}
                </span>
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-slate-700">{u.firstName} {u.lastName}</p>
                  <p className="truncate text-[11px] text-slate-400">{u.employeeCode} · {timeAgo(u.createdAt)}</p>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
