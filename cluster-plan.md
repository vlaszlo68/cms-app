# Cluster Plan

## Cel

A jelenlegi CMS backend tobb Tomcat replika mellett is megbizhatoan mukodjon Docker Swarm vagy mas cluster kornyezetben ugy, hogy a mostani egyszeru, lokalis `HttpSession` alapu mukodes tovabbra is megmaradjon.

A valtas konfiguracio alapjan tortenjen. A konfiguracio feloldasi sorrendje kotelezoen:

1. environment variable,
2. `web.xml` context-param,
3. fallback: hagyomanyos jelenlegi `HttpSession` mukodes.

A store modok:

- `http`: jelenlegi, Tomcat memoriaban tarolt HTTP session
- `jdbc`: uj, PostgreSQL-ben tarolt alkalmazas-session
- `redis`: kesobbi, Redisben tarolt alkalmazas-session

Az elso kor celja nem a teljes auth rendszer ujrairasa, hanem a session allapot hozzaferesenek kozpontositasa es a store csereje. Az elso implementacios korben csak a `http` es `jdbc` store keszuljon el, de az absztrakcio legyen ugy kialakitva, hogy a `redis` kesobb uj `AppSessionStore` implementaciokent legyen hozzaadhato a servlet/filter/auth retegek ujabb atirasa nelkul.

## Kiindulo allapot

### Architektura

A backend jelenleg framework-light Java 21 servlet/JDBC alkalmazas:

- Tomcat 9 WAR deployment
- Servlet API, nincs Spring
- JDBC + HikariCP
- PostgreSQL
- Gson JSON
- request-scoped tranzakciok `TransactionFilter` + `TransactionContext` alapon
- adatbazis migraciok `src/main/resources/db/migration/` alatt

A reteghatarok:

- DAO: SQL es mapping
- Service: uzleti logika
- Servlet/filter: HTTP, JSON, session, security

Az uj session store-nak ezt a felosztast kovetnie kell. A servlet/filter reteg kezelheti a sessiont, a DAO reteg nem tudhat HTTP-rol vagy cookie-rol.

### Jelenlegi request filter sorrend

`web.xml` szerinti aktualis sorrend:

1. `requestLoggingFilter`
2. `exceptionHandlingFilter`
3. `corsFilter`
4. `securityHeadersFilter`
5. `characterEncodingFilter`
6. `authFilter`
7. `httpSessionContextFilter`
8. `csrfFilter`
9. `transactionFilter`

Fontos kovetkezmeny: jelenleg az `AuthFilter`, `HttpSessionContextFilter` es `CsrfFilter` a `TransactionFilter` elott fut. Ez `jdbc` session store eseten problema lehet, mert a session betoltes/mentes adatbazist igenyelne, de a request-scoped tranzakcio meg nincs megnyitva.

Ezert a session store bevezetesenel kulon dontes kell:

- vagy a session store sajat, rovid DB kapcsolatokat hasznal a `DatabaseConfig.getConnection()` felol,
- vagy a filter sorrendet at kell rendezni ugy, hogy a `TransactionFilter` a sessiont hasznalo filterek elott fusson.

Javasolt elso megoldas: a `JdbcAppSessionStore` sajat, explicit kapcsolatot hasznaljon, es ne fuggjon a request uzleti tranzakciojatol. Igy az auth/CSRF ellenorzes nem keveredik a kesobbi uzleti muvelet rollback/commit eletciklusaval.

### Jelenlegi session adatok

Most a kovetkezo adatok elnek `HttpSession`-ben:

- `user`: `hu.laci.cms.backend.dto.auth.AuthenticatedUser`
- `csrfToken`: CSRF token
- CAPTCHA allapot:
  - captcha id
  - expected answer
  - purpose
  - createdAt
  - attempts

Ezeket jelenleg kozvetlenul olvassak/irjak:

- `AuthServlet`
- `MeServlet`
- `LogoutServlet`
- `CaptchaServlet`
- `RegisterServlet`
- `UserServlet`
- `AuthFilter`
- `CsrfFilter`
- `HttpSessionContextFilter`
- `RequestLoggingFilter`
- `CsrfTokenSupport`

### Cluster problema

Swarm alatt a frontend Nginx statikus fajlokat szolgal ki es `/api/` alatt proxyz. A frontend Nginx alapvetoen stateless.

A backend Tomcat jelenleg stateful, mert a session allapot a Tomcat process memoriaban van. Ha ugyanazon bongeszo `JSESSIONID` cookie-val masik Tomcat replikara kerul, az a replika nem latja:

- a bejelentkezett usert,
- a CSRF tokent,
- a korabban generalt CAPTCHA allapotot.

A PostgreSQL stateful es kozos minden backend replika szamara, ezert a session allapot kozos tarolasara alkalmas.

Sticky session Nginx/Swarm oldalon rovid tavon segithet, de nem eleg jo vegallapot:

- kontener ujraindulasnal session veszik,
- rolling update alatt session szakadhat,
- Swarm VIP mogott az open source Nginx nem feltetlenul latja kulon az egyes Tomcat taskokat,
- az alkalmazas tovabbra is backend memoriahoz kotott marad.

## Cel architektura

### Uj session absztrakcio

Be kell vezetni egy alkalmazasszintu session reteget, amely elrejti, hogy az allapot `HttpSession`-ben, adatbazisban vagy kesobb Redisben van.

Javasolt csomag:

```text
hu.laci.cms.backend.config.session
```

Ez a csomag mar letezik `SessionContext` miatt, de annak szerepe request-local audit context. Az uj komponensek az alkalmazas-session tarolasat kezelnek.

Javasolt osztalyok/interfeszek:

```text
config.session
|-- AppSession.java
|-- AppSessionAttribute.java
|-- AppSessionAttributeType.java
|-- AppSessionStore.java
|-- AppSessionManager.java
|-- AppSessionConfig.java
|-- AppSessionConfigListener.java
|-- AppSessionStoreMode.java
|-- HttpSessionAppSessionStore.java
|-- JdbcAppSessionStore.java
|-- RedisAppSessionStore.java
|-- SessionCookieSupport.java
`-- SessionIdGenerator.java
```

Megjegyzes: a `RedisAppSessionStore` ebben a fejlesztesi korben csak tervezett bovitesi pont. A fajlt/osztalyt nem kell letrehozni addig, amig Redis store tenylegesen nem keszul. Az architekturanak viszont mar most ugy kell kinezni, hogy kesobb ez az osztaly a store interfesz moge illesztheto legyen. Ez kotelezo elfogadasi feltetel, nem opcionalis optimalizacio.

Elvart fuggosegi irany:

```text
Servlets / Filters
      |
      v
