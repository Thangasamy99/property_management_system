import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import logo from '../assets/pcc-logo.png'

/**
 * Route guard: waits for the silent session restore, then requires an
 * authenticated user and (optionally) a backend permission.
 */
export default function ProtectedRoute({ permission }) {
  const { user, ready, hasPermission } = useAuth()
  const location = useLocation()

  if (!ready) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center gap-4">
          <img src={logo} alt="PCC" className="h-16 w-16 animate-pulse-glow rounded-full ring-4 ring-pcc-100" />
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-pcc-200 border-t-pcc-600" />
        </div>
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (permission && !hasPermission(permission)) {
    return <Navigate to={landing(user)} replace />
  }

  return <Outlet />
}

function landing(user) {
  const codes = (user?.roles ?? []).map((r) => r.code)
  const map = {
    ADMIN: '/admin/dashboard',
    MANAGER: '/manager/dashboard',
    ACCOUNTANT: '/accountant/dashboard',
    PROPERTY_OFFICER: '/property/dashboard',
    LAND_OFFICER: '/land/dashboard',
  }
  for (const c of Object.keys(map)) if (codes.includes(c)) return map[c]
  return '/login'
}
