import { clsx } from 'clsx'
import { Check, Copy, Inbox } from 'lucide-react'
import { useState, ReactNode, createContext, useContext, useCallback } from 'react'
import { createPortal } from 'react-dom'

/* ── Status Badge ── */
const badgeCfg: Record<string, { bg: string; dot: string; pulse?: boolean }> = {
  SUCCESS: { bg: 'bg-status-success/8 text-status-success border-status-success/15', dot: 'bg-status-success' },
  ACTIVE: { bg: 'bg-status-success/8 text-status-success border-status-success/15', dot: 'bg-status-success', pulse: true },
  FAILED: { bg: 'bg-status-error/8 text-status-error border-status-error/15', dot: 'bg-status-error' },
  EXHAUSTED: { bg: 'bg-surface-4 text-text-muted border-border-subtle', dot: 'bg-text-muted' },
  PENDING: { bg: 'bg-status-pending/8 text-status-pending border-status-pending/15', dot: 'bg-status-pending', pulse: true },
  PAUSED: { bg: 'bg-status-warning/8 text-status-warning border-status-warning/15', dot: 'bg-status-warning' },
  DISABLED: { bg: 'bg-status-error/8 text-status-error border-status-error/15', dot: 'bg-status-error' },
}

export function StatusBadge({ status }: { status: string }) {
  const c = badgeCfg[status] || { bg: 'bg-surface-3 text-text-secondary border-border-subtle', dot: 'bg-text-muted' }
  return (
    <span className={clsx('inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-2xs font-medium border', c.bg)}>
      <span className={clsx('w-1.5 h-1.5 rounded-full', c.dot, c.pulse && 'anim-pulse-glow')} />
      {status}
    </span>
  )
}

/* ── Copy Button ── */
export function CopyButton({ text, label, className }: { text: string; label?: string; className?: string }) {
  const [copied, setCopied] = useState(false)
  const toast = useToast()
  const copy = async (e: React.MouseEvent) => {
    e.stopPropagation()
    await navigator.clipboard.writeText(text)
    setCopied(true); toast('Copied to clipboard')
    setTimeout(() => setCopied(false), 2000)
  }
  return (
    <button onClick={copy} className={clsx('inline-flex items-center gap-1.5 font-mono text-2xs text-text-secondary hover:text-accent-bright transition-colors rounded-md px-1.5 py-0.5 -mx-1 hover:bg-accent/5', className)}>
      {copied ? <Check size={11} className="text-status-success" /> : <Copy size={11} className="opacity-40" />}
      <span className="truncate">{label || text}</span>
    </button>
  )
}

/* ── Empty State ── */
export function EmptyState({ icon, message, action }: { icon?: ReactNode; message: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 anim-fade-up">
      <div className="w-14 h-14 rounded-2xl bg-surface-3/80 flex items-center justify-center mb-4 border border-border-subtle">
        {icon || <Inbox size={22} strokeWidth={1.5} className="text-text-muted" />}
      </div>
      <p className="text-sm text-text-muted mb-5 max-w-xs text-center leading-relaxed">{message}</p>
      {action}
    </div>
  )
}

/* ── Metric ── */
export function Metric({ label, value, sub, accent }: { label: string; value: string | number; sub?: string; accent?: string }) {
  return (
    <div>
      <p className="text-text-muted text-2xs mb-1.5 uppercase tracking-wider font-medium">{label}</p>
      <p className={clsx('text-[1.75rem] font-semibold tabular-nums tracking-tight leading-none', accent)}>{value}</p>
      {sub && <p className="text-text-muted text-2xs mt-1.5">{sub}</p>}
    </div>
  )
}

/* ── Table ── */
export function Table({ headers, children }: { headers: string[]; children: ReactNode }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead><tr className="border-b border-border">
          {headers.map(h => <th key={h} className="text-left text-text-muted text-2xs font-medium py-3 px-4 first:pl-0">{h}</th>)}
        </tr></thead>
        <tbody className="divide-y divide-border-subtle/50 stagger">{children}</tbody>
      </table>
    </div>
  )
}

/* ── Tabs ── */
export function Tabs({ tabs, active, onChange }: { tabs: { key: string; label: string; count?: number }[]; active: string; onChange: (k: string) => void }) {
  return (
    <div className="flex gap-0.5 bg-surface-1 rounded-xl p-1 w-fit border border-border-subtle">
      {tabs.map(t => (
        <button key={t.key} onClick={() => onChange(t.key)} className={clsx(
          'px-4 py-2 text-[13px] rounded-lg transition-all duration-150 font-medium',
          active === t.key ? 'bg-surface-3 text-text-primary shadow-sm border border-border-subtle' : 'text-text-muted hover:text-text-secondary border border-transparent'
        )}>
          {t.label}
          {t.count !== undefined && <span className={clsx('ml-1.5 text-2xs tabular-nums', active === t.key ? 'text-text-secondary' : 'text-text-muted')}>{t.count}</span>}
        </button>
      ))}
    </div>
  )
}

