# Projektleírás

Ez a dokumentum a `cms-app` backend aktuális állapotát foglalja össze. A projekt egy framework-light Java 21 alapú, Mavennel épülő WAR alkalmazás Tomcat 9 futtatási környezethez. A backend servlet/JDBC architektúrát használ, PostgreSQL adatbázissal, HikariCP connection poollal, Gson JSON feldolgozással, BCrypt jelszókezeléssel, session alapú autentikációval és opcionális JDBC-backed session/rate limiter tárolással.

## 1. Technológiai áttekintés

- Java 21
- Maven WAR build, végleges artifact név: `cms-app.war`
- Servlet API 4.0.1, Tomcat 9 futtatási környezet
- PostgreSQL JDBC driver 42.7.3
- HikariCP 5.1.0
- Gson 2.13.2
- jBCrypt 0.4
- Logback 1.2.13
- JUnit Jupiter 5.11.4, Surefire 3.5.4

Fő belépési pontok:

- `/hello`: egyszerű health endpoint
- `/api/auth/config`: auth és password policy konfiguráció
- `/api/auth/captcha`: SVG CAPTCHA generálás
- `/api/auth/login`: bejelentkezés
- `/api/auth/me`: aktuális user és CSRF token
- `/api/auth/logout`: kijelentkezés
- `/api/auth/register`: public regisztráció
- `/api/users`, `/api/users/*`: admin-only user management
- `/api/media`, `/api/media/*`: admin-only media library management
- `/api/media/{id}/content`: media binary content response for frontend preview/download
- `/api/menus`, `/api/menus/*`: admin-only menü CRUD és menüpontlista
- `/api/menu-items`, `/api/menu-items/*`: admin-only menüpont CRUD
- `/api/public/menus/{code}`: publikus, fa struktúrájú menülekérdezés
- `/api/templates`, `/api/templates/*`: admin-only template konfiguráció
- `/api/site-settings`: admin-only globális webhelybeállítások
- `/api/page-blocks`, `/api/page-blocks/*`: admin-only PageBlock CRUD

## 2. Indítás és globális konfiguráció

### `hu.laci.cms.Main`

Minimális Java entry point. A normál futás WAR/Tomcat alapú, ezért az alkalmazás életciklusát nem ez az osztály vezérli.

### `DatabaseConfigListener`

Servlet context listener. Induláskor inicializálja a `DatabaseConfig` állapotát, majd lefuttatja az adatbázis migrációkat a `DatabaseMigrationRunner` segítségével. Leálláskor lezárja a HikariCP pool erőforrásait.

### `DatabaseConfig`

Központi adatbázis-konfiguráció és connection pool kezelő. A beállításokat environment-first sorrendben olvassa:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `web.xml` context-param értékek: `db.jdbcUrl`, `db.username`, `db.password`
- beépített lokális defaultok: `localhost:5432/cms_db`, `cms_user`, `cms_pw`

### `DatabaseMigrationRunner`

Verziózott SQL migrációkat futtat a `src/main/resources/db/migration` könyvtárból. A lefutott migrációkat a `schema_migrations` táblában tartja nyilván, checksumot ellenőriz, és PostgreSQL advisory lockkal védi a párhuzamos indulást.

Aktuális migrációk:

- `V1__initial_schema.sql`
- `V2__user_role_and_active.sql`
- `V3__user_audit_columns.sql`
- `V4__user_registration_state.sql`
- `V5__app_sessions.sql`
- `V6__rate_limits.sql`
- `V7__pages.sql`
- `V8__media_library.sql`
- `V9__menus.sql`
- `V10__menu_item_targets_and_default_menus.sql`
- `V11__templates_and_site_settings.sql`
- `V12__page_types_and_blocks.sql`

### `TransactionContext`

Thread-local request tranzakciós állapot. A `TransactionFilter` nyitja és zárja, a DAO réteg pedig ezen keresztül használja az aktuális request connectiont.

### `DaoRegistryListener` és `DaoRegistry`

Induláskor regisztrálja a DAO-kat. Az aktív mappingek: `User`, `Page`, `Media`, `Menu`, `MenuItem`, `Template` és `SiteSettings`. A servlet és service réteg ezen keresztül jut a megfelelő DAO-khoz.

