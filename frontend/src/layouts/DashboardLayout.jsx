import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, Users, ShieldCheck, History, ScrollText, UserCircle,
  LogOut, Menu, ChevronDown, ChevronRight, Bell, KeyRound, ShieldAlert,
} from 'lucide-react'
import { landingPathFor, useAuth } from '../context/AuthContext'
import { apiFetch } from '../services/api'
import ChangePasswordModal from '../components/ChangePasswordModal'
import logo from '../assets/pcc-logo.png'

const NAV = [
  { key: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/users', label: 'Employees', permission: 'USER_VIEW', icon: Users },
  { to: '/roles', label: 'Roles & Permissions', permission: 'ROLE_VIEW', icon: ShieldCheck },
  { to: '/login-history', label: 'Login History', icon: History },
  { to: '/audit-logs', label: 'Audit Logs', permission: 'AUDIT_LOG_VIEW', icon: ScrollText },
  { to: '/profile', label: 'My Profile', icon: UserCircle },
]

const PAGE_TITLES = {
  '/users': 'Employees',
  '/roles': 'Roles & Permissions',
  '/login-history': 'Login History',
  '/audit-logs': 'Audit Logs',
  '/profile': 'My Profile',
}

function NotificationsBell() {
  const { hasPermission } = useAuth()
  const [open, setOpen] = useState(false)
  const [events, setEvents] = useState([])
  const ref = useRef(null)
  const canSeeAll = hasPermission('AUDIT_LOG_VIEW')

  useEffect(() => {
    const onClick = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false)
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  useEffect(() => {
    if (!open) return
    const url = canSeeAll
      ? '/login-history?eventType=LOGIN_FAILED&size=6'
      : '/auth/login-history?size=6'
    apiFetch(url).then(async (r) => r.ok && setEvents((await r.json()).content)).catch(() => {})
  }, [open, canSeeAll])

  const failed = canSeeAll ? events : events.filter((e) => e.eventType === 'LOGIN_FAILED' || e.eventType === 'ACCOUNT_LOCKED')

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((v) => !v)}
        className="relative rounded-xl border border-slate-200 p-2.5 text-slate-500 transition-all hover:border-pcc-300 hover:text-pcc-700"
        aria-label="Notifications"
      >
        <Bell className="h-4.5 w-4.5 h-5 w-5" />
        {failed.length > 0 && <span className="absolute -right-0.5 -top-0.5 h-2.5 w-2.5 rounded-full bg-red-500 ring-2 ring-white" />}
      </button>
      {open && (
        <div className="absolute right-0 mt-2 w-80 overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-xl animate-scale-in">
          <div className="border-b border-slate-100 px-4 py-3">
            <p className="text-sm font-semibold text-slate-800">Security alerts</p>
            <p className="text-xs text-slate-400">{canSeeAll ? 'Recent failed logins' : 'Your recent sign-in issues'}</p>
          </div>
          <ul className="max-h-72 overflow-y-auto">
            {failed.length === 0 && (
              <li className="px-4 py-6 text-center text-sm text-slate-400">No alerts — everything looks good</li>
            )}
            {failed.map((e) => (
              <li key={e.id} className="flex items-start gap-3 border-b border-slate-50 px-4 py-3 last:border-0">
                <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-red-500" />
                <div className="min-w-0">
                  <p className="text-xs font-medium text-slate-700">
                    {e.eventType.replace(/_/g, ' ')}{canSeeAll ? ` — ${e.username}` : ''}
                  </p>
                  <p className="truncate text-[11px] text-slate-400">{e.failureReason ?? e.ipAddress ?? ''} · {new Date(e.createdAt).toLocaleString()}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

export default function DashboardLayout() {
  const { user, logout, hasPermission } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [pwdOpen, setPwdOpen] = useState(false)
  const menuRef = useRef(null)
  const dashboardPath = landingPathFor(user)

  const pageTitle = PAGE_TITLES[location.pathname] ?? 'Dashboard'

  const items = NAV
    .map((n) => ({ ...n, to: n.key === 'dashboard' ? dashboardPath : n.to }))
    .filter((n) => !n.permission || hasPermission(n.permission))

  useEffect(() => {
    const onClick = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  useEffect(() => setMobileOpen(false), [location.pathname])

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  const sidebar = (
    <aside className="flex h-full w-64 flex-col bg-pcc-900 text-white">
      <div className="flex items-center gap-3 border-b border-white/10 px-5 py-5">
        <img src={logo} alt="PCC" className="h-10 w-10 rounded-lg bg-white object-contain p-1" />
        <div>
          <p className="text-sm font-semibold leading-tight">PCC Cameroon</p>
          <p className="text-[11px] text-white/50">Property &amp; Land Management</p>
        </div>
      </div>

      <p className="px-6 pb-2 pt-5 text-[10px] font-semibold uppercase tracking-[0.18em] text-white/40">Main menu</p>
      <nav className="flex-1 space-y-0.5 px-3">
        {items.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.label}
              to={item.to}
              className={({ isActive }) =>
                `group relative flex items-center gap-3 rounded-lg px-4 py-2.5 text-sm font-medium transition-all duration-200 ${
                  isActive
                    ? 'bg-white/12 text-white ring-1 ring-white/15'
                    : 'text-white/60 hover:bg-white/8 hover:text-white'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <span className={`absolute left-0 top-1/2 h-5 w-0.5 -translate-y-1/2 rounded-r-full bg-pcc-400 transition-opacity ${isActive ? 'opacity-100' : 'opacity-0'}`} />
                  <Icon className="h-[18px] w-[18px] shrink-0" strokeWidth={1.8} />
                  {item.label}
                </>
              )}
            </NavLink>
          )
        })}
      </nav>

      <div className="border-t border-white/10 p-4">
        <div className="mb-3 flex items-center gap-3 px-1">
          <span className="flex h-9 w-9 items-center justify-center rounded-full bg-pcc-500/30 text-sm font-bold ring-1 ring-white/20">
            {user?.firstName?.[0]}{user?.lastName?.[0]}
          </span>
          <div className="min-w-0">
            <p className="truncate text-sm font-medium">{user?.firstName} {user?.lastName}</p>
            <p className="truncate text-xs text-white/50">{user?.roles?.map((r) => r.name).join(', ')}</p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="flex w-full items-center justify-center gap-2 rounded-lg bg-white/10 px-4 py-2.5 text-sm font-medium text-white/80 transition-colors hover:bg-red-500/80 hover:text-white"
        >
          <LogOut className="h-4 w-4" />
          Logout
        </button>
      </div>
    </aside>
  )

  return (
    <div className="flex min-h-screen bg-app-bg text-ink">
      <div className="hidden shrink-0 lg:block">{sidebar}</div>

      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-ink/50 backdrop-blur-sm animate-fade-in" onClick={() => setMobileOpen(false)} />
          <div className="absolute inset-y-0 left-0 animate-slide-right">{sidebar}</div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        {/* topbar */}
        <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur-md sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <button
              className="rounded-lg p-2 text-slate-500 transition-colors hover:bg-slate-100 lg:hidden"
              onClick={() => setMobileOpen(true)}
              aria-label="Open menu"
            >
              <Menu className="h-5 w-5" />
            </button>

            <div className="min-w-0">
              <nav className="flex items-center gap-1.5 text-xs text-slate-400" aria-label="Breadcrumb">
                <span>PCC</span>
                <ChevronRight className="h-3 w-3" />
                <span className="font-medium text-slate-500">Admin Panel</span>
                <ChevronRight className="h-3 w-3" />
                <span className="font-semibold text-pcc-700">{pageTitle}</span>
              </nav>
              <h1 className="mt-0.5 text-base font-bold text-ink sm:text-lg">{pageTitle}</h1>
            </div>

            <div className="ml-auto flex items-center gap-2 sm:gap-3">
              <NotificationsBell />

              {user?.roles?.map((r) => (
                <span key={r.code} className="hidden rounded-full bg-pcc-100 px-3 py-1 text-xs font-semibold text-pcc-700 md:inline">
                  {r.name}
                </span>
              ))}

              {/* user dropdown */}
              <div className="relative" ref={menuRef}>
                <button
                  onClick={() => setMenuOpen((v) => !v)}
                  className="flex items-center gap-2.5 rounded-xl border border-slate-200 px-2.5 py-1.5 transition-all hover:border-pcc-300 hover:shadow-sm"
                >
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-pcc-700 text-xs font-bold text-white">
                    {user?.firstName?.[0]}{user?.lastName?.[0]}
                  </span>
                  <span className="hidden text-left sm:block">
                    <span className="block text-xs font-semibold text-slate-700">{user?.username}</span>
                    <span className="block text-[10px] text-slate-400">{user?.employeeCode}</span>
                  </span>
                  <ChevronDown className={`h-4 w-4 text-slate-400 transition-transform duration-300 ${menuOpen ? 'rotate-180' : ''}`} />
                </button>

                {menuOpen && (
                  <div className="absolute right-0 mt-2 w-56 overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-xl animate-scale-in">
                    <div className="border-b border-slate-100 bg-pcc-50/60 px-4 py-3">
                      <p className="text-sm font-semibold text-slate-800">{user?.firstName} {user?.lastName}</p>
                      <p className="truncate text-xs text-slate-400">{user?.email}</p>
                    </div>
                    <button
                      onClick={() => { setMenuOpen(false); navigate('/profile') }}
                      className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-slate-600 transition-colors hover:bg-pcc-50 hover:text-pcc-700"
                    >
                      <UserCircle className="h-4 w-4" /> My profile
                    </button>
                    <button
                      onClick={() => { setMenuOpen(false); setPwdOpen(true) }}
                      className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-slate-600 transition-colors hover:bg-pcc-50 hover:text-pcc-700"
                    >
                      <KeyRound className="h-4 w-4" /> Change password
                    </button>
                    <button
                      onClick={handleLogout}
                      className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-red-500 transition-colors hover:bg-red-50"
                    >
                      <LogOut className="h-4 w-4" /> Logout
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <div key={location.pathname} className="animate-fade-in">
            <Outlet />
          </div>
        </main>
      </div>

      <ChangePasswordModal open={pwdOpen} onClose={() => setPwdOpen(false)} />
    </div>
  )
}
