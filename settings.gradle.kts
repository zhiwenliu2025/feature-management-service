pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "feature-management-service"

include("fms-common")
include("fms-rule-engine")
include("fms-console")
include("fms-server")
