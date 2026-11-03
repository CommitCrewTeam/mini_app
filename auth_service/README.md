auth_service/
├── build.gradle                  # deps: common, web, jpa, validation, actuator,
│                                 #       security, oauth2-resource-server, oauth2-jose,
│                                 #       nimbus-jose-jwt, kafka, liquibase, postgres
├── settings.gradle               # để build standalone (include ':common' -> ../common)
├── gradlew / gradlew.bat
├── gradle/wrapper/...
└── src/main/
├── java/com/miniecommerce/auth/
│   ├── AuthServiceApplication.java
│   ├── domain/                       # DDD: aggregate, value object, enum, event
│   │   └── event/
│   ├── app/                          # application layer
│   │   ├── command/                  # input DTO nội bộ (RegisterAccountCommand...)
│   │   ├── port/inbound/             # *UseCase interfaces
│   │   ├── port/outbound/            # *Port interfaces (repository, jwt, event...)
│   │   └── service/                  # *ApplicationService implements UseCase
│   ├── adapter/
│   │   ├── inbound/rest/             # *Controller
│   │   │   ├── dto/                  # request/response records
│   │   │   └── mapper/               # *RestMapper
│   │   ├── outbound/
│   │   │   ├── persistence/          # *Entity, *JpaRepository, *PersistenceAdapter
│   │   │   │   └── mapper/           # *PersistenceMapper
│   │   │   ├── security/             # PasswordHasher + Jwt adapter (riêng của auth)
│   │   │   ├── messaging/            # Kafka producer
│   │   │   └── stub/                 # adapter giả để chạy local
│   │   └── shared/batch/
│   └── config/                       # SecurityConfig, JwtProperties...
└── resources/
├── application.yml               # port 8091, db auth_service, jpa validate, liquibase
└── db/changelog/
├── db.changelog-master.yaml  # đang rỗng: databaseChangeLog: []
└── changes/                  # 001-create-accounts-table.yaml (sẽ thêm sau)