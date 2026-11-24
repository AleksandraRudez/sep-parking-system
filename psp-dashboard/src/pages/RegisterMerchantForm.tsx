import { useState, type FormEvent } from 'react'
import { adminApi } from '../api'
import { Alert } from '../components/Alert'
import { UrlFields } from '../components/UrlFields'
import type { MerchantCreated, MerchantUrls, PaymentMethod } from '../types'

interface Props {
  methods: PaymentMethod[]
  onCancel: () => void
  onCreated: (result: MerchantCreated) => void
}

const EMPTY_URLS: MerchantUrls = { successUrl: '', failedUrl: '', errorUrl: '', webhookUrl: '' }

export function RegisterMerchantForm({ methods, onCancel, onCreated }: Props) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [urls, setUrls] = useState(EMPTY_URLS)
  const [selected, setSelected] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function toggle(code: string) {
    setSelected(s => (s.includes(code) ? s.filter(c => c !== code) : [...s, code]))
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (selected.length === 0) {
      setError('Izaberite bar jedan način plaćanja.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      onCreated(await adminApi.register({ name, email, ...urls, paymentMethods: selected }))
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={submit}>
      <Alert kind="error" message={error} />
      <label className="field">
        <span className="label">Naziv klijenta</span>
        <input required maxLength={150} value={name} onChange={e => setName(e.target.value)} />
      </label>
      <label className="field">
        <span className="label">Email (korisničko ime za prijavu)</span>
        <input type="email" required maxLength={150} value={email} onChange={e => setEmail(e.target.value)} />
      </label>

      <UrlFields value={urls} onChange={setUrls} />

      <fieldset className="field">
        <legend className="label">Načini plaćanja</legend>
        <div className="checkbox-group">
          {methods.map(m => (
            <label key={m.code} className="checkbox">
              <input type="checkbox" checked={selected.includes(m.code)} onChange={() => toggle(m.code)} />
              {m.name}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="actions">
        <button type="button" className="secondary" onClick={onCancel}>
          Otkaži
        </button>
        <button type="submit" disabled={submitting}>
          {submitting ? 'Registrovanje...' : 'Registruj'}
        </button>
      </div>
    </form>
  )
}
