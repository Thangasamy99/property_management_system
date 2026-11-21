import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, landingPathFor, useAuth } from './context/AuthContext.jsx'
import { ToastProvider } from './context/ToastContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import DashboardLayout from './layouts/DashboardLayout.jsx'
import LoginPage from './pages/LoginPage.jsx'
import DashboardHome from './pages/dashboard/DashboardHome.jsx'
import UsersPage from './pages/dashboard/UsersPage.jsx'
import RolesPage from './pages/dashboard/RolesPage.jsx'
import LoginHistoryPage from './pages/dashboard/LoginHistoryPage.jsx'
import AuditLogsPage from './pages/dashboard/AuditLogsPage.jsx'
import ProfilePage from './pages/dashboard/ProfilePage.jsx'
import PropertiesPage from './pages/property/PropertiesPage.jsx'
import BuildingsPage from './pages/property/BuildingsPage.jsx'
import ApartmentsPage from './pages/property/ApartmentsPage.jsx'
import TenantsPage from './pages/property/TenantsPage.jsx'
import RentalsPage from './pages/property/RentalsPage.jsx'
import MaintenancePage from './pages/property/MaintenancePage.jsx'
import LandDashboardPage from './pages/land/LandDashboardPage.jsx'
import LandParcelsPage from './pages/land/LandParcelsPage.jsx'
import LandSurveysPage from './pages/land/LandSurveysPage.jsx'
import OwnershipPage from './pages/land/OwnershipPage.jsx'
import AcquisitionsPage from './pages/land/AcquisitionsPage.jsx'
import LandSalesPage from './pages/land/LandSalesPage.jsx'
import TransfersPage from './pages/land/TransfersPage.jsx'
import TransactionsPage from './pages/land/TransactionsPage.jsx'
import FinanceDashboardPage from './pages/finance/FinanceDashboardPage.jsx'
import RentCollectionPage from './pages/finance/RentCollectionPage.jsx'
import LandAcquisitionPaymentsPage from './pages/finance/LandAcquisitionPaymentsPage.jsx'
import LandSaleReceiptsPage from './pages/finance/LandSaleReceiptsPage.jsx'
import OtherIncomePage from './pages/finance/OtherIncomePage.jsx'
import ExpensesPage from './pages/finance/ExpensesPage.jsx'
import ReceiptsPage from './pages/finance/ReceiptsPage.jsx'
import RefundsPage from './pages/finance/RefundsPage.jsx'
import LedgerPage from './pages/finance/LedgerPage.jsx'
import CashbookPage from './pages/finance/CashbookPage.jsx'
import FinanceReportsPage from './pages/finance/FinanceReportsPage.jsx'
import DocumentDashboardPage from './pages/documents/DocumentDashboardPage.jsx'
import DocumentsPage from './pages/documents/DocumentsPage.jsx'
import DocumentDetailPage from './pages/documents/DocumentDetailPage.jsx'
import CategoriesPage from './pages/documents/CategoriesPage.jsx'
import ExpiringDocumentsPage from './pages/documents/ExpiringDocumentsPage.jsx'
import WorkflowDashboardPage from './pages/workflows/WorkflowDashboardPage.jsx'
import RequestsPage from './pages/workflows/RequestsPage.jsx'
import WorkflowDetailPage from './pages/workflows/WorkflowDetailPage.jsx'
import DefinitionsPage from './pages/workflows/DefinitionsPage.jsx'
import DelegationsPage from './pages/workflows/DelegationsPage.jsx'
import NotificationsPage from './pages/workflows/NotificationsPage.jsx'
import RpDashboardsPage from './pages/reporting/DashboardsPage.jsx'
import RpReportsPage from './pages/reporting/ReportsPage.jsx'
import RpHistoryPage from './pages/reporting/HistoryPage.jsx'

function RootRedirect() {
  const { user, ready } = useAuth()
  if (!ready) return null
  return <Navigate to={user ? landingPathFor(user) : '/login'} replace />
}