### `SecurityConfigListener`, `SecurityConfig`, `PasswordPolicyConfig`

Induláskor betölti az auth, CAPTCHA és password policy beállításokat. A `web.xml` aktuális defaultjai fejlesztői környezethez lazák:

- `password.min.length=2`
- uppercase/lowercase/digit/special követelmények: `false`
- `auth.max.failed.attempts=5`
- `auth.lock.minutes=15`
- login és regisztrációs CAPTCHA: `true`

### `AppSessionConfigListener`, `AppSessionConfig`, `AppSessionManager`

Az alkalmazás saját session absztrakcióját inicializálja. A servlet/filter réteg az `AppSessionManager` facade-on keresztül dolgozik, ezért a mögöttes tároló lehet Tomcat `HttpSession` vagy PostgreSQL-backed JDBC store.

Konfigurációs sorrend:

- `SESSION_STORE_MODE`, `SESSION_COOKIE_NAME`, `SESSION_TIMEOUT_MINUTES`, `SESSION_COOKIE_SECURE`, `SESSION_COOKIE_SAMESITE`
- `web.xml` context-param értékek
- beépített defaultok

Aktuális lokális/default mód:

- `session.store.mode=http`
- `session.cookie.name=CMS_SESSION_ID`
- `session.timeout.minutes=30`
- `session.cookie.secure=false`
- `session.cookie.sameSite=Lax`

Támogatott session store-ok:

- `HttpSessionAppSessionStore`
- `JdbcAppSessionStore`

### `RateLimiterConfigListener`, `RateLimiterConfig`, `RateLimiterManager`

Az auth és CAPTCHA limiterek store-független inicializálását végzi. A default lokális mód továbbra is process-local memória:

- `rateLimiter.store.mode=memory`

Environment változóval cluster teszthez átállítható:

- `RATE_LIMITER_STORE_MODE=jdbc`

Támogatott limiter store-ok:

- `memory`
- `jdbc`

## 3. HTTP request életút és filterek

A filter sorrendet a `src/main/webapp/WEB-INF/web.xml` határozza meg.

### `RequestLoggingFilter`

Naplózza a request metódusát, URI-ját, státuszát, futási idejét, remote címet és user információt.

### `ExceptionHandlingFilter`

Elkapja a nem kezelt kivételeket. API request esetén egységes JSON hibaválaszt ír `ApiResponse.error` formában.

### `CorsFilter`

Credentialös lokális frontend kéréseket enged. Az aktuális `web.xml` allowed origin listája:

- `http://localhost:5173`
- `http://127.0.0.1:5173`
- `http://192.168.97.181:8083`

Expose-olja a `X-Captcha-Id` headert.

### `SecurityHeadersFilter`

Alap biztonsági és cache tiltó headereket ad a válaszhoz, például `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Cache-Control`.

### `CharacterEncodingFilter`

UTF-8 karakterkódolást állít be requestre és response-ra.

### `AuthFilter`

Az `/api/*` útvonalakon ellenőrzi a bejelentkezett application session usert. Public kivételek:

- `/api/auth/login`
- `/api/auth/logout`
- `/api/auth/config`
- `/api/auth/captcha`
- `/api/auth/register`

### `AppSessionContextFilter`

Az application sessionből átmásolja a bejelentkezett user id-t a `SessionContext` thread-local tárolóba. Ezt a DAO audit mezőkitöltése használja.

### `CsrfFilter`

State-changing `/api/*` kéréseknél ellenőrzi az `X-CSRF-Token` headert. A safe HTTP metódusokat, valamint a public login és register POST endpointokat kihagyja.

### `TransactionFilter`

Requestenként tranzakciót nyit. Siker esetén commitol, hiba vagy rollback-only állapot esetén rollbackel.

## 4. Session réteg

### `AppSession`

Store-semleges session modell. Tartalmazza a session id-t, opcionális `AuthenticatedUser` snapshotot, CSRF tokent, időbélyegeket, lejárati időt és typed attribútumokat.

### `AuthenticatedUser`

Sessionben tárolt user snapshot. Tartalma: id, loginName, email, role.

### `AppSessionAttribute` és `AppSessionAttributeType`

