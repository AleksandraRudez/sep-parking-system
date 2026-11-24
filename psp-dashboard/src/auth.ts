import Keycloak from 'keycloak-js'
import { config } from './config'

export const keycloak = new Keycloak({
  url: config.keycloakUrl,
  realm: config.keycloakRealm,
  clientId: config.keycloakClientId,
})

export type Role = 'SUPER_ADMIN' | 'MERCHANT'

export function hasRole(role: Role): boolean {
  return keycloak.tokenParsed?.realm_access?.roles?.includes(role) ?? false
}

/** Vraca vazeci access token; osvezava ga ako istice za manje od 30 s. */
export async function accessToken(): Promise<string> {
  await keycloak.updateToken(30)
  return keycloak.token!
}

export function logout() {
  keycloak.logout({ redirectUri: window.location.origin })
}
