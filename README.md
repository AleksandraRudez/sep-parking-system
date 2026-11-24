# sep-parking-system

Projekat iz predmeta Sistemi elektronskog plaćanja (2026/2027): online parking sistem i Payment Service Provider (PSP).

## Struktura

| Folder | Opis |
|---|---|
| `infra/` | Docker Compose za lokalni razvoj: PostgreSQL (baze `psp`, `parking`, `bank`, `keycloak`) i Keycloak |
| `psp-core/` | PSP jezgro: klijenti (merchanti), način plaćanja, pretplate (Spring Boot, port 8081) |
| `psp-dashboard/` | PSP Dashboard: administratorski i klijentski portal (React + TypeScript, port 5173) |
| `bank-service/` | Simulator Acquirer banke: računi, kartice, forma za plaćanje karticom (Spring Boot, port 8090) |
| `demo-shop/` | Test prodavac koji se integriše sa PSP-om, zamena za parking sistem tokom razvoja (port 8070) |

## Pokretanje

### Tajne (lozinke, ključevi)

Lozinke i ključevi se **ne komituju**. Čuvaju se u `.env` fajlovima koji su u `.gitignore`; na git-u su samo šabloni `.env.example`.

Pre prvog pokretanja:
```
copy infra\.env.example infra\.env
copy psp-core\.env.example psp-core\.env
copy bank-service\.env.example bank-service\.env
copy demo-shop\.env.example demo-shop\.env
```
pa u `.env` fajlove upisati vrednosti (dogovor u timu, ne preko git-a). Iste vrednosti moraju biti:
- `DB_PASSWORD` u svim fajlovima;
- `PSP_CORE_CLIENT_SECRET` u `infra\.env` i `psp-core\.env`;
- `BANK_PSP_HMAC_SECRET` u `psp-core\.env` i `bank-service\.env`.

`demo-shop\.env` se popunjava tek posle registracije klijenta "Demo Shop" (vidi [Plaćanje karticom](#plaćanje-karticom)).

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
5. **Banka**:
   ```
   cd bank-service
   mvnw.cmd spring-boot:run
   ```
6. **Demo Shop** (posle registracije, vidi ispod):
   ```
   cd demo-shop
   mvnw.cmd spring-boot:run
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
- Banka: http://localhost:8090
- Demo Shop: http://localhost:8070

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

### API za sisteme prodavaca

Autentifikacija zaglavljima `X-Merchant-Id` i `X-Api-Key`.

| Metoda | Putanja | Opis |
|---|---|---|
| POST | `/api/v1/payments` | Inicijalizacija transakcije (`amount`, `currency`, `merchantOrderId`, `merchantTimestamp`); vraća `paymentUrl` na koji se preusmerava kupac. Ponovljen zahtev sa istim `merchantOrderId` vraća istu transakciju. |
| GET | `/api/v1/payments/{paymentId}` | Status transakcije |
| GET | `/api/v1/payments?merchantOrderId=...` | Status transakcije po broju naloga prodavca |

Ishod plaćanja PSP šalje i asinhrono, POST-om na webhook URL prodavca. Telo je potpisano: `X-Signature` = HMAC-SHA256(`X-Timestamp` + "." + telo), ključ je SHA-256 hash API ključa (hex).

## Plaćanje karticom

Tok: prodavac → PSP (`/api/v1/payments`) → kupac bira način plaćanja na PSP stranici → PSP traži od banke `PAYMENT_URL` (HMAC potpis) → kupac unosi karticu na stranici banke → banka rezerviše sredstva i javlja PSP-u → PSP javlja prodavcu (webhook) i vraća kupca na SUCCESS/FAILED/ERROR URL.

### Podešavanje test prodavca

1. U PSP Dashboard-u, kao super admin, registrovati klijenta:
   - Success URL: `http://localhost:8070/payment/success`
   - Failed URL: `http://localhost:8070/payment/failed`
   - Error URL: `http://localhost:8070/payment/error`
   - Webhook URL: `http://localhost:8070/api/psp/webhook`
   - Način plaćanja: Platna kartica
2. U koloni "Banka (MERCHANT_ID)" kliknuti **Poveži** i upisati `ACQ-1002`.
3. MERCHANT_ID i API ključ upisati u `demo-shop\.env` (`PSP_MERCHANT_ID`, `PSP_API_KEY`) i pokrenuti Demo Shop.

Banka pri prvom pokretanju upisuje prodavce `ACQ-1001` (Parking Servis d.o.o.) i `ACQ-1002` (Demo Shop d.o.o.).

### Test kartice

| Broj | Ime na kartici | Važi do | CVV | Stanje računa |
|---|---|---|---|---|
| `4242 4242 4242 4242` | Petar Petrovic | 12/29 | 123 | 100.000 RSD |
| `5555 5555 5555 4444` | Ana Anic | 06/28 | 456 | 500 RSD |
| `4000 0566 5566 5556` | Marko Markovic | 03/30 | 789 | 20.000 RSD |

Banka čuva samo HMAC broja kartice i CVV-a (ključ `BANK_CARD_HMAC_KEY`), nikad same brojeve.
