# Projektleírás

Ez a dokumentum a backend projekt osztályait logikai sorrendben mutatja be. A rendszer egy framework-light Java 21 servlet/JDBC alapú CMS backend, Tomcat 9 futtatási környezettel, PostgreSQL adatbázissal, Gson JSON feldolgozással és session alapú autentikációval.

## 1. Alkalmazásindítás és globális konfiguráció

### `hu.laci.cms.Main`

Egyszerű belépési pont osztály. A WAR/Tomcat alapú futásban nem ez indítja az alkalmazást, inkább minimális Java entry pointként van jelen.

### `hu.laci.cms.backend.config.database.DatabaseConfigListener`

Servlet context listener. Alkalmazásinduláskor inicializálja az adatbázis kapcsolatkezelést a `DatabaseConfig` segítségével, majd lefuttatja a migrációkat a `DatabaseMigrationRunner` osztályon keresztül. Leálláskor lezárja az adatbázis erőforrásokat.

### `hu.laci.cms.backend.config.database.DatabaseConfig`

Központi adatbázis konfiguráció és connection pool kezelő. A DB beállításokat először környezeti változókból, majd `web.xml` context-param értékekből, végül beépített defaultokból olvassa. HikariCP poolon keresztül ad `Connection` példányokat.

Kapcsolatok:

- használja: `ServletContextParameters`
- használják: DAO-k, `TransactionContext`, migrációs runner
- inicializálja: `DatabaseConfigListener`

### `hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner`

Az adatbázis séma verziózott migrációit futtatja a `src/main/resources/db/migration` könyvtárból. Nyilvántartja a lefutott migrációkat a `schema_migrations` táblában, checksumot ellenőriz, és PostgreSQL advisory lockkal védi a párhuzamos futást.

### `hu.laci.cms.backend.config.database.TransactionContext`

Request-szintű tranzakciós állapotot tart thread-local alapon. A `TransactionFilter` nyitja és zárja a tranzakciót, a DAO réteg pedig ezen keresztül kapja meg az aktuális request connectionjét.

Kapcsolatok:

- használja: `DatabaseConfig`
- használják: `BaseDao`, `TransactionFilter`

### `hu.laci.cms.backend.config.app.DaoRegistryListener`

Servlet context listener. Alkalmazásinduláskor inicializálja a `DaoRegistry` statikus DAO nyilvántartását, leálláskor törli azt.

### `hu.laci.cms.backend.config.app.ServletContextParameters`

Kis segédosztály `ServletContext` init paraméterek olvasásához és típuskonverziójához. A konfigurációs osztályok ezen keresztül olvasnak string, int és boolean értékeket.

### `hu.laci.cms.backend.config.security.SecurityConfigListener`

Servlet context listener. Alkalmazásinduláskor betölti az auth, password policy és CAPTCHA feature flag beállításokat a `SecurityConfig` statikus állapotába. Leálláskor reseteli a konfigurációt.

### `hu.laci.cms.backend.config.security.SecurityConfig`

Központi biztonsági konfiguráció. Tartalmazza a login lockout limitet, lock időt, CAPTCHA kapcsolókat és a `PasswordPolicyConfig` értékeit.

Kapcsolatok:

- használja: `ServletContextParameters`, `PasswordPolicyConfig`
- használják: `AuthServlet`, `RegisterServlet`, `AuthConfigServlet`, `AuthService`, `UserService`

### `hu.laci.cms.backend.config.security.PasswordPolicyConfig`

Immutable password policy értékobjektum. Beállításai: minimum hossz, nagybetű/kisbetű/szám/speciális karakter követelmények.

### `hu.laci.cms.backend.config.session.SessionContext`

Thread-local request context a sessionből származó adatokhoz. Jelenleg az aktuális bejelentkezett user id-t tárolja, amit az audit mezők kitöltése használ.

Kapcsolatok:

- tölti: `HttpSessionContextFilter`
- használja: `BaseDao` audit mezőkhöz

## 2. HTTP request életút és filterek

A filter sorrendet a `web.xml` határozza meg. A kérés a servlet előtt több keresztmetszeti ellenőrzésen megy át.

### `RequestLoggingFilter`

Méri és naplózza a request végleges státuszát, futási idejét, metódusát, URI-ját, távoli címet és user információt.