function LoginRoute() {
  const { user, ready } = useAuth()
  if (!ready) return null
  return user ? <Navigate to={landingPathFor(user)} replace /> : <LoginPage />
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginRoute />} />
          <Route path="/" element={<RootRedirect />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<DashboardLayout />}>
              <Route path="/admin/dashboard" element={<DashboardHome />} />
              <Route path="/manager/dashboard" element={<DashboardHome />} />
              <Route path="/accountant/dashboard" element={<DashboardHome />} />
              <Route path="/property/dashboard" element={<DashboardHome />} />
              <Route path="/land/dashboard" element={<DashboardHome />} />
              <Route path="/login-history" element={<LoginHistoryPage />} />
              <Route path="/profile" element={<ProfilePage />} />

              <Route element={<ProtectedRoute permission="USER_VIEW" />}>
                <Route path="/users" element={<UsersPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="ROLE_VIEW" />}>
                <Route path="/roles" element={<RolesPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="AUDIT_LOG_VIEW" />}>
                <Route path="/audit-logs" element={<AuditLogsPage />} />
              </Route>

              <Route element={<ProtectedRoute permission="PROPERTY_VIEW" />}>
                <Route path="/properties" element={<PropertiesPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="BUILDING_VIEW" />}>
                <Route path="/buildings" element={<BuildingsPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="APARTMENT_VIEW" />}>
                <Route path="/apartments" element={<ApartmentsPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="TENANT_VIEW" />}>
                <Route path="/tenants" element={<TenantsPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="RENTAL_VIEW" />}>
                <Route path="/rentals" element={<RentalsPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="MAINTENANCE_VIEW" />}>
                <Route path="/maintenance" element={<MaintenancePage />} />
              </Route>

              <Route element={<ProtectedRoute permission="LAND_VIEW" />}>
                <Route path="/land" element={<LandDashboardPage />} />
                <Route path="/land-parcels" element={<LandParcelsPage />} />
                <Route path="/land-surveys" element={<LandSurveysPage />} />
                <Route path="/land-ownership" element={<OwnershipPage />} />
                <Route path="/land-acquisitions" element={<AcquisitionsPage />} />
                <Route path="/land-sales" element={<LandSalesPage />} />
                <Route path="/land-transfers" element={<TransfersPage />} />
                <Route path="/land-transactions" element={<TransactionsPage />} />
              </Route>

              <Route element={<ProtectedRoute permission="FINANCE_DASHBOARD_VIEW" />}>
                <Route path="/finance" element={<FinanceDashboardPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="RENT_COLLECTION_VIEW" />}>
                <Route path="/finance/rent-collections" element={<RentCollectionPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="LAND_PAYMENT_VIEW" />}>
                <Route path="/finance/land-acquisition-payments" element={<LandAcquisitionPaymentsPage />} />
                <Route path="/finance/land-sale-receipts" element={<LandSaleReceiptsPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="INCOME_VIEW" />}>
                <Route path="/finance/other-income" element={<OtherIncomePage />} />
              </Route>
              <Route element={<ProtectedRoute permission="EXPENSE_VIEW" />}>
                <Route path="/finance/expenses" element={<ExpensesPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="RECEIPT_VIEW" />}>
                <Route path="/finance/receipts" element={<ReceiptsPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="REFUND_VIEW" />}>
                <Route path="/finance/refunds" element={<RefundsPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="LEDGER_VIEW" />}>
                <Route path="/finance/ledger" element={<LedgerPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="CASH_ACCOUNT_VIEW" />}>
                <Route path="/finance/cashbook" element={<CashbookPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="FINANCIAL_REPORT_VIEW" />}>
                <Route path="/finance/reports" element={<FinanceReportsPage />} />
              </Route>

              <Route element={<ProtectedRoute permission="DOCUMENT_VIEW" />}>
                <Route path="/documents" element={<DocumentDashboardPage />} />
                <Route path="/documents/all" element={<DocumentsPage />} />
                <Route path="/documents/property" element={<DocumentsPage domain="PROPERTY" />} />
                <Route path="/documents/land" element={<DocumentsPage domain="LAND" />} />
                <Route path="/documents/finance" element={<DocumentsPage domain="FINANCE" />} />
                <Route path="/documents/archived" element={<DocumentsPage archived />} />
                <Route path="/documents/:id" element={<DocumentDetailPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="DOCUMENT_CATEGORY_VIEW" />}>
                <Route path="/documents/categories" element={<CategoriesPage />} />
              </Route>
              <Route element={<ProtectedRoute permission="DOCUMENT_EXPIRY_VIEW" />}>
                <Route path="/documents/expiring" element={<ExpiringDocumentsPage />} />
              </Route>

              <Route element={<ProtectedRoute permission="WORKFLOW_VIEW" />}>
                <Route path="/workflows" element={<WorkflowDashboardPage />} />
                <Route path="/workflows/all" element={<RequestsPage mode="all" />} />
                <Route path="/workflows/mine" element={<RequestsPage mode="mine" />} />
                <Route path="/workflows/pending" element={<RequestsPage mode="pending" />} />
                <Route path="/workflows/overdue" element={<RequestsPage mode="overdue" />} />
                <Route path="/workflows/returned" element={<RequestsPage mode="returned" />} />
                <Route path="/workflows/history" element={<RequestsPage mode="history" />} />
                <Route path="/workflows/detail/:id" element={<WorkflowDetailPage />} />
                <Route path="/workflows/definitions" element={<DefinitionsPage />} />
                <Route path="/workflows/delegations" element={<DelegationsPage />} />
                <Route path="/workflows/notifications" element={<NotificationsPage />} />
                <Route path="/reporting" element={<RpDashboardsPage kind="executive" />} />
                <Route path="/reporting/property-analytics" element={<RpDashboardsPage kind="property" />} />
                <Route path="/reporting/land-analytics" element={<RpDashboardsPage kind="land" />} />
                <Route path="/reporting/finance-analytics" element={<RpDashboardsPage kind="finance" />} />
                <Route path="/reporting/workflow-analytics" element={<RpDashboardsPage kind="workflow" />} />
                <Route path="/reporting/document-analytics" element={<RpDashboardsPage kind="documents" />} />
                <Route path="/reporting/property-reports" element={<RpReportsPage domain="PROPERTY" />} />
                <Route path="/reporting/land-reports" element={<RpReportsPage domain="LAND" />} />
                <Route path="/reporting/finance-reports" element={<RpReportsPage domain="FINANCE" />} />
                <Route path="/reporting/workflow-reports" element={<RpReportsPage domain="WORKFLOW" />} />
                <Route path="/reporting/document-reports" element={<RpReportsPage domain="DOCUMENT" />} />
                <Route path="/reporting/employee-reports" element={<RpReportsPage domain="IDENTITY" />} />
                <Route path="/reporting/audit-reports" element={<RpReportsPage domain="IDENTITY" />} />
                <Route path="/reporting/history" element={<RpHistoryPage />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
