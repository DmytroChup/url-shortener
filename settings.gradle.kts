rootProject.name = "url-shortener"

listOf("contracts", "url-service", "analytics-service", "api-gateway", "auth-service")
    .filter { rootDir.resolve(it).isDirectory }
    .forEach { include(it) }
