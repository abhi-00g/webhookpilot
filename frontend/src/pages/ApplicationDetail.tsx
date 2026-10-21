import { useEffect, useState, useCallback, useMemo } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { ArrowLeft, Plus, Trash2, Link as LinkIcon, Zap, Send, RefreshCw, CheckSquare, Square } from 'lucide-react'
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import { clsx } from 'clsx'
import { apps, eventTypes, endpoints, events, App, EventType, Endpoint, Event } from '../api/client'
import { StatusBadge, CopyButton, EmptyState, Metric, Tabs, Table, Mono, TimeAgo, Pagination, Modal, useToast, StatusBar } from '../components/ui'

type TabKey = 'events' | 'endpoints' | 'eventTypes'

export default function ApplicationDetail() {
  const { appId } = useParams<{ appId: string }>()
  const nav = useNavigate(); const toast = useToast()
  const [app, setApp] = useState<App | null>(null)
  const [tab, setTab] = useState<TabKey>('events')
  const [ets, setEts] = useState<EventType[]>([])
  const [eps, setEps] = useState<Endpoint[]>([])
  const [evts, setEvts] = useState<Event[]>([])
  const [evtPage, setEvtPage] = useState({ total: 0, pages: 0, current: 0 })
  const [loading, setLoading] = useState(true)
  const [autoRefresh, setAutoRefresh] = useState(false)

  const loadAll = useCallback(async (page = 0) => {
    if (!appId) return
    try {
      const [a, et, ep, ev] = await Promise.all([apps.get(appId), eventTypes.list(appId), endpoints.list(appId), events.list(appId, page, 20)])
      setApp(a); setEts(et); setEps(ep); setEvts(ev.content)
      setEvtPage({ total: ev.totalElements, pages: ev.totalPages, current: ev.number })
    } finally { setLoading(false) }
  }, [appId])

  useEffect(() => { loadAll() }, [loadAll])
  useEffect(() => { if (!autoRefresh) return; const id = setInterval(() => loadAll(evtPage.current), 5000); return () => clearInterval(id) }, [autoRefresh, loadAll, evtPage.current])

  // Analytics
  const allAttempts = useMemo(() => evts.flatMap(e => e.deliveryAttempts), [evts])
  const statusCounts = useMemo(() => {
    const m: Record<string, number> = { SUCCESS: 0, FAILED: 0, PENDING: 0, EXHAUSTED: 0 }
    allAttempts.forEach(a => { m[a.status] = (m[a.status] || 0) + 1 })
    return Object.entries(m).map(([status, count]) => ({ status, count }))
  }, [allAttempts])
  const totalDel = allAttempts.length
  const successDel = allAttempts.filter(a => a.status === 'SUCCESS').length
  const rate = totalDel > 0 ? Math.round((successDel / totalDel) * 100) : 0

  // Simple timeline chart data
  const chartData = useMemo(() => {
    if (evts.length === 0) return []
    const buckets = new Map<string, { time: string; events: number; delivered: number }>()
    evts.forEach(e => {
      const d = new Date(e.createdAt)
      const key = `${d.getHours().toString().padStart(2,'0')}:${(Math.floor(d.getMinutes()/5)*5).toString().padStart(2,'0')}`
      if (!buckets.has(key)) buckets.set(key, { time: key, events: 0, delivered: 0 })
      const b = buckets.get(key)!
      b.events++
      b.delivered += e.deliveryAttempts.filter(a => a.status === 'SUCCESS').length > 0 ? 1 : 0
    })
    return Array.from(buckets.values()).sort((a, b) => a.time.localeCompare(b.time))
  }, [evts])

  if (loading || !app) return <div className="max-w-5xl mx-auto px-8 py-10"><div className="space-y-4">{[1,2,3].map(i => <div key={i} className="skeleton h-20 rounded-xl" />)}</div></div>

  return (
    <div className="max-w-5xl mx-auto px-8 py-8 anim-fade-up">
      <button onClick={() => nav('/')} className="btn-ghost flex items-center gap-1.5 -ml-3 mb-5 text-[13px]">
        <ArrowLeft size={14} /> Applications
      </button>

      <div className="flex items-start justify-between mb-2">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-accent/8 flex items-center justify-center border border-accent/15">
            <span className="text-accent font-semibold">{app.name[0].toUpperCase()}</span>
          </div>
          <div>
            <h1 className="text-xl font-semibold">{app.name}</h1>
            <div className="flex items-center gap-4 mt-0.5">
              <CopyButton text={app.apiKey} label={`${app.apiKey.slice(0, 22)}...`} />
              <Mono>ID: {app.id.slice(0, 8)}</Mono>
            </div>
          </div>
        </div>
        <button onClick={async () => { if (!confirm('Delete this application?')) return; await apps.delete(appId!); toast('Deleted'); nav('/') }} className="btn-danger flex items-center gap-1.5 text-[13px]"><Trash2 size={12} /> Delete</button>
      </div>

      {/* Overview section */}
      <div className="grid grid-cols-3 gap-4 mt-8 mb-8">
        {/* Metrics card */}
        <div className="card p-5 col-span-1">
          <div className="space-y-5">
            <Metric label="Events" value={evtPage.total} />
            <Metric label="Delivery rate" value={`${rate}%`} sub={`${successDel} / ${totalDel}`}
              accent={rate >= 90 ? 'text-status-success' : rate >= 50 ? 'text-status-warning' : totalDel > 0 ? 'text-status-error' : ''} />
            <Metric label="Endpoints" value={eps.filter(e => e.status === 'ACTIVE').length} sub={`${eps.length} total · ${ets.length} event types`} />
          </div>
        </div>

        {/* Chart card */}
        <div className="card p-5 col-span-2">
          <p className="text-text-muted text-2xs uppercase tracking-wider font-medium mb-4">Event activity</p>
          {chartData.length > 1 ? (
            <ResponsiveContainer width="100%" height={150}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="eventGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#818cf8" stopOpacity={0.3} />
                    <stop offset="100%" stopColor="#818cf8" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="deliveredGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#34d399" stopOpacity={0.3} />
                    <stop offset="100%" stopColor="#34d399" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="time" tick={{ fill: '#505571', fontSize: 10 }} axisLine={false} tickLine={false} />
                <YAxis hide />
                <Tooltip contentStyle={{ background: '#12141d', border: '1px solid #262b3d', borderRadius: 8, fontSize: 12 }}
                  labelStyle={{ color: '#8b90a8' }} itemStyle={{ color: '#e4e6ef' }} />
                <Area type="monotone" dataKey="events" stroke="#818cf8" fill="url(#eventGrad)" strokeWidth={2} name="Events" />
                <Area type="monotone" dataKey="delivered" stroke="#34d399" fill="url(#deliveredGrad)" strokeWidth={2} name="Delivered" />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[150px] flex items-center justify-center text-text-muted text-xs">
              Send a few events to see activity data
            </div>
          )}
          {totalDel > 0 && <div className="mt-4"><StatusBar counts={statusCounts} /></div>}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center justify-between mb-6">
        <Tabs active={tab} onChange={k => setTab(k as TabKey)} tabs={[
          { key: 'events', label: 'Events', count: evtPage.total },
          { key: 'endpoints', label: 'Endpoints', count: eps.length },
          { key: 'eventTypes', label: 'Event types', count: ets.length },
        ]} />
        {tab === 'events' && (
          <button onClick={() => setAutoRefresh(!autoRefresh)}
            className={clsx('btn-ghost flex items-center gap-1.5 text-2xs', autoRefresh && 'text-status-success')}>
            <RefreshCw size={12} className={autoRefresh ? 'animate-spin' : ''} style={autoRefresh ? { animationDuration: '3s' } : {}} />
            {autoRefresh ? 'Live' : 'Auto-refresh'}
          </button>
        )}
      </div>

      {tab === 'events' && <EventsTab appId={appId!} app={app} events={evts} ets={ets} page={evtPage} onReload={loadAll} />}
      {tab === 'endpoints' && <EndpointsTab appId={appId!} endpoints={eps} eventTypes={ets} onReload={() => loadAll(evtPage.current)} />}
      {tab === 'eventTypes' && <EventTypesTab appId={appId!} eventTypes={ets} onReload={() => loadAll(evtPage.current)} />}
    </div>
  )
}

