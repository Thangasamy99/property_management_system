import { useCallback, useEffect, useState } from 'react'
import { financeFetch } from '../../services/api'
import { useToast } from '../../context/ToastContext'
import Modal from '../../components/Modal'
import DataTable from '../../components/DataTable'
import { inputCls } from '../property/PropertiesPage'
import { fmtXaf, fmtDate, label } from './financeShared'
import { Printer } from 'lucide-react'

export default function ReceiptsPage() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [view, setView] = useState(null)

  const load = useCallback(async () => {
    const res = await financeFetch(`/receipts?page=${page}&size=10${q ? `&search=${encodeURIComponent(q)}` : ''}`)
    if (res.ok) setData(await res.json())
    else toast('Failed to load receipts', 'error')
  }, [page, q]) // eslint-disable-line

  useEffect(() => { load() }, [load])

  const openReceipt = async (row) => {
    const res = await financeFetch(`/receipts/${row.id}`)
    if (res.ok) setView(await res.json())
    else toast('Receipt not found', 'error')
  }

  return (
    <div>
      <div className="mb-4">
        <input className={`${inputCls} !h-10 w-72`} placeholder="Search receipt no. / payer…" value={q}
          onChange={(e) => { setQ(e.target.value); setPage(0) }} />
      </div>
      <DataTable
        columns={[{ label: 'Receipt No.' }, { label: 'Payment' }, { label: 'Payer' }, { label: 'Amount' }, { label: 'Date' }, { label: 'Method' }, { label: 'Type' }, { label: '' }]}
        rows={data?.content} loading={!data}
        page={page} totalPages={data?.totalPages ?? 0} onPage={setPage}
        empty="No receipts issued">
        {data?.content.map((r) => (
          <tr key={r.id} className="border-b border-slate-50 hover:bg-slate-50/60">
            <td className="px-5 py-3.5 font-medium text-slate-700">{r.receiptNo}</td>
            <td className="px-5 py-3.5 text-xs text-slate-500">{r.paymentRef ?? '—'}</td>
            <td className="px-5 py-3.5 text-slate-600">{r.payerName}</td>
            <td className="px-5 py-3.5 font-semibold">{fmtXaf(r.amount)}</td>
            <td className="px-5 py-3.5 text-slate-500">{fmtDate(r.paymentDate)}</td>
            <td className="px-5 py-3.5 text-slate-500">{label(r.paymentMethod)}</td>
            <td className="px-5 py-3.5 text-slate-500">{label(r.transactionType)}</td>
            <td className="px-5 py-3.5 text-right">
              <button onClick={() => openReceipt(r)} className="rounded-lg bg-pcc-50 px-3 py-1.5 text-xs font-semibold text-pcc-700 hover:bg-pcc-100">View</button>
            </td>
          </tr>
        ))}
      </DataTable>

      <Modal open={!!view} onClose={() => setView(null)} title={`Receipt ${view?.receiptNo}`} width="max-w-md">
        {view && (
          <div id="printable-receipt" className="rounded-xl border border-slate-200 p-6 text-sm">
            <div className="mb-4 text-center">
              <p className="text-base font-bold text-pcc-900">Presbyterian Church in Cameroon</p>
              <p className="text-xs text-slate-500">Finance Department — Official Receipt</p>
            </div>
            <dl className="space-y-2">
              {[
                ['Receipt No.', view.receiptNo], ['Payment ref', view.paymentRef], ['Payer', view.payerName],
                ['Amount', fmtXaf(view.amount)], ['Amount in words', view.amountInWords],
                ['Payment date', fmtDate(view.paymentDate)], ['Payment method', label(view.paymentMethod)],
                ['Payment reference', view.paymentReference], ['Transaction type', label(view.transactionType)],
                ['Related reference', view.relatedReference], ['Received by', view.receivedByName],
              ].map(([k, v]) => v != null && v !== '' && (
                <div key={k} className="flex justify-between gap-4 border-b border-dashed border-slate-100 pb-1.5">
                  <dt className="text-xs text-slate-500">{k}</dt><dd className="text-right font-medium text-slate-700">{v}</dd>
                </div>
              ))}
            </dl>
            <button onClick={() => window.print()}
              className="mt-5 flex h-10 w-full items-center justify-center gap-2 rounded-xl bg-pcc-700 text-sm font-semibold text-white hover:bg-pcc-900">
              <Printer size={15} /> Print receipt
            </button>
          </div>
        )}
      </Modal>
    </div>
  )
}
