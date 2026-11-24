import { useEffect, useState, type FormEvent } from 'react'
import { merchantApi } from '../api'
import { Alert } from '../components/Alert'
import { Modal } from '../components/Modal'
import { SecretField } from '../components/SecretField'
import { UrlFields } from '../components/UrlFields'
import type { ApiKey, Merchant, MerchantUrls, Subscription } from '../types'

export function MerchantPage() {
  const [merchant, setMerchant] = useState<Merchant | null>(null)
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([])
  const [error, setError] = useState<string | null>(null)

  async function load() {
    try {
      const [me, subs] = await Promise.all([merchantApi.me(), merchantApi.paymentMethods()])
      setMerchant(me)
      setSubscriptions(subs)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  useEffect(() => {
    load()
  }, [])

  if (!merchant) {
    return error ? <Alert kind="error" message={error} /> : <p className="muted">Učitavanje...</p>
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>{merchant.name}</h1>
          <p className="muted">
            MERCHANT_ID: <code>{merchant.merchantId}</code>
          </p>
        </div>
      </div>

      <PaymentMethodsCard subscriptions={subscriptions} onChanged={load} />
      <UrlsCard merchant={merchant} onSaved={setMerchant} />
      <ApiKeyCard />
    </section>
  )
}

function PaymentMethodsCard({ subscriptions, onChanged }: { subscriptions: Subscription[]; onChanged: () => void }) {
  const [busy, setBusy] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const activeCount = subscriptions.filter(s => s.active).length

  async function toggle(s: Subscription) {
    setBusy(s.code)
    setError(null)
    try {
      if (s.active) await merchantApi.deactivate(s.code)
      else await merchantApi.activate(s.code)
      onChanged()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="card">
      <h2>Načini plaćanja</h2>
      <p className="muted">Kupci će na PSP-u moći da biraju samo aktivne načine. Bar jedan mora ostati aktivan.</p>
      <Alert kind="error" message={error} />
      <ul className="method-list">
        {subscriptions.map(s => {
          const lastActive = s.active && activeCount === 1
          return (
            <li key={s.code}>
              <div>
                <div className="method-name">{s.name}</div>
                <div className={s.active ? 'status-on' : 'status-off'}>{s.active ? 'Aktivan' : 'Neaktivan'}</div>
              </div>
              <button
                className={s.active ? 'secondary' : ''}
                disabled={busy !== null || lastActive}
                title={lastActive ? 'Mora ostati bar jedan aktivan način plaćanja' : undefined}
                onClick={() => toggle(s)}
              >
                {busy === s.code ? '...' : s.active ? 'Deaktiviraj' : 'Aktiviraj'}
              </button>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

function UrlsCard({ merchant, onSaved }: { merchant: Merchant; onSaved: (m: Merchant) => void }) {
  const [urls, setUrls] = useState<MerchantUrls>(merchant.urls)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      onSaved(await merchantApi.updateUrls(urls))
      setSuccess('URL-ovi su sačuvani.')
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="card" onSubmit={submit}>
      <h2>URL-ovi</h2>
      <Alert kind="error" message={error} />
      <Alert kind="success" message={success} />
      <UrlFields value={urls} onChange={setUrls} />
      <div className="actions">
        <button type="submit" disabled={saving}>
          {saving ? 'Čuvanje...' : 'Sačuvaj'}
        </button>
      </div>
    </form>
  )
}

function ApiKeyCard() {
  const [confirming, setConfirming] = useState(false)
  const [newKey, setNewKey] = useState<ApiKey | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function rotate() {
    setError(null)
    try {
      setNewKey(await merchantApi.rotateApiKey())
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setConfirming(false)
    }
  }

  return (
    <div className="card">
      <h2>API ključ</h2>
      <p className="muted">
        Vaš sistem šalje MERCHANT_ID i API ključ uz svaki zahtev ka PSP-u. Ključ se čuva samo kao hash, pa ga
        nije moguće ponovo prikazati. Ako ga izgubite, generišite novi.
      </p>
      <Alert kind="error" message={error} />
      <div className="actions">
        <button className="danger" onClick={() => setConfirming(true)}>
          Generiši novi API ključ
        </button>
      </div>

      {confirming && (
        <Modal title="Novi API ključ" onClose={() => setConfirming(false)}>
          <p>Trenutni API ključ će odmah prestati da važi. Vaš sistem mora da pređe na novi ključ. Nastaviti?</p>
          <div className="actions">
            <button className="secondary" onClick={() => setConfirming(false)}>
              Otkaži
            </button>
            <button className="danger" onClick={rotate}>
              Generiši
            </button>
          </div>
        </Modal>
      )}

      {newKey && (
        <Modal title="Novi API ključ">
          <Alert kind="warning" message="Ključ se prikazuje samo sada. Kopirajte ga pre zatvaranja." />
          <SecretField label="MERCHANT_ID" value={newKey.merchantId} />
          <SecretField label="API ključ" value={newKey.apiKey} />
          <div className="actions">
            <button onClick={() => setNewKey(null)}>Ključ je sačuvan</button>
          </div>
        </Modal>
      )}
    </div>
  )
}