Typed JSON session attribútum modell. A jelenlegi konkrét használat a CAPTCHA állapot:

- `attributeName=captcha`
- `attributeType=CAPTCHA_STATE`

### `HttpSessionAppSessionStore`

A Tomcat `HttpSession` működést csomagolja az `AppSessionStore` interfész mögé. Ez az aktuális lokális/default session mód.

### `JdbcAppSessionStore`

PostgreSQL-backed session store cluster tesztekhez. Saját rövid JDBC kapcsolatokat használ, nem függ a request üzleti tranzakciójától. A böngésző felé `CMS_SESSION_ID` cookie-t használ, az adatbázisban pedig a session id SHA-256 hashét tárolja.

Kapcsolódó táblák:

- `app_sessions`
- `app_session_attributes`

### `SessionCookieSupport` és `SessionIdGenerator`

Közös cookie-kezelő és nagy entrópiájú, URL-safe session id generátor external session store-okhoz.

## 5. Rate limiter réteg

### `AttemptRateLimiter`

Sikertelen próbálkozásokra épülő lockout limiter interfész. Login és regisztrációs próbálkozások használják.

### `RequestRateLimiter`

Fix időablakos request limiter interfész. CAPTCHA generálásnál aktív.

### `InMemoryRateLimiter` és `InMemoryRequestRateLimiter`

Process-local memória alapú implementációk. Ez az aktuális lokális/default limiter mód.

### `JdbcAttemptRateLimiter`

PostgreSQL-backed failed-attempt lockout limiter. A `rate_limits` táblát használja.

Aktív namespace-ek:

- `login_failed_attempts`
- `registration_attempts`

### `JdbcRequestRateLimiter`

PostgreSQL-backed fixed-window request limiter. A `rate_limits` táblát használja.

Aktív namespace:

- `captcha_generation`

## 6. Servlet support és API envelope

### `JsonServletSupport`

Közös servlet ősosztály JSON endpointokhoz. Egységes Gson példányt, JSON response írást és hiba envelope kezelést ad.

### `CsrfTokenSupport`

CSRF token generáló segéd. A token tárolását az `AppSessionManager` és az aktuális session store végzi.

### `ApiResponse<T>` és `ApiErrorResponse`

Egységes API válaszmodell. Sikeres válasznál `success=true` és `data`, hibánál `success=false` és `error` mező kerül ki.

## 7. Auth és public endpointok

### `HelloServlet`

`GET /hello`. Egyszerű JSON health endpoint.

### `AuthConfigServlet`

`GET /api/auth/config`. Visszaadja a login és regisztrációs CAPTCHA kapcsolókat, valamint a frontend számára releváns jelszó policyt.

### `CaptchaServlet`

`GET /api/auth/captcha`. SVG CAPTCHA-t generál, a CAPTCHA id-t `X-Captcha-Id` headerben küldi vissza, a megoldást és metaadatokat pedig az application sessionben tárolja typed attribútumként.

### `AuthServlet`

`POST /api/auth/login`. JSON bodyból `LoginRequest` DTO-t olvas, opcionálisan CAPTCHA-t validál, majd `AuthService` segítségével hitelesít. Siker esetén authenticated application sessiont hoz létre, `AuthenticatedUser` snapshotot és CSRF tokent tárol, majd `AuthUserResponse` választ ad.

### `MeServlet`

`GET /api/auth/me`. Az aktuális session usert és CSRF tokent adja vissza. Bejelentkezés nélkül `AUTH_REQUIRED` hibát ad.

### `LogoutServlet`

`POST /api/auth/logout`. Invalidálja az aktuális application sessiont.

### `RegisterServlet`

`POST /api/auth/register`. Public regisztrációt kezel. Rate limiteli a próbálkozásokat, CAPTCHA-t validál, password policyt ellenőriz, majd inactive, pending `USER` fiókot hoz létre.

## 8. Admin user endpointok

### `UserServlet`

Admin-only user management a `/api/users` és `/api/users/*` útvonalakon.

Aktuális műveletek:

