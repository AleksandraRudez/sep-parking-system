export interface MerchantUrls {
  successUrl: string
  failedUrl: string
  errorUrl: string
  webhookUrl: string
}

export interface Merchant {
  id: string
  merchantId: string
  name: string
  email: string
  urls: MerchantUrls
  acquirerMerchantId: string | null
  active: boolean
  createdAt: string
  activePaymentMethods: string[]
}

export interface MerchantCreated {
  merchant: Merchant
  temporaryPassword: string
  apiKey: string
}

export interface PaymentMethod {
  code: string
  name: string
}

export interface Subscription extends PaymentMethod {
  active: boolean
}

export interface ApiKey {
  merchantId: string
  apiKey: string
}

export interface RegisterMerchantRequest extends MerchantUrls {
  name: string
  email: string
  paymentMethods: string[]
}
