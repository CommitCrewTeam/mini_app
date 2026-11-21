# config_service

Config Server runs on `http://localhost:6001` with the native profile. It serves `config-repo/api_gateway.yml` to the application named `api_gateway`.

From the repository root, run `.\gradlew.bat :config_service:bootRun`, then start `.\gradlew.bat :api_gateway:bootRun` in another terminal.

Gateway requires Config Server at startup and loads its port (6002) and routes from this repository. Other services currently keep their settings in their own application.yml.

CONFIG_USER and CONFIG_PASSWORD configure Basic authentication on both server and client. CONFIG_REPO_PATH overrides the configuration directory; defaults support both module and repository-root working directories. CONFIG_URL on gateway defaults to `http://localhost:6001`.

Read `/api_gateway/default` using Config Server credentials and check that propertySources contains api_gateway.yml, server.port=6002 and the gateway routes. Restart gateway after editing this configuration.

See [gateway documentation](../api_gateway/README.md) for all ports and public URL conventions.