AppSessionManager
      |
      v
AppSessionStore
      |
      +-- HttpSessionAppSessionStore
      +-- JdbcAppSessionStore
      `-- RedisAppSessionStore
```

Ebbol kovetkezo szabaly: a servlet/filter/auth osztalyok nem tartalmazhatnak `HttpSession`, JDBC vagy Redis specifikus session tarolasi dontest. Ezek csak az `AppSessionManager` publikus muveleteit hivhatjak.

### `AppSession`

Immutable vagy kontrollaltan modosithato value object, amely a jelenlegi sessionben tarolt adatokat tartalmazza.

Javasolt mezok:

```java
String id;
AuthenticatedUser authenticatedUser;
String csrfToken;
Map<String, AppSessionAttribute> attributes;
Instant createdAt;
Instant lastAccessedAt;
Instant expiresAt;
boolean dirty;
boolean invalidated;
```

Megfontolas:

- `authenticatedUser` nullable, mert CAPTCHA mar login elott is sessiont igenyel.
- az osszetett vagy workflow-specifikus session adatok nem kulon mezok, hanem typed attribumok.
- `dirty` flag segit elkerulni felesleges DB update-eket.
- `invalidated` flag logout es lejart session torles miatt hasznos.

### `AppSessionAttribute`

Altalanos session attribum modell osszetettebb, rovid eletu vagy jovobeli session adatokhoz.

Javasolt mezok:

```java
String name;
AppSessionAttributeType type;
String jsonValue;
Instant createdAt;
Instant updatedAt;
```

Az `AppSessionAttribute` celja, hogy a session alapmodell ne bovuljon minden uj workflow-specifikus adattal. A store implementacio dontheti el, hogy az attribumot SQL masodik tablaban, Redis hash mezoben vagy Redis JSON-ben tarolja.

### `AppSessionAttributeType`

Enum jellegu tipusleiras a session attribum JSON tartalmanak ertelmezesehez.

Elso ismert ertek:

```text
CAPTCHA_STATE
```

Kesobbi peldak:

```text
FORM_DRAFT
WIZARD_STATE
TEMP_UPLOAD_STATE
MFA_CHALLENGE_STATE
```

Fontos: az `attribute_name` es az `attribute_type` kulon fogalom. A name a sessionen beluli logikai kulcs, peldaul `captcha`; a type azt mondja meg, milyen JSON objektum van ott, peldaul `CAPTCHA_STATE`.

### `AppSessionStore`

Store interfesz, amelynek elso korben ket implementacioja lesz: `HttpSessionAppSessionStore` es `JdbcAppSessionStore`. Kesobb ugyanide kerulhet `RedisAppSessionStore` harmadik implementaciokent.

Javasolt felelosseg:

- session keresese requestbol
- uj session letrehozasa
- session mentese
- session invalidalasa
- lejart session figyelmen kivul hagyasa

Javasolt metodusok:

```java
Optional<AppSession> find(HttpServletRequest request, HttpServletResponse response);
AppSession create(HttpServletRequest request, HttpServletResponse response);
void save(HttpServletRequest request, HttpServletResponse response, AppSession session);
void invalidate(HttpServletRequest request, HttpServletResponse response);
```

A `HttpServletResponse` azert kell, mert cookie-t kell tudni allitani/torolni.

Redis-bovithethosegi kovetelmenyek:

- az interfesz ne tartalmazzon JDBC-specifikus fogalmakat, peldaul SQL-t, connectiont, row versiont vagy `id_hash` mezot,
- az interfesz ne tartalmazzon Redis-specifikus fogalmakat, peldaul key prefixet, TTL parancsot vagy Redis client tipust,
- minden store ugyanazt az `AppSession` alkalmazasmodellt kapja es adja vissza,
- a store-on kivuli kodnak nem kell tudnia, hogy az adott session oszlopokban, JSON-ben, Redis hash-ben vagy Tomcat session attribumokban van tarolva.
- osszetett session adatokhoz a store-ok ugyanazt az `AppSessionAttribute` modellt hasznaljak, nem store-specifikus payload API-t.

### `AppSessionManager`

Kozponti facade, amit a servlet/filter reteg hasznal.

Javasolt feladatok:

- az aktualis store kivalasztasa
- session betoltese es request attribute-ba cache-elese
- authenticated user lekerese
- CSRF token biztositas
- `CAPTCHA_STATE` attribum olvasasa/irasa a CAPTCHA workflow szamara
- altalanos typed session attribumok olvasasa/irasa jovobeli workflow-k szamara
- logout invalidalas
- store-specifikus reszletek elrejtese a servlet/filter/auth kod elol

Javasolt request attribute:

```text
hu.laci.cms.backend.config.session.AppSession
```

Igy egy request alatt nem kell tobbszor DB-bol sessiont olvasni.

Stabil szerzodes: ha a Redis store kesobb bekerul, akkor az `AppSessionManager` publikus felulete nem valtozhat. Csak a store factory/config kaphat uj `REDIS` agat, es csak az uj Redis store-hoz kozvetlenul tartozo osztalyok/deployment beallitasok valtozhatnak. Az auth servlet, CSRF filter, auth filter, CAPTCHA servlet es logout/me endpoint kodjahoz Redis bevezetese miatt nem szabad ujra hozzanyulni.

### `AppSessionConfig`

Environment-first konfiguracio, `web.xml` fallbackkel.

Kotelezo feloldasi sorrend:

1. `SESSION_STORE_MODE` environment variable,
2. `session.store.mode` `web.xml` context-param,
3. bepitett default: `http`.

Ez azt jelenti, hogy ha sem environment variable, sem `web.xml` param nincs megadva, az alkalmazas pontosan a hagyomanyos mostani `HttpSession` alapu mukodessel indul.

