import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, ChevronRight, Key, Layers } from 'lucide-react'
import { apps, App } from '../api/client'
import { CopyButton, EmptyState, TimeAgo, useToast } from '../components/ui'

export default function Applications() {
  const [list, setList] = useState<App[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [name, setName] = useState('')
  const [creating, setCreating] = useState(false)
  const nav = useNavigate()
  const toast = useToast()

  const load = async () => { try { setList(await apps.list()) } finally { setLoading(false) } }
  useEffect(() => { load() }, [])

  const create = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    setCreating(true)
    try { await apps.create(name.trim()); setName(''); setShowCreate(false); toast('Application created'); load() }
    finally { setCreating(false) }
  }

  return (
    <div className="max-w-3xl mx-auto px-8 py-10 anim-fade-up">
      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="text-xl font-semibold">Applications</h1>
          <p className="text-text-muted text-sm mt-1">Manage your webhook applications and API keys.</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-1.5 text-[13px]">
          <Plus size={15} /> New app
        </button>
      </div>

      {showCreate && (
        <form onSubmit={create} className="card p-5 mb-6 anim-fade-up">
          <label className="label">Application name</label>
          <div className="flex gap-3">
            <input autoFocus value={name} onChange={e => setName(e.target.value)} className="input-field flex-1" placeholder="My E-Commerce Store" />
            <button type="submit" disabled={creating} className="btn-primary shrink-0">{creating ? 'Creating...' : 'Create'}</button>
            <button type="button" onClick={() => setShowCreate(false)} className="btn-ghost shrink-0">Cancel</button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="space-y-3">{[1,2,3].map(i => <div key={i} className="skeleton h-[76px] rounded-xl" />)}</div>
      ) : list.length === 0 ? (
        <EmptyState icon={<Layers size={22} strokeWidth={1.5} className="text-text-muted" />}
          message="No applications yet. Create one to start sending webhooks."
          action={<button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-1.5 text-[13px]"><Plus size={15} /> Create your first app</button>} />
      ) : (
        <div className="space-y-3 stagger">
          {list.map(app => (
            <button key={app.id} onClick={() => nav(`/app/${app.id}`)}
              className="card-interactive w-full px-5 py-4 flex items-center justify-between text-left group anim-fade-up">
              <div className="min-w-0">
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-8 h-8 rounded-lg bg-accent/8 flex items-center justify-center border border-accent/15 group-hover:bg-accent/12 transition-colors">
                    <span className="text-accent text-xs font-semibold">{app.name[0].toUpperCase()}</span>
                  </div>
                  <p className="font-medium text-sm group-hover:text-accent-bright transition-colors">{app.name}</p>
                </div>
                <div className="flex items-center gap-4 ml-11">
                  <span className="flex items-center gap-1.5 text-text-muted">
                    <Key size={11} className="opacity-40" />
                    <CopyButton text={app.apiKey} label={app.apiKey.slice(0, 20) + '...'} />
                  </span>
                  <TimeAgo iso={app.createdAt} />
                </div>
              </div>
              <ChevronRight size={16} className="text-text-muted group-hover:text-accent shrink-0 group-hover:translate-x-0.5 transition-all duration-200" />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
