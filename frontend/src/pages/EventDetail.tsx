import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Zap, Clock, Server } from 'lucide-react'
import { clsx } from 'clsx'
import { events, Event } from '../api/client'
import { StatusBadge, Mono, CopyButton } from '../components/ui'

const statusDot: Record<string, string> = {
  SUCCESS: 'bg-status-success shadow-[0_0_6px_theme(colors.status.success/0.4)]',
  FAILED: 'bg-status-error shadow-[0_0_6px_theme(colors.status.error/0.4)]',
  EXHAUSTED: 'bg-text-muted',
  PENDING: 'bg-status-pending shadow-[0_0_6px_theme(colors.status.pending/0.4)]',
}

export default function EventDetail() {
  const { appId, eventId } = useParams<{ appId: string; eventId: string }>()
  const nav = useNavigate()
  const [event, setEvent] = useState<Event | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!appId || !eventId) return
    events.get(appId, eventId).then(setEvent).finally(() => setLoading(false))
  }, [appId, eventId])

  if (loading) return <div className="max-w-4xl mx-auto px-8 py-10">
    <div className="space-y-4">{[1,2,3].map(i => <div key={i} className="skeleton h-16 rounded-lg" />)}</div>
  </div>
  if (!event) return <div className="text-text-muted text-sm py-16 text-center">Event not found</div>

  const sorted = [...event.deliveryAttempts].sort((a, b) => a.attemptNumber - b.attemptNumber)

  return (
    <div className="max-w-4xl mx-auto px-8 py-8 animate-fade-in">
      <button onClick={() => nav(`/app/${appId}`)} className="btn-ghost flex items-center gap-1.5 -ml-3 mb-5 text-[13px]">
        <ArrowLeft size={14} /> Back
      </button>

      <div className="flex items-center gap-3 mb-6">
        <div className="w-8 h-8 rounded-lg bg-accent/10 flex items-center justify-center">
          <Zap size={16} className="text-accent" />
        </div>
        <div>
          <h1 className="text-lg font-semibold">Event detail</h1>
          <Mono>{event.id}</Mono>
        </div>
      </div>

      <div className="card p-5 mb-8">
        <div className="grid grid-cols-2 gap-y-4 gap-x-8 text-sm">
          <div>
            <p className="label mb-1">Event type</p>
            <span className="code-inline">{event.eventType}</span>
          </div>
          <div>
            <p className="label mb-1">Created</p>
            <p className="text-text-secondary text-sm">{new Date(event.createdAt).toLocaleString()}</p>
          </div>
          <div>
            <p className="label mb-1">Event ID</p>
            <CopyButton text={event.id} />
          </div>
          <div>
            <p className="label mb-1">Idempotency key</p>
            <p className="text-text-secondary font-mono text-xs">{event.idempotencyKey || '—'}</p>
          </div>
        </div>

        <div className="mt-5 pt-5 border-t border-border-subtle">
          <p className="label mb-2">Payload</p>
          <pre className="bg-surface-0 border border-border-subtle rounded-lg p-4 text-xs font-mono text-text-secondary overflow-x-auto leading-relaxed">
            {JSON.stringify(event.payload, null, 2)}
          </pre>
        </div>
      </div>

      <div className="flex items-center gap-3 mb-5">
        <h2 className="text-sm font-semibold">Delivery attempts</h2>
        <span className="text-text-muted text-2xs tabular-nums bg-surface-3 px-2 py-0.5 rounded-full">
          {sorted.length}
        </span>
      </div>

      {sorted.length === 0 ? (
        <div className="card p-8 text-text-muted text-sm text-center">
          No delivery attempts. This event may not have matching endpoint subscriptions.
        </div>
      ) : (
        <div className="relative ml-3">
          <div className="absolute left-[5px] top-4 bottom-4 w-px bg-border-subtle" />

          <div className="space-y-3 stagger">
            {sorted.map(attempt => (
              <div key={attempt.id} className="relative flex gap-5 animate-fade-in">
                <div className="relative z-10 mt-5 shrink-0">
                  <div className={clsx(
                    'w-[11px] h-[11px] rounded-full ring-[3px] ring-surface-0',
                    statusDot[attempt.status] || 'bg-text-muted'
                  )} />
                </div>

                <div className="card flex-1 px-5 py-4">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-3">
                      <span className="text-text-muted font-mono text-2xs font-medium bg-surface-1 px-1.5 py-0.5 rounded">
                        #{attempt.attemptNumber}
                      </span>
                      <StatusBadge status={attempt.status} />
                      {attempt.httpStatusCode && (
                        <span className="text-text-secondary text-2xs flex items-center gap-1 font-mono">
                          <Server size={10} className="opacity-50" /> {attempt.httpStatusCode}
                        </span>
                      )}
                    </div>
                    <span className="text-text-muted text-2xs tabular-nums">
                      {attempt.createdAt ? new Date(attempt.createdAt).toLocaleTimeString() : '—'}
                    </span>
                  </div>

                  {attempt.errorMessage && (
                    <div className="bg-status-error/5 border border-status-error/10 rounded-md px-3 py-2 mt-2">
                      <p className="text-status-error text-xs font-mono leading-relaxed">{attempt.errorMessage}</p>
                    </div>
                  )}

                  {attempt.responseBody && (
                    <div className="mt-3">
                      <p className="label mb-1">Response</p>
                      <pre className="bg-surface-0 border border-border-subtle rounded px-3 py-2 text-2xs font-mono text-text-secondary overflow-x-auto max-h-20">
                        {attempt.responseBody}
                      </pre>
                    </div>
                  )}

                  {attempt.nextRetryAt && (
                    <p className="text-status-pending text-2xs mt-3 flex items-center gap-1">
                      <Clock size={10} /> Next retry: {new Date(attempt.nextRetryAt).toLocaleTimeString()}
                    </p>
                  )}

                  <div className="mt-2">
                    <Mono>Endpoint: {attempt.endpointId.slice(0, 12)}</Mono>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
