
````markdown
# Проект 2 — Система модерации обращений клиентов  
---

## Описание проекта

Данный проект реализует сервис модерации обращений клиентов на базе событийной архитектуры.  
Система принимает события об обращениях клиентов, обогащает данные через внешний сервис и принимает решение о дальнейшем прохождении обращения на основе бизнес-правил.

Проект состоит из нескольких микросервисов, взаимодействующих через Kafka, REST API и Redis.

---

## Архитектура системы

Client / Load Test (k6)
        |
        v
+------------------+
| Event Producer   |
|  REST /send      |
+------------------+
        |
        v
+------------------+
| Kafka Topic-1    |
+------------------+
        |
        v
+------------------------------+
| Service-1: Moderation        |
| - Idempotency check          |
| - Active appeal check        |
| - Working hours check        |
+------------------------------+
        |
        +---- REST ----> +---------------------+
        |                | Service-2: Enrichment|
        |                |  (Redis cache)       |
        |                +----------+-----------+
        |                           |
        |                           v
        |                       +--------+
        |                       | Redis  |
        |                       +--------+
        |
        v
+------------------+
| PostgreSQL       |
| processed_events |
| appeals          |
+------------------+

(APPROVED only)
        |
        v
+------------------+
| Kafka Topic-2    |
+------------------+

Metrics:
Service-1, Service-2 -> Prometheus -> Grafana

* Kafka используется для событийной обработки и обеспечения масштабируемости.
* Идемпотентность реализована на уровне Service-1 через таблицу `processed_events`,
  так как Kafka использует at-least-once delivery.
* Service-2 вынесен в отдельный сервис, чтобы:
  - не смешивать бизнес-логику модерации и работу с кэшем
  - изолировать возможные проблемы Redis
* WebFlux используется для неблокирующего вызова Service-2.
* Все блокирующие операции (JPA, Kafka) выполняются в `boundedElastic` пуле,
  чтобы не блокировать event-loop.
---

## Компоненты системы

| Компонент                  | Назначение                                         |
| -------------------------- | -------------------------------------------------- |
| **Event Producer**         | REST API для генерации событий обращений           |
| **Kafka Topic-1**          | Входной поток событий                              |
| **Service-1 (Moderation)** | Основная бизнес-логика и правила модерации         |
| **Service-2 (Enrichment)** | Обогащение данных из Redis                         |
| **Redis**                  | Кэш расширенной информации                         |
| **PostgreSQL**             | Хранение обработанных событий и активных обращений |
| **Kafka Topic-2**          | Поток успешно прошедших модерацию событий          |
| **Prometheus**             | Сбор метрик                                        |
| **Grafana**                | Визуализация метрик                                |

---
##  Технологический стек

* Java 21
* Spring Boot
* Spring WebFlux (WebClient)
* Apache Kafka
* Redis
* PostgreSQL
* Spring Data JPA
* Micrometer + Prometheus
* Grafana
* Docker / Docker Compose
* k6 (load testing)


## Реализованный функционал

### Service-1 — Moderation Service

* Приём событий из Kafka (Topic-1)
* Вызов Service-2 для обогащения данных
* Идемпотентная обработка событий
* Применение бизнес-правил
* Публикация результата в Topic-2 при успешной модерации
* Хранение состояния в PostgreSQL
* Метрики через Micrometer + Prometheus

### Service-2 — Enrichment Service

* REST API для получения расширенной информации
* Хранение данных в Redis
* Корректная обработка отсутствующих данных
* Метрики через Micrometer

---

## Бизнес-правила модерации и их проверка

1. **Идемпотентность**
   Если событие с таким `eventId` уже было обработано — повторная обработка не выполняется.
Отправить одно и то же событие с одинаковым `eventId` дважды — в Topic-2 попадёт только одно сообщение.

2. **Наличие активных обращений**
   Если у клиента уже есть активное обращение по той же категории — событие отклоняется.
Отправить событие с тем же `clientId` и `category`, когда уже есть активное обращение — событие будет отклонено.

