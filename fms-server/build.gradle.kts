plugins {
    alias(libs.plugins.spring.boot)
    java
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":fms-common"))
    implementation(project(":fms-rule-engine"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.aspectj)
    implementation(libs.spring.boot.flyway)
    implementation(libs.springdoc.openapi)
    implementation(libs.caffeine)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:4.1.0")

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.redis)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("fms-server")
}

springBoot {
    mainClass.set("com.fms.FmsApplication")
}
