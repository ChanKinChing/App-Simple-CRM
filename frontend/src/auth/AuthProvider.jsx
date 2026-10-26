import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, authStorage, onUnauthorized } from '../api/client'
import { AuthContext } from './context'

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    const stored = authStorage.read()
    if (!stored?.token) return null
    if (stored.expiresAt && new Date(stored.expiresAt).getTime() <= Date.now()) {
      authStorage.clear()
      return null
    }
    return stored
  })

  useEffect(() => onUnauthorized(() => setAuth(null)), [])

  const login = useCallback(async (username, password) => {
    const { data } = await api.post('/auth/login', { username, password })
    const value = { token: data.token, expiresAt: data.expiresAt, user: data.user }
    authStorage.write(value)
    setAuth(value)
    return value
  }, [])

  const logout = useCallback(() => {
    authStorage.clear()
    setAuth(null)
  }, [])

  const value = useMemo(
    () => ({ token: auth?.token ?? null, user: auth?.user ?? null, login, logout }),
    [auth, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
