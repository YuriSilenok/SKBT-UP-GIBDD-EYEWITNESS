# Production на силенок.рф — 24.11.2026

VDS 193.124.115.164 переведена в рабочий режим по решению владельца. API опубликован на https://силенок.рф/ без отдельного тестового порта; существующий сайт в корне и другие его API сохранены. Приложение использует PostgreSQL 17.12 на localhost:55432, службу gibdd.service и отдельную службу gibdd-postgresql.service. Исторические имена каталогов/системного пользователя с test не являются режимом работы приложения.

Автопубликация Android выполняет только release с постоянными новыми рабочими ключами; новые APK не имеют .test, суффикса -test и тестового баннера. Первая рабочая публикация использует следующую последовательную версию после 1.0.0. Старые тестовые APK сохранены в истории как реальные исторические сборки и не выдаются за рабочие.

Домен и документы: config/app.properties, overrides config/local.properties/-P или GitHub Variables. Текущие значения: API_BASE_URL=https://xn--e1afhclgq.xn--p1ai/; PRIVACY_POLICY_URL=https://xn--e1afhclgq.xn--p1ai/legal/policy; PRIVACY_CONSENT_URL=.../legal/consent; PRIVACY_REGULATION_URL=.../legal/regulation. CONSENT_VERSION=2026-11-17-v1 должен совпадать с сервером. Исходные документы опубликованы через эти маршруты; утверждение текстов и согласование принятого хранения до 30 суток по-прежнему выполняет оператор.

Рабочие ключи и пароли находятся только в GitHub Secrets APK_RELEASE_* и закрытом root-каталоге /var/lib/gibdd-signing на VDS. Резервную копию keystore и паролей храните также офлайн у уполномоченного владельца. При утрате рабочего ключа совместимое обновление приложения невозможно. Тестовая .test-установка не обновляется в рабочую: это другая идентичность приложения и новый ключ.

На сервере /etc/gibdd/production.env принадлежит root:gibddtest, mode 0640; содержит только серверные настройки и пароль PostgreSQL. База доступна приложению только по loopback с SCRAM, наружу порт не открыт. /etc/gibdd/production.enabled переключает ограниченный deployment-контроллер на PostgreSQL-профиль. Перед обновлением сохраняются pg_dump и media; при ошибке старта выполняется восстановление. Автообновление не очищает данные. Миграция из SQLite выполняется scripts/migrate_sqlite_to_postgres.py при остановленных SQLite-писателях; она требует пустой PostgreSQL и оставляет оригинал без изменений.

PostgreSQL 17.12 установлен отдельно из официального архива с проверкой SHA256, каталог /opt/gibdd-postgresql/17.12; бинарники не заменяют системные сервисы других проектов. Точечные права доступа даны только группе gibddpg, БД — 0700. За обновления minor PostgreSQL и обслуживание ОС отвечает администратор. На текущей VDS Ubuntu 20.04.6; план обновления ОС/поддержки нужно вести отдельно, эта операция ОС не обновляла.

Проверка: systemctl status gibdd gibdd-postgresql; curl --fail https://силенок.рф/health; cat /opt/gibdd-test-20261117/deployment.json. Другие службы pond и student-rescue не заменяются deployment-процессом. Юридическое согласование права размещения у КГУ, требования к оператору, резервным копиям и утверждённым документам не подтверждаются фактом изменения домена или названия release.

Ниже оставлена общая инструкция первичного развёртывания; её прежние тестовые настройки — история, актуальная эксплуатация описана выше.

---

# Развёртывание на продакшене

Инструкция для нового согласованного сервера КГУ в России, Ubuntu 24.04 LTS, PostgreSQL и одного процесса API. Текущая VDS остаётся тестовой. `api.example.org` и `www.example.org` ниже — примеры, их необходимо заменить будущими адресами. Развёртывание на неизвестный будущий домен не выполнялось.

## 1. Где менять адреса до сборки APK

В каждом Android-репозитории есть `config/app.properties`. Для локальной боевой сборки скопируйте `config/production.example.properties` в `config/local.properties` и замените значения:

```properties
apiBaseUrl=https://api.example.org/
privacyPolicyUrl=https://www.example.org/documents/privacy/v1/
privacyConsentUrl=https://www.example.org/documents/consent/v1/
privacyRegulationUrl=https://www.example.org/documents/regulation/v1/
consentVersion=2026-11-17-v1
```

`apiBaseUrl` используется обоими приложениями и определяет также WSS. Остальные поля используются EYEWITNESS. Приоритет: Gradle `-Pимя=значение` → `config/local.properties` → `config/app.properties`. Пустая ссылка на документ означает `apiBaseUrl + legal/policy`, `legal/consent` или `legal/regulation`. По умолчанию оставлен тестовый сервер. Адрес API должен быть HTTPS, с завершающим `/`, без query/fragment и логина/пароля. Документы — HTTPS. Международный домен в URL задавайте в Punycode.