### `ExceptionHandlingFilter`

Elkapja a nem kezelt kivételeket, és API request esetén egységes JSON hibaválaszt ír `ApiResponse.error` formában.

### `CorsFilter`

Beállítja a CORS headereket a helyi frontend fejlesztési originjeihez. Credentialös kéréseket enged, és expose-olja a `X-Captcha-Id` headert.

### `SecurityHeadersFilter`

Alap biztonsági és cache tiltó headereket ad a válaszhoz, például `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Cache-Control`.

### `CharacterEncodingFilter`

UTF-8 karakterkódolást állít be requestre és response-ra.

### `AuthFilter`

Az `/api/*` útvonalakon ellenőrzi, hogy van-e bejelentkezett session user. Kivételt képeznek a public auth endpointok: login, logout, config, captcha, register. Sikertelen ellenőrzéskor `AUTH_REQUIRED` hibát ír.

Kapcsolatok:

- sessionben ezt keresi: `AuthenticatedUser`
- JSON válaszhoz használja: `ApiResponse`

### `HttpSessionContextFilter`

A HTTP sessionből átmásolja a bejelentkezett user id-t a `SessionContext` thread-local tárolóba, majd request végén törli. Így a DAO audit mezőkitöltésnek nem kell servlet API-t ismernie.

### `CsrfFilter`

Állapotmódosító `/api/*` kéréseknél ellenőrzi az `X-CSRF-Token` headert. Kihagyja a safe metódusokat, valamint a public login és register POST endpointokat.

Kapcsolatok:

- token session attribútum: `CsrfTokenSupport.SESSION_ATTRIBUTE`

### `TransactionFilter`

Requestenként tranzakciót nyit a `TransactionContext` segítségével. Normál lefutásnál commitol, hiba vagy rollback-only állapot esetén rollbackel.

## 3. Servlet support osztályok

### `JsonServletSupport`

Közös servlet ősosztály JSON API végpontokhoz. Tartalmazza a közös Gson példányt, sikeres JSON válasz írását, és egységes error envelope írását.

Kapcsolatok:

- használja: `ApiResponse`, `ApiErrorResponse`
- leszármazottai: auth servlet osztályok és `UserServlet`

### `CsrfTokenSupport`

CSRF token létrehozó és sessionben tároló segéd. Login és `me` válaszoknál biztosítja, hogy a frontend kapjon tokent.

## 4. Public és auth servlet réteg

### `HelloServlet`

Egyszerű health endpoint a `/hello` útvonalon. JSON választ ad, amivel ellenőrizhető, hogy az alkalmazás fut.

### `AuthConfigServlet`

`GET /api/auth/config` endpoint. Visszaadja, hogy loginhoz és regisztrációhoz aktív-e a CAPTCHA, valamint a jelszó policy frontend számára releváns szabályait.

Kapcsolatok:

- használja: `SecurityConfig`
- válasz DTO: `AuthConfigResponse`, `PasswordPolicyResponse`

### `CaptchaServlet`

`GET /api/auth/captcha` endpoint. SVG CAPTCHA-t generál, a CAPTCHA id-t a `X-Captcha-Id` headerbe teszi, a megoldást és metaadatokat sessionben tárolja.

Kapcsolatok:

- használja: `CaptchaService`
- generálási limithez használja: `InMemoryRequestRateLimiter`
- session attribútumok: CAPTCHA id, válasz, purpose, createdAt, attempts

### `AuthServlet`

`POST /api/auth/login` endpoint. JSON bodyból `LoginRequest` DTO-t olvas, opcionálisan CAPTCHA-t validál, majd az `AuthService` segítségével hitelesít. Sikeres login esetén session id-t rotál, sessionbe teszi az `AuthenticatedUser` objektumot, CSRF tokent hoz létre, majd `AuthUserResponse` választ ír.

Kapcsolatok:

- DAO beszerzése: `DaoRegistry.getDao(User.class)`
- service: `AuthService`
- CAPTCHA: `CaptchaService`
- rate limiter: `InMemoryRateLimiter`
- session user DTO: `AuthenticatedUser`

### `MeServlet`

`GET /api/auth/me` endpoint. A sessionben tárolt `AuthenticatedUser` alapján visszaadja az aktuális felhasználót és egy CSRF tokent. Ha nincs user, `AUTH_REQUIRED` hibát ad.

