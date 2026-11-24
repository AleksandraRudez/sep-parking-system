# sep-parking-system

Projekat iz predmeta Sistemi elektronskog plaćanja (2026/2027): online parking sistem i Payment Service Provider (PSP).

## Struktura

| Folder | Opis |
|---|---|
| `infra/` | Docker Compose za lokalni razvoj: PostgreSQL (baze `psp`, `parking`, `bank`, `keycloak`) i Keycloak |
| `psp-core/` | PSP jezgro: klijenti (merchanti), način plaćanja, pretplate (Spring Boot, port 8081) |
| `psp-dashboard/` | PSP Dashboard: administratorski i klijentski portal (React + TypeScript, port 5173) |

## Pokretanje

### Tajne (lozinke, ključevi)

Lozinke i ključevi se **ne komituju**. Čuvaju se u `.env` fajlovima koji su u `.gitignore`; na git-u su samo šabloni `.env.example`.

Pre prvog pokretanja:
```
copy infra\.env.example infra\.env
copy psp-core\.env.example psp-core\.env
```
pa u oba `.env` fajla upisati vrednosti (dogovor u timu, ne preko git-a). `PSP_CORE_CLIENT_SECRET` i `DB_PASSWORD` moraju biti isti u oba fajla.

### Varijanta A: bez Docker-a (Windows)

1. **PostgreSQL** (jednom): u Windows terminalu pokrenuti (umesto `LOZINKA` vrednost `DB_PASSWORD` iz `infra\.env`)
   ```
   "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -v db_password=LOZINKA -f infra\postgres\create-databases.sql
   ```
2. **Keycloak**: skinuti [Keycloak 26.0.7](https://github.com/keycloak/keycloak/releases/download/26.0.7/keycloak-26.0.7.zip) i raspakovati u `%USERPROFILE%\tools\`, pa pokrenuti
   ```
   infra\keycloak\start-keycloak.cmd
   ```
3. **PSP Core**:
   ```
   cd psp-core
   mvnw.cmd spring-boot:run
   ```
4. **PSP Dashboard**:
   ```
   cd psp-dashboard
   npm install
   npm run dev
   ```

### Varijanta B: Docker

```
cd infra
docker compose up -d
```

### Adrese i nalozi

- Keycloak: http://localhost:8180 (admin konzola: `admin` / `KEYCLOAK_ADMIN_PASSWORD` iz `infra\.env`)
- Realm `psp`, super admin: `superadmin` / `PSP_SUPERADMIN_PASSWORD` iz `infra\.env`
- PSP Core: http://localhost:8081
- PSP Dashboard: http://localhost:5173

## PSP Core API

| Metoda | Putanja | Uloga | Opis |
|---|---|---|---|
| GET | `/api/admin/merchants` | SUPER_ADMIN | Lista klijenata |
| POST | `/api/admin/merchants` | SUPER_ADMIN | Registracija klijenta (vraća privremenu lozinku i API ključ, jednom) |
| GET | `/api/admin/payment-methods` | SUPER_ADMIN | Svi načini plaćanja koje PSP podržava |
| GET | `/api/merchant/me` | MERCHANT | Podaci o klijentu |
| PUT | `/api/merchant/me/urls` | MERCHANT | Izmena success/failed/error/webhook URL-ova |
| POST | `/api/merchant/me/api-key` | MERCHANT | Novi API ključ (stari prestaje da važi) |
| GET | `/api/merchant/me/payment-methods` | MERCHANT | Načini plaćanja i da li su aktivni |
| PUT | `/api/merchant/me/payment-methods/{code}` | MERCHANT | Aktivacija načina plaćanja |
| DELETE | `/api/merchant/me/payment-methods/{code}` | MERCHANT | Deaktivacija (mora ostati bar jedan aktivan) |
