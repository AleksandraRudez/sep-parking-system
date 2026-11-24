@echo off
rem Pokrece Keycloak lokalno na Windows-u (bez Docker-a), na portu 8180.
rem Putanja do Keycloak-a moze da se promeni preko promenljive KEYCLOAK_HOME.
if "%KEYCLOAK_HOME%"=="" set KEYCLOAK_HOME=%USERPROFILE%\tools\keycloak-26.0.7

if not exist "%KEYCLOAK_HOME%\data\import" mkdir "%KEYCLOAK_HOME%\data\import"
copy /Y "%~dp0psp-realm.json" "%KEYCLOAK_HOME%\data\import\psp-realm.json" >nul

if "%JAVA_HOME%"=="" if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"

rem Tajne se citaju iz infra\.env (nije na git-u); sablon je infra\.env.example
if not exist "%~dp0..\.env" (
  echo Nedostaje fajl infra\.env - kopirajte infra\.env.example u infra\.env i popunite vrednosti.
  exit /b 1
)
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%~dp0..\.env") do set "%%a=%%b"

set KC_BOOTSTRAP_ADMIN_USERNAME=admin
set KC_BOOTSTRAP_ADMIN_PASSWORD=%KEYCLOAK_ADMIN_PASSWORD%
set JAVA_OPTS_KC_HEAP=-Xms128m -Xmx512m

call "%KEYCLOAK_HOME%\bin\kc.bat" start-dev --http-port 8180 --import-realm