- `GET /api/users`: felhasználók listázása
- `GET /api/users/{id}`: egy felhasználó lekérése
- `POST /api/users`: user létrehozása
- `PUT /api/users/{id}`: user módosítása
- `DELETE /api/users/{id}`: soft deactivation
- `POST /api/users/{id}/approve`: regisztráció jóváhagyása
- `POST /api/users/{id}/reject`: regisztráció elutasítása

Az admin jogosultságot a servlet külön ellenőrzi az `AuthenticatedUser.role == ADMIN` feltétellel.

### Menü endpointok

Az admin végpontok `ADMIN` jogosultságot igényelnek, a state-changing műveletekre a közös CSRF szabály érvényes.

- `GET /api/menus`: menük listázása
- `GET /api/menus/{id}`: egy menü lekérése
- `POST /api/menus`: menü létrehozása
- `PUT /api/menus/{id}`: menü módosítása
- `DELETE /api/menus/{id}`: menü és menüpontjainak törlése
- `GET /api/menus/{id}/items`: menüpontok lapos, rendezett listája
- `POST /api/menu-items`: menüpont létrehozása
- `PUT /api/menu-items/{id}`: menüpont módosítása
- `DELETE /api/menu-items/{id}`: menüpont és gyermek-részfájának törlése

A `GET /api/public/menus/{code}` publikus, autentikáció nélküli endpoint. Csak aktív menüt és látható elemeket ad vissza fa struktúrában. A válasz elemei tartalmazzák a `title`, `targetType`, `pageId`, `targetUrl` és `children` mezőket.

Aktuális lokális `FOOTER` elemek:

- `ÁSZF` -> PAGE, `aszf` oldal
- `GDPR` -> PAGE, `gdpr` oldal
- `Support` -> URL, `mailto:support@example.com`

### Template és Site Settings endpointok

A Template konfigurációs entitás, nem tárol HTML-t vagy React kódot. Az admin API:

- `GET /api/templates`
- `GET /api/templates/{id}`
- `POST /api/templates`
- `PUT /api/templates/{id}`
- `DELETE /api/templates/{id}`: soft deaktiválás

A migráció létrehozza a `STANDARD`, `LANDING` és `BLOG` template-eket. A Page request és response DTO-k `templateId` mezőt tartalmaznak. Hiányzó értéknél a Page service a `STANDARD` template-et menti.

A Site Settings a Page és Template moduloktól független globális webhely-konfiguráció:

- `GET /api/site-settings`
- `PUT /api/site-settings`

Az adatbázis és a service egyetlen `site_settings` rekordot tart fenn.

### PageType és PageBlock endpointok

A Page `pageType` mezője:

- `CONTENT`: a `content` kötelező
- `BLOCK`: a `content` opcionális, az oldal PageBlock rekordokból épül fel

Admin endpointok:

- `GET /api/pages/{id}/blocks`
- `GET /api/page-blocks/{id}`
- `POST /api/page-blocks`
- `PUT /api/page-blocks/{id}`
- `DELETE /api/page-blocks/{id}`
- `GET /api/pages/{id}?includeBlocks=true`: összetett `{page, blocks}` válasz

A blokkok `sortOrder`, majd id szerint rendezettek. A `blockType` szabad szöveg, a `configJson` pedig backend oldali értelmezés és validáció nélkül tárolódik.

## 9. DTO-k

Auth DTO-k:

- `LoginRequest`: loginName, password, captchaId, captchaAnswer, captchaHoneypot
- `RegisterRequest`: loginName, userName, emailAddress, password, captchaId, captchaAnswer, captchaHoneypot
- `AuthenticatedUser`: session user snapshot
- `AuthUserResponse`: user adatok és CSRF token
- `AuthConfigResponse`: CAPTCHA és password policy config
- `PasswordPolicyResponse`: frontendnek küldött password policy

User DTO-k:

- `UserRequestBase`: admin create/update user requestek kozos mezoinek alap DTO-ja
- `CreateUserRequest`: admin user létrehozási request
- `UpdateUserRequest`: admin user módosítási request
- `UserResponse`: password hash nélküli user válasz

Menü DTO-k:

- `MenuResponse`, `CreateMenuRequest`, `UpdateMenuRequest`
- `MenuItemResponse`, `CreateMenuItemRequest`, `UpdateMenuItemRequest`
- `PublicMenuItemResponse`: rekurzív publikus faelem
- a menüpont DTO-k a `targetType` és `targetUrl` mezőket is kezelik

