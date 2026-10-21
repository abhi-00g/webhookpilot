import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { Webhook, LayoutDashboard, LogOut } from 'lucide-react'
import { clsx } from 'clsx'
import { useAuth } from '../context/AuthContext'

export default function Layout() {
  const { email, logout } = useAuth()
  const nav = useNavigate()

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Top accent */}
      <div className="fixed top-0 left-0 right-0 h-[2px] z-50 bg-gradient-to-r from-accent-dim via-accent to-mint opacity-70" />

      {/* Sidebar */}
      <aside className="w-52 shrink-0 bg-surface-1 border-r border-border-subtle flex flex-col pt-[2px]">
        <div className="px-4 h-14 flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-lg bg-accent/10 flex items-center justify-center border border-accent/20">
            <Webhook size={14} className="text-accent" />
          </div>
          <span className="font-semibold text-[13px] tracking-tight">WebhookPilot</span>
        </div>

        <nav className="flex-1 px-2 py-2 overflow-y-auto">
          <NavLink to="/" end className={({ isActive }) => clsx(
            'flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px] transition-all duration-150',
            isActive ? 'bg-accent/10 text-accent-bright font-medium' : 'text-text-secondary hover:text-text-primary hover:bg-surface-2'
          )}>
            <LayoutDashboard size={15} /> Applications
          </NavLink>
        </nav>

        <div className="px-3 py-3 border-t border-border-subtle">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2 min-w-0">
              <div className="w-6 h-6 rounded-full bg-gradient-to-br from-accent/20 to-mint/20 flex items-center justify-center shrink-0 border border-border-subtle">
                <span className="text-accent-bright text-2xs font-semibold">{email?.[0]?.toUpperCase() || '?'}</span>
              </div>
              <span className="text-2xs text-text-muted truncate">{email}</span>
            </div>
            <button onClick={() => { logout(); nav('/login') }}
              className="text-text-muted hover:text-status-error transition-colors shrink-0 p-1 rounded-lg hover:bg-status-error/10" title="Sign out">
              <LogOut size={13} />
            </button>
          </div>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 overflow-y-auto pt-[2px] bg-grid-pattern">
        <Outlet />
      </main>
    </div>
  )
}