Это конфигурация **на момент сборки**. После изменения домена или ссылок APK нужно пересобрать и установить обновление. Смена серверного `.env` не меняет адрес в уже установленном APK. В `local.properties` не нужно хранить ключи/пароли; этот файл исключён из Git. Для общего утверждённого конфига можно изменить отслеживаемый `config/app.properties`, сделать commit и push.

Кнопки политики, согласия и регламента открывают именно настроенные ссылки в браузере. Лицензии остаются внутри приложения. Документы должны открываться без регистрации, из сети пользователей, на официальном ресурсе оператора. Обновление ссылки на политику/согласие требует нового активного согласия локально. При изменении ссылки или содержания любого значимого документа увеличьте `consentVersion` и `CONSENT_VERSION` на сервере одновременно. Используйте неизменяемые версионные URL и храните утверждённые версии: журнал сервера записывает версию согласия, а не копию удалённой страницы. Старый APK с другой версией получит 409 при попытке выдать согласие и требует обновления.

Если документы будут раздаваться через `/legal/*` самого API, перед боевым выпуском замените `app/legal/policy.txt`, `consent.txt`, `regulation.txt` утверждёнными текстами и зафиксируйте изменения. Первоначальные документы содержат положения, которые ещё требуют согласования с принятым хранением общей записи до 30 суток.

## 2. Подготовка сервера

Создайте DNS A для API-домена. AAAA публикуйте только при настроенном IPv6. Разрешите входящие 80/443 и административный SSH с доверенных адресов. Не открывайте PostgreSQL и Uvicorn наружу.

```bash
sudo apt update
sudo apt install -y git python3.12-venv postgresql nginx certbot python3-certbot-nginx gettext-base
sudo adduser --system --group --home /opt/gibdd gibdd
sudo install -d -o gibdd -g gibdd -m 0750 /opt/gibdd
sudo install -d -o gibdd -g gibdd -m 0700 /var/lib/gibdd /var/lib/gibdd/media
sudo install -d -o root -g gibdd -m 0750 /etc/gibdd
sudo -u gibdd git clone https://github.com/YuriSilenok/SKBT-UP-GIBDD-SERVER.git /opt/gibdd/app
sudo -u gibdd python3.12 -m venv /opt/gibdd/venv
sudo -u gibdd /opt/gibdd/venv/bin/pip install -r /opt/gibdd/app/requirements.lock.txt
```

Планируйте ресурсы с запасом под PostgreSQL, обработку изображений и хранилище. Приложение ограничивает суммарное хранилище media 2 GiB; отслеживайте заполнение и ошибки отказа загрузки. Для тяжёлой нагрузки нужен отдельный расчёт ёмкости. Многопроцессный запуск пока не поддерживается для общих лимитов и событий: используйте `--workers 1`.

Создайте отдельную БД. Пароль задайте интерактивно, не вставляйте его в общедоступные команды или Git:

```bash
sudo -u postgres psql
```

```sql
CREATE ROLE gibdd LOGIN;
\password gibdd
CREATE DATABASE gibdd OWNER gibdd;
\q
```

Настройте PostgreSQL на localhost, для TCP — SCRAM-аутентификацию, без trust для рабочей базы. Из `/opt/gibdd/app/.env.example` создайте `/etc/gibdd/server.env` с реальными реквизитами:

```dotenv
DB_NAME=gibdd
DB_USER=gibdd
DB_PASSWORD="REPLACE_WITH_REAL_SECRET"
DB_HOST=127.0.0.1
DB_PORT=5432
MEDIA_STORAGE_DIR=/var/lib/gibdd/media
CONSENT_VERSION=2026-11-17-v1
ADMIN_PANEL_PASSWORD=
```

```bash
sudo chown root:gibdd /etc/gibdd/server.env
sudo chmod 0640 /etc/gibdd/server.env
# Перед миграцией существующей базы обязательно сделать резервную копию.
sudo systemd-run --wait --pipe --collect --uid=gibdd --gid=gibdd \
  --working-directory=/opt/gibdd/app \
  -p EnvironmentFile=/etc/gibdd/server.env \
  /opt/gibdd/venv/bin/python -m scripts.init_db
sudo cp /opt/gibdd/app/deploy/gibdd.service /etc/systemd/system/gibdd.service
sudo systemctl daemon-reload
sudo systemctl enable --now gibdd
curl --fail http://127.0.0.1:8001/health
```