Template és Site Settings DTO-k:

- `TemplateResponse`, `CreateTemplateRequest`, `UpdateTemplateRequest`
- `SiteSettingsResponse`, `SaveSiteSettingsRequest`

PageBlock DTO-k:

- `PageBlockResponse`
- `CreatePageBlockRequest`
- `UpdatePageBlockRequest`
- `PageWithBlocksResponse`

## 10. Service réteg

### `AuthService`

Login üzleti logika. Login név alapján betölti a usert, BCrypttel ellenőrzi a jelszót, figyeli az active állapotot, és sikertelen próbálkozásokat lockout limiterrel kezel.

### `RegistrationService`

Public regisztráció üzleti logika. Validálja az inputot, CAPTCHA-t, email formátumot, duplicate login/email állapotot és password policyt, majd inactive, pending `USER` fiókot hoz létre BCrypt hash-sel.

### `CaptchaService`

Matematikai SVG CAPTCHA generálás és validáció. Kezeli a TTL-t, próbálkozásszámot, minimum megoldási időt és purpose kötést.

### `UserService`

Admin user management üzleti logika. Listáz, lekér, létrehoz, módosít, deaktivál, regisztrációt jóváhagy vagy elutasít. Ellenőrzi a kötelező mezőket, email formátumot, duplicate login/email állapotot és password policyt.

### `MediaService`

Admin media library üzleti logika. Listázza és lekéri a média metaadatokat, kezeli a multipart feltöltést, hard delete-et, valamint külön metódussal adja vissza a média bináris tartalmat. A sima `GET /api/media/{id}` csak JSON metaadatot ad, a `GET /api/media/{id}/content` pedig JSON envelope nélküli bináris választ küld `Content-Type`, `Content-Length` és `Content-Disposition: inline` headerekkel.

### `MenuService`

Admin menü CRUD üzleti logika. Validálja a kötelező `name` és `code` mezőket, biztosítja a code egyediségét, és DTO-ra mapeli a menüket. A `MAIN` és `FOOTER` alapmenüket a V10 migráció idempotensen létrehozza.

### `MenuItemService`

Menüpont CRUD, hierarchia-validáció és publikus faépítés. A parent csak ugyanahhoz a menühöz tartozhat, és ciklus nem hozható létre. A target normalizálása create és update esetén azonos:

- `PAGE`: `pageId` kötelező, `targetUrl` mentéskor `null`
- `URL`: `targetUrl` kötelező, trimelve tárolódik, `pageId` mentéskor `null`
- hiányzó `targetType`: visszafelé kompatibilisen `PAGE`

A publikus fa csak aktív menüt és látható elemeket ad vissza, `sortOrder`, majd id szerint rendezve.

### `TemplateService`

Template listázás, lekérés, létrehozás, módosítás, code alapú keresés és deaktiválás. Validálja a kötelező `code` és `name` mezőket, valamint a code egyediségét.

### `SiteSettingsService`

Betölti vagy szükség esetén létrehozza az egyetlen globális beállításrekordot, majd teljes mezőkészlettel frissíti azt.

### `PageBlockService`

Blokklistázás, lekérés, létrehozás, módosítás és törlés. Ellenőrzi a `pageId` és `blockType` mezőket, valamint a hivatkozott Page létezését.

### `PasswordPolicyValidator`

A `PasswordPolicyConfig` alapján listázza a megsértett jelszó szabályokat, például `TOO_SHORT`, `MISSING_UPPERCASE`, `MISSING_DIGIT`.

### Service exceptionök

- `AuthServiceException`
- `UserServiceException`
- `PageServiceException`
- `MediaServiceException`
- `MenuServiceException`

A servlet réteg ezeket HTTP státuszra és egységes JSON hibára fordítja.

## 11. DAO és persistence infrastruktúra

### `CrudDao<T, P>`

Generikus CRUD interfész. Alap műveletek: `findAll`, `findById`, `save`, `create`, `update`, `deleteById`, `delete`.

### `BaseDao<T, P>`

