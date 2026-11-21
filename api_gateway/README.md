# API Gateway

Start config_service first, then api_gateway in separate terminals from the repository root:

```powershell
.\gradlew.bat :config_service:bootRun
.\gradlew.bat :api_gateway:bootRun
```

Gateway listens on `http://localhost:6002`. Config Server supplies its port and routes from `config_service/config-repo/api_gateway.yml`. The local application.yml only contains its application name and Config Server connection. Restart gateway after changing the remote configuration.

| Service / application name | Port |
| --- | --- |
| config_service | 6001 |
| api_gateway | 6002 |
| auth_service | 6003 |
| inventory_service | 6004 |
| order_service | 6005 |
| payment_service | 6006 |
| shipping_service | 6007 |
| product_service | 6008 |
| user_service | 6009 |
| plan_service | 6010 |
| pricing_service | 6011 |
| notification_service | 6012 |
| analytics_service | 6013 |

seed_service is a command-line job (`web-application-type: none`), so it has no HTTP port or gateway route. Infrastructure ports (Postgres, Redis, Kafka) stay unchanged.

Each business service has a route `/{directory_name}/api/v1/**`. Gateway removes only the service name (`StripPrefix=1`), preserving `/api/v1` for the controller. Internal and downstream actuator endpoints are not exposed.

Examples:

- `http://localhost:6002/auth_service/api/v1/auth/login`
- `http://localhost:6002/auth_service/api/v1/users/{userId}/roles`
- `http://localhost:6002/inventory_service/api/v1/phones/imports`
- `http://localhost:6002/order_service/api/v1/orders`
- `http://localhost:6002/payment_service/api/v1/payment-methods`
- `http://localhost:6002/shipping_service/api/v1/shipping-methods`
- `http://localhost:6002/product_service/api/v1/cache-demo/products/{id}`

Routes do not create endpoints: services without controllers still return 404. The old FE Products CRUD page targets `/product_service/api/v1/products`, but product_service currently implements only cache-demo and product-lock controllers.

Existing authentication behavior is unchanged; the public-path list now uses the service-prefixed URLs.

The gateway source on origin/main references TokenVerificationPort but does not include
that interface or an implementation. The interface is now included with the existing
`Mono<TokenVerification> verify(String accessToken)` contract. A production adapter bean
is still missing, so passing unit tests does not mean the application can start.
TokenAuthenticationService and its constructor have not been changed. This routing
update does not add a JWT verifier or change Redis/security configuration.

Override service destinations with AUTH_SERVICE_URL, INVENTORY_SERVICE_URL, etc. Config Server connection uses CONFIG_URL (default `http://localhost:6001`), CONFIG_USER and CONFIG_PASSWORD.

FE uses relative service-prefixed paths. Vite dev/preview forwards all these paths to GATEWAY_URL (default `http://localhost:6002`). Production hosting must reverse proxy the same service-prefixed paths to gateway and serve the SPA for non-API paths.
