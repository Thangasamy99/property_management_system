import { useEffect, useState } from 'react'
import { landFetch } from '../../services/api'
import { inputCls } from '../property/PropertiesPage'

/** Dropdown of registered parcels (loads once per mount). */
export default function ParcelSelect({ value, onChange, required = true, status }) {
  const [parcels, setParcels] = useState(null)

  useEffect(() => {
    const q = new URLSearchParams({ page: 0, size: 100 })
    if (status) q.set('status', status)
    landFetch(`/land-parcels?${q}`).then(async (r) => {
      if (r?.ok) setParcels((await r.json()).content ?? [])
      else setParcels([])
    }).catch(() => setParcels([]))
  }, [status])

  return (
    <select required={required} className={inputCls} value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">{parcels === null ? 'Loading parcels…' : parcels.length === 0 ? 'No parcels registered' : 'Select a parcel'}</option>
      {(parcels ?? []).map((p) => (
        <option key={p.id} value={p.id}>{p.parcelReference} — {p.name}</option>
      ))}
    </select>
  )
}
