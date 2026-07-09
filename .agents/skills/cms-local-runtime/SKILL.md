---
name: cms-local-runtime
description: Use when starting or stopping the local CMS backend, Tomcat, WAR deployment, local DB environment variables, or temporary CAPTCHA disablement.
---

# CMS Local Runtime

Use this skill for local backend runtime operations in this project.

This skill is the source of truth for local backend startup and shutdown.

## Local Settings

- Project root: `D:\java\cms_project\cms-app`
- Tomcat candidate paths: first check `C:\Tomcat 9.0`, then `C:\tomcat9`, for `bin\catalina.bat`.
- Tomcat path: if no candidate path is valid, discover a Tomcat 9 installation on the `C:` drive for each runtime operation. Prefer a path whose directory name contains both `Tomcat` and `9`; if multiple valid Tomcat 9 paths are found, ask the user which one to use.
- App context: `http://localhost:8080/cms-app`
- PostgreSQL host: `localhost`
- PostgreSQL port: `5432`
- Database: `cms_db`
- Database user: `cms_user`
- Database password: `cms_pw`
- Expected local URLs: `http://localhost:8080/cms-app/hello` and `http://localhost:8080/cms-app/api/auth/config`

## Startup

- If the user explicitly asks for CAPTCHA to be disabled for this startup, set both `captcha.login.enabled=false` and `captcha.registration.enabled=false` in `src/main/webapp/WEB-INF/web.xml` before building.
- Do not disable CAPTCHA unless the user explicitly asks for it for that startup.
- Run `mvn package -DskipTests` from the project root.
- Discover `TomcatHome` by first checking the Tomcat candidate paths. If none contain `bin\catalina.bat`, search the `C:` drive for a Tomcat 9 directory that contains `bin\catalina.bat`, using the Tomcat path selection rules from Local Settings.
- Redeploy `target/cms-app.war` to `$TomcatHome\webapps\cms-app.war`, replacing the old expanded app and WAR.
- Set `CATALINA_HOME=$TomcatHome`, `CATALINA_BASE=$TomcatHome`, `DB_HOST=localhost`, `DB_PORT=5432`, `DB_NAME=cms_db`, `DB_USER=cms_user`, and `DB_PASSWORD=cms_pw` before starting Tomcat.
- Start Tomcat in an independent Windows terminal window with `Start-Process -FilePath "cmd.exe" -ArgumentList "/k", $startCommand`, where `$startCommand` sets the required environment variables and then calls `"$TomcatHome\bin\catalina.bat" run`.
- Use `catalina.bat run` in that independent terminal so Tomcat owns that window and the OpenCode shell prompt is not held by Tomcat's stdout/stderr handles.
- Never start Tomcat by directly invoking `& "$TomcatHome\bin\catalina.bat" start` or `& "$TomcatHome\bin\catalina.bat" run` inside the tool session.
- The independent-terminal `Start-Process` startup command must be the final tool call in the startup flow.
- After that `Start-Process` tool call returns, do not run any other tool call, command, healthcheck, file read, todo/status update, or verification.
- Immediately send the final response: `Az alkalmazás elindult.` Add `Captcha kikapcsolva.` only when CAPTCHA was explicitly disabled for that startup.

## Stop

- Discover `TomcatHome` by first checking the Tomcat candidate paths. If none contain `bin\catalina.bat`, search the `C:` drive for a Tomcat 9 directory that contains `bin\catalina.bat`, using the same selection rules as startup.
- Stop Tomcat with `$TomcatHome\bin\catalina.bat stop` using `CATALINA_HOME=$TomcatHome` and `CATALINA_BASE=$TomcatHome`.
- If CAPTCHA was disabled for the current startup, restore both `captcha.login.enabled=true` and `captcha.registration.enabled=true` in `src/main/webapp/WEB-INF/web.xml` at the end of the stop flow.
- After stopping, send a concise final response.

## Healthchecks

- Do not run startup healthchecks unless the user explicitly asks for them or they were announced before starting Tomcat.
- If a healthcheck is explicitly requested, run it before the final response, but never after the detached startup command unless the user requested that exact behavior.

## Known Local Admin

- Login: `tester`
- Name: `Tester Mester`
- Email: `testermester@example.com`
- Role: `ADMIN`
- Active: `T`
- Registration state: `COMPLETED`