Javasolt parameterek:

```xml
<context-param>
    <param-name>session.store.mode</param-name>
    <param-value>http</param-value>
</context-param>

<context-param>
    <param-name>session.cookie.name</param-name>
    <param-value>CMS_SESSION_ID</param-value>
</context-param>

<context-param>
    <param-name>session.timeout.minutes</param-name>
    <param-value>30</param-value>
</context-param>

<context-param>
    <param-name>session.cookie.secure</param-name>
    <param-value>false</param-value>
</context-param>

<context-param>
    <param-name>session.cookie.sameSite</param-name>
    <param-value>Lax</param-value>
</context-param>
```

Alapertelmezes:

- `session.store.mode=http`, hogy a mostani mukodes regresszio nelkul megmaradjon.
- `session.cookie.secure=false` lokalis HTTP fejlesztes miatt.
- production HTTPS mellett `session.cookie.secure=true`.

Kornyezeti valtozo tamogatas opcionaalis, de ajanlott a Docker/Swarm miatt:

- `SESSION_STORE_MODE`
- `SESSION_COOKIE_NAME`
- `SESSION_TIMEOUT_MINUTES`
- `SESSION_COOKIE_SECURE`
- `SESSION_COOKIE_SAMESITE`

Kesobbi Redis store-hoz varhato uj konfiguracio:

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `REDIS_DATABASE`
- `REDIS_TIMEOUT_MILLIS`

Ezeket nem kell bevezetni a `jdbc` korben, de a `AppSessionConfig` es `AppSessionStoreMode` kialakitasanal ne legyen olyan dontes, ami egy kesobbi `REDIS` modot nehezitene.

A projektben a DB config mar env-first modellt hasznal. Uj session config eseteben kotelezo ugyanezt a mintat kovetni:

1. environment variable
2. `web.xml` context-param
3. bepitett default, amely session store modnal `http`

## Store modok

### `http` mod

Cel: a jelenlegi viselkedes minimalis valtoztatassal megmaradjon.

Mukodes:

- tovabbra is Tomcat `HttpSession` hasznalat
- `user` es `csrfToken` ugyanazokkal az attribumnevekkel maradhat
- a CAPTCHA allapot a manager felol `CAPTCHA_STATE` typed attribum, de a `HttpSessionAppSessionStore` belul megtarthatja a jelenlegi CAPTCHA session attribumnevekhez valo lekepezest
- login eseten `request.changeSessionId()` megmaradhat
- logout eseten `session.invalidate()` megmaradhat

Ebben a modban nem kell sajat `CMS_SESSION_ID` cookie. A Tomcat `JSESSIONID` maradhat.

Elony:

- lokalis Tomcat fejlesztes es jelenlegi tesztek kis kockazattal tovabb mukodnek
- a store absztrakcio bevezetese utan is ellenorizheto, hogy nem valtozott az API viselkedes

### `jdbc` mod

Cel: tobb Tomcat replika kozos session allapotot hasznaljon.

Mukodes:

- a bongeszo sajat alkalmazas cookie-t kap, peldaul `CMS_SESSION_ID`
- a cookie erteke random, nagy entropiaju session id
- DB-ben nem a nyers id, hanem hash tarolodik
- minden backend replika ugyanabbol a DB tablabol olvassa/irja a sessiont
- lejart session nem ervenyes
- logout torli vagy invalidalja a DB session rekordot es torli a cookie-t

Javasolt session id:

- 32 byte cryptographic random
- Base64 URL-safe, padding nelkul
- DB-ben SHA-256 hash

Javasolt cookie tulajdonsagok:

- `HttpOnly`
- `Path=/`
- `SameSite=Lax`
- `Secure` csak HTTPS eseten
- `Max-Age` vagy session cookie dontes szerint

Elso korben session cookie eleg, mert a backend DB-ben kezeli a lejarest.

### `redis` mod kesobbi boviteskent

Cel: ugyanazt az alkalmazas-session allapotot Redisben tarolni, elsosorban gyors, TTL-alapu, cluster-kompatibilis mukodeshez.

Mukodes:

- a bongeszo ugyanugy alkalmazas cookie-t kap, peldaul `CMS_SESSION_ID`
- a cookie erteke tovabbra is random, nagy entropiaju session id
- Redisben a session kulcs a session id hash alapjan kepzodik
- a session ertek tarolhato JSON-kent vagy Redis hash mezokben
- az osszetett session adatok ugyanazzal az `attribute_name`, `attribute_type`, `json_value` modellel tarolodnak, mint JDBC modban
- a session lejaratat Redis TTL kezeli
- logout torli a Redis kulcsot es a cookie-t

Pelda Redis kulcs:

```text
cms:session:<sessionIdHash>
cms:session:<sessionIdHash>:attr:captcha
```

Fontos architekturalis szabaly:

- Redis bevezetesehez csak `RedisAppSessionStore`, Redis kliens dependency, Redis config es deployment config valtozzon.
- Tilos Redis bevezetese miatt modositani azokat az osztalyokat, amelyek mar az `AppSessionManager` moge lettek atvezetve.
- Ha Redis bevezetese servlet/filter/auth kodmodositast igenyel, az elso koros session absztrakcio hianyosnak minosul, es a Redis store implementalasa elott az absztrakciot kell javitani.

Redis akkor jo kovetkezo lepes, ha:

- a session es rate limiter allapotot nem akarjuk PostgreSQL-re terhelni,
- nativ TTL-alapu, rovid eletu allapotkezeles kell,
- production-szerubb cluster infrastruktura gyakorlasa a cel,
- kesobb a rate limiter store is Redisre kerulne.

Redis nem cel az elso korben, mert uj infrastruktura komponenst, Java kliens dependency-t, timeout/reconnect kezelest es Swarm service konfiguraciot hozna be.

### Redis implementacio kesobbi fo vonalai

Ez a szakasz a kesobbi Redis store fo tervezesi donteseit rogziti. Nem resze az elso implementacios kornek. Azert szerepel itt, hogy a most megvalositando `AppSession`, `AppSessionAttribute`, `AppSessionStore`, `AppSessionManager`, cookie es session id kezeles ne hozzon olyan dontest, amely a Redis store-t kesobb ujabb servlet/filter/auth atirashoz kotne.

