const env = import.meta.env

export const config = {
  apiUrl: env.VITE_API_URL ?? 'http://localhost:8081',
  keycloakUrl: env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  keycloakRealm: env.VITE_KEYCLOAK_REALM ?? 'psp',
  keycloakClientId: env.VITE_KEYCLOAK_CLIENT_ID ?? 'psp-dashboard',
}
