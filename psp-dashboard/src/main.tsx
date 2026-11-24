import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { keycloak } from './auth'
import './index.css'

const root = ReactDOM.createRoot(document.getElementById('root')!)

keycloak
  .init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
  .then(() => {
    root.render(
      <React.StrictMode>
        <App />
      </React.StrictMode>,
    )
  })
  .catch(() => {
    root.render(
      <div className="center-message">
        Keycloak nije dostupan. Proverite da li je pokrenut na {keycloak.authServerUrl}.
      </div>,
    )
  })