#### Scope

Kesobb letrehozando:

- `RedisAppSessionStore`
- Redis kliens dependency, valasztott klienssel, peldaul Jedis vagy Lettuce
- Redis konfiguracio `AppSessionConfig` vagy kulon `RedisSessionConfig` alatt
- Docker/Swarm Redis service
- Redis store unit/integracios tesztek

Nem modosulhat Redis miatt:

- `AuthServlet`
- `MeServlet`
- `LogoutServlet`
- `CaptchaServlet`
- `RegisterServlet`
- `UserServlet`
- `AuthFilter`
- `CsrfFilter`
- `HttpSessionContextFilter`
- `RequestLoggingFilter`
- frontend API szerzodes

#### Konfiguracio

Varhato env-first konfiguracio:

```text
SESSION_STORE_MODE=redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
REDIS_TIMEOUT_MILLIS=2000
REDIS_KEY_PREFIX=cms
```

`web.xml` fallback parameterek akkor legyenek, ha a projektben tovabbra is kovetni akarjuk az env -> web.xml -> default mintat.

#### Kulcsmodell

Javasolt kulcsok:

```text
<prefix>:session:<sessionIdHash>
<prefix>:session:<sessionIdHash>:attr:<attributeName>
```

Pelda:

```text
cms:session:abc123hash
cms:session:abc123hash:attr:captcha
```

Az alap session kulcs tartalmazza:

- user id
- login name
- email
- role
- csrf token
- created at
- last accessed at
- expires at
- invalidated flag, ha delete helyett soft invalidate mellett dontunk

Az attribum kulcs tartalmazza:

- attribute name
- attribute type
- json value
- created at
- updated at

Alternativ megoldas: egy Redis hash kulcsban tarolni az alap session mezoket es az attribumokat `attr:<name>:type`, `attr:<name>:json` mezokkel. Az elso Redis implementacioban a kulon attribum kulcs egyszerubb cleanup es payload meret szempontbol, de a store interfesznek mindket lekepezest el kell fednie.

#### TTL

Redisben a TTL legyen a session lejarat elsodleges mechanizmusa.

Kovetelmenyek:

- az alap session kulcs kapjon TTL-t a `session.timeout.minutes` alapjan,
- minden session attribum kulcs ugyanazt vagy rovidebb TTL-t kapjon,
- session activity eseten az alap session kulcs TTL-je frissuljon,
- az attribum kulcsok TTL-je is frissuljon, ha a session egesze meghosszabbodik,
- logout eseten az alap session kulcs es az osszes kapcsolodo attribum kulcs torlodjon.

Fontos: a Redis TTL viselkedes miatt az `AppSession` modellben tovabbra is maradjon `expiresAt`, mert a JDBC store ezt hasznalja, es a manager/store-fuggetlen logika szamara is hasznos. Redis store-ban az `expiresAt` a TTL-bol szamithato vagy a payloadban is tarolhato diagnosztikai celra.

#### Attribumok

A Redis store ugyanazt az alkalmazasi attribum modellt hasznalja, mint a JDBC store:

```text
attribute_name
attribute_type
json_value
created_at
updated_at
```

CAPTCHA tarolas Redisben:

```text
key: cms:session:<sessionIdHash>:attr:captcha
type: CAPTCHA_STATE
json: {"id":"...","answer":12,"purpose":"login","createdAt":1710000000000,"attempts":0}
```

Kovetelmeny: ha a CAPTCHA most `AppSessionAttribute` alapon kerul megvalositasra, a Redis store kesobb csak ezt az attribumot menti mas backendbe. Nem lehet kulon Redis-specifikus CAPTCHA API-t bevezetni.

#### Muveletek

`RedisAppSessionStore.find(...)`:

- cookie-bol session id olvasas,
- session id hash kepzes,
- alap session kulcs olvasasa,
- ha nincs kulcs, nincs session,
- ha invalidated vagy lejartnak tekintheto, session nincs,
- attribum kulcsok vagy hash mezok betoltese,
- `AppSession` osszeallitasa.

`RedisAppSessionStore.create(...)`:

- uj session id generalas a kozos `SessionIdGenerator`-ral,
- cookie beallitasa a kozos `SessionCookieSupport`-tal,
- alap session kulcs letrehozasa TTL-lel,
- ures attribum map.

`RedisAppSessionStore.save(...)`:

- alap session mezok mentese,
- dirty vagy aktualis attribumok mentese,
- torolt attribumok torlese,
- TTL frissitese.

`RedisAppSessionStore.invalidate(...)`:

- cookie-bol session id olvasas,
- alap session kulcs es kapcsolodo attribum kulcsok torlese,
- cookie torlese.

#### Kapcsolatkezeles es hibak

A Redis store-nak explicit timeoutokat kell hasznalnia. Nem lehet vegtelen varakozas egy session lookup miatt.

Hiba eseten javasolt viselkedes:

- auth/CSRF ellenorzesnel Redis elerhetetlenseg ne engedjen at vedett requestet,
- a felhasznalo inkabb kapjon `AUTH_REQUIRED` vagy kontrollalt `INTERNAL_ERROR` valaszt, mint hogy auth bypass tortenjen,
- a hibat logolni kell token/session id nyers ertek nelkul.

Ezt a pontos HTTP hibastrategiat a Redis implementacio elott kell veglegesiteni, de a biztonsagi alapelv most rogzitett: store hiba nem vezethet jogosulatlan hozzafereshez.

#### Swarm deployment

Kesobbi Swarm service:

```yaml
redis:
  image: redis:7
  networks:
    - cms-network
  deploy:
    replicas: 1
```

Production-szerubb mukodeshez kesobb donteni kell:

- Redis persistence: RDB/AOF vagy tisztan ephemeral session store,
- Redis password/TLS,
- single Redis vs Sentinel/cluster,
- memory limit es eviction policy.

Az elso Redis store teszthez eleg lehet egy single Redis service, mert a cel az alkalmazas session externalizaciojanak kiprobalasa, nem Redis HA architektura felepitese.

#### Mostani implementaciot erinto kovetkezmenyek