### `LogoutServlet`

`POST /api/auth/logout` endpoint. Ha van session, invalidálja, majd sikeres logout választ ír. Bejelentkezett állapotban CSRF védelem vonatkozik rá.

### `RegisterServlet`

`POST /api/auth/register` endpoint. Public regisztrációt kezel. Rate limiteli a regisztrációs próbálkozásokat, CAPTCHA-t validál, majd a `RegistrationService` segítségével pending, inactive `USER` fiókot hoz létre.

Kapcsolatok:

- DAO beszerzése: `DaoRegistry.getDao(User.class)`
- service: `RegistrationService`
- password policy: `PasswordPolicyValidator`
- CAPTCHA: `CaptchaService`
- regisztrációs limiter: `InMemoryRateLimiter`

### `UserServlet`

Admin-only user CRUD endpointok a `/api/users` és `/api/users/*` útvonalakon.

Fő műveletek:

- `GET /api/users`: felhasználók listázása
- `GET /api/users/{id}`: egy felhasználó lekérése
- `POST /api/users`: user létrehozása
- `PUT /api/users/{id}`: user módosítása
- `DELETE /api/users/{id}`: soft deactivation
- `POST /api/users/{id}/approve`: regisztráció jóváhagyása
- `POST /api/users/{id}/reject`: regisztráció elutasítása

Kapcsolatok:

- admin ellenőrzéshez olvassa: `AuthenticatedUser`
- service: `UserService`
- request DTO-k: `CreateUserRequest`, `UpdateUserRequest`
- response DTO: `UserResponse`

## 5. Auth és user DTO-k

### `LoginRequest`

Login JSON request DTO. Mezői: loginName, password, captchaId, captchaAnswer, captchaHoneypot.

### `RegisterRequest`

Public regisztráció JSON request DTO. Mezői: loginName, userName, emailAddress, password, captchaId, captchaAnswer, captchaHoneypot.

### `AuthenticatedUser`

Sessionben tárolt, serializable user DTO. Csak a session/auth szempontból szükséges adatokat tartalmazza: id, loginName, email, role.

### `AuthUserResponse`

Login és `me` válasz DTO. Az `AuthenticatedUser` adatai mellett CSRF tokent is tartalmaz.

### `AuthConfigResponse`

Auth konfigurációs válasz DTO. Login/registration CAPTCHA kapcsolókat és password policy választ tartalmaz.

### `PasswordPolicyResponse`

Frontendnek küldött password policy DTO. A minimum hosszt és karakterkövetelményeket írja le.

### `CreateUserRequest`

Admin user létrehozási request DTO. Tartalmazza a login, név, email, jelszó, role, active és registrationStatus mezőket.

### `UpdateUserRequest`

Admin user módosítási request DTO. A create requesthez hasonló, de üres jelszó esetén a meglévő hash megmarad.

### `UserResponse`

User API válasz DTO. Nem tartalmaz password hash-t, viszont tartalmazza az id-t, login/user/email adatokat, role-t, active állapotot, registrationStatus-t és audit timestamp mezőket.

### `ApiResponse<T>`

Közös API envelope sikeres és hibás válaszokhoz. Siker esetén `success=true` és `data`, hiba esetén `success=false` és `error`.

### `ApiErrorResponse`

Közös hiba DTO. Tartalmazza a hibakódot, üzenetet, opcionális validációs hibakód listát.

## 6. Service réteg

### `AuthService`

Login üzleti logika. Login név alapján betölti a usert, BCrypttel ellenőrzi a jelszót, figyeli az active állapotot, és sikertelen próbálkozásokat lockout limiterrel kezeli.

Kapcsolatok:

- DAO: `UserDao`
- limiter: `InMemoryRateLimiter`
- kivétel wrapper: `AuthServiceException`

### `AuthServiceException`

Auth service infrastruktúra vagy váratlan hiba RuntimeException típusa.

### `RegistrationService`

Public regisztráció üzleti logika. Validálja a requestet, CAPTCHA-t, email formátumot, duplicate login/email állapotot, password policyt, majd inactive, pending `USER` fiókot hoz létre BCrypt hash-sel.

Kapcsolatok:

