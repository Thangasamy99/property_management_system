import { useState } from 'react'
import { apiFetch } from '../services/api'
import Modal from './Modal'

export default function ChangePasswordModal({ open, onClose }) {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' })
  const [state, setState] = useState({ loading: false, error: '', success: '' })

  const submit = async (e) => {
    e.preventDefault()
    setState({ loading: true, error: '', success: '' })
    if (form.newPassword !== form.confirm) {
      setState({ loading: false, error: 'Passwords do not match', success: '' })
      return
    }
    const res = await apiFetch('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword: form.currentPassword, newPassword: form.newPassword }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      setState({ loading: false, error: '', success: 'Password changed. Other sessions were signed out.' })
      setForm({ currentPassword: '', newPassword: '', confirm: '' })
    } else {
      setState({ loading: false, error: data.message ?? 'Failed to change password', success: '' })
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Change Password">
      <form onSubmit={submit} className="space-y-4">
        {state.error && <p className="rounded-xl bg-red-50 px-4 py-2.5 text-sm text-red-600 ring-1 ring-red-200">{state.error}</p>}
        {state.success && <p className="rounded-xl bg-emerald-50 px-4 py-2.5 text-sm text-emerald-700 ring-1 ring-emerald-200">{state.success}</p>}
        {[
          ['currentPassword', 'Current password'],
          ['newPassword', 'New password'],
          ['confirm', 'Confirm new password'],
        ].map(([key, label]) => (
          <div key={key}>
            <label className="mb-1.5 block text-sm font-medium text-slate-700">{label}</label>
            <input
              type="password"
              required
              minLength={key === 'currentPassword' ? 1 : 8}
              value={form[key]}
              onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none transition-all focus:border-pcc-500 focus:bg-white focus:ring-4 focus:ring-pcc-500/15"
            />
          </div>
        ))}
        <button
          type="submit"
          disabled={state.loading}
          className="w-full rounded-xl bg-gradient-to-r from-pcc-700 to-pcc-500 py-2.5 text-sm font-semibold text-white shadow-lg shadow-pcc-500/25 transition-all hover:-translate-y-0.5 hover:shadow-xl disabled:opacity-60"
        >
          {state.loading ? 'Saving…' : 'Change Password'}
        </button>
      </form>
    </Modal>
  )
}
