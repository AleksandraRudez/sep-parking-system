import type { ReactNode } from 'react'

interface Props {
  title: string
  onClose?: () => void
  children: ReactNode
}

export function Modal({ title, onClose, children }: Props) {
  return (
    <div className="modal-backdrop">
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-header">
          <h2>{title}</h2>
          {onClose && (
            <button className="icon-button" onClick={onClose} aria-label="Zatvori">
              ×
            </button>
          )}
        </div>
        {children}
      </div>
    </div>
  )
}
