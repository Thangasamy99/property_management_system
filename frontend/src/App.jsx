import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, landingPathFor, useAuth } from './context/AuthContext.jsx'
import { ToastProvider } from './context/ToastContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import DashboardLayout from './layouts/DashboardLayout.jsx'
import LoginPage from './pages/LoginPage.jsx'
import DashboardHome from './pages/dashboard/DashboardHome.jsx'
import UsersPage from './pages/dashboard/UsersPage.jsx'
import RolesPage from './pages/dashboard/RolesPage.jsx'
import LoginHistoryPage from './pages/dashboard/LoginHistoryPage.jsx'
import AuditLogsPage from './pages/dashboard/AuditLogsPage.jsx'
import ProfilePage from './pages/dashboard/ProfilePage.jsx'

function RootRedirect() {
  const { user, ready } = useAuth()
  if (!ready) return null
  return <Navigate to={user ? landingPathFor(user) : '/login'} replace />
}

function LoginRoute() {
  const { user, ready } = useAuth()
  if (!ready) return null
  return user ? <Navigate to={landingPathFor(user)} replace /> : <LoginPage />
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginRoute />} />
          <Route path="/" element={<RootRedirect />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<DashboardLayout />}>
              <Route path="/admin/dashboard" element={<DashboardHome />} />
              <Route path="/manager/dashboard" element={<DashboardHome />} />
              <Route path="/accountant/dashboard" element={<DashboardHome />} />
              <Route path="/property/dashboard" element={<DashboardHome />} />
              <Route path="/land/dashboard" element={<DashboardHome />} />
              <Route path="/login-history" element={<LoginHistoryPage />} />
              <Route path="/profile" element={<ProfilePage />} />

              <Route element={<ProtectedRoute permission="USER_VIEW" />}>
                <Route path="/users" element={<UsersPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="ROLE_VIEW" />}>
                <Route path="/roles" element={<RolesPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="AUDIT_LOG_VIEW" />}>
                <Route path="/audit-logs" element={<AuditLogsPage />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
