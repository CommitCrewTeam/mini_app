# Learning Quartz

The job cancels PENDING orders after 30 minutes, scanning up to 100 orders each minute.

## Use the bundled Quartz schema

Spring Boot runs the PostgreSQL script bundled inside the Quartz dependency with
`spring.quartz.jdbc.initialize-schema=always` (the default in this learning setup).
There is no copied Quartz SQL file or Quartz Liquibase migration in this project.
Liquibase still manages business tables.

The bundled script drops and recreates Quartz tables on startup, clearing saved
jobs and triggers. The application's JobDetail/Trigger beans register the job again.

## Try two instances

1. Start one instance once with the default `always` to initialize the tables.
2. Stop it.
3. Set `QUARTZ_INITIALIZE_SCHEMA=never` on BOTH instances.
4. Start both with different HTTP ports and the same database/schema.

Never use `always` while another instance is using those Quartz tables.
Keep `never` for later restarts that should preserve schedules.

Both instances use scheduler-name `order-service-cluster`, JDBC JobStore,
`isClustered=true`, and `instanceId=AUTO`. The job has
`@DisallowConcurrentExecution` and the shared JobKey `orders.expire-pending-orders`.
These settings coordinate execution across instances through the database.

Inspect QRTZ_JOB_DETAILS for the job, QRTZ_TRIGGERS for trigger state,
QRTZ_FIRED_TRIGGERS for execution ownership, QRTZ_SCHEDULER_STATE for heartbeats,
and QRTZ_LOCKS for rows used by database locks.

## Tests

Run `gradlew.bat :order_service:test` from the repository root.
Tests cover expiration, stale updates, and two JDBC schedulers sharing H2.
PostgreSQL multi-instance operation still needs a smoke test.

Reference: https://docs.spring.io/spring-boot/reference/io/quartz.html