- DAO: `UserDao`
- password policy: `PasswordPolicyValidator`
- CAPTCHA validálás: `CaptchaService`
- hibákhoz használja: `UserServiceException`

### `CaptchaService`

Matematikai SVG CAPTCHA generálás és validáció. Kezeli a 3 perces TTL-t, 2 próbálkozást, 1 másodperces minimum megoldási időt, és a login/registration purpose kötést. Több feladatvariánst és SVG zajt generál.

Kapcsolatok:

- visszaadja: `CaptchaChallenge`
- validációs eredmény: `CaptchaValidationResult`
- session attribútum neveket konstansként biztosítja servlet rétegnek

### `CaptchaChallenge`

CAPTCHA generálás eredménye: id, expectedAnswer, SVG string.

### `UserService`

Admin user management üzleti logika. Listáz, lekér, létrehoz, módosít, deaktivál, regisztrációt jóváhagy vagy elutasít. Ellenőrzi a kötelező mezőket, email formátumot, duplicate login/email állapotot és password policyt. BCrypt hash-t állít elő.

Kapcsolatok:

- DAO: `UserDao`
- password policy: `PasswordPolicyValidator`
- request DTO-k: `CreateUserRequest`, `UpdateUserRequest`
- response DTO: `UserResponse`
- model: `User`, `UserRole`, `RegistrationState`

### `UserServiceException`

User és regisztrációs service hibák RuntimeException típusa. Hibakódot és opcionális validációs hibalistát hordoz, amit a servlet réteg HTTP státuszra és JSON hibára fordít.

### `PasswordPolicyValidator`

Jelszó policy ellenőrző. A `PasswordPolicyConfig` alapján listázza a megsértett szabályokat, például `TOO_SHORT`, `MISSING_UPPERCASE`, `MISSING_DIGIT`.

### `InMemoryRateLimiter`

Sikertelen próbálkozásokra épülő lockout limiter. Login és regisztrációs próbálkozás limiteléshez használatos.

### `InMemoryRequestRateLimiter`

Fix időablakos request limiter. CAPTCHA generálásnál használatos, ahol rövid idő alatt túl sok challenge kérését kell megfogni.

## 7. DAO réteg és persistence infrastruktúra

### `DaoRegistry`

Statikus registry, amely entity class alapján visszaadja a hozzá tartozó DAO-t. Jelenleg `User.class -> UserDaoImpl` regisztrációt tartalmaz.

Kapcsolatok:

- inicializálja: `DaoRegistryListener`
- használják: servlet osztályok, `BaseDao` statikus helper metódusai

### `CrudDao<T, P>`

Generikus CRUD DAO interfész. Alap műveletek: findAll, findById, save, create, update, deleteById.

### `BaseDao<T, P>`

Generikus JDBC DAO alapimplementáció. Reflection és annotation alapján épít SQL-t, kezeli a CRUD műveleteket, filterezést, rendezést, joinokat, custom SQL helperöket, audit mezőkitöltést és típuskonverziókat.

Kapcsolatok:

- használja: `TransactionContext`
- entity metadata: `DbTable`, `DbColumn`
- query API: `QuerySpec`, `JoinSpec`, `SortOrder`, `FilterOperation`
- audit user: `SessionContext`

### `RowMapper<T>`

ResultSet sorból objektumot előállító funkcionális interfész. DAO custom query-k és belső mapping használja.

### `DataAccessException`

DAO réteg RuntimeException típusa. SQL és adat-hozzáférési hibákat csomagol.

### `UserDao`

User-specifikus DAO interfész. A generikus `CrudDao<User, UserProperty>` mellé login név és email alapú keresést definiál.

### `UserDaoImpl`

User DAO JDBC implementáció. A `BaseDao<User, UserProperty>` funkcióira épül, és megvalósítja a `findByLoginName` és `findByEmail` metódusokat.

## 8. DAO annotációk

### `DbTable`

Entity osztály annotáció. Megadja az adatbázis tábla nevét, amelyből a `BaseDao` SQL-t generál.

### `DbColumn`

Entity mező annotáció. Megadja az oszlopnevet, valamint hogy a mező insertelhető és/vagy updatelhető-e.

## 9. Modell és query osztályok

### `BaseEntity`

Minden persistált entity őse. Az `id` mezőt tartalmazza.

### `AuditableEntity`