A Redis terv miatt mar az elso `http` + `jdbc` korben kotelezo:

- a session id generalas store-fuggetlen legyen,
- a cookie kezeles store-fuggetlen legyen,
- az osszetett session adatok `AppSessionAttribute` modellben legyenek,
- a CAPTCHA ne legyen JDBC oszlopokhoz kotve,
- az `AppSessionStore` interfesz ne tartalmazzon SQL, Redis vagy Tomcat session fogalmakat,
- az `AppSessionManager` publikus API-ja Redis hozzaadasakor ne valtozzon,
- a store factory legyen bovitheto uj `REDIS` aggal.

## Adatbazis terv

### Migration

Uj migracio:

```text
src/main/resources/db/migration/V5__app_sessions.sql
```

Javasolt tablak:

```sql
CREATE TABLE app_sessions (
    id_hash VARCHAR(128) PRIMARY KEY,
    user_id BIGINT NULL,
    login_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    role VARCHAR(50) NULL,
    csrf_token VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    invalidated CHAR(1) NOT NULL DEFAULT 'F'
);

CREATE TABLE app_session_attributes (
    id BIGSERIAL PRIMARY KEY,
    session_id_hash VARCHAR(128) NOT NULL,
    attribute_name VARCHAR(100) NOT NULL,
    attribute_type VARCHAR(100) NOT NULL,
    json_value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_app_session_attributes_session
        FOREIGN KEY (session_id_hash)
        REFERENCES app_sessions (id_hash)
        ON DELETE CASCADE,

    CONSTRAINT uq_app_session_attributes_name
        UNIQUE (session_id_hash, attribute_name)
);
```

Javasolt indexek:

```sql
CREATE INDEX idx_app_sessions_expires_at ON app_sessions (expires_at);
CREATE INDEX idx_app_session_attributes_session ON app_session_attributes (session_id_hash);
```

Opcionalis index:

```sql
CREATE INDEX idx_app_sessions_user_id ON app_sessions (user_id);
```

### Mezo dontesek

Az `app_sessions` tabla csak a session alapadatait es a gyakran olvasott auth/security mezoket tartalmazza.

Az `AuthenticatedUser` snapshotot erdemes oszlopokra bontva tarolni, nem JSON-kent.

Indok:

- auth/filter/audit es logging miatt gyakran olvasott adat
- egyszerubb JDBC kod
- role validalas egyszeru
- kesobb user_id alapjan takaritas vagy admin logout lehetseges

A CSRF token szinten az `app_sessions` tablaban marad.

Indok:

- minden state-changing requestnel gyorsan ellenorzott skalar security adat,
- nem workflow-specifikus,
- nem varhato, hogy osszetett objektumma bovul.

Az osszetett vagy workflow-specifikus session adatok az `app_session_attributes` tablaba kerulnek JSON-kent.

Elso konkret hasznalat mar a mostani mukodesben:

```text
attribute_name = captcha
attribute_type = CAPTCHA_STATE
```

Pelda `json_value`:

```json
{
  "id": "captcha-id",
  "answer": 12,
  "purpose": "login",
  "createdAt": 1710000000000,
  "attempts": 0
}
```

Ez valtja ki a korabban tervezett `captcha_id`, `captcha_answer`, `captcha_purpose`, `captcha_created_at`, `captcha_attempts` oszlopokat. Ezek nem kerulnek az `app_sessions` tablaba.

Nem kell foreign key `users(id)`-ra az elso korben. A session snapshot akkor is lezarhato/ervenytelenitheto, ha a user rekord kozben modositva lett. Kesobb lehet donteni, hogy minden requestnel friss user allapotot ellenorzunk-e.

Az `app_session_attributes.session_id_hash` idegen kulccsal mutat az `app_sessions.id_hash` mezore. Az `ON DELETE CASCADE` kotelezo, hogy session torleskor az osszes kapcsolodo attribum is automatikusan torlodjon.

A `UNIQUE (session_id_hash, attribute_name)` kotelezo, hogy egy sessionen belul egy logikai attribum neve egyszer szerepeljen. Ha kesobb tobb azonos tipusu objektum kell, azok kulon `attribute_name` ertekkel tarolhatok.

### Redis lekepezes

A ket tablas JDBC modellnek Redisben is kozvetlenul lekepezhetonek kell lennie.

Javasolt Redis modell kesobb:

```text
cms:session:<sessionIdHash>                  -> alap session mezok
cms:session:<sessionIdHash>:attr:<name>      -> egy session attribum JSON payloadja
```

Vagy Redis hash hasznalata eseten:

```text
cms:session:<sessionIdHash>
  userId
  loginName
  email
  role
  csrfToken
  createdAt
  lastAccessedAt
  expiresAt
  attr:captcha:type = CAPTCHA_STATE
  attr:captcha:json = {...}
```

Kovetelmeny: a JDBC attribum modell es a Redis attribum modell ugyanazt az alkalmazasi fogalmat hasznalja: `attribute_name`, `attribute_type`, `json_value`. Emiatt Redis hozzaadasakor nem kell ujra modellezni a CAPTCHA vagy jovobeli osszetett session objektumokat.

### Lejart session takaritas

Elso korben eleg lazy cleanup:

- `find` soran ha lejart vagy invalidated, akkor torolheto vagy ervenytelennek tekintheto.
- `create` vagy `save` soran alkalmi cleanup futtathato limitaltan.

Ne legyen kulon background thread az elso implementacioban, mert Tomcat lifecycle es cluster korulmenyek kozott extra kockazat.

## Kodatalakitasi terv

### 1. Konfiguracio bevezetese

Letrehozando:

- `AppSessionStoreMode`
- `AppSessionConfig`
- `AppSessionConfigListener`

`web.xml`:

- uj context-paramok
- uj listener a `SecurityConfigListener` mintajara

Elfogadasi feltetelek:

