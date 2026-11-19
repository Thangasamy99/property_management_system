import { useCallback, useEffect, useState } from 'react'
import { apiFetch } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import ConfirmDialog from '../../components/ConfirmDialog'
import { Plus, Eye, Pencil, ShieldCheck, KeyRound, Ban, CheckCircle } from 'lucide-react'

const EMPTY_FORM = { employeeCode: '', username: '', firstName: '', lastName: '', email: '', phone: '', password: '', roleCodes: [] }

function useRoles() {
  const [roles, setRoles] = useState([])
  useEffect(() => {
    apiFetch('/roles').then(async (r) => r.ok && setRoles(await r.json())).catch(() => {})
  }, [])
  return roles
}

function CreateEmployeeModal({ open, onClose, onCreated }) {
  const roles = useRoles()
  const [form, setForm] = useState(EMPTY_FORM)
  const [state, setState] = useState({ loading: false, error: '', tempPassword: null })

  const toggleRole = (code) =>
    setForm((f) => ({ ...f, roleCodes: f.roleCodes.includes(code) ? f.roleCodes.filter((c) => c !== code) : [...f.roleCodes, code] }))

  const submit = async (e) => {
    e.preventDefault()
    setState({ loading: true, error: '', tempPassword: null })
    const body = { ...form, password: form.password || null }
    const res = await apiFetch('/users', { method: 'POST', body: JSON.stringify(body) })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      setState({ loading: false, error: '', tempPassword: data.temporaryPassword })
      setForm(EMPTY_FORM)
      onCreated?.()
    } else {
      setState({ loading: false, error: data.message ?? 'Failed to create employee', tempPassword: null })
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New Employee" width="max-w-xl">
      {state.tempPassword ? (
        <div className="text-center">
          <span className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-emerald-600">
            <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </span>
          <h4 className="font-semibold text-slate-800">Employee created</h4>
          <p className="mt-1 text-sm text-slate-500">Share this temporary password — it must be changed at first login.</p>
          <p className="mt-3 select-all rounded-xl bg-slate-100 px-4 py-3 font-mono text-lg font-bold tracking-wider text-pcc-700">
            {state.tempPassword}
          </p>
          <button onClick={onClose} className="mt-5 w-full rounded-xl bg-pcc-600 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-pcc-700">
            Done
          </button>
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          {state.error && <p className="rounded-xl bg-red-50 px-4 py-2.5 text-sm text-red-600 ring-1 ring-red-200">{state.error}</p>}
          <div className="grid gap-4 sm:grid-cols-2">
            {[
              ['employeeCode', 'Employee Code', 'PCC-1002'],
              ['username', 'Username', 'jdoe'],
              ['firstName', 'First Name', 'John'],
              ['lastName', 'Last Name', 'Doe'],
              ['email', 'Email', 'jdoe@pcc.cm'],
              ['phone', 'Phone (optional)', '+2376XXXXXXXX'],
            ].map(([key, label, ph]) => (
              <div key={key}>
                <label className="mb-1 block text-xs font-medium text-slate-600">{label}</label>
                <input
                  required={key !== 'phone'}
                  type={key === 'email' ? 'email' : 'text'}
                  placeholder={ph}
                  value={form[key]}
                  onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm outline-none transition-all focus:border-pcc-500 focus:bg-white focus:ring-4 focus:ring-pcc-500/15"
                />
              </div>
            ))}
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-600">Password (optional — leave empty to generate)</label>
            <input
              type="text"
              placeholder="Min 8 characters"
              minLength={8}
              value={form.password}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm outline-none transition-all focus:border-pcc-500 focus:bg-white focus:ring-4 focus:ring-pcc-500/15"
            />
          </div>
          {roles.length > 0 && (
            <div>
              <p className="mb-2 text-xs font-medium text-slate-600">Assign roles</p>
              <div className="flex flex-wrap gap-2">
                {roles.map((r) => (
                  <button
                    type="button"
                    key={r.code}
                    onClick={() => toggleRole(r.code)}
                    className={`rounded-full px-3.5 py-1.5 text-xs font-semibold ring-1 transition-all duration-200 ${
                      form.roleCodes.includes(r.code)
                        ? 'bg-pcc-600 text-white ring-pcc-600 shadow-md shadow-pcc-500/30'
                        : 'bg-slate-50 text-slate-600 ring-slate-200 hover:bg-pcc-50 hover:text-pcc-700 hover:ring-pcc-300'
                    }`}
                  >
                    {r.name}
                  </button>
                ))}
              </div>
            </div>
          )}
          <button
            type="submit"
            disabled={state.loading}
            className="w-full rounded-xl bg-gradient-to-r from-pcc-700 to-pcc-500 py-3 text-sm font-semibold text-white shadow-lg shadow-pcc-500/25 transition-all hover:-translate-y-0.5 hover:shadow-xl disabled:opacity-60"
          >
            {state.loading ? 'Creating…' : 'Create Employee'}
          </button>
        </form>
      )}
    </Modal>
  )
}

