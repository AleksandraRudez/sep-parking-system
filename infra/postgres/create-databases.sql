-- Za lokalni PostgreSQL bez Docker-a. Pokrenuti jednom kao korisnik postgres,
-- uz lozinku iz DB_PASSWORD (infra/.env):
--   psql -U postgres -v db_password=LOZINKA -f create-databases.sql
CREATE USER sep WITH PASSWORD :'db_password';
CREATE DATABASE psp OWNER sep;
CREATE DATABASE parking OWNER sep;
CREATE DATABASE bank OWNER sep;
