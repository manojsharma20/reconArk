plugins { id("reconark.spring-library") }

description = "Spring Boot auto-configuration that boots the reconArk kernel from 'reconark.composition.*'."

dependencies {
    api(project(":kernel:plugin-runtime"))
    api(project(":domain:spi"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.slf4j:slf4j-api")
    compileOnly("org.springframework.boot:spring-boot-actuator")
    compileOnly("io.micrometer:micrometer-core")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