- ha `SESSION_STORE_MODE` meg van adva, az felulirja a `web.xml` `session.store.mode` erteket
- ha `SESSION_STORE_MODE` nincs megadva, de `web.xml` `session.store.mode` van, akkor a `web.xml` ertek ervenyesul
- ha egyik sincs megadva, `http` mod indul, vagyis a hagyomanyos jelenlegi `HttpSession` mukodes marad
- hibas explicit mode ertek eseten startup fail legyen, mert cluster konfiguracios hiba ne maradjon rejtve
- a mode enum/factory szerkezete bovitheto legyen kesobbi `redis` aggal anelkul, hogy a sessiont hasznalo servlet/filter kod valtozna
- Redis store hozzaadasakor az `AuthServlet`, `MeServlet`, `LogoutServlet`, `CaptchaServlet`, `RegisterServlet`, `UserServlet`, `AuthFilter`, `CsrfFilter`, `HttpSessionContextFilter` es `RequestLoggingFilter` nem modosulhat Redis-specifikus okbol

Megjegyzes: a fallback `http` csak hianyzo konfiguraciora vonatkozik. Ha valaki explicit hibas erteket ad meg environmentben vagy `web.xml`-ben, az ne essen vissza csendben `http` modra.

### 2. Session model es store interfesz

Letrehozando:

- `AppSession`
- `AppSessionStore`
- `AppSessionManager`

Elso implementacio: csak `HttpSessionAppSessionStore`.

Ebben a lepesben meg nem kell DB.

Cel:

- a jelenlegi `HttpSession` kozvetlen hasznalat nagy resze atkeruljon a `SessionManager` moge
- viselkedes ne valtozzon
- az `AppSessionStore` szerzodes ne legyen sem JDBC-, sem Redis-specifikus
- kesobbi `RedisAppSessionStore` hozzaadasakor csak a store factory/config es az uj implementacio valtozhat

### 3. Auth filterek atallitasa

Atalakitando:

- `AuthFilter`
- `CsrfFilter`
- `HttpSessionContextFilter`
- `RequestLoggingFilter`

Cel:

- ne kozvetlenul `request.getSession(false)` + `session.getAttribute("user")` logika legyen bennuk
- `AppSessionManager` adja vissza az authenticated usert es CSRF tokent

Fontos:

- public endpoint lista maradjon `request.getServletPath()` alapu, mert root context es `/cms-app` alatt is mukodik
- API error response shape ne valtozzon

### 4. Auth servlet layer atallitasa

Atalakitando:

- `AuthServlet`
- `MeServlet`
- `LogoutServlet`
- `CaptchaServlet`
- `RegisterServlet`
- `UserServlet`
- `CsrfTokenSupport`

Cel:

- login: `AppSessionManager` hozza letre vagy rotalja az auth sessiont
- me: `AppSessionManager`-bol olvas usert es biztosit CSRF tokent
- logout: `AppSessionManager.invalidate(...)`
- captcha: `AppSessionManager`-en keresztul menti a captcha allapotot
- register/login captcha validalas: `AppSessionManager`-bol olvassa es update-eli a captcha probalkozast
- UserServlet admin ellenorzes: `AppSessionManager` vagy request-context alapu authenticated user

CSRF token generalas maradhat `CsrfTokenSupport` felelossege, de az osztaly ne taroljon kozvetlenul `HttpSession`-be. Javasolt szetvalasztas:

- `CsrfTokenSupport.createToken()`
- token tarolas/ensure: `AppSessionManager.ensureCsrfToken(...)`

### 5. HTTP mod regresszio teszt

Mielott JDBC store keszul, `http` modban minden jelenlegi auth flow-t ellenorizni kell.

Minimum ellenorzesek:

- `GET /hello`
- `GET /api/auth/config`
- `GET /api/auth/captcha?purpose=login`
- `POST /api/auth/login`
- `GET /api/auth/me`
- CSRF-vel vedett endpoint mukodik
- CSRF nelkul vedett endpoint `403 CSRF_INVALID`
- `POST /api/auth/logout`
- logout utan `GET /api/auth/me` -> `401 AUTH_REQUIRED`
- registration captcha flow

Automata tesztek:

- `mvn test`

### 6. JDBC migration

Letrehozando:

- `V5__app_sessions.sql`

Ellenorzendo:

- migracio tobb app instance indulasanal is biztonsagos marad, mert a jelenlegi `DatabaseMigrationRunner` PostgreSQL advisory lockot hasznal
- az `app_sessions` es `app_session_attributes` tablak letrejonnek
- a foreign key, cascade delete, unique constraint es indexek letrejonnek
- checksum stabil

### 7. `JdbcAppSessionStore`

Letrehozando:

- `JdbcAppSessionStore`
- `SessionCookieSupport`
- `SessionIdGenerator`

Feladatok:

- cookie olvasas
- session id hash keszites
- DB rekord betoltes
- session attribum rekordok betoltese
- lejart/invalidalt session kezelese
- uj rekord beszurasa
- rekord update
- session attribum upsert `session_id_hash + attribute_name` alapjan
- session attribum torles, amikor egy workflow allapot elfogy, peldaul CAPTCHA validalas utan
- logout invalidalas vagy delete
- cookie torles

DB hozzaferes:

- `DatabaseConfig.getConnection()` hasznalata
- try-with-resources
- explicit commit/rollback kerdes:
  - ha Hikari connection default autoCommit true, egyszeru muveletek mehetnek auto-committal
  - ha a projektben konzisztencia miatt explicit tranzakcio kell, akkor a store sajat rovid tranzakciot kezeljen

Javaslat: a session store sajat rovid, explicit tranzakciot hasznaljon ott, ahol tobb statement tartozik ossze.

Redis-bovithethosegi ellenorzes ebben a lepesben:

- a JDBC implementacio ne szivarogtasson DB reszleteket az `AppSessionManager` publikus API-jaba,
- a session id generalas es cookie kezeles legyen ujrahasznalhato a kesobbi Redis store-hoz,
- az `AppSession` ne tartalmazzon olyan mezot, amely csak az SQL tabla miatt letezik,
- az `AppSessionAttribute` modell ne tartalmazzon olyan mezot, amely csak a JDBC masodik tabla miatt letezik,
- a CAPTCHA es CSRF kezeles ne hivjon kozvetlenul JDBC store metodust.

### 8. Session mentesi pontok

El kell donteni, hogy a session mentese mikor tortenjen.

Javasolt egyszeru modell:

