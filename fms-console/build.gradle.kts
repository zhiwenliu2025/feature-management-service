plugins {
    java
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(platform(libs.vaadin.bom))
    implementation(libs.vaadin.spring.boot.starter)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation("org.springframework.boot:spring-boot-starter-json")

    implementation(project(":fms-common"))
}
