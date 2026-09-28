rootProject.name = "url-shortener"

include("contracts")

if (rootDir.resolve("url-service").isDirectory) {
    include("url-service")
}

if (rootDir.resolve("analytics-service").isDirectory) {
    include("analytics-service")
}