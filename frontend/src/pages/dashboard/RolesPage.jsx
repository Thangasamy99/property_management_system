import { useEffect, useState } from 'react'
import { apiFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useSearchParams } from 'react-router-dom'
import Modal from '../../components/Modal'

// Map backend permission modules to the panel's display groups
const MODULE_GROUPS = [
  ['Identity', ['AUTH', 'USER', 'ROLE']],
  ['Property', ['PROPERTY']],
  ['Land', ['LAND']],
  ['Finance', ['FINANCE', 'PAYMENT', 'EXPENSE', 'FINANCIAL']],
  ['Documents', ['DOCUMENT']],
  ['Workflow', ['WORKFLOW']],
  ['Reports', ['REPORT']],
  ['Administration', ['SYSTEM', 'SETTINGS', 'AUDIT']],
]

function groupModules(moduleMap) {
  const used = new Set()
  const groups = MODULE_GROUPS.map(([label, keys]) => {
    const perms = keys.flatMap((k) => moduleMap[k] ?? [])
    perms.forEach((p) => used.add(p.code))
    return [label, perms]
  })
  const rest = Object.entries(moduleMap)
    .filter(([k]) => !MODULE_GROUPS.some(([, keys]) => keys.includes(k)))
    .flatMap(([, perms]) => perms.filter((p) => !used.has(p.code)))
  if (rest.length) groups.push(['Other', rest])
  return groups
}

