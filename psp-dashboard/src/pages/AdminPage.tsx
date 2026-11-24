import { useEffect, useState, type FormEvent } from 'react'
import { adminApi } from '../api'
import { Alert } from '../components/Alert'
import { Modal } from '../components/Modal'
import { SecretField } from '../components/SecretField'
import type { Merchant, MerchantCreated, PaymentMethod } from '../types'
import { RegisterMerchantForm } from './RegisterMerchantForm'

export function AdminPage() {
  const [merchants, setMerchants] = useState<Merchant[]>([])
  const [methods, setMethods] = useState<PaymentMethod[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [created, setCreated] = useState<MerchantCreated | null>(null)
  const [acquirerFor, setAcquirerFor] = useState<Merchant | null>(null)

  async function load() {
    try {
      const [m, pm] = await Promise.all([adminApi.merchants(), adminApi.paymentMethods()])
      setMerchants(m)
      setMethods(pm)
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const methodName = (code: string) => methods.find(m => m.code === code)?.name ?? code

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Klijenti</h1>
          <p className="muted">Sistemi koji koriste PSP za naplatu.</p>
        </div>
        <button onClick={() => setShowForm(true)}>Registruj klijenta</button>
      </div>

      <Alert kind="error" message={error} />

      <div className="card">
        {loading ? (
          <p className="muted">Učitavanje...</p>
        ) : merchants.length === 0 ? (
          <p className="muted">Još nema registrovanih klijenata.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Naziv</th>
                <th>MERCHANT_ID</th>
                <th>Email</th>
                <th>Aktivni načini plaćanja</th>
                <th>Banka (MERCHANT_ID)</th>
                <th>Registrovan</th>
              </tr>
            </thead>
            <tbody>
              {merchants.map(m => (
                <tr key={m.id}>
                  <td>{m.name}</td>
                  <td>
                    <code>{m.merchantId}</code>
                  </td>
                  <td>{m.email}</td>
                  <td>
                    {m.activePaymentMethods.map(code => (
                      <span key={code} className="chip">
                        {methodName(code)}
                      </span>
                    ))}
                  </td>
                  <td>
                    {m.acquirerMerchantId && <code>{m.acquirerMerchantId}</code>}{' '}
                    <button className="secondary small" onClick={() => setAcquirerFor(m)}>
                      {m.acquirerMerchantId ? 'Izmeni' : 'Poveži'}
                    </button>
                  </td>
                  <td>{new Date(m.createdAt).toLocaleDateString('sr-RS')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showForm && (
        <Modal title="Registracija klijenta" onClose={() => setShowForm(false)}>
          <RegisterMerchantForm
            methods={methods}
            onCancel={() => setShowForm(false)}
            onCreated={result => {
              setShowForm(false)
              setCreated(result)
              load()
            }}
          />
        </Modal>
      )}

      {acquirerFor && (
        <Modal title="Povezivanje sa bankom" onClose={() => setAcquirerFor(null)}>
          <AcquirerForm
            merchant={acquirerFor}
            onCancel={() => setAcquirerFor(null)}
            onSaved={() => {
              setAcquirerFor(null)
              load()
            }}
          />
        </Modal>
      )}

      {created && (
        <Modal title="Klijent je registrovan">
          <Alert
            kind="warning"
            message="Privremena lozinka i API ključ se prikazuju samo sada. Kopirajte ih i bezbedno prosledite klijentu."
          />
          <div className="field">
            <span className="label">Korisničko ime</span>
            <code>{created.merchant.email}</code>
          </div>
          <SecretField label="MERCHANT_ID" value={created.merchant.merchantId} />
          <SecretField label="Privremena lozinka (mora se promeniti pri prvoj prijavi)" value={created.temporaryPassword} />
          <SecretField label="API ključ" value={created.apiKey} />
          <div className="actions">
            <button onClick={() => setCreated(null)}>Podaci su sačuvani</button>
          </div>
        </Modal>
      )}
    </section>
  )
}

/** MERCHANT_ID koji je Acquirer banka dodelila klijentu; potreban za plaćanje karticom i QR kodom. */
function AcquirerForm({ merchant, onCancel, onSaved }: { merchant: Merchant; onCancel: () => void; onSaved: () => void }) {
  const [value, setValue] = useState(merchant.acquirerMerchantId ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await adminApi.setAcquirer(merchant.id, value)
      onSaved()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit}>
      <p className="muted">
        MERCHANT_ID koji je banka dodelila klijentu <strong>{merchant.name}</strong>. Bez njega klijent ne može da
        prima plaćanja karticom.
      </p>
      <Alert kind="error" message={error} />
      <label className="field">
        <span className="label">MERCHANT_ID u banci</span>
        <input required maxLength={32} placeholder="npr. ACQ-1001" value={value} onChange={e => setValue(e.target.value)} />
      </label>
      <div className="actions">
        <button type="button" className="secondary" onClick={onCancel}>
          Otkaži
        </button>
        <button type="submit" disabled={saving}>
          {saving ? 'Čuvanje...' : 'Sačuvaj'}
        </button>
      </div>
    </form>
  )
}
