import { hasRole, keycloak, logout } from './auth'
import { AdminPage } from './pages/AdminPage'
import { MerchantPage } from './pages/MerchantPage'

export default function App() {
  const isAdmin = hasRole('SUPER_ADMIN')
  const isMerchant = hasRole('MERCHANT')
  const user = keycloak.tokenParsed?.preferred_username as string | undefined

  return (
    <>
      <header className="topbar">
        <div className="brand">PSP Dashboard</div>
        <div className="user">
          <span className="role-badge">{isAdmin ? 'Super admin' : isMerchant ? 'Klijent' : 'Bez uloge'}</span>
          <span>{user}</span>
          <button className="secondary" onClick={logout}>
            Odjava
          </button>
        </div>
      </header>
      <main className="content">
        {isAdmin && <AdminPage />}
        {!isAdmin && isMerchant && <MerchantPage />}
        {!isAdmin && !isMerchant && (
          <div className="center-message">Vaš nalog nema pristup PSP portalu.</div>
        )}
      </main>
    </>
  )
}