Пример службы находится в `deploy/gibdd.service`: отдельный пользователь, один worker, запись только в `/var/lib/gibdd`, закрытые права файлов. `scripts.staging_app` и SQLite для продакшена не использовать. Ручную административную панель не публиковать; основной интерфейс сотрудника — EMPLOYEE.

## 3. HTTPS и nginx

Используйте сертификат организации либо получите сертификат ACME для нового домена. Примеры ниже предполагают Let's Encrypt и доступный TCP 80. Не заменяйте конфигурации посторонних сайтов.

```bash
export API_DOMAIN=api.example.org
cd /opt/gibdd/app
# Подставляем ТОЛЬКО API_DOMAIN. Остальные $переменные принадлежат nginx.
envsubst '${API_DOMAIN}' < deploy/nginx-http.conf.template | sudo tee /etc/nginx/sites-available/gibdd >/dev/null
sudo ln -s /etc/nginx/sites-available/gibdd /etc/nginx/sites-enabled/gibdd
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d "$API_DOMAIN" --redirect
```

До первичной выдачи CHIEF ограничьте регистрацию только внешним IP ответственного сотрудника. Замените пример 192.0.2.10 своим адресом:

```bash
printf 'allow 192.0.2.10;\ndeny all;\n' | sudo tee /etc/nginx/snippets/gibdd-bootstrap.conf >/dev/null
envsubst '${API_DOMAIN}' < deploy/nginx-https.conf.template | sudo tee /etc/nginx/sites-available/gibdd >/dev/null
sudo nginx -t
sudo systemctl reload nginx
curl --fail "https://$API_DOMAIN/health"
sudo certbot renew --dry-run
```

Шаблон настраивает WSS Upgrade, заменяет входной X-Forwarded-For, TLS, ограничения запросов/тела и отключает access log с путями сообщений. В nginx error log могут попадать технические идентификаторы; ограничьте доступ и срок хранения, подключите ротацию. Не добавляйте журналирование Authorization, тел сообщений, геоданных и query string. При корпоративном сертификате замените пути ssl_certificate/ssl_certificate_key в итоговом конфиге; ключи не помещайте в Git.

Сначала установите боевой EMPLOYEE у уполномоченного сотрудника и проверьте получение CHIEF. После этого можно открыть регистрацию остальным:

```bash
printf '# Bootstrap completed; application rate limits remain enabled.\n' | sudo tee /etc/nginx/snippets/gibdd-bootstrap.conf >/dev/null
sudo nginx -t && sudo systemctl reload nginx
```

Не удаляйте историю AUTO_ASSIGNED и не переносите тестовую БД. Желательно иметь второго уполномоченного CHIEF. При потере всех руководящих устройств зарегистрируйте проверенную новую employee-установку и выполните на сервере команду восстановления с её UUID через аналогичный `systemd-run` с EnvironmentFile: `/opt/gibdd/venv/bin/python -m scripts.recover_chief UUID`. Публичного обхода авторизации нет.

## 4. Боевые APK через GitHub Actions

Обычный GitHub Release теперь используется для обоих режимов. **Метка GitHub Release и Android build type — разные настройки.** Debug остаётся `.test` с тестовой подписью. Android release не имеет `.test`, не debug и подписывается отдельным боевым ключом. Конвейер не переключает рабочую подпись на тестовую при отсутствии секрета: публикация завершится ошибкой.

В Settings → Secrets and variables → Actions обоих Android-репозиториев установите Variables:

| Variable | Пример |
|---|---|
| API_BASE_URL | https://api.example.org/ |
| PRIVACY_POLICY_URL | https://www.example.org/documents/privacy/v1/ |
| PRIVACY_CONSENT_URL | https://www.example.org/documents/consent/v1/ |
| PRIVACY_REGULATION_URL | https://www.example.org/documents/regulation/v1/ |
| CONSENT_VERSION | То же значение, что в серверном CONSENT_VERSION |
| APK_BUILD_VARIANT | release — установить последним, когда сервер и ключ готовы |

Ссылки/версия документов обязательны в CI для release EYEWITNESS. Это overrides поверх файлов config. Не помещайте пароли в Variables. На pull request релиз не публикуется и ключ не передаётся сборке.

Secrets для боевой подписи каждого приложения:

- APK_RELEASE_KEYSTORE_BASE64 — Base64 **утверждённого** keystore;
- APK_RELEASE_KEY_ALIAS — alias нужного ключа;
- APK_RELEASE_STORE_PASSWORD — пароль keystore;
- APK_RELEASE_KEY_PASSWORD — пароль ключа.