/* ── Events Tab ── */
function EventsTab({ appId, app, events: evts, ets, page, onReload }: { appId: string; app: App; events: Event[]; ets: EventType[]; page: { total: number; pages: number; current: number }; onReload: (p?: number) => void }) {
  const [showTest, setShowTest] = useState(false)
  const [testType, setTestType] = useState('')
  const [testPayload, setTestPayload] = useState('{\n  "orderId": "12345",\n  "amount": 99.99\n}')
  const [sending, setSending] = useState(false)
  const toast = useToast()

  const sendTest = async () => {
    if (!testType) return; setSending(true)
    try {
      const res = await fetch('/api/events/ingest', { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-API-Key': app.apiKey }, body: JSON.stringify({ eventType: testType, payload: JSON.parse(testPayload) }) })
      if (!res.ok) throw new Error((await res.json()).error || 'Failed')
      toast('Test event sent'); setShowTest(false); onReload()
    } catch (err: any) { alert(err.message) } finally { setSending(false) }
  }

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button onClick={() => { setShowTest(true); if (ets.length) setTestType(ets[0].name) }} className="btn-secondary flex items-center gap-1.5 text-[13px]"><Send size={13} /> Send test event</button>
      </div>
      <Modal open={showTest} onClose={() => setShowTest(false)} title="Send test event">
        <div className="space-y-4">
          <div><label className="label">Event type</label>
            <select value={testType} onChange={e => setTestType(e.target.value)} className="input-field">
              {ets.length === 0 && <option value="">Create an event type first</option>}
              {ets.map(et => <option key={et.id} value={et.name}>{et.name}</option>)}
            </select></div>
          <div><label className="label">Payload (JSON)</label>
            <textarea value={testPayload} onChange={e => setTestPayload(e.target.value)} className="input-field font-mono text-xs h-32 resize-none" /></div>
          <div className="flex gap-2 justify-end">
            <button onClick={() => setShowTest(false)} className="btn-ghost">Cancel</button>
            <button onClick={sendTest} disabled={sending || !testType} className="btn-primary">{sending ? 'Sending...' : 'Send event'}</button>
          </div>
        </div>
      </Modal>
      {evts.length === 0 ? (
        <EmptyState icon={<Zap size={20} strokeWidth={1.5} className="text-text-muted" />} message="No events yet. Send one using the button above or the API." />
      ) : (
        <>
          <Table headers={['Event', 'Type', 'Delivery', 'Time']}>
            {evts.map(evt => {
              const a = evt.deliveryAttempts
              const s = a.some(x => x.status === 'SUCCESS') ? 'SUCCESS' : a.some(x => x.status === 'PENDING') ? 'PENDING' : a.some(x => x.status === 'FAILED' || x.status === 'EXHAUSTED') ? 'FAILED' : 'PENDING'
              return (
                <tr key={evt.id} className="group hover:bg-surface-2/80 transition-colors anim-fade-up">
                  <td className="py-3 px-4 first:pl-0">
                    <Link to={`/app/${appId}/event/${evt.id}`} className="flex items-center gap-2 hover:text-accent-bright transition-colors">
                      <Zap size={12} className="text-accent shrink-0" />
                      <span className="font-mono text-2xs group-hover:text-accent-bright">{evt.id.slice(0, 12)}</span>
                    </Link></td>
                  <td className="py-3 px-4"><span className="code-tag">{evt.eventType}</span></td>
                  <td className="py-3 px-4"><div className="flex items-center gap-2"><StatusBadge status={s} /><span className="text-text-muted text-2xs tabular-nums">{a.length}</span></div></td>
                  <td className="py-3 px-4"><TimeAgo iso={evt.createdAt} /></td>
                </tr>
              )
            })}
          </Table>
          <Pagination current={page.current} total={page.pages} onChange={p => onReload(p)} />
        </>
      )}
    </div>
  )
}

/* ── Endpoints Tab ── */
function EndpointsTab({ appId, endpoints: eps, eventTypes: ets, onReload }: { appId: string; endpoints: Endpoint[]; eventTypes: EventType[]; onReload: () => void }) {
  const [showCreate, setShowCreate] = useState(false); const [url, setUrl] = useState(''); const [desc, setDesc] = useState('')
  const [selEts, setSelEts] = useState<Set<string>>(new Set()); const [creating, setCreating] = useState(false)
  const [editSubs, setEditSubs] = useState<string | null>(null); const [editSelEts, setEditSelEts] = useState<Set<string>>(new Set())
  const toast = useToast()
  const toggle = (set: Set<string>, setFn: (s: Set<string>) => void, id: string) => { const n = new Set(set); n.has(id) ? n.delete(id) : n.add(id); setFn(n) }

  const create = async (e: React.FormEvent) => {
    e.preventDefault(); if (!url.trim()) return; setCreating(true)
    try { const ep = await endpoints.create(appId, url.trim(), desc.trim() || undefined); if (selEts.size > 0) await endpoints.setSubscriptions(appId, ep.id, Array.from(selEts)); setUrl(''); setDesc(''); setSelEts(new Set()); setShowCreate(false); toast('Endpoint created'); onReload() } finally { setCreating(false) }
  }

  return (
    <div>
      <div className="flex justify-end mb-4"><button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-1.5 text-[13px]"><Plus size={14} /> Add endpoint</button></div>
      {showCreate && (
        <form onSubmit={create} className="card p-5 mb-4 space-y-4 anim-fade-up">
          <div><label className="label">URL</label><input value={url} onChange={e => setUrl(e.target.value)} className="input-field" placeholder="https://your-server.com/webhooks" required /></div>
          <div><label className="label">Description</label><input value={desc} onChange={e => setDesc(e.target.value)} className="input-field" placeholder="Production handler" /></div>
          {ets.length > 0 && (<div><label className="label">Subscribe to event types</label><div className="space-y-1 mt-1">
            {ets.map(et => (<button key={et.id} type="button" onClick={() => toggle(selEts, setSelEts, et.id)} className="flex items-center gap-2 w-full text-left px-2.5 py-2 rounded-lg hover:bg-surface-3 transition-colors">
              {selEts.has(et.id) ? <CheckSquare size={15} className="text-accent" /> : <Square size={15} className="text-text-muted" />}<span className="code-tag">{et.name}</span></button>))}
          </div></div>)}
          <div className="flex gap-2"><button type="submit" disabled={creating} className="btn-primary text-[13px]">{creating ? 'Creating...' : 'Add'}</button><button type="button" onClick={() => setShowCreate(false)} className="btn-ghost text-[13px]">Cancel</button></div>
        </form>
      )}
      {eps.length === 0 ? (<EmptyState icon={<LinkIcon size={20} strokeWidth={1.5} className="text-text-muted" />} message="No endpoints. Add one to receive webhooks." />) : (
        <div className="space-y-3 stagger">
          {eps.map(ep => (
            <div key={ep.id} className="card px-5 py-4 anim-fade-up">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-start gap-3 min-w-0">
                  <div className="w-8 h-8 rounded-lg bg-surface-3 flex items-center justify-center shrink-0 border border-border-subtle mt-0.5"><LinkIcon size={14} className="text-text-muted" /></div>
                  <div className="min-w-0"><p className="text-sm font-medium flex items-center gap-2.5 flex-wrap"><span className="font-mono text-xs truncate">{ep.url}</span><StatusBadge status={ep.status} /></p>
                    {ep.description && <p className="text-text-muted text-2xs mt-0.5">{ep.description}</p>}</div>
                </div>
                <div className="flex gap-1 shrink-0">
                  <button onClick={() => { setEditSubs(editSubs === ep.id ? null : ep.id); setEditSelEts(new Set(ep.subscribedEventTypes.map(e => e.id))) }} className="btn-ghost text-2xs">Subscriptions</button>
                  <button onClick={async () => { await endpoints.update(appId, ep.id, { status: ep.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE' }); toast(ep.status === 'ACTIVE' ? 'Paused' : 'Resumed'); onReload() }} className="btn-ghost text-2xs">{ep.status === 'ACTIVE' ? 'Pause' : 'Resume'}</button>
                </div>
              </div>
              <div className="flex items-center gap-4 ml-11"><CopyButton text={ep.secret} label="Signing secret" /><span className="text-text-muted text-2xs tabular-nums">{ep.subscribedEventTypes.length} sub{ep.subscribedEventTypes.length !== 1 && 's'}</span></div>
              {editSubs === ep.id && (
                <div className="mt-4 pt-4 border-t border-border-subtle anim-fade-up">
                  <p className="label mb-2">Event type subscriptions</p>
                  {ets.length === 0 ? <p className="text-text-muted text-xs">No event types yet.</p> : (
                    <div className="space-y-1">{ets.map(et => (<button key={et.id} onClick={() => toggle(editSelEts, setEditSelEts, et.id)} className="flex items-center gap-2 w-full text-left px-2.5 py-2 rounded-lg hover:bg-surface-3 transition-colors">
                      {editSelEts.has(et.id) ? <CheckSquare size={15} className="text-accent" /> : <Square size={15} className="text-text-muted" />}<span className="code-tag">{et.name}</span></button>))}</div>
                  )}
                  <div className="flex gap-2 mt-3">
                    <button onClick={async () => { await endpoints.setSubscriptions(appId, ep.id, Array.from(editSelEts)); toast('Subscriptions updated'); setEditSubs(null); onReload() }} className="btn-primary text-[13px]">Save</button>
                    <button onClick={() => setEditSubs(null)} className="btn-ghost text-[13px]">Cancel</button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/* ── Event Types Tab ── */
function EventTypesTab({ appId, eventTypes: ets, onReload }: { appId: string; eventTypes: EventType[]; onReload: () => void }) {
  const [show, setShow] = useState(false); const [name, setName] = useState(''); const [desc, setDesc] = useState(''); const [creating, setCreating] = useState(false); const toast = useToast()
  const create = async (e: React.FormEvent) => { e.preventDefault(); if (!name.trim()) return; setCreating(true); try { await eventTypes.create(appId, name.trim(), desc.trim() || undefined); setName(''); setDesc(''); setShow(false); toast('Event type created'); onReload() } finally { setCreating(false) } }

  return (
    <div>
      <div className="flex justify-end mb-4"><button onClick={() => setShow(true)} className="btn-primary flex items-center gap-1.5 text-[13px]"><Plus size={14} /> Add event type</button></div>
      {show && (
        <form onSubmit={create} className="card p-5 mb-4 space-y-3 anim-fade-up">
          <div><label className="label">Name</label><input value={name} onChange={e => setName(e.target.value)} className="input-field font-mono text-xs" placeholder="order.created" required /><p className="text-text-muted text-2xs mt-1">Dotted lowercase format</p></div>
          <div><label className="label">Description</label><input value={desc} onChange={e => setDesc(e.target.value)} className="input-field" placeholder="Fired when a new order is placed" /></div>
          <div className="flex gap-2"><button type="submit" disabled={creating} className="btn-primary text-[13px]">{creating ? 'Creating...' : 'Add'}</button><button type="button" onClick={() => setShow(false)} className="btn-ghost text-[13px]">Cancel</button></div>
        </form>
      )}
      {ets.length === 0 ? (<EmptyState icon={<Zap size={20} strokeWidth={1.5} className="text-text-muted" />} message="No event types. Create one like order.created." />) : (
        <Table headers={['Name', 'Description', 'Created', '']}>
          {ets.map(et => (
            <tr key={et.id} className="hover:bg-surface-2/80 transition-colors anim-fade-up">
              <td className="py-3 px-4 first:pl-0"><span className="code-tag">{et.name}</span></td>
              <td className="py-3 px-4 text-text-secondary text-sm">{et.description || <span className="text-text-muted">—</span>}</td>
              <td className="py-3 px-4"><TimeAgo iso={et.createdAt} /></td>
              <td className="py-3 px-4 text-right"><button onClick={async () => { if (!confirm('Delete?')) return; await eventTypes.delete(appId, et.id); toast('Deleted'); onReload() }} className="text-text-muted hover:text-status-error transition-colors p-1.5 rounded-lg hover:bg-status-error/10"><Trash2 size={13} /></button></td>
            </tr>
          ))}
        </Table>
      )}
    </div>
  )
}
