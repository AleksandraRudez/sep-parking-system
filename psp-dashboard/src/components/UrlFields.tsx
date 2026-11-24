import type { MerchantUrls } from '../types'

const FIELDS: { key: keyof MerchantUrls; label: string; hint: string }[] = [
  { key: 'successUrl', label: 'Success URL', hint: 'Stranica na koju se kupac vraća posle uspešnog plaćanja' },
  { key: 'failedUrl', label: 'Failed URL', hint: 'Stranica za neuspešno plaćanje' },
  { key: 'errorUrl', label: 'Error URL', hint: 'Stranica za grešku tokom plaćanja' },
  { key: 'webhookUrl', label: 'Webhook URL', hint: 'Endpoint na koji PSP asinhrono javlja status transakcije' },
]

interface Props {
  value: MerchantUrls
  onChange: (value: MerchantUrls) => void
}

export function UrlFields({ value, onChange }: Props) {
  return (
    <>
      {FIELDS.map(f => (
        <label key={f.key} className="field">
          <span className="label">{f.label}</span>
          <input
            type="url"
            required
            placeholder="https://..."
            value={value[f.key]}
            onChange={e => onChange({ ...value, [f.key]: e.target.value })}
          />
          <span className="hint">{f.hint}</span>
        </label>
      ))}
    </>
  )
}
