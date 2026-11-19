import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, Users, ShieldCheck, History, ScrollText, UserCircle,
  LogOut, Menu, ChevronDown, ChevronRight, ChevronsLeft, Plus, Bell, KeyRound, ShieldAlert,
} from 'lucide-react'
import { landingPathFor, useAuth } from '../context/AuthContext'
import { apiFetch } from '../services/api'
import ChangePasswordModal from '../components/ChangePasswordModal'
import logo from '../assets/pcc-logo.png'

const NAV_ITEMS = [
  { key: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/users', label: 'Employees', permission: 'USER_VIEW', icon: Users, add: '/users?new=1', badgeKey: 'employees' },
  { to: '/roles', label: 'Roles & Permissions', permission: 'ROLE_VIEW', icon: ShieldCheck, add: '/roles?new=1' },
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
        className="relative flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition-colors hover:border-pcc-300 hover:text-pcc-700"
        aria-label="Notifications"
        aria-expanded={open}
      >
        <Bell className="h-5 w-5" strokeWidth={1.8} />
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
                <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-red-500" strokeWidth={1.8} />
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
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem('pcc-nav-collapsed') === '1')
  const [counts, setCounts] = useState({})
  const menuRef = useRef(null)
  const dashboardPath = landingPathFor(user)

  const pageTitle = PAGE_TITLES[location.pathname] ?? 'Dashboard'

  const items = NAV_ITEMS
    .map((n) => ({ ...n, to: n.key === 'dashboard' ? dashboardPath : n.to }))
    .filter((n) => !n.permission || hasPermission(n.permission))

  // real badge counts (only what the user may see)
  useEffect(() => {
    if (!hasPermission('USER_VIEW')) return
    apiFetch('/stats/dashboard')
      .then(async (r) => r.ok && setCounts({ employees: (await r.json()).totalEmployees }))
      .catch(() => {})
  }, [hasPermission])

  const toggleCollapsed = () => {
    setCollapsed((c) => {
      localStorage.setItem('pcc-nav-collapsed', c ? '0' : '1')
      return !c
    })
  }

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
    <aside className="flex h-full" aria-label="Main navigation">
      {/* icon rail */}
      <div className="flex w-[68px] flex-col items-center border-r border-white/10 bg-pcc-950 py-4">
        <img src={logo} alt="PCC Cameroon" className="h-10 w-10 rounded-lg bg-white object-contain p-1" />
        <nav className="mt-6 flex flex-1 flex-col items-center gap-2" aria-label="Icon navigation">
          {items.map((item) => {
            const Icon = item.icon
            return (
              <NavLink
                key={item.label}
                to={item.to}
                title={item.label}
                aria-label={item.label}
                className={({ isActive }) =>
                  `relative flex h-11 w-11 items-center justify-center rounded-xl transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-pcc-300 ${
                    isActive ? 'bg-white/15 text-white ring-1 ring-white/20' : 'text-white/55 hover:bg-white/10 hover:text-white'
                  }`
                }
              >
                <Icon className="h-5 w-5" strokeWidth={1.8} aria-hidden="true" />
                {item.badgeKey && counts[item.badgeKey] != null && (
                  <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-pcc-400 px-1 text-[9px] font-bold text-pcc-950">
                    {counts[item.badgeKey]}
                  </span>
                )}
              </NavLink>
            )
          })}
        </nav>
        {collapsed && (
          <button
            onClick={toggleCollapsed}
            title="Expand sidebar"
            aria-label="Expand sidebar"
            className="mb-2 flex h-11 w-11 items-center justify-center rounded-xl text-white/55 transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-pcc-300"
          >
            <ChevronRight className="h-5 w-5" strokeWidth={1.8} />
          </button>
        )}
        <button
          onClick={handleLogout}
          title="Logout"
          aria-label="Logout"
          className="flex h-11 w-11 items-center justify-center rounded-xl text-white/55 transition-colors hover:bg-red-500/80 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-pcc-300"
        >
          <LogOut className="h-5 w-5" strokeWidth={1.8} />
        </button>
      </div>

      {/* label panel */}
      {!collapsed && (
        <div className="flex w-60 flex-col bg-pcc-900 text-white animate-[slide-right_0.2s_ease-out]">
          {/* panel header */}
          <div className="flex items-center justify-between gap-2 px-5 py-5">
            <div className="min-w-0">
              <p className="truncate text-sm font-bold leading-tight tracking-tight">PCC Cameroon</p>
              <p className="truncate text-[11px] text-white/50">Property &amp; Land Management</p>
            </div>
            <button
              onClick={toggleCollapsed}
              title="Collapse sidebar"
              aria-label="Collapse sidebar"
              className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md text-white/45 transition-colors hover:bg-white/10 hover:text-white"
            >
              <ChevronsLeft className="h-4 w-4" strokeWidth={1.8} />
            </button>
          </div>

          {/* flat nav list */}
          <nav className="flex-1 overflow-y-auto px-3 pb-4" aria-label="Full navigation">
            <ul className="space-y-0.5">
              {items.map((item) => {
                const Icon = item.icon
                return (
                  <li key={item.label}>
                    <div className="group relative flex items-center">
                      <NavLink
                        to={item.to}
                        className={({ isActive }) =>
                          `flex min-h-[44px] flex-1 items-center gap-3 rounded-lg px-3 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-pcc-300 ${
                            isActive ? 'bg-white/12 text-white' : 'text-white/60 hover:bg-white/8 hover:text-white'
                          }`
                        }
                      >
                        <Icon className="h-[18px] w-[18px] shrink-0" strokeWidth={1.8} aria-hidden="true" />
                        <span className="truncate">{item.label}</span>
                        {item.badgeKey && counts[item.badgeKey] != null && (
                          <span className="ml-auto rounded-md bg-white/12 px-1.5 py-0.5 text-[10px] font-bold text-white/80">
                            {counts[item.badgeKey]}
                          </span>
                        )}
                      </NavLink>
                      {item.add && (
                        <button
                          onClick={() => navigate(item.add)}
                          title={`New — ${item.label}`}
                          aria-label={`Create new in ${item.label}`}
                          className="absolute right-2 flex h-6 w-6 items-center justify-center rounded-md text-white/45 opacity-0 transition-all hover:bg-white/15 hover:text-white focus-visible:opacity-100 group-hover:opacity-100"
                        >
                          <Plus className="h-4 w-4" strokeWidth={2} />
                        </button>
                      )}
                    </div>
                  </li>
                )
              })}
            </ul>
          </nav>

          {/* user footer */}
          <div className="border-t border-white/10 p-4">
            <div className="flex items-center gap-3 px-1.5">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-pcc-500/30 text-sm font-bold ring-1 ring-white/20">
                {user?.firstName?.[0]}{user?.lastName?.[0]}
              </span>
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{user?.firstName} {user?.lastName}</p>
                <p className="truncate text-xs text-white/50">{user?.roles?.map((r) => r.name).join(', ')}</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </aside>
  )

  return (
    <div className="flex min-h-screen bg-app-bg text-ink">
      {/* skip link for keyboard users */}
      <a
        href="#main-content"
        className="sr-only rounded-lg bg-pcc-700 px-4 py-2 text-sm font-medium text-white focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[100]"
      >
        Skip to content
      </a>

      <div className="hidden shrink-0 lg:block">{sidebar}</div>

      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation menu">
          <div className="absolute inset-0 bg-ink/50 backdrop-blur-sm animate-fade-in" onClick={() => setMobileOpen(false)} />
          <div className="absolute inset-y-0 left-0 animate-slide-right">{sidebar}</div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        {/* header */}
        <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur-md">
          <div className="flex min-h-[64px] items-center gap-3 px-4 py-2.5 sm:px-6 lg:px-8">
            <button
              className="flex h-10 w-10 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 lg:hidden"
              onClick={() => setMobileOpen(true)}
              aria-label="Open navigation menu"
              aria-expanded={mobileOpen}
            >
              <Menu className="h-5 w-5" strokeWidth={1.8} />
            </button>

            <div className="min-w-0">
              <nav className="flex items-center gap-1 text-[11px] text-slate-400" aria-label="Breadcrumb">
                <span>PCC</span>
                <ChevronRight className="h-3 w-3" aria-hidden="true" />
                <span className="font-medium text-slate-500">Admin Panel</span>
                <ChevronRight className="h-3 w-3" aria-hidden="true" />
                <span className="font-semibold text-pcc-700">{pageTitle}</span>
              </nav>
              <h1 className="truncate text-base font-bold tracking-tight text-ink sm:text-lg">{pageTitle}</h1>
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
                  className="flex min-h-[44px] items-center gap-2.5 rounded-xl border border-slate-200 px-2.5 py-1.5 transition-colors hover:border-pcc-300 hover:bg-slate-50"
                  aria-label="Account menu"
                  aria-expanded={menuOpen}
                >
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-pcc-700 text-xs font-bold text-white">
                    {user?.firstName?.[0]}{user?.lastName?.[0]}
                  </span>
                  <span className="hidden text-left sm:block">
                    <span className="block text-xs font-semibold text-slate-700">{user?.username}</span>
                    <span className="block text-[10px] text-slate-400">{user?.employeeCode}</span>
                  </span>
                  <ChevronDown className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${menuOpen ? 'rotate-180' : ''}`} aria-hidden="true" />
                </button>

                {menuOpen && (
                  <div className="absolute right-0 mt-2 w-56 overflow-hidden rounded-2xl border border-slate-100 bg-white py-1 shadow-xl animate-scale-in" role="menu">
                    <div className="border-b border-slate-100 bg-pcc-50/60 px-4 py-3">
                      <p className="text-sm font-semibold text-slate-800">{user?.firstName} {user?.lastName}</p>
                      <p className="truncate text-xs text-slate-400">{user?.email}</p>
                    </div>
                    <button role="menuitem" onClick={() => { setMenuOpen(false); navigate('/profile') }}
                      className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-slate-600 transition-colors hover:bg-pcc-50 hover:text-pcc-700">
                      <UserCircle className="h-4 w-4" strokeWidth={1.8} /> My profile
                    </button>
                    <button role="menuitem" onClick={() => { setMenuOpen(false); setPwdOpen(true) }}
                      className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-slate-600 transition-colors hover:bg-pcc-50 hover:text-pcc-700">
                      <KeyRound className="h-4 w-4" strokeWidth={1.8} /> Change password
                    </button>
                    <button role="menuitem" onClick={handleLogout}
                      className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-red-500 transition-colors hover:bg-red-50">
                      <LogOut className="h-4 w-4" strokeWidth={1.8} /> Logout
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </header>

        {/* main content */}
        <main id="main-content" className="flex-1 p-4 sm:p-6 lg:p-8" tabIndex={-1}>
          <div key={location.pathname} className="animate-fade-in">
            <Outlet />
          </div>
        </main>

        {/* footer */}
        <footer className="mt-auto border-t border-slate-200 bg-white px-4 py-4 sm:px-6 lg:px-8">
          <div className="flex flex-col items-center justify-between gap-2 text-xs text-slate-400 sm:flex-row">
            <p>© {new Date().getFullYear()} PCC Cameroon — Property &amp; Land Management System</p>
            <span>Identity Service v0.0.1</span>
          </div>
        </footer>
      </div>

      <ChangePasswordModal open={pwdOpen} onClose={() => setPwdOpen(false)} />
    </div>
  )
}
