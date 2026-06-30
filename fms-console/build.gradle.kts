plugins {
    java
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(platform(libs.vaadin.bom))
    implementation(libs.vaadin.spring.boot.starter)
    implementation(libs.spring.boot.starter.security)

    implementation(project(":fms-common"))
}
