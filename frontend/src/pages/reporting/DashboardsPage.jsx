import { useEffect, useState } from 'react'
import { Kpi, Section, SourceHealth, BarChart, PeriodPicker, useLoaded, xaf, num } from './rpShared.jsx'

const KINDS = {
  executive: { path: '/dashboard/executive' },
  property: { path: '/dashboard/property' },
  land: { path: '/dashboard/land' },
  finance: { path: '/dashboard/finance' },
  workflow: { path: '/dashboard/workflow' },
  documents: { path: '/dashboard/documents' },
}

export default function DashboardsPage({ kind }) {
  const [{ loading, error, data }, load] = useLoaded()
  const [period, setPeriod] = useState('month')
  const [from, setFrom] = useState(''); const [to, setTo] = useState('')

  useEffect(() => {
    const q = new URLSearchParams()
    q.set('period', period)
    if (period === 'custom') { if (from) q.set('from', from); if (to) q.set('to', to) }
    load(`${KINDS[kind].path}?${q}`)
  }, [kind, period, from, to])

  if (loading && !data) return <p className="p-6 text-sm text-slate-500">Loading dashboard…</p>
  if (error) return <p className="p-6 text-sm text-red-600">{error}</p>
  if (!data) return null
  const d = data

  const byStatusBars = (obj) => Object.entries(obj ?? {})
    .map(([label, value]) => ({ label: label.replaceAll('_', ' '), value }))

  return (
    <div className="space-y-4">
      <SourceHealth sources={d.sources} generatedAt={d.generatedAt} />
      {(kind === 'executive' || kind === 'finance') &&
        <PeriodPicker period={period} setPeriod={setPeriod} from={from} to={to} setFrom={setFrom} setTo={setTo} />}

      {d.property && (
        <Section title="Property portfolio" warn={d.sources?.find(s => s.service === 'PROPERTY' && s.status !== 'OK')}>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Kpi label="Properties" value={num(d.property.totalProperties ?? d.summary?.totalProperties)} />
            <Kpi label="Buildings" value={num(d.property.totalBuildings ?? d.summary?.totalBuildings)} />
            <Kpi label="Apartments" value={num(d.property.totalApartments ?? d.summary?.totalApartments)} />
            <Kpi label="Occupancy" value={d.property.occupancyPercent != null ? d.property.occupancyPercent + '%' : '—'} tone="text-violet-700" />
            <Kpi label="Occupied" value={num(d.property.occupiedApartments ?? d.summary?.apartmentsByStatus?.OCCUPIED)} />
            <Kpi label="Vacant" value={num(d.property.vacantApartments ?? d.summary?.apartmentsByStatus?.VACANT)} />
            <Kpi label="Agreements" value={num(d.property.totalAgreements ?? d.summary?.totalAgreements)} />
            <Kpi label="Expiring ≤60d" value={num(d.property.expiringAgreements)} tone="text-amber-600" />
          </div>
          {(d.summary?.apartmentsByStatus || d.summary?.propertiesByStatus) &&
            <div className="mt-4"><BarChart data={byStatusBars(d.summary?.apartmentsByStatus ?? d.summary?.propertiesByStatus)} /></div>}
        </Section>
      )}
      {d.summary && kind === 'property' && (
        <Section title="Property portfolio">
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Kpi label="Properties" value={num(d.summary.totalProperties)} />
            <Kpi label="Buildings" value={num(d.summary.totalBuildings)} />
            <Kpi label="Apartments" value={num(d.summary.totalApartments)} />
            <Kpi label="Agreements" value={num(d.summary.totalAgreements)} />
            <Kpi label="Open maintenance" value={num(d.summary.openMaintenance)} />
          </div>
          <div className="mt-4"><BarChart data={byStatusBars(d.summary.apartmentsByStatus)} /></div>
        </Section>
      )}

      {(d.land || (kind === 'land' && d.summary)) && (
        <Section title="Land portfolio" warn={d.sources?.find(s => s.service === 'LAND' && s.status !== 'OK')}>
          {(() => { const s = d.land ?? d.summary; return (
            <>
              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                <Kpi label="Parcels" value={num(s.totalParcels)} />
                <Kpi label="Recorded area (sqm)" value={num(s.totalAreaSqm)} />
                <Kpi label="Sales" value={num(s.totalSales ?? d.totalSalesCount)} />
                <Kpi label="Pending transfers" value={num(s.pendingTransfers ?? d.totalTransfers)} />
                <Kpi label="Acquisitions" value={num(d.totalAcquisitions)} />
              </div>
              <div className="mt-4"><BarChart data={byStatusBars(s.parcelsByStatus)} /></div>
              {d.areaByRegion && Object.keys(d.areaByRegion).length > 0 && (
                <div className="mt-4"><p className="mb-1 text-xs font-medium text-slate-500">Area by region (sqm)</p>
                  <BarChart data={byStatusBars(d.areaByRegion)} /></div>)}
            </>) })()}
        </Section>
      )}

      {d.finance && (
        <Section title="Finance overview (XAF)" warn={d.sources?.find(s => s.service === 'FINANCE' && s.status !== 'OK')}>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Kpi label="Collected this month" value={xaf(d.finance.collectionsThisMonth)} />
            <Kpi label="Expenses this month" value={xaf(d.finance.expensesThisMonth)} tone="text-red-600" />
            <Kpi label="Outstanding rent" value={xaf(d.finance.outstandingRent)} tone="text-amber-600" />
            <Kpi label="Cash balance" value={xaf(d.finance.cashBalance)} />
            <Kpi label="Bank balance" value={xaf(d.finance.bankBalance)} />
            <Kpi label="Land sale receipts" value={xaf(d.finance.landSaleReceipts)} />
            <Kpi label="Land acq. paid" value={xaf(d.finance.landAcquisitionPaid)} />
          </div>
          {d.finance.monthlySeries &&
            <div className="mt-4"><p className="mb-1 text-xs font-medium text-slate-500">Monthly collections</p>
              <BarChart money data={Object.entries(d.finance.monthlySeries).map(([label, value]) => ({ label, value }))} /></div>}
        </Section>
      )}
      {d.summary && kind === 'finance' && (
        <Section title="Finance overview (XAF)">
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Kpi label="Collected today" value={xaf(d.summary.collectionsToday)} />
            <Kpi label="Collected this month" value={xaf(d.summary.collectionsThisMonth)} />
            <Kpi label="Expenses this month" value={xaf(d.summary.expensesThisMonth)} tone="text-red-600" />
            <Kpi label="Outstanding rent" value={xaf(d.summary.outstandingRent)} tone="text-amber-600" />
            <Kpi label="Cash balance" value={xaf(d.summary.cashBalance)} />
            <Kpi label="Bank balance" value={xaf(d.summary.bankBalance)} />
          </div>
          {d.summary.monthlySeries &&
            <div className="mt-4"><BarChart money data={Object.entries(d.summary.monthlySeries).map(([label, value]) => ({ label, value }))} /></div>}
        </Section>
      )}

      {(d.workflow || (kind === 'workflow' && d.workflow)) && (
        <Section title="Workflow overview" warn={d.sources?.find(s => s.service === 'WORKFLOW' && s.status !== 'OK')}>
          {(() => { const w = d.workflow ?? {}; return (
            <>
              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                <Kpi label="Pending approvals" value={num(w.pendingApprovals ?? w.myPendingTasks)} tone="text-amber-600" />
                <Kpi label="Approved" value={num(w.approved ?? w.approvedInstances)} tone="text-emerald-600" />
                <Kpi label="Rejected" value={num(w.rejected ?? w.rejectedInstances)} tone="text-red-600" />
                <Kpi label="Returned" value={num(w.returned ?? w.returnedInstances)} />
                <Kpi label="Overdue" value={num(w.overdueTasks ?? w.overdue)} tone="text-red-600" />
                <Kpi label="Integration failures" value={num(w.integrationFailed)} />
              </div>
              <div className="mt-4"><BarChart data={byStatusBars({
                  SUBMITTED: w.submitted, PENDING: w.pendingApproval, APPROVED: w.approved,
                  REJECTED: w.rejected, RETURNED: w.returned, COMPLETED: w.completed })} /></div>
            </>) })()}
        </Section>
      )}

      {d.documents && (
        <Section title="Document overview" warn={d.sources?.find(s => s.service === 'DOCUMENT' && s.status !== 'OK')}>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Kpi label="Total documents" value={num(d.documents.totalDocuments ?? d.documents.activeDocuments)} />
            <Kpi label="Archived" value={num(d.documents.archivedDocuments)} />
            <Kpi label="Expired" value={num(d.documents.expiry?.expired ?? d.documents.expired)} tone="text-red-600" />
            <Kpi label="Expiring ≤30d" value={num(d.documents.expiry?.expiringIn30Days ?? d.documents.expiringIn30Days)} tone="text-amber-600" />
            <Kpi label="Expiring ≤60d" value={num(d.documents.expiry?.expiringIn60Days ?? d.documents.expiringIn60Days)} tone="text-amber-600" />
          </div>
          {(d.documents.byCategory ?? d.documents.documentsByCategory) &&
            <div className="mt-4"><BarChart data={byStatusBars(d.documents.byCategory ?? d.documents.documentsByCategory)} /></div>}
        </Section>
      )}
      {d.summary && kind === 'documents' && (
        <Section title="Document overview">
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Kpi label="Total" value={num(d.summary.totalDocuments ?? d.summary.activeDocuments)} />
            <Kpi label="Archived" value={num(d.summary.archivedDocuments)} />
            <Kpi label="Expired" value={num(d.expiry?.expired ?? d.summary.expired)} tone="text-red-600" />
            <Kpi label="Expiring ≤30d" value={num(d.expiry?.expiringIn30Days)} tone="text-amber-600" />
            <Kpi label="Expiring ≤60d" value={num(d.expiry?.expiringIn60Days)} tone="text-amber-600" />
          </div>
          {d.categoriesByDomain &&
            <div className="mt-4"><p className="mb-1 text-xs font-medium text-slate-500">Categories by domain</p>
              <BarChart data={byStatusBars(d.categoriesByDomain)} /></div>}
        </Section>
      )}

      {kind === 'executive' && !d.property && !d.land && !d.finance &&
        <p className="text-sm text-slate-500">All sources unavailable — check service status.</p>}
    </div>
  )
}