Для существующего рабочего приложения используйте его исходный ключ, иначе обновление не установится. Новый ключ создавайте только для нового приложения, храните офлайн резервную копию. В Android Studio используйте Generate Signed App Bundle or APK; секреты не сохраняйте в исходниках. Пример передачи имеющегося keystore в GitHub без временного Base64-файла (PowerShell):

```powershell
$encoded = [Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\secure\production.jks'))
$encoded | gh secret set APK_RELEASE_KEYSTORE_BASE64 --repo YuriSilenok/SKBT-UP-GIBDD-EYEWITNESS
Remove-Variable encoded
# Пароли и alias задайте через интерфейс GitHub Secrets; не отправляйте их в чат.
```

Ручной выпуск: Actions → Android APK → Run workflow → main → build_variant=release → publish=true. После настройки APK_BUILD_VARIANT=release push в main также будет собирать рабочий вариант. Для ручного теста выберите debug; его тестовый ключ хранится отдельно. Unit tests выполняются для общего debug-кода, а Lint/assemble — для выбранного варианта. Подписание выполняется отдельным заданием после успешных проверок; Gradle не получает боевой ключ.

Релиз содержит `APP-release.apk`, `SHA256SUMS.txt`, `THIRD-PARTY-NOTICES.zip` и `BUILD-INFO.json`. Проверьте в BUILD-INFO фактические домен, ссылки, applicationId, версию и commit. Версии публикаций последовательны: v1.0.0, v1.0.1 и далее. VersionCode увеличивается на единицу относительно опубликованных APK; не уменьшайте его для последующих выпусков. Test и release имеют разные applicationId, поэтому тестовая установка не превращается в боевую простым обновлением.

Для локальной сборки после заполнения config/local.properties:

```powershell
.\gradlew.bat --no-configuration-cache testDebugUnitTest lintRelease assembleRelease
```

Это создаёт **неподписанный** app/build/outputs/apk/release/app-release-unsigned.apk. Подпишите его утверждённым рабочим ключом через Android Studio либо apksigner с паролями через env. Не раздавайте unsigned APK и не подписывайте боевой вариант debug-ключом.

## 5. Проверки перед открытием пользователям

Проверьте HTTPS/цепочку сертификата, /health, WSS, согласие по новым ссылкам, совпадение версии согласия, получение сообщения двумя сотрудниками, подтверждение без удаления общей записи, отзыв с удалением своих вложений, запрет доступа к чужим данным, защиту последнего CHIEF и скачивание Excel. Используйте только вымышленные данные в приёмочном прогоне; очистку стенда с реальными данными не выполняйте.

Контролируйте `systemctl status gibdd`, `journalctl -u gibdd`, свободный диск, ошибки фоновой очистки и доступность БД. Срок содержания — до 30 суток, очистка API раз в минуту; просмотр не продлевает срок. Уведомления Android могут задерживаться и сами не обеспечивают норматив дежурства 15 минут.

Резервные копии БД и медиа шифруйте, храните у согласованного оператора, ограничьте доступ и утвердите срок удаления с учётом 30 суток и отзывов согласия. При восстановлении сначала применяйте актуальные отзывы/очистку истёкших данных в закрытой среде, затем открывайте API. Схема резервирования и удаление копий требуют отдельного согласования; текущий код не управляет внешними резервными копиями.

Обновление: согласованное окно → backup → `systemctl stop gibdd` → проверенный commit/tag через git pull --ff-only → pip install из lock → миграция через приведённый systemd-run → start → /health и функциональная проверка. Откат БД выполняется по заранее проверенному плану; старый код может быть несовместим с новой схемой. Ключи, .env и рабочие данные не отправлять в Git.

Рабочий запуск также требует утверждённых документов, согласования хранения общей записи до 30 суток, полномочий сотрудников, цепочки авторских прав и организационных мер защиты. Эта инструкция и HTTPS не являются аттестацией ИСПДн.

## Автообновление существующей тестовой VDS

Для VDS 193.124.115.164 настроен отдельный серверный workflow Server CI and VDS deployment. Push main проходит тесты PostgreSQL и вызывает ограниченную SSH-команду. Она забирает именно текущий main из GitHub, устанавливает закреплённые зависимости, выполняет повторные тесты SQLite на VDS, сохраняет резервную копию БД/вложений и переключает только gibdd-test.service. При ошибке старта/health-check возвращаются прежняя служба и данные. Основной сайт и другие приложения не обновляются этим процессом. Для будущего production-сервера нужен отдельный согласованный deployment; автоматически переносить тестовые настройки в КГУ нельзя.

Автообновление сохраняет данные. Однократная очистка 24.11.2026 выполнена отдельно администратором; deployment-ключ не может её вызвать. Подробности — deploy/VDS_AUTODEPLOY.md серверного репозитория.
