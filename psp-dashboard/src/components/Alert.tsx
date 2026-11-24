export function Alert({ kind, message }: { kind: 'error' | 'success' | 'warning'; message: string | null }) {
  if (!message) return null
  return <div className={`alert alert-${kind}`}>{message}</div>
}
