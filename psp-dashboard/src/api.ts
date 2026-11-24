import { accessToken } from './auth'
import { config } from './config'
import type {
  ApiKey, Merchant, MerchantCreated, MerchantUrls, PaymentMethod, RegisterMerchantRequest, Subscription,
} from './types'

/** Greska sa backenda; poruka dolazi iz polja "detail" (ProblemDetail). */
export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await fetch(config.apiUrl + path, {
    method,
    headers: {
      Authorization: `Bearer ${await accessToken()}`,
      ...(body !== undefined && { 'Content-Type': 'application/json' }),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    let message = `Greska ${res.status}`
    try {
      const problem = await res.json()
      if (problem.detail) message = problem.detail
    } catch {
      // odgovor nema telo
    }
    throw new ApiError(res.status, message)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}

export const adminApi = {
  merchants: () => request<Merchant[]>('GET', '/api/admin/merchants'),
  register: (req: RegisterMerchantRequest) => request<MerchantCreated>('POST', '/api/admin/merchants', req),
  paymentMethods: () => request<PaymentMethod[]>('GET', '/api/admin/payment-methods'),
  setAcquirer: (id: string, acquirerMerchantId: string) =>
    request<Merchant>('PUT', `/api/admin/merchants/${id}/acquirer`, { acquirerMerchantId }),
}

export const merchantApi = {
  me: () => request<Merchant>('GET', '/api/merchant/me'),
  updateUrls: (urls: MerchantUrls) => request<Merchant>('PUT', '/api/merchant/me/urls', urls),
  rotateApiKey: () => request<ApiKey>('POST', '/api/merchant/me/api-key'),
  paymentMethods: () => request<Subscription[]>('GET', '/api/merchant/me/payment-methods'),
  activate: (code: string) => request<void>('PUT', `/api/merchant/me/payment-methods/${code}`),
  deactivate: (code: string) => request<void>('DELETE', `/api/merchant/me/payment-methods/${code}`),
}