3. **Рабочее время**
   Для категорий `urgent`, `credit`, `loan` обращения вне интервала **09:00–18:00 (Asia/Almaty)** автоматически отклоняются.
 Отправить событие категории `loan` вне интервала 09:00–18:00 — событие будет отклонено.

4. **Проверка по данным Enrichment Service**
   Если у клиента слишком много активных обращений — событие отклоняется.

## Запуск проекта

### Требования

* Docker
* Docker Compose
* Свободные порты:
  `8081`, `8082`, `8083`, `9092`, `19092`, `5432`, `6379`, `3000`, `19090`

---

### Полный запуск системы

```bash
docker compose up --build
```

После запуска будут подняты:

* Kafka + Zookeeper
* PostgreSQL
* Redis
* Service-1 (Moderation)
* Service-2 (Enrichment)
* Event Producer
* Prometheus
* Grafana

---

### Проверка сервисов

#### Event Producer

```http
POST http://localhost:8083/send
```

Пример тела запроса:

```json
{
  "clientId": 123,
  "category": "support",
  "theme": "Test appeal",
  "createdAt": "2026-02-07T10:00:00+05:00"
}
```

#### Moderation Service Health

```http
GET http://localhost:8081/actuator/health
```

---

## Метрики и мониторинг

### Grafana (локально)

```
http://localhost:3000
```

* Login: `admin`
* Password: `admin123`

---

## Нагрузочное тестирование (k6)

### Общая информация

| Параметр          | Описание                                  |
| ----------------- | ----------------------------------------- |
| Тип тестирования  | Нагрузочное (event-driven pipeline)       |
| Источник нагрузки | Event Producer                            |
| Цель              | Проверка устойчивости и времени обработки |
| Инструмент        | k6                                        |

### Grafana Cloud snapshot
 [https://ilesbeksara.grafana.net/dashboard/snapshot/cwz4WkpLO2SNrtTGr5i4NUCVjWsZECce](https://ilesbeksara.grafana.net/dashboard/snapshot/cwz4WkpLO2SNrtTGr5i4NUCVjWsZECce)

---

##  Основные метрики системы

| Метрика                | Описание                     |
| ---------------------- | ---------------------------- |
| Total Requests         | Общее число запросов         |
| Kafka Consumption Rate | Скорость обработки сообщений |
| Error Rate             | Близко к 0                   |
| JVM Memory             | Стабильное использование     |
| CPU Usage              | Без резких пиков             |
| Uptime                 | Непрерывная работа           |

Общая статистика:
[https://snapshots.raintank.io/dashboard/snapshot/pdgB93slL9SQWPHVsSjoeq8pONYMOa7q](https://snapshots.raintank.io/dashboard/snapshot/pdgB93slL9SQWPHVsSjoeq8pONYMOa7q)

---

## JVM Micrometer метрики

### Service-1 (Moderation)

[https://snapshots.raintank.io/dashboard/snapshot/Z0ktVicY6ZLqBxs99XDLk05qKa8AYrFN](https://snapshots.raintank.io/dashboard/snapshot/Z0ktVicY6ZLqBxs99XDLk05qKa8AYrFN)

### Service-2 (Enrichment)

[https://snapshots.raintank.io/dashboard/snapshot/rn8gkRQvwA5yBP0ok9O7co2OtDXmajYq](https://snapshots.raintank.io/dashboard/snapshot/rn8gkRQvwA5yBP0ok9O7co2OtDXmajYq)

---
## Ограничения и дальнейшее развитие

* Интеграция с Elasticsearch была начата, но не завершена из-за проблем
  с media-type заголовками и маппингом дат.
* В текущей версии Elasticsearch не участвует в основном потоке обработки событий.
* При наличии дополнительного времени планируется:
  - завершить интеграцию Elasticsearch для аналитики и поиска
  - добавить алерты в Grafana
  - расширить сценарии нагрузочного тестирования


## Автор
Илесбек Сара