- `AppSessionManager` request attribute-ban tartja a betoltott sessiont
- mutacio utan azonnal `store.save(...)` hivodik
- nem kell kulon response-vegi filter

Elony:

- kevesebb lifecycle komplexitas
- servlet/filter kodbol egyertelmuen latszik, mikor valtozik az allapot

Hatrany:

- tobb DB update lehet egy request alatt

Kesobbi optimalizacio:

- `AppSessionPersistenceFilter`, amely request vegen dirty sessiont ment

Elso implementacioban az azonnali mentest javasolt valasztani.

### 9. Swarm konfiguracio

`docker-compose-swarm-test.yml` aktualizalasa:

- backend replikak novelese teszthez, peldaul 3
- Tomcat environment:
  - `SESSION_STORE_MODE=jdbc`
  - `SESSION_COOKIE_NAME=CMS_SESSION_ID`
  - `SESSION_TIMEOUT_MINUTES=30`
  - `SESSION_COOKIE_SECURE=false` helyi HTTP teszthez

Pelda:

```yaml
environment:
  DB_HOST: cms-swarm-postgres
  DB_PORT: 5432
  DB_NAME: cms_db
  DB_USER: cms_user
  DB_PASSWORD: cms_pw
  SESSION_STORE_MODE: jdbc
  SESSION_COOKIE_NAME: CMS_SESSION_ID
  SESSION_TIMEOUT_MINUTES: 30
  SESSION_COOKIE_SECURE: "false"
```

Nginx config alapvetoen maradhat stateless reverse proxy.

Nem cel:

- sticky session bevezetese
- Swarm DNSRR upstream trukkozes
- Nginx Plus feature-ok
- Nginx sticky session csak diagnosztikai vagy rovid tavu workaround lehet, nem architekturalis celallapot

### 10. Cluster validacio

Manualis teszt Swarm alatt:

1. Induljon 1 DB, 3 backend, 2 frontend Nginx.
2. `GET /api/auth/config` valaszoljon.
3. `GET /api/auth/captcha?purpose=login` tobb probalkozas utan is mukodjon.
4. Login utan a response adjon CSRF tokent.
5. Tobbszor egymas utan `GET /api/auth/me` mukodjon akkor is, ha a requestek kulonbozo backend replikakra mennek.
6. CSRF-vedett admin endpoint mukodjon.
7. Logout utan minden replikan `AUTH_REQUIRED` legyen.
8. Egy backend kontener ujrainditasa utan a session megmaradjon, ha nem jart le es nem az ujrainditott node lokalis memoriajaban volt.

Hasznos diagnosztika:

- backend logban latszodjon a kontener/node azonositasa
- request logging tartalmazza a usert
- session store mod logolodjon startupkor

## Rate limiter kulon feladat

A session store megoldasa utan marad egy cluster-szintu biztonsagi res:

- login rate limiter: `InMemoryRateLimiter`
- registration rate limiter: `InMemoryRateLimiter`
- captcha generation limiter: `InMemoryRequestRateLimiter`

Ezek jelenleg replikankent kulon memoriaban vannak, tehat 3 backend replika mellett a valos limit nagyjabol haromszorozodhat, es a request eloszlastol fugg.

Ez nem blokkolja a session konzisztenciat, de production-cluster szempontbol kesobb javitando.

Javasolt kesobbi terv:

- `rateLimiter.store.mode=memory|jdbc|redis`
- `RateLimiterStore` interfesz
- DB tabla kulcs, counter, window/lock expiry mezokkel
- kesobb Redis implementacio TTL-es counterekkel

Elso session fejlesztesbe ezt nem erdemes belekeverni, mert kulon biztonsagi es concurrency terulet.

## Biztonsagi megfontolasok

### Session fixation

`http` modban a jelenlegi `request.changeSessionId()` megmarad.

`jdbc` modban login utan uj alkalmazas-session id-t kell generalni. Ha login elott mar volt CAPTCHA session, akkor sikeres loginnal:

- vagy uj session rekord keszul es a regi CAPTCHA session torlodik,
- vagy a meglevo session id rotalodik uj id-re.

Javaslat: login siker eseten mindig uj session id es uj DB rekord legyen, a regi session invalidalasa mellett.

### CSRF

A CSRF token tovabbra is sessionhez kotott.

JDBC modban:

- token DB-ben tarolodik
- frontend tovabbra is `data.csrfToken`-bol dolgozik
- `X-CSRF-Token` header szerzodes nem valtozik

### Cookie

Production HTTPS eseten:

- `Secure=true`
- `HttpOnly=true`
- `SameSite=Lax` alapertelmezett

Ha a frontend es backend kulon originen fut credentialos CORS-szal, akkor `SameSite=None; Secure` lehet szukseges. Ezt kulon deployment dontesnek kell kezelni.

### Sensitive logging

Session id-t, CSRF tokent, captcha valaszt nem szabad nyersen logolni.

Ha session diagnosztika kell, csak rovid hash prefix keruljon logba.

## API kompatibilitas

Az alabbi API szerzodes nem valtozhat:

- successful login response shape
- `GET /api/auth/me` response shape
- `X-CSRF-Token` header hasznalata
- API envelope: `success/data` es `success/error`
- auth hibakodok:
  - `AUTH_REQUIRED`
  - `CSRF_INVALID`
  - `INVALID_CREDENTIALS`
  - `CAPTCHA_INVALID`
  - `RATE_LIMITED`

A frontend nem tudhatja es nem kezelheti kulon, hogy `http` vagy `jdbc` session store fut.
A frontendnek kesobb sem kell tudnia, ha a store `redis` modra valt.

Egyetlen kliensoldali figyelendo pont:

- `jdbc` modban a cookie neve lehet `CMS_SESSION_ID`, nem `JSESSIONID`
- mivel a browser automatikusan kuldi a cookie-t, a frontend kodnak ez nem kell, hogy szamitsson, ha tovabbra is `credentials: "include"` van beallitva

## Tesztelesi terv

### Unit tesztek

Javasolt uj tesztek:

