// A deployable Spring Boot service. Compiles against SPI only; plugins are runtimeOnly ("Lego":
// which plugins a service carries is a distribution decision, which ones are active is composition).
plugins {
    id("reconark.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":platform:spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

springBoot {
    buildInfo()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    imageName = "ghcr.io/manojsharma20/reconark-${project.name}:${project.version}"
}
