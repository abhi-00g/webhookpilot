import { createContext, useContext, useState, useCallback, ReactNode } from 'react'
import { auth as authApi, setToken, getToken } from '../api/client'

interface AuthContextType {
  isAuthenticated: boolean
  email: string | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType>(null!)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [email, setEmail] = useState<string | null>(
    localStorage.getItem('whp_email')
  )

  const isAuthenticated = !!getToken()

  const login = useCallback(async (em: string, pw: string) => {
    const res = await authApi.login(em, pw)
    setToken(res.token)
    setEmail(res.email)
    localStorage.setItem('whp_email', res.email)
  }, [])

  const register = useCallback(async (em: string, pw: string) => {
    const res = await authApi.register(em, pw)
    setToken(res.token)
    setEmail(res.email)
    localStorage.setItem('whp_email', res.email)
  }, [])

  const logout = useCallback(() => {
    setToken(null)
    setEmail(null)
    localStorage.removeItem('whp_email')
  }, [])

  return (
    <AuthContext.Provider value={{ isAuthenticated, email, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
