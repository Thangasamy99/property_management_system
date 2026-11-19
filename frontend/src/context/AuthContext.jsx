import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { loginApi, logoutApi, refreshSession } from '../services/api'

const AuthContext = createContext(null)

// Landing route per primary role (priority order).
const ROLE_LANDING = {
  ADMIN: '/admin/dashboard',
  MANAGER: '/manager/dashboard',
  ACCOUNTANT: '/accountant/dashboard',
  PROPERTY_OFFICER: '/property/dashboard',
  LAND_OFFICER: '/land/dashboard',
}

export function landingPathFor(user) {
  const codes = (user?.roles ?? []).map((r) => r.code)
  for (const code of Object.keys(ROLE_LANDING)) {
    if (codes.includes(code)) return ROLE_LANDING[code]
  }
  return '/dashboard'
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false)

  // Restore session on first load via the HttpOnly refresh cookie.
  useEffect(() => {
    refreshSession()
      .then((u) => u && setUser(u))
      .catch(() => {})
      .finally(() => setReady(true))
  }, [])

  const login = useCallback(async (username, password) => {
    const data = await loginApi(username, password)
    setUser(data.user)
    return data.user
  }, [])

  const logout = useCallback(async () => {
    await logoutApi()
    setUser(null)
  }, [])

  const hasPermission = useCallback(
    (code) => (user?.permissions ?? []).includes(code),
    [user],
  )

  const value = useMemo(
    () => ({ user, ready, login, logout, hasPermission, setUser }),
    [user, ready, login, logout, hasPermission],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