Auditálható entity ős. A `BaseEntity` mezői mellé `createdAt`, `updatedAt`, `createdBy`, `updatedBy` mezőket ad.

### `User`

Felhasználó persistence entity. `AuditableEntity` leszármazott, `users` táblára mappel. Tartalmazza a userName, loginName, emailAddress, passwordHash, role, active és registrationState mezőket.

Kapcsolatok:

- DAO: `UserDaoImpl`
- service: `AuthService`, `UserService`, `RegistrationService`
- API-ba közvetlenül nem kerül ki; `UserResponse` és `AuthenticatedUser` DTO-kra mappelődik

### `UserRole`

Felhasználói szerepkör enum. Értékei: `ADMIN`, `USER`.

### `RegistrationState`

Regisztrációs életciklus enum. Értékei: `PENDING`, `EMAIL_VERIFICATION_REQUIRED`, `COMPLETED`, `REJECTED`.

### `UserProperty`

User query property konstansok gyűjtője. A `QuerySpec` és `BaseDao` típusbiztosabb filter/sort/join felületéhez ad property objektumokat.

### `BaseProperty`

Query property alapmodell. Tartalmazza a Java property nevet, és helper metódusokat filter, sort, like, join jellegű query elemek felépítéséhez.

### `QuerySpec<P>`

Generikus query leíró. Tartalmazhat filtereket, sortokat és joinokat. A DAO réteg ebből épít SQL WHERE, ORDER BY és JOIN részeket.

### `JoinSpec`

Join leíró objektum. Meghatározza a join típusát, forrás/target entityt, összekötő propertyket, cél mapping propertyt, SQL aliast és opcionális extra feltételeket.

### `JoinType`

Join típus enum. Például `INNER` és `LEFT`.

### `FilterOperation`

Filter műveletek enumja. Támogatott műveletek például `EQUALS`, `LIKE`, `LESS`, `GREATER`, `IN`, `BETWEEN`.

### `LikeFilterPosition`

LIKE filter wildcard pozíció enum. Meghatározza, hogy a `%` prefixként, suffixként vagy mindkét oldalon kerüljön a paraméterhez.

### `SortOrder<P>`

Egy rendezési mező és irány értékobjektuma. A `QuerySpec` használja.

### `SortDirection`

Rendezési irány enum. Értékei tipikusan `ASC` és `DESC`.

## 10. Tipikus request folyamatok

### Login

1. `AuthFilter` publicként átengedi a `/api/auth/login` kérést.
2. `CsrfFilter` kihagyja a login POST-ot.
3. `TransactionFilter` tranzakciót nyit.
4. `AuthServlet` beolvassa a `LoginRequest` DTO-t.
5. Ha aktív, `CaptchaService` validálja a sessionben tárolt CAPTCHA-t.
6. `AuthService` a `UserDao` segítségével betölti a usert és BCrypttel ellenőrzi a jelszót.
7. Siker esetén `AuthServlet` sessiont rotál, `AuthenticatedUser` objektumot és CSRF tokent tárol, majd `AuthUserResponse` választ ad.

### Public regisztráció

1. `RegisterServlet` beolvassa a `RegisterRequest` DTO-t.
2. `CaptchaService` ellenőrzi a CAPTCHA id-t, választ, TTL-t, próbálkozásszámot és purpose-t.
3. `RegistrationService` validálja az inputot, duplicate állapotot és password policyt.
4. `UserDaoImpl` létrehozza az inactive, pending `USER` rekordot.
5. A response `UserResponse`, password hash nélkül.

### Admin user kezelés

1. `AuthFilter` bejelentkezett sessiont kér.
2. `CsrfFilter` state-changing metódusoknál CSRF tokent kér.
3. `UserServlet` külön ellenőrzi, hogy az `AuthenticatedUser.role == ADMIN`.
4. `UserService` végrehajtja a validációt és üzleti műveletet.
5. `UserDaoImpl` a `BaseDao` generikus SQL műveletein keresztül olvas/ír.

### DAO művelet tranzakcióval

1. `TransactionFilter` `TransactionContext.begin()` hívással request connectiont nyit.
2. Service DAO-t hív.
3. `BaseDao` a `TransactionContext` aktuális connectionjét használja.
4. Sikeres request végén commit, hiba esetén rollback történik.