- `AppSessionConfigTest`
  - defaultok
  - valid `http`
  - valid `jdbc`
  - `SESSION_STORE_MODE` elsobbseget elvez a `web.xml` ertekkel szemben
  - hianyzo env es hianyzo `web.xml` eseten `http` default
  - explicit hibas env vagy `web.xml` mode startup/config hibat okoz, nem csendes `http` fallbacket
  - kesobbi `redis` mod helye egyertelmu legyen: vagy mar enum ertek tesztelve, vagy explicit dokumentaltan meg nincs engedelyezve
  - invalid mode kezelese
- `SessionIdGeneratorTest`
  - nem ures
  - eleg hosszu
  - ket generalas nem azonos
- `SessionCookieSupportTest`
  - cookie letrehozas
  - cookie torles
  - secure/sameSite beallitas
- `AppSessionAttributeTest` vagy annak megfelelo manager/store teszt
  - `attribute_name`, `attribute_type`, `json_value` megorzese
  - `CAPTCHA_STATE` typed attribum kezelese store-fuggetlenul

### Integracios tesztek

Javasolt DB-backed tesztek:

- `JdbcAppSessionStoreTest`
  - create + find
  - authenticated user mentese
  - csrf token mentese
  - `CAPTCHA_STATE` attribum mentese, olvasasa, frissitese es torlese
  - session torles/invalidate utan az attribum rekordok is eltunnek vagy ervenytelenne valnak
  - invalidate
  - expired session nem ervenyes

Megjegyzes:

- a projektben a DAO tesztek jelenleg is PostgreSQL-t igenyelnek, ez illeszkedik a meglevo tesztmodellhez

### Servlet/filter flow tesztek

Ha nincs servlet test infrastruktura, elso korben maradhat manualis curl/browser teszt.

Manualis minimum:

- login -> me -> CSRF protected request -> logout -> me
- captcha -> login/registration validalas
- `http` mod lokalis Tomcat alatt
- `jdbc` mod egy Tomcat alatt
- `jdbc` mod tobb Tomcat replika alatt
- kesobbi Redis store eseten ugyanez a flow fusson `session.store.mode=redis` mellett, servlet/filter kodmodositas nelkul

## Kockazatok es dontesi pontok

### Filter sorrend

Ha a `JdbcAppSessionStore` a request tranzakciot akarja hasznalni, a filter sorrendet at kell rendezni. Ez nagyobb regresszios kockazat.

Javaslat:

- session store sajat DB kapcsolatot hasznaljon
- `TransactionFilter` maradjon ott, ahol most van

### Session snapshot frissessege

Az `AuthenticatedUser` session snapshot tartalmaz role-t es emailt. Ha admin kozben modositja a user role-t vagy aktiv allapotot, a sessionben regi adat maradhat.

Ez most is igy van `HttpSession` mellett. A JDBC session store ezt nem rontja, csak lathatobb lesz.

Kesobbi dontes:

- minden requestnel DB-bol frissiteni a user auth allapotot,
- vagy admin user modositaskor invalidalni az erintett user sessionjeit.

Elso korben nem kell megvaltoztatni a jelenlegi szemantikat.

### DB terheles

JDBC session store minden session-aware requestnel DB olvasast jelenthet.

Mitigacio:

- request attribute cache
- csak dirty session mentese
- index `expires_at`
- egyszeru, kis sorok

Ehhez az alkalmazashoz a session adatmennyiseg kicsi, PostgreSQL elegendo.

### Cleanup

Ha nincs hatter takaritas, lejart session sorok felgyulhetnek.

Elso korben lazy cleanup eleg. Kesobb lehet:

- startup cleanup
- periodikus admin endpoint
- DB scheduled job
- explicit cleanup parancs

## Fejlesztesi sorrend osszefoglalva

1. `AppSessionConfig` es `session.store.mode` bevezetese.
2. `AppSession`, Redis-kompatibilis `AppSessionStore`, `AppSessionManager` letrehozasa.
3. `HttpSessionAppSessionStore` implementalasa.
4. Meglevo servlet/filter session hozzaferesek atvezetese `AppSessionManager` moge.
5. `http` mod teljes regresszio ellenorzese.
6. `V5__app_sessions.sql` migracio.
7. `JdbcAppSessionStore`, cookie support es session id generator.
8. `jdbc` mod lokalis egy Tomcat ellenorzese.
9. Swarm compose environment frissites `SESSION_STORE_MODE=jdbc` ertekkel.
10. Swarm teszt 3 backend + 2 frontend replikaval.
11. Kesobbi, kulon feladatkent `RedisAppSessionStore` hozzaadasa ugyanazon interfesz moge, servlet/filter atiras nelkul.
12. Kulon kovetkezo feladatkent rate limiter store absztrakcio.

## Nem cel ebben a korben

- JWT bevezetese
- Spring Security vagy mas framework
- Redis kotelezo vagy azonnali bevezetese
- Redis miatt servlet/filter/auth reteg ujboli atirasa
- Nginx sticky sessionre epulo cluster mukodes; a vegleges irany a backend session allapot kulso store-ba vitele
- teljes user auth allapot minden request alatti DB-frissitese
- in-memory rate limiterek azonnali clusteresitese
- frontend API szerzodes modositasa

## Varhato eredmeny

`session.store.mode=http` mellett az alkalmazas ugy mukodik, mint most.

`session.store.mode=jdbc` mellett:

- barmely Tomcat replika ki tudja szolgalni ugyanazt a bejelentkezett usert,
- CSRF token validalas replikak kozott is mukodik,
- CAPTCHA flow replikak kozott is mukodik,
- logout minden replikara ervenyes,
- backend kontener ujraindulas nem torli automatikusan a sessiont,
- a frontend Nginx tovabbra is stateless reverse proxy marad.

Kesobbi `session.store.mode=redis` mellett ugyanezt az alkalmazasi viselkedest kell kapni, de Redis TTL-alapu tarolassal. Ennek bevezetese nem igenyelhet ujabb session-hozzaferesi atvezetesi munkat a servlet/filter/auth osztalyokban.

Hosszu tavon a Redis store tekintheto a session es rovid eletu security/workflow allapot leginkabb cluster-termeszetes tarolasanak. A JDBC store az elso, kevesebb infrastruktura-komponenst igenylo cluster-kompatibilis lepes; a sticky session nem vegallapot, mert a state tovabbra is egy-egy Tomcat peldany memoriaban maradna.
