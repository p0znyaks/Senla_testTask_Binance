# Binance Price Service

Сервис раз в N секунд забирает среднюю цену торговой пары с Binance, сохраняет в PostgreSQL и отдаёт через REST.

## Требования

- Java 17+
- Docker и `docker compose`
- Gradle (используется wrapper `./gradlew`)

## 1. Клонировать репозиторий

```bash
git clone https://github.com/p0znyaks/Senla_testTask_Binance.git
cd Senla_testTask_Binance
```

## 2. Настроить переменные окружения

```bash
cp .env.example .env
```

Файл `.env` не попадает в гит — у каждого разработчика своя копия.

| Переменная | По умолчанию | Зачем |
|---|---|---|
| `POSTGRES_USER` | `binance` | Пользователь БД |
| `POSTGRES_PASSWORD` | `binance` | Пароль БД |
| `POSTGRES_DB` | `binance` | Имя базы |
| `POSTGRES_IMAGE` | `postgres:17-alpine` | Образ PostgreSQL |
| `POSTGRES_PORT` | `5433` | Порт БД на хосте |
| `APP_PORT` | `8080` | Порт приложения |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/binance` | JDBC для локального запуска |
| `SPRING_DATASOURCE_URL_DOCKER` | `jdbc:postgresql://db:5432/binance` | JDBC внутри Docker |
| `SPRING_DATASOURCE_USERNAME` | `binance` | Имя пользователя для приложения |
| `SPRING_DATASOURCE_PASSWORD` | `binance` | Пароль для приложения |
| `BINANCE_POLL_INTERVAL` | `PT30S` | Интервал опроса Binance (ISO-8601) |
| `BINANCE_SYMBOL` | `BNBUSDT` | Торговая пара |
| `BINANCE_BASE_URL` | `https://api.binance.com` | Адрес API Binance |
| `BINANCE_CONNECT_TIMEOUT` | `5s` | Таймаут соединения |
| `BINANCE_READ_TIMEOUT` | `10s` | Таймаут чтения |
| `BINANCE_SCHEDULER_ENABLED` | `true` | Включить фоновый опрос |

## 3. Запуск через Docker Compose

```bash
docker compose up --build
```

Поднимутся `db` (PostgreSQL) и `app` (приложение на порту `APP_PORT`).  
База инициализируется сама через Liquibase.

## 4. Запуск локально (без Docker)

Нужен запущенный PostgreSQL на хосте с настройками из `.env`.

```bash
./gradlew bootRun
```

Приложение подхватит `.env` через `application.yml`.

## 5. REST API

Базовый адрес: `http://localhost:<APP_PORT>/api/v1/prices`

Ошибки всегда в одном формате:

```json
{"error": "<сообщение>"}
```

| Статус | Когда |
|---|---|
| `400` | `from`/`to` отсутствуют, `from` позже `to`, или некорректная пагинация (`page < 0`, `size < 1`, `size > 200`) |
| `404` | Нет записей за запрошенный диапазон (average, trend, latest) |

Binance запрашивает только фоновый планировщик, а не REST-эндпоинты, так что ошибки Binance не попадают в API.

### 5.1 Последняя цена

`GET /api/v1/prices/latest`

Ответ:

```json
{
  "symbol": "BNBUSDT",
  "price": "300.0",
  "fetchedAt": "2026-09-03T00:00:00Z"
}
```

- `symbol` — торговая пара (по умолчанию `BNBUSDT`)
- `price` — цена (`BigDecimal`, без `double`)
- `fetchedAt` — время получения записи, ISO-8601 в UTC

Возвращает `404`, если записей ещё нет.

### 5.2 Средняя цена за период

`GET /api/v1/prices/average?from=2026-09-01T00:00:00Z&to=2026-09-30T00:00:00Z`

Ответ:

```json
{
  "averagePrice": 200.0
}
```

Параметры `from` и `to` — ISO-8601. `from` не может быть позже `to`, иначе `400`.  
Если записей за период нет — `404`.

### 5.3 Записи с пагинацией

`GET /api/v1/prices?from=...&to=...&page=0&size=20`

Ответ:

```json
{
  "items": [
    {
      "symbol": "BNBUSDT",
      "price": "300.0",
      "fetchedAt": "2026-09-03T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "hasNext": true
}
```

- `page` — с нуля, должен быть `>= 0`
- `size` — от `1` до `200`
- Пустой диапазон возвращает `200` с пустым `items`

### 5.4 Тренд за период

`GET /api/v1/prices/trend?from=2026-09-01T00:00:00Z&to=2026-09-30T00:00:00Z`

Ответ:

```json
{
  "symbol": "BNBUSDT",
  "start_date": "2026-09-01",
  "end_date": "2026-09-30",
  "trend": "UPWARD"
}
```

Значения `trend`: `UPWARD`, `DOWNWARD`, `FLAT`.  
Если записей нет — `404`.

## 6. Как работает планировщик

- Опрос Binance — с фиксированной задержкой: следующий запуск начинается только после окончания предыдущего плюс `BINANCE_POLL_INTERVAL`.
- Пара берётся из `BINANCE_SYMBOL` (по умолчанию `BNBUSDT`).
- Каждый опрос — HTTP-запрос к `https://api.binance.com/api/v3/avgPrice`.
- В БД максимум одна строка на пару Binance (`ON CONFLICT (symbol, close_time) DO NOTHING`). Повторный опрос в ту же минуту пропускается.
- Если Binance не прислал `closeTime`, используется время получения — строка всё равно сохранится.
- Ошибка Binance не останавливает расписание и не портит данные. Планировщик пишет предупреждение в лог и продолжает.
- При `429 Too Many Requests` планировщик читает заголовок `Retry-After` и ставит паузу до указанного момента, затем возобновляет обычный интервал.

## 7. Тесты

```bash
./gradlew test
```

Что покрыто:
- `BinanceClientTest` — успешный ответ, обработка `429`, не-JSON тело ошибки, некорректный `Retry-After`.
- `PriceFetchServiceTest` — сохранение цены, fallback `closeTime` на время получения, отклонение ответа без цены.
- `PriceQueryServiceTest` — средняя цена, страница, тренд, проверка диапазона.
- `PriceRecordSchedulerTest` — пауза после `429`, продолжение после других ошибок.
- `PriceRecordControllerIntegrationTest` — интеграционный тест с Testcontainers (PostgreSQL) + MockMvc: все эндпоинты, коды ответов `200/400/404`.

## 8. Сборка

```bash
./gradlew build
```