function EditRolesModal({ user: target, onClose, onSaved }) {
  const roles = useRoles()
  const [selected, setSelected] = useState([])
  const [state, setState] = useState({ loading: false, error: '' })

  useEffect(() => {
    if (target) setSelected(target.roles.map((r) => r.code))
  }, [target])

  const save = async () => {
    setState({ loading: true, error: '' })
    const res = await apiFetch(`/users/${target.id}/roles`, {
      method: 'PUT',
      body: JSON.stringify({ roleCodes: selected }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      onSaved?.()
      onClose()
    } else {
      setState({ loading: false, error: data.message ?? 'Failed to update roles' })
    }
  }

  return (
    <Modal open={!!target} onClose={onClose} title={`Roles — ${target?.username ?? ''}`}>
      {state.error && <p className="mb-4 rounded-xl bg-red-50 px-4 py-2.5 text-sm text-red-600 ring-1 ring-red-200">{state.error}</p>}
      <div className="space-y-2">
        {roles.map((r) => {
          const checked = selected.includes(r.code)
          return (
            <button
              key={r.code}
              type="button"
              onClick={() => setSelected((s) => checked ? s.filter((c) => c !== r.code) : [...s, r.code])}
              className={`flex w-full items-center justify-between rounded-xl border px-4 py-3 text-left text-sm transition-all duration-200 ${
                checked ? 'border-pcc-400 bg-pcc-50 ring-1 ring-pcc-300' : 'border-slate-200 hover:border-pcc-200 hover:bg-slate-50'
              }`}
            >
              <span>
                <span className="block font-medium text-slate-800">{r.name}</span>
                <span className="block text-xs text-slate-400">{r.description}</span>
              </span>
              <span className={`flex h-5 w-5 items-center justify-center rounded-full border transition-all ${checked ? 'border-pcc-600 bg-pcc-600 text-white' : 'border-slate-300'}`}>
                {checked && (
                  <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="3">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                )}
              </span>
            </button>
          )
        })}
      </div>
      <button
        onClick={save}
        disabled={state.loading}
        className="mt-5 w-full rounded-xl bg-gradient-to-r from-pcc-700 to-pcc-500 py-2.5 text-sm font-semibold text-white shadow-lg shadow-pcc-500/25 transition-all hover:-translate-y-0.5 disabled:opacity-60"
      >
        {state.loading ? 'Saving…' : 'Save Roles'}
      </button>
    </Modal>
  )
}

function EditEmployeeModal({ user: target, onClose, onSaved }) {
  const [form, setForm] = useState(null)
  const [state, setState] = useState({ loading: false, error: '' })

  useEffect(() => {
    if (target) {
      setForm({
        employeeCode: target.employeeCode, username: target.username,
        firstName: target.firstName, lastName: target.lastName,
        email: target.email, phone: target.phone ?? '',
      })
      setState({ loading: false, error: '' })
    }
  }, [target])

  const submit = async (e) => {
    e.preventDefault()
    setState({ loading: true, error: '' })
    const res = await apiFetch(`/users/${target.id}`, { method: 'PUT', body: JSON.stringify(form) })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      onSaved?.()
      onClose()
    } else {
      setState({ loading: false, error: data.message ?? 'Failed to update employee' })
    }
  }

  return (
    <Modal open={!!target} onClose={onClose} title={`Edit — ${target?.username ?? ''}`} width="max-w-xl">
      {form && (
        <form onSubmit={submit} className="space-y-4">
          {state.error && <p className="rounded-xl bg-red-50 px-4 py-2.5 text-sm text-red-600 ring-1 ring-red-200">{state.error}</p>}
          <div className="grid gap-4 sm:grid-cols-2">
            {[
              ['employeeCode', 'Employee Code'], ['username', 'Username'],
              ['firstName', 'First Name'], ['lastName', 'Last Name'],
              ['email', 'Email'], ['phone', 'Phone'],
            ].map(([key, label]) => (
              <div key={key}>
                <label className="mb-1 block text-xs font-medium text-slate-600">{label}</label>
                <input
                  required={key !== 'phone'}
                  type={key === 'email' ? 'email' : 'text'}
                  minLength={key === 'username' ? 3 : 1}
                  value={form[key]}
                  onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm outline-none transition-all focus:border-pcc-500 focus:bg-white focus:ring-4 focus:ring-pcc-500/15"
                />
              </div>
            ))}
          </div>
          <button
            type="submit"
            disabled={state.loading}
            className="w-full rounded-xl bg-gradient-to-r from-pcc-700 to-pcc-500 py-2.5 text-sm font-semibold text-white shadow-lg shadow-pcc-500/25 transition-all hover:-translate-y-0.5 disabled:opacity-60"
          >
            {state.loading ? 'Saving…' : 'Save Changes'}
          </button>
        </form>
      )}
    </Modal>
  )
}

function ViewEmployeeModal({ user: target, onClose }) {
  if (!target) return null
  const rows = [
    ['Employee code', target.employeeCode],
    ['Username', `@${target.username}`],
    ['Full name', `${target.firstName} ${target.lastName}`],
    ['Email', target.email],
    ['Phone', target.phone ?? '—'],
    ['Status', target.accountStatus],
    ['Roles', target.roles.map((r) => r.name).join(', ') || 'none'],
    ['Last login', target.lastLoginAt ? new Date(target.lastLoginAt).toLocaleString() : 'Never'],
    ['Created', new Date(target.createdAt).toLocaleString()],
    ['Updated', new Date(target.updatedAt).toLocaleString()],
  ]
  return (
    <Modal open onClose={onClose} title="Employee Details">
      <div className="mb-5 flex items-center gap-4">
        <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-pcc-600 to-pcc-400 text-xl font-bold text-white">
          {target.firstName?.[0]}{target.lastName?.[0]}
        </span>
        <div>
          <p className="font-semibold text-slate-800">{target.firstName} {target.lastName}</p>
          <p className="text-sm text-slate-400">{target.email}</p>
        </div>
      </div>
      <dl className="space-y-2.5">
        {rows.map(([k, v]) => (
          <div key={k} className="flex justify-between gap-4 border-b border-slate-50 pb-2.5 text-sm last:border-0">
            <dt className="text-slate-400">{k}</dt>
            <dd className="text-right font-medium text-slate-700">{v}</dd>
          </div>
        ))}
      </dl>
    </Modal>
  )
}

export default function UsersPage() {
  const { hasPermission } = useAuth()
  const { toast } = useToast()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [allRoles, setAllRoles] = useState([])
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editRoles, setEditRoles] = useState(null)
  const [editUser, setEditUser] = useState(null)
  const [viewUser, setViewUser] = useState(null)
  const [confirmStatus, setConfirmStatus] = useState(null)
  const [confirmBusy, setConfirmBusy] = useState(false)
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (hasPermission('ROLE_VIEW')) {
      apiFetch('/roles').then(async (r) => r.ok && setAllRoles(await r.json())).catch(() => {})
    }
  }, [hasPermission])

  const load = useCallback(() => {
    setLoading(true)
    const q = search ? `&search=${encodeURIComponent(search)}` : ''
    apiFetch(`/users?page=${page}&size=10&sort=id${q}`)
      .then(async (res) => {
        if (!res.ok) throw new Error(`Request failed (${res.status})`)
        setData(await res.json())
        setError('')
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [page, search])

  useEffect(load, [load])

  const toggleStatus = async () => {
    const u = confirmStatus
    setConfirmBusy(true)
    const res = await apiFetch(`/users/${u.id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status: u.accountStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }),
    })
    const body = await res.json().catch(() => ({}))
    setConfirmBusy(false)
    setConfirmStatus(null)
    if (res.ok) {
      toast(`${u.username} ${u.accountStatus === 'ACTIVE' ? 'deactivated' : 'activated'} successfully`, 'success')
      load()
    } else {
      toast(body.message ?? 'Action failed', 'error')
    }
  }

  const doResetPassword = async (u) => {
    const res = await apiFetch(`/users/${u.id}/reset-password`, { method: 'POST' })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      setNotice(`Temporary password for ${u.username}: ${data.temporaryPassword}`)
      setTimeout(() => setNotice(''), 15000)
    } else {
      toast(data.message ?? 'Reset failed', 'error')
    }
  }

  const rows = (data?.content ?? []).filter((u) => {
    if (statusFilter && u.accountStatus !== statusFilter) return false
    if (roleFilter && !u.roles.some((r) => r.code === roleFilter)) return false
    return true
  })

  return (
    <div className="mx-auto max-w-6xl animate-slide-up">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Employees</h2>
          <p className="text-sm text-slate-500">
            {data ? `${data.totalElements} employee${data.totalElements === 1 ? '' : 's'} in the Identity Service` : 'Employee accounts'}
          </p>
        </div>
        <div className="flex flex-wrap gap-2.5">
          <input
            type="search"
            placeholder="Search name, username, email…"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0) }}
            className="w-full max-w-xs rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm outline-none transition-all focus:border-pcc-500 focus:ring-4 focus:ring-pcc-500/15"
          />
          {allRoles.length > 0 && (
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-600 outline-none transition-all focus:border-pcc-500 focus:ring-4 focus:ring-pcc-500/15"
            >
              <option value="">All roles</option>
              {allRoles.map((r) => <option key={r.code} value={r.code}>{r.name}</option>)}
            </select>
          )}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-600 outline-none transition-all focus:border-pcc-500 focus:ring-4 focus:ring-pcc-500/15"
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          {hasPermission('USER_CREATE') && (
            <button
              onClick={() => setCreateOpen(true)}
              className="flex shrink-0 items-center gap-2 rounded-xl bg-pcc-700 px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-pcc-700/20 transition-all hover:-translate-y-0.5 hover:bg-pcc-800"
            >
              <Plus className="h-4 w-4" strokeWidth={2.2} />
              <span className="hidden sm:inline">New Employee</span>
              <span className="sm:hidden">New</span>
            </button>
          )}
        </div>
      </div>

      {notice && (
        <div className="mb-4 flex items-center justify-between rounded-xl bg-pcc-50 px-4 py-3 text-sm font-medium text-pcc-800 ring-1 ring-pcc-200 animate-slide-up">
          <span className="select-all">{notice}</span>
          <button onClick={() => setNotice('')} className="ml-4 text-pcc-400 hover:text-pcc-700">✕</button>
        </div>
      )}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {error ? (
          <p className="p-8 text-center text-sm text-red-600">{error}</p>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wider text-slate-500">
                  <tr>
                    <th className="px-5 py-3.5">Employee</th>
                    <th className="px-5 py-3.5">Code</th>
                    <th className="hidden px-5 py-3.5 md:table-cell">Email</th>
                    <th className="hidden px-5 py-3.5 sm:table-cell">Roles</th>
                    <th className="px-5 py-3.5">Status</th>
                    <th className="hidden px-5 py-3.5 lg:table-cell">Created</th>
                    <th className="px-5 py-3.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {loading
                    ? Array.from({ length: 5 }).map((_, i) => (
                        <tr key={i}>
                          {Array.from({ length: 7 }).map((__, j) => (
                            <td key={j} className="px-5 py-4">
                              <div className="h-4 rounded-md bg-slate-100 animate-shimmer" style={{ backgroundImage: 'linear-gradient(90deg,#f1f5f9 25%,#e2e8f0 50%,#f1f5f9 75%)', backgroundSize: '400px 100%' }} />
                            </td>
                          ))}
                        </tr>
                      ))
                    : rows.map((u) => (
                        <tr key={u.id} className="transition-colors hover:bg-pcc-50/50">
                          <td className="px-5 py-3.5">
                            <div className="flex items-center gap-3">
                              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-pcc-500 to-pcc-300 text-xs font-bold text-white">
                                {u.firstName?.[0]}{u.lastName?.[0]}
                              </span>
                              <div>
                                <p className="font-medium text-slate-800">{u.firstName} {u.lastName}</p>
                                <p className="text-xs text-slate-400">@{u.username}</p>
                              </div>
                            </div>
                          </td>
                          <td className="px-5 py-3.5 font-mono text-xs text-slate-600">{u.employeeCode}</td>
                          <td className="hidden px-5 py-3.5 text-slate-600 md:table-cell">{u.email}</td>
                          <td className="hidden px-5 py-3.5 sm:table-cell">
                            <div className="flex flex-wrap gap-1">
                              {u.roles.map((r) => (
                                <span key={r.code} className="rounded-full bg-pcc-100 px-2 py-0.5 text-[11px] font-semibold text-pcc-700">{r.code}</span>
                              ))}
                              {u.roles.length === 0 && <span className="text-xs text-slate-300">none</span>}
                            </div>
                          </td>
                          <td className="px-5 py-3.5">
                            <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${
                              u.accountStatus === 'ACTIVE'
                                ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'
                                : 'bg-slate-100 text-slate-500 ring-1 ring-slate-200'
                            }`}>
                              <span className={`h-1.5 w-1.5 rounded-full ${u.accountStatus === 'ACTIVE' ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                              {u.accountStatus}
                            </span>
                          </td>
                          <td className="hidden px-5 py-3.5 text-xs text-slate-500 lg:table-cell">
                            {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                          </td>
                          <td className="px-5 py-3.5">
                            <div className="flex justify-end gap-0.5">
                              <button onClick={() => setViewUser(u)} title="View details"
                                className="rounded-lg p-2 text-slate-400 transition-all hover:bg-slate-100 hover:text-slate-600">
                                <Eye className="h-4.5 w-4.5 h-5 w-5" strokeWidth={1.8} />
                              </button>
                              {hasPermission('USER_UPDATE') && (
                                <button onClick={() => setEditUser(u)} title="Edit employee"
                                  className="rounded-lg p-2 text-slate-400 transition-all hover:bg-sky-50 hover:text-sky-600">
                                  <Pencil className="h-4.5 w-4.5 h-5 w-5" strokeWidth={1.8} />
                                </button>
                              )}
                              {hasPermission('USER_ASSIGN_ROLE') && (
                                <button onClick={() => setEditRoles(u)} title="Edit roles"
                                  className="rounded-lg p-2 text-slate-400 transition-all hover:bg-pcc-50 hover:text-pcc-600">
                                  <ShieldCheck className="h-4.5 w-4.5 h-5 w-5" strokeWidth={1.8} />
                                </button>
                              )}
                              {hasPermission('USER_UPDATE') && (
                                <button onClick={() => doResetPassword(u)} title="Reset password"
                                  className="rounded-lg p-2 text-slate-400 transition-all hover:bg-amber-50 hover:text-amber-600">
                                  <KeyRound className="h-4.5 w-4.5 h-5 w-5" strokeWidth={1.8} />
                                </button>
                              )}
                              {hasPermission('USER_DEACTIVATE') && (
                                <button onClick={() => setConfirmStatus(u)} title={u.accountStatus === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                                  className={`rounded-lg p-2 transition-all ${
                                    u.accountStatus === 'ACTIVE'
                                      ? 'text-slate-400 hover:bg-red-50 hover:text-red-500'
                                      : 'text-slate-400 hover:bg-emerald-50 hover:text-emerald-600'
                                  }`}>
                                  {u.accountStatus === 'ACTIVE'
                                    ? <Ban className="h-4.5 w-4.5 h-5 w-5" strokeWidth={1.8} />
                                    : <CheckCircle className="h-4.5 w-4.5 h-5 w-5" strokeWidth={1.8} />}
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between border-t border-slate-200 px-5 py-3 text-xs text-slate-500">
              <span>{data?.totalElements} employees · page {data?.page + 1} of {Math.max(data?.totalPages, 1)}</span>
              <div className="flex gap-2">
                <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}
                  className="rounded-lg border border-slate-200 px-3 py-1.5 font-medium transition-colors hover:bg-slate-50 disabled:opacity-40">Prev</button>
                <button disabled={page >= data?.totalPages - 1} onClick={() => setPage((p) => p + 1)}
                  className="rounded-lg border border-slate-200 px-3 py-1.5 font-medium transition-colors hover:bg-slate-50 disabled:opacity-40">Next</button>
              </div>
            </div>
          </>
        )}
      </div>

      <CreateEmployeeModal open={createOpen} onClose={() => setCreateOpen(false)} onCreated={() => { load(); toast('Employee created', 'success') }} />
      <EditEmployeeModal user={editUser} onClose={() => setEditUser(null)} onSaved={() => { load(); toast('Employee updated', 'success') }} />
      <ViewEmployeeModal user={viewUser} onClose={() => setViewUser(null)} />
      <EditRolesModal user={editRoles} onClose={() => setEditRoles(null)} onSaved={() => { load(); toast('Roles updated', 'success') }} />
      <ConfirmDialog
        open={!!confirmStatus}
        onClose={() => setConfirmStatus(null)}
        onConfirm={toggleStatus}
        loading={confirmBusy}
        danger={confirmStatus?.accountStatus === 'ACTIVE'}
        title={confirmStatus?.accountStatus === 'ACTIVE' ? 'Deactivate employee' : 'Activate employee'}
        confirmLabel={confirmStatus?.accountStatus === 'ACTIVE' ? 'Deactivate' : 'Activate'}
        message={confirmStatus?.accountStatus === 'ACTIVE'
          ? `Deactivate ${confirmStatus?.firstName} ${confirmStatus?.lastName} (@${confirmStatus?.username})? They will no longer be able to sign in.`
          : `Reactivate ${confirmStatus?.firstName} ${confirmStatus?.lastName} (@${confirmStatus?.username})?`}
      />
    </div>
  )
}
