# Cluster JDBC Todo

## Cel

A jelenlegi JDBC-alapu cluster-kompatibilis mukodes mellett a megmaradt rizikok es kovetkezo lepesek legyenek egy helyen rogzitve.

## Aktualis verified baseline

- A session state mar JDBC-backed opcioval rendelkezik.
- A rate limiter state mar JDBC-backed opcioval rendelkezik.
- A Menu, Template, Site Settings, PageType es PageBlock modulok mar keszen vannak a backendben.
- Utoljara ellenorzott build eredmeny: 123 teszt, 0 hiba.
- A backend jelenleg le van allitva.

## Mai allapot

- a session state mar nem JVM memoriaban el:
  - `SESSION_STORE_MODE=http|jdbc`
  - `AppSessionManager`
  - `HttpSessionAppSessionStore`
  - `JdbcAppSessionStore`
- a rate limiter state mar nem JVM memoriaban el:
  - `RATE_LIMITER_STORE_MODE=memory|jdbc`
  - `RateLimiterManager`
  - `JdbcAttemptRateLimiter`
  - `JdbcRequestRateLimiter`
- a filter nev mar nem felrevezeto:
  - `AppSessionContextFilter`
- a helyi defaultok explicit maradtak:
  - `web.xml`: `http` / `memory`
  - swarm test env: `jdbc` / `jdbc`

## Nyitott feladatok

1. **Tobb node-os integracios bizonyitas**
   - 2-3 Tomcat replikan vegigtesztelni:
     - login
     - `/api/auth/me`
     - CSRF vedett request
     - logout
     - captcha
   - Ellenorizni, hogy a requestek kulonbozo node-ra kerulese mellett is stabil a viselkedes.

2. **Ugyanazon session parhuzamos frissitesenek kezelesi szabalyai**
   - tisztazni, hogy last-write-wins eleg-e
   - meg kell nezni, mi tortenik tobb tabbol vagy gyors egymas utani requesteknel
   - kulonosen erintett:
     - CSRF token
     - CAPTCHA state
     - session attribute update

3. **Lejart rekordok takaritasi strategiaja**
   - session sorok
   - session attribute sorok
   - rate limit sorok
   - meg kell donteni:
     - lazy cleanup eleg-e
     - kell-e periodikus cleanup job
     - kell-e admin endpoint vagy scheduled task

4. **AuthenticatedUser snapshot frissessege**
   - megmaradhat-e a sessionben tarolt snapshot
   - vagy kell-e user role / active allapot valtozasnal session invalidalas
   - a jelenlegi szemantikat nem szabad csendben megvaltoztatni

5. **DB terheles es indexeles ellenorzese**
   - session-aware requestek DB olvasasi mintaja
   - `expires_at`, `last_accessed_at`, `updated_at` indexeles
   - rate limiter update minta
   - szukseg eseten tuning, nem architekturavaltas

6. **Redis-boviteshez szukseges freeze pontok ellenorzese**
   - az `AppSessionManager` publikus API-ja ne valtozzon
   - a servlet/filter/auth reteg ne kapjon uj store-specifikus fuggoseget
   - a typed attribum modell maradjon `attribute_name`, `attribute_type`, `json_value`

## Elfogadasi elv

Ha a kovetkezo lepes barmelyike ujra servlet/filter/auth atirast igenyelne csak azert, mert a session vagy a rate limiter store valt, akkor az absztrakcio nem eleg jo. Ilyenkor az absztrakciot kell javitani, nem a felszini hasznalatot.