/* ── Helpers ── */
export function Mono({ children, className }: { children: ReactNode; className?: string }) {
  return <span className={clsx('font-mono text-2xs text-text-muted', className)}>{children}</span>
}

export function TimeAgo({ iso }: { iso: string | null }) {
  if (!iso) return <span className="text-text-muted text-2xs">—</span>
  const s = Math.floor((Date.now() - new Date(iso).getTime()) / 1000)
  const txt = s < 60 ? `${s}s ago` : s < 3600 ? `${Math.floor(s/60)}m ago` : s < 86400 ? `${Math.floor(s/3600)}h ago` : new Date(iso).toLocaleDateString()
  return <span className="text-text-muted text-2xs tabular-nums">{txt}</span>
}

export function Pagination({ current, total, onChange }: { current: number; total: number; onChange: (p: number) => void }) {
  if (total <= 1) return null
  return (
    <div className="flex items-center justify-center gap-2 mt-6">
      <button onClick={() => onChange(current-1)} disabled={current===0} className="btn-ghost text-xs disabled:opacity-30">Prev</button>
      <span className="text-text-muted text-xs px-3 tabular-nums">{current+1} / {total}</span>
      <button onClick={() => onChange(current+1)} disabled={current>=total-1} className="btn-ghost text-xs disabled:opacity-30">Next</button>
    </div>
  )
}

/* ── Modal ── */
export function Modal({ open, onClose, title, children }: { open: boolean; onClose: () => void; title: string; children: ReactNode }) {
  if (!open) return null
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />
      <div className="relative card p-6 w-full max-w-lg anim-fade-up shadow-2xl border-border bg-surface-2">
        <h2 className="text-base font-semibold mb-5">{title}</h2>
        {children}
      </div>
    </div>,
    document.body
  )
}

/* ── Toast ── */
type ToastFn = (msg: string) => void
const ToastCtx = createContext<ToastFn>(() => {})
export const useToast = () => useContext(ToastCtx)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<{ id: number; msg: string }[]>([])
  const add: ToastFn = useCallback((msg) => {
    const id = Date.now()
    setItems(t => [...t, { id, msg }])
    setTimeout(() => setItems(t => t.filter(x => x.id !== id)), 2500)
  }, [])
  return (
    <ToastCtx.Provider value={add}>
      {children}
      {createPortal(
        <div className="fixed top-5 right-5 z-[100] flex flex-col gap-2">
          {items.map(t => (
            <div key={t.id} className="anim-slide-in bg-surface-3 border border-border text-sm text-text-primary px-4 py-2.5 rounded-xl shadow-xl flex items-center gap-2.5">
              <div className="w-5 h-5 rounded-full bg-status-success/15 flex items-center justify-center"><Check size={12} className="text-status-success" /></div>
              {t.msg}
            </div>
          ))}
        </div>,
        document.body
      )}
    </ToastCtx.Provider>
  )
}

/* ── Status Distribution Bar ── */
export function StatusBar({ counts }: { counts: { status: string; count: number }[] }) {
  const total = counts.reduce((s, c) => s + c.count, 0)
  if (total === 0) return null
  const colors: Record<string, string> = { SUCCESS: 'bg-status-success', FAILED: 'bg-status-error', PENDING: 'bg-status-pending', EXHAUSTED: 'bg-text-muted' }
  return (
    <div>
      <div className="flex h-2 rounded-full overflow-hidden bg-surface-0 gap-px">
        {counts.filter(c => c.count > 0).map(c => (
          <div key={c.status} className={clsx('rounded-full transition-all', colors[c.status] || 'bg-text-muted')}
            style={{ width: `${(c.count / total) * 100}%` }} />
        ))}
      </div>
      <div className="flex gap-4 mt-2">
        {counts.filter(c => c.count > 0).map(c => (
          <span key={c.status} className="flex items-center gap-1.5 text-2xs text-text-muted">
            <span className={clsx('w-2 h-2 rounded-full', colors[c.status])} />
            {c.status} <span className="text-text-secondary tabular-nums">{c.count}</span>
          </span>
        ))}
      </div>
    </div>
  )
}