function RoleFormModal({ role, onClose, onSaved }) {
  const isEdit = !!role
  const [form, setForm] = useState({ code: role?.code ?? '', name: role?.name ?? '', description: role?.description ?? '' })
  const [state, setState] = useState({ loading: false, error: '' })

  useEffect(() => {
    setForm({ code: role?.code ?? '', name: role?.name ?? '', description: role?.description ?? '' })
    setState({ loading: false, error: '' })
  }, [role])

  const submit = async (e) => {
    e.preventDefault()
    setState({ loading: true, error: '' })
    const res = await apiFetch(isEdit ? `/roles/${role.id}` : '/roles', {
      method: isEdit ? 'PUT' : 'POST',
      body: JSON.stringify(form),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      onSaved?.()
      onClose()
    } else {
      setState({ loading: false, error: data.message ?? 'Failed to save role' })
    }
  }

  return (
    <Modal open onClose={onClose} title={isEdit ? `Edit Role — ${role.code}` : 'New Role'}>
      <form onSubmit={submit} className="space-y-4">
        {state.error && <p className="rounded-xl bg-red-50 px-4 py-2.5 text-sm text-red-600 ring-1 ring-red-200">{state.error}</p>}
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-600">Role Code</label>
          <input
            required
            disabled={isEdit}
            placeholder="e.g. SUPERVISOR"
            value={form.code}
            onChange={(e) => setForm((f) => ({ ...f, code: e.target.value.toUpperCase().replace(/\s/g, '_') }))}
            className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 font-mono text-sm uppercase outline-none transition-all focus:border-pcc-500 focus:bg-white focus:ring-4 focus:ring-pcc-500/15 disabled:opacity-60"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-600">Role Name</label>
          <input
            required
            placeholder="e.g. Supervisor"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm outline-none transition-all focus:border-pcc-500 focus:bg-white focus:ring-4 focus:ring-pcc-500/15"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-600">Description</label>
          <textarea
            rows={2}
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm outline-none transition-all focus:border-pcc-500 focus:bg-white focus:ring-4 focus:ring-pcc-500/15"
          />
        </div>
        <button
          type="submit"
          disabled={state.loading}
          className="w-full rounded-xl bg-pcc-700 hover:bg-pcc-800 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors disabled:opacity-60"
        >
          {state.loading ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Role'}
        </button>
      </form>
    </Modal>
  )
}

function PermissionEditorModal({ role, onClose, onSaved }) {
  const [modules, setModules] = useState(null)
  const [selected, setSelected] = useState(new Set(role.permissions))
  const [state, setState] = useState({ loading: false, error: '' })

  useEffect(() => {
    setSelected(new Set(role.permissions))
    apiFetch('/permissions/modules')
      .then(async (r) => r.ok && setModules(await r.json()))
      .catch(() => setState((s) => ({ ...s, error: 'Failed to load permissions' })))
  }, [role])

  const toggle = (code) =>
    setSelected((s) => {
      const next = new Set(s)
      next.has(code) ? next.delete(code) : next.add(code)
      return next
    })

  const toggleGroup = (perms) => {
    const allOn = perms.every((p) => selected.has(p.code))
    setSelected((s) => {
      const next = new Set(s)
      perms.forEach((p) => (allOn ? next.delete(p.code) : next.add(p.code)))
      return next
    })
  }

  const save = async () => {
    setState({ loading: true, error: '' })
    const res = await apiFetch(`/roles/${role.id}/permissions`, {
      method: 'PUT',
      body: JSON.stringify({ permissionCodes: [...selected] }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      onSaved?.()
      onClose()
    } else {
      setState({ loading: false, error: data.message ?? 'Failed to save permissions' })
    }
  }

  return (
    <Modal open onClose={onClose} title={`Permissions — ${role.name}`} width="max-w-2xl">
      {state.error && <p className="mb-4 rounded-xl bg-red-50 px-4 py-2.5 text-sm text-red-600 ring-1 ring-red-200">{state.error}</p>}
      {!modules ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => <div key={i} className="h-16 rounded-xl bg-slate-100 animate-shimmer" style={{ backgroundImage: 'linear-gradient(90deg,#f1f5f9 25%,#e2e8f0 50%,#f1f5f9 75%)', backgroundSize: '400px 100%' }} />)}
        </div>
      ) : (
        <div className="max-h-[55vh] space-y-5 overflow-y-auto pr-1">
          {groupModules(modules).map(([label, perms]) =>
            perms.length === 0 ? null : (
              <div key={label}>
                <div className="mb-2 flex items-center justify-between">
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">{label}</p>
                  <button type="button" onClick={() => toggleGroup(perms)} className="text-[11px] font-semibold text-pcc-600 hover:text-pcc-800">
                    {perms.every((p) => selected.has(p.code)) ? 'Deselect all' : 'Select all'}
                  </button>
                </div>
                <div className="grid gap-1.5 sm:grid-cols-2">
                  {perms.map((p) => (
                    <button
                      type="button"
                      key={p.code}
                      onClick={() => toggle(p.code)}
                      title={p.description}
                      className={`flex items-center gap-2.5 rounded-lg border px-3 py-2 text-left transition-all duration-200 ${
                        selected.has(p.code)
                          ? 'border-pcc-400 bg-pcc-50 ring-1 ring-pcc-300'
                          : 'border-slate-200 hover:border-pcc-200 hover:bg-slate-50'
                      }`}
                    >
                      <span className={`flex h-4 w-4 shrink-0 items-center justify-center rounded border transition-all ${selected.has(p.code) ? 'border-pcc-600 bg-pcc-600 text-white' : 'border-slate-300'}`}>
                        {selected.has(p.code) && (
                          <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="3">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                          </svg>
                        )}
                      </span>
                      <span className="text-xs font-medium text-slate-700">{p.code}</span>
                    </button>
                  ))}
                </div>
              </div>
            ),
          )}
        </div>
      )}
      <div className="mt-5 flex items-center justify-between gap-3">
        <p className="text-xs text-slate-400">{selected.size} permission{selected.size === 1 ? '' : 's'} selected</p>
        <button
          onClick={save}
          disabled={state.loading || !modules}
          className="rounded-xl bg-pcc-700 hover:bg-pcc-800 px-6 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors disabled:opacity-60"
        >
          {state.loading ? 'Saving…' : 'Save Permissions'}
        </button>
      </div>
    </Modal>
  )
}

function RoleUsersModal({ role, onClose }) {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)

  useEffect(() => {
    setPage(0)
    setData(null)
  }, [role])

  useEffect(() => {
    if (!role) return
    apiFetch(`/roles/${role.id}/users?page=${page}&size=8`)
      .then(async (r) => r.ok && setData(await r.json()))
      .catch(() => {})
  }, [role, page])

  if (!role) return null
  return (
    <Modal open onClose={onClose} title={`Users — ${role.name}`} width="max-w-lg">
      {!data ? (
        <div className="space-y-2">{Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-12 rounded-xl bg-slate-100 animate-shimmer" style={{ backgroundImage: 'linear-gradient(90deg,#f1f5f9 25%,#e2e8f0 50%,#f1f5f9 75%)', backgroundSize: '400px 100%' }} />)}</div>
      ) : (
        <>
          <ul className="space-y-2">
            {data.content.length === 0 && <li className="py-6 text-center text-sm text-slate-400">No users have this role</li>}
            {data.content.map((u) => (
              <li key={u.id} className="flex items-center gap-3 rounded-xl border border-slate-100 px-4 py-3 animate-fade-in">
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-pcc-500 to-pcc-300 text-xs font-bold text-white">
                  {u.firstName?.[0]}{u.lastName?.[0]}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-slate-800">{u.firstName} {u.lastName}</p>
                  <p className="truncate text-xs text-slate-400">@{u.username} · {u.employeeCode}</p>
                </div>
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${u.accountStatus === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                  {u.accountStatus}
                </span>
              </li>
            ))}
          </ul>
          {data.totalPages > 1 && (
            <div className="mt-4 flex justify-center gap-2 text-xs">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="rounded-lg border border-slate-200 px-3 py-1.5 disabled:opacity-40">Prev</button>
              <button disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)} className="rounded-lg border border-slate-200 px-3 py-1.5 disabled:opacity-40">Next</button>
            </div>
          )}
        </>
      )}
    </Modal>
  )
}