Generikus JDBC DAO alapimplementáció. Reflection és annotáció alapján épít SQL-t, kezeli a CRUD műveleteket, filterezést, rendezést, joinokat, custom SQL helperöket, audit mezőkitöltést és típuskonverziókat. A joinos select SQL ugyanabban a builder útvonalban építi a joinolt táblák mezőit és a tényleges SQL `JOIN` clause-okat. A `delete(entity)` az entity id-je alapján delegál `deleteById`-re, a `deleteEntity(entity)` pedig a `DaoRegistry` alapján statikus segédként használható.

### `UserDao` és `UserDaoImpl`

User-specifikus DAO interfész és JDBC implementáció. A generikus CRUD műveletek mellett login név és email alapú keresést ad.

### Menü DAO-k

- `MenuDao` / `MenuDaoImpl`: generikus CRUD és `findByCode`
- `MenuItemDao` / `MenuItemDaoImpl`: generikus CRUD, `findByMenuId` és `findRootItems`

A `BaseDao` a `MenuItem` annotált `target_type` és `target_url` mezőit automatikusan kezeli INSERT, UPDATE, SELECT és row mapping során. Az enum adatbázisban a nevével (`PAGE`, `URL`) tárolódik.

### Template és Site Settings DAO-k

- `TemplateDao` / `TemplateDaoImpl`: `findByCode`, `findActive`, generikus CRUD
- `SiteSettingsDao` / `SiteSettingsDaoImpl`: singleton rekord lekérése és generikus CRUD

### PageBlock DAO

- `PageBlockDao` / `PageBlockDaoImpl`
- `findByPageId`: minden blokk rendezve
- `findVisibleByPageId`: csak látható blokkok rendezve

### DAO annotációk

- `DbTable`: entity osztályhoz tartozó adatbázis tábla neve
- `DbColumn`: entity mezőhöz tartozó oszlopnév, insert/update flags

### DAO segédosztályok

- `RowMapper<T>`
- `DataAccessException`

## 12. Modell és query osztályok

### Entity modellek

- `BaseEntity`: közös `id`
- `AuditableEntity`: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- `User`: `users` táblára mappelt user entity
- `Page`: `pages` táblára mappelt CMS oldal
- `Media`: `media` táblára mappelt média metaadat
- `Menu`: `menus` táblára mappelt menü
- `MenuItem`: `menu_items` táblára mappelt hierarchikus menüpont
- `Template`: `templates` táblára mappelt frontend layout konfiguráció
- `SiteSettings`: `site_settings` táblára mappelt globális webhely-konfiguráció
- `PageBlock`: `page_blocks` táblára mappelt összetett oldalelem
- `PageType`: `CONTENT`, `BLOCK`

### User enumok és propertyk

- `UserRole`: `ADMIN`, `USER`
- `RegistrationState`: `PENDING`, `EMAIL_VERIFICATION_REQUIRED`, `COMPLETED`, `REJECTED`
- `UserProperty`: user query property konstansok
- `AuditableProperty`: közös audit property konstansok auditálható entity-khez

### Menü enumok és propertyk

- `MenuItemTargetType`: `PAGE`, `URL`
- `MenuProperty`: menü query property konstansok
- `MenuItemProperty`: tartalmazza többek között a `TARGET_TYPE` és `TARGET_URL` propertyket

### Query modell

- `BaseProperty`
- `AuditableProperty`
- `QuerySpec<P>`
- `JoinSpec`
- `JoinType`
- `FilterOperation`
- `LikeFilterPosition`
- `SortOrder<P>`
- `SortDirection`

Ezeket a `BaseDao` használja típusosabb filter/sort/join SQL generáláshoz.

## 13. Tipikus request flow-k

### Login

1. `AuthFilter` publicként átengedi a `/api/auth/login` kérést.
2. `CsrfFilter` kihagyja a login POST-ot.
3. `TransactionFilter` tranzakciót nyit.
4. `AuthServlet` beolvassa a `LoginRequest` DTO-t.
5. Ha aktív, az `AppSessionManager.validateCaptcha(...)` validálja és frissíti a sessionben tárolt CAPTCHA állapotot a `CaptchaService` szabályai alapján.
6. `AuthService` a `UserDao` segítségével betölti a usert és BCrypttel ellenőrzi a jelszót.
7. Siker esetén új authenticated application session jön létre, user snapshot és CSRF token tárolódik.

