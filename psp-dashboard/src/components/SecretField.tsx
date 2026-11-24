import { useState } from 'react'

/** Prikaz tajne vrednosti (lozinka, API kljuc) sa dugmetom za kopiranje. */
export function SecretField({ label, value }: { label: string; value: string }) {
  const [copied, setCopied] = useState(false)

  async function copy() {
    await navigator.clipboard.writeText(value)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <div className="field">
      <span className="label">{label}</span>
      <div className="secret">
        <code>{value}</code>
        <button type="button" className="secondary" onClick={copy}>
          {copied ? 'Kopirano' : 'Kopiraj'}
        </button>
      </div>
    </div>
  )
}