function RoleCard({ role, index, userCount, canEdit, canAssignPerm, onEdit, onPermissions, onUsers }) {
  const [expanded, setExpanded] = useState(false)
  const shown = expanded ? role.permissions : role.permissions.slice(0, 8)

  return (
    <div
      className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition-shadow duration-200 hover:shadow-md animate-fade-in"
      style={{ animationDelay: `${index * 90}ms` }}
    >
      <div className={`px-6 py-4 text-white bg-pcc-900`}>
        <div className="flex items-start justify-between gap-2">
          <div>
            <h3 className="font-bold">{role.name}</h3>
            <p className="text-xs opacity-80">
              {role.code}{userCount != null && ` · ${userCount} user${userCount === 1 ? '' : 's'}`}
            </p>
          </div>
          <div className="flex gap-1">
            {canEdit && (
              <button onClick={() => onEdit(role)} title="Edit role" className="rounded-lg bg-white/15 p-1.5 transition-colors hover:bg-white/30">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
            )}
            {canAssignPerm && (
              <button onClick={() => onPermissions(role)} title="Edit permissions" className="rounded-lg bg-white/15 p-1.5 transition-colors hover:bg-white/30">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                </svg>
              </button>
            )}
          </div>
        </div>
      </div>
      <div className="p-6">
        <p className="text-sm text-slate-500">{role.description}</p>
        <div className="mt-4 flex items-center justify-between">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
            {role.permissions.length} permissions
          </p>
          <button onClick={() => onUsers(role)} className="text-xs font-semibold text-pcc-600 transition-colors hover:text-pcc-800">
            View users →
          </button>
        </div>
        <div className="mt-2 flex flex-wrap gap-1.5">
          {shown.sort().map((p) => (
            <span key={p} className="rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600 animate-fade-in">{p}</span>
          ))}
        </div>
        {role.permissions.length > 8 && (
          <button onClick={() => setExpanded((v) => !v)} className="mt-3 text-xs font-semibold text-pcc-600 transition-colors hover:text-pcc-800">
            {expanded ? 'Show less ↑' : `Show all ${role.permissions.length} ↓`}
          </button>
        )}
      </div>
    </div>
  )
}

export default function RolesPage() {
  const { hasPermission } = useAuth()
  const [roles, setRoles] = useState(null)
  const [userCounts, setUserCounts] = useState({})
  const [error, setError] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editRole, setEditRole] = useState(null)
  const [permRole, setPermRole] = useState(null)
  const [usersRole, setUsersRole] = useState(null)

  const canCreate = hasPermission('ROLE_CREATE')
  const canEdit = hasPermission('ROLE_UPDATE')
  const canAssignPerm = hasPermission('ROLE_ASSIGN_PERMISSION')
  const [searchParams, setSearchParams] = useSearchParams()

  // deep-link: /roles?new=1 opens the create dialog (sidebar "+" action)
  useEffect(() => {
    if (searchParams.get('new') === '1' && canCreate) {
      setCreateOpen(true)
      setSearchParams({}, { replace: true })
    }
  }, [searchParams, canCreate, setSearchParams])

  const load = () =>
    apiFetch('/roles')
      .then(async (res) => {
        if (!res.ok) throw new Error(`Request failed (${res.status})`)
        const list = await res.json()
        setRoles(list)
        const counts = await Promise.all(
          list.map((r) =>
            apiFetch(`/roles/${r.id}/users?size=1`)
              .then(async (x) => [r.id, x.ok ? (await x.json()).totalElements : null])
              .catch(() => [r.id, null]),
          ),
        )
        setUserCounts(Object.fromEntries(counts))
      })
      .catch((e) => setError(e.message))

  useEffect(() => {
    load()
  }, [])

  return (
    <div className="mx-auto max-w-6xl animate-slide-up">
      <div className="mb-6 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Roles &amp; Permissions</h2>
          <p className="text-sm text-slate-500">Role-permission mapping is configured in the database — no code changes required.</p>
        </div>
        {canCreate && (
          <button
            onClick={() => setCreateOpen(true)}
            className="flex shrink-0 items-center gap-2 rounded-xl bg-pcc-700 hover:bg-pcc-800 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
            </svg>
            <span className="hidden sm:inline">New Role</span>
            <span className="sm:hidden">New</span>
          </button>
        )}
      </div>

      {error && <p className="rounded-xl bg-red-50 p-4 text-sm text-red-600">{error}</p>}

      {!roles && !error && (
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-56 rounded-2xl bg-slate-100 animate-shimmer"
              style={{ backgroundImage: 'linear-gradient(90deg,#f1f5f9 25%,#e2e8f0 50%,#f1f5f9 75%)', backgroundSize: '400px 100%' }} />
          ))}
        </div>
      )}

      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {roles?.map((role, i) => (
          <RoleCard
            key={role.code} role={role} index={i} userCount={userCounts[role.id]}
            canEdit={canEdit} canAssignPerm={canAssignPerm}
            onEdit={setEditRole} onPermissions={setPermRole} onUsers={setUsersRole}
          />
        ))}
      </div>

      {createOpen && <RoleFormModal role={null} onClose={() => setCreateOpen(false)} onSaved={load} />}
      {editRole && <RoleFormModal role={editRole} onClose={() => setEditRole(null)} onSaved={load} />}
      {permRole && <PermissionEditorModal role={permRole} onClose={() => setPermRole(null)} onSaved={load} />}
      {usersRole && <RoleUsersModal role={usersRole} onClose={() => setUsersRole(null)} />}
    </div>
  )
}