### Public regisztráció

1. `RegisterServlet` beolvassa a `RegisterRequest` DTO-t.
2. A regisztrációs limiter ellenőrzi a próbálkozásokat.
3. Az `AppSessionManager.validateCaptcha(...)` validálja és frissíti a sessionben tárolt CAPTCHA állapotot a `CaptchaService` szabályai alapján.
4. `RegistrationService` validálja az inputot, duplicate állapotot és password policyt.
5. `UserDaoImpl` létrehozza az inactive, pending `USER` rekordot.

### Admin user kezelés

1. `AuthFilter` bejelentkezett sessiont kér.
2. `CsrfFilter` state-changing metódusoknál CSRF tokent kér.
3. `UserServlet` ellenőrzi az admin szerepkört.
4. `UserService` végrehajtja a validációt és üzleti műveletet.
5. `UserDaoImpl` a `BaseDao` generikus SQL műveletein keresztül olvas/ír.

### DAO művelet tranzakcióval

1. `TransactionFilter` `TransactionContext.begin()` hívással request connectiont nyit.
2. Service DAO-t hív.
3. `BaseDao` a `TransactionContext` aktuális connectionjét használja.
4. Siker esetén commit, hiba esetén rollback történik.

## 14. Tesztek

Aktuális JUnit tesztek:

- `UserServiceTest`
- `UserDaoImplTest`
- `CaptchaServiceTest`
- `SecurityConfigTest`
- `RateLimiterConfigTest`
- `InMemoryRequestRateLimiterTest`
- `BaseDaoBooleanMappingTest`
- `AppSessionConfigTest`
- `MediaServiceTest`
- `PageServiceTest`
- `PageDaoImplTest`
- `MenuServiceTest`
- `TemplateAndSiteSettingsServiceTest`
- `PageBlockServiceTest`

A teszt logolási konfiguráció: `src/test/resources/logback-test.xml`.

Aktuális ellenőrzött eredmény: 123 teszt, 0 hiba.

## 15. Docker, CI és futtatási környezet

### `docker-compose.yml`

Lokális compose stack:

- PostgreSQL 15, host port `5433`
- Tomcat backend, host port `8081`
- frontend build konténer a szomszédos `../frontend` projektből
- Nginx reverse/static kiszolgáló, host port `8083`
- Jenkins, host port `8082`

### `Jenkinsfile`

CI/deploy pipeline:

- backend checkout
- frontend checkout `../frontend` könyvtárba
- Maven build `maven:3.9.9-eclipse-temurin-21` konténerben
- WAR deploy a `cms-tomcat` konténerbe `ROOT.war` néven
- frontend rebuild
- health check a `/hello` endpointon

### `docker-compose-swarm-test.yml`

Cluster teszt stack. A Tomcat service környezeti változókkal explicit JDBC módra áll:

- `SESSION_STORE_MODE=jdbc`
- `RATE_LIMITER_STORE_MODE=jdbc`

A swarm fájl jelenleg `cms-swarm-tomcat` esetén 1 replikát állít be, az Nginx service esetén 2 replikát.

## 16. Cluster/JDBC aktuális állapot

Implementált:

- store-független application session facade
- Tomcat `HttpSession` session store
- PostgreSQL-backed session store
- process-local memória rate limiterek
- PostgreSQL-backed rate limiterek
- session és rate limiter táblák migrációi
- swarm teszt konfiguráció JDBC session/rate limiter móddal

Lokális/default működés:

- session: `http`
- rate limiter: `memory`

Cluster teszthez elérhető működés:

- session: `jdbc`
- rate limiter: `jdbc`

Nyitott cluster validációs pontok a `cluster-jdbc-todo.md` alapján:

- több Tomcat replika végigtesztelése login/me/logout/CSRF/CAPTCHA flow-val
- ugyanazon session párhuzamos frissítésének szabályai
- lejárt session és rate limit rekordok takarítási stratégiája
- `AuthenticatedUser` snapshot frissessége admin módosításkor
- DB load, indexelés és tuning ellenőrzése
- későbbi Redis bővítés API freeze pontjai
