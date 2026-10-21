import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Webhook, Check, X, Eye, EyeOff, Zap, Shield, RefreshCw, BarChart3 } from 'lucide-react'
import { clsx } from 'clsx'
import { useAuth } from '../context/AuthContext'

const PW_RULES = [
  { label: '8+ characters', test: (p: string) => p.length >= 8 },
  { label: 'Uppercase', test: (p: string) => /[A-Z]/.test(p) },
  { label: 'Lowercase', test: (p: string) => /[a-z]/.test(p) },
  { label: 'Number', test: (p: string) => /[0-9]/.test(p) },
  { label: 'Special char', test: (p: string) => /[^A-Za-z0-9]/.test(p) },
]
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const FEATURES = [
  { icon: Zap, title: 'Event fan-out', desc: 'Kafka-powered delivery to every subscribed endpoint' },
  { icon: RefreshCw, title: 'Automatic retries', desc: 'Exponential backoff with configurable retry policies' },
  { icon: Shield, title: 'HMAC signatures', desc: 'Cryptographic payload verification on every delivery' },
  { icon: BarChart3, title: 'Full observability', desc: 'Track every delivery attempt, response, and retry' },
]

export default function Login() {
  const [isReg, setIsReg] = useState(false)
  const [email, setEmail] = useState('')
  const [pw, setPw] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [touched, setTouched] = useState({ email: false, pw: false, confirm: false })
  const { login, register } = useAuth()
  const nav = useNavigate()

  const emailOk = EMAIL_RE.test(email)
  const checks = useMemo(() => PW_RULES.map(r => ({ ...r, pass: r.test(pw) })), [pw])
  const pwOk = checks.every(c => c.pass)
  const match = pw === confirm && confirm.length > 0
  const canReg = emailOk && pwOk && match

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (isReg && !canReg) return
    setError(''); setLoading(true)
    try {
      if (isReg) await register(email, pw); else await login(email, pw)
      nav('/')
    } catch (err: any) { setError(err.message || 'Something went wrong') }
    finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen flex">
      {/* Left panel — feature showcase */}
      <div className="hidden lg:flex lg:w-[480px] xl:w-[520px] shrink-0 bg-surface-1 border-r border-border-subtle flex-col justify-between p-10 bg-grid-pattern relative overflow-hidden">
        {/* Gradient orb */}
        <div className="absolute -top-32 -left-32 w-96 h-96 bg-accent/5 rounded-full blur-3xl" />
        <div className="absolute -bottom-32 -right-32 w-80 h-80 bg-mint/5 rounded-full blur-3xl" />

        <div className="relative">
          <div className="flex items-center gap-2.5 mb-16">
            <div className="w-9 h-9 rounded-xl bg-accent/10 flex items-center justify-center border border-accent/20">
              <Webhook size={17} className="text-accent" />
            </div>
            <span className="font-semibold text-sm tracking-tight">WebhookPilot</span>
          </div>

          <h2 className="text-2xl font-semibold leading-snug mb-3">
            Reliable webhook delivery,<br />from first event to last retry.
          </h2>
          <p className="text-text-secondary text-sm leading-relaxed mb-12 max-w-sm">
            Send events from your application and let WebhookPilot handle fan-out, signing, retries, and observability.
          </p>

          <div className="space-y-5">
            {FEATURES.map(f => (
              <div key={f.title} className="flex gap-3.5">
                <div className="w-8 h-8 rounded-lg bg-surface-3/80 flex items-center justify-center shrink-0 border border-border-subtle">
                  <f.icon size={15} className="text-accent" />
                </div>
                <div>
                  <p className="text-sm font-medium text-text-primary">{f.title}</p>
                  <p className="text-2xs text-text-muted mt-0.5">{f.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <p className="text-text-muted text-2xs relative">Open source webhook delivery platform</p>
      </div>

      {/* Right panel — auth form */}
      <div className="flex-1 flex items-center justify-center px-6 bg-grid-pattern relative">
        <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-accent/30 to-transparent" />

        <div className="w-full max-w-[380px] anim-fade-up">
          {/* Mobile logo */}
          <div className="flex items-center gap-2.5 mb-8 lg:hidden">
            <div className="w-9 h-9 rounded-xl bg-accent/10 flex items-center justify-center border border-accent/20">
              <Webhook size={17} className="text-accent" />
            </div>
            <span className="font-semibold tracking-tight">WebhookPilot</span>
          </div>

          <h1 className="text-xl font-semibold mb-1">{isReg ? 'Create your account' : 'Welcome back'}</h1>
          <p className="text-text-muted text-sm mb-7">
            {isReg ? 'Start delivering webhooks in minutes.' : 'Sign in to your dashboard.'}
          </p>

          {error && (
            <div className="bg-status-error/8 border border-status-error/20 text-status-error text-sm rounded-xl px-4 py-3 mb-5">
              {error}
            </div>
          )}

          <form onSubmit={submit} className="space-y-4">
            <div>
              <label className="label">Email</label>
              <input type="email" value={email} onChange={e => setEmail(e.target.value)}
                onBlur={() => setTouched(t => ({ ...t, email: true }))}
                className={clsx('input-field', touched.email && !emailOk && email.length > 0 && 'input-error')}
                placeholder="you@company.com" required />
              {touched.email && !emailOk && email.length > 0 && (
                <p className="text-status-error text-2xs mt-1.5">Enter a valid email address</p>
              )}
            </div>

            <div>
              <label className="label">Password</label>
              <div className="relative">
                <input type={showPw ? 'text' : 'password'} value={pw} onChange={e => setPw(e.target.value)}
                  onBlur={() => setTouched(t => ({ ...t, pw: true }))}
                  className="input-field pr-10" placeholder={isReg ? 'Create a password' : 'Your password'} required />
                <button type="button" onClick={() => setShowPw(!showPw)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary transition-colors">
                  {showPw ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
              {isReg && touched.pw && pw.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2">
                  {checks.map((c, i) => (
                    <span key={i} className={clsx(
                      'inline-flex items-center gap-1 text-2xs px-2 py-0.5 rounded-full border',
                      c.pass ? 'bg-status-success/8 text-status-success border-status-success/15' : 'bg-surface-3 text-text-muted border-border-subtle'
                    )}>
                      {c.pass ? <Check size={10} /> : <X size={10} />} {c.label}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {isReg && (
              <div>
                <label className="label">Confirm password</label>
                <input type="password" value={confirm} onChange={e => setConfirm(e.target.value)}
                  onBlur={() => setTouched(t => ({ ...t, confirm: true }))}
                  className={clsx('input-field', touched.confirm && confirm.length > 0 && !match && 'input-error')}
                  placeholder="Re-enter password" required />
                {touched.confirm && confirm.length > 0 && (
                  match
                    ? <p className="text-status-success text-2xs mt-1.5 flex items-center gap-1"><Check size={10} /> Passwords match</p>
                    : <p className="text-status-error text-2xs mt-1.5">Passwords do not match</p>
                )}
              </div>
            )}

            <button type="submit" disabled={loading || (isReg && !canReg)} className="btn-primary w-full mt-2">
              {loading ? 'Loading...' : isReg ? 'Create account' : 'Sign in'}
            </button>
          </form>

          <p className="text-center text-text-muted text-sm mt-6">
            {isReg ? 'Have an account?' : 'Need an account?'}{' '}
            <button onClick={() => { setIsReg(!isReg); setError(''); setConfirm(''); setTouched({ email: false, pw: false, confirm: false }) }}
              className="text-accent hover:text-accent-bright transition-colors font-medium">
              {isReg ? 'Sign in' : 'Create one'}
            </button>
          </p>
        </div>
      </div>
    </div>
  )
}
