plugins { id("reconark.spring-library") }

description = "Shared API conventions: JWT resource-server security, role mapping, RFC 9457 problem details."

dependencies {
    api(project(":kernel:plugin-api"))
    api("org.springframework.boot:spring-boot-starter-webmvc")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server") // verify starter name per Boot 4.x
    api("org.springframework.boot:spring-boot-starter-validation")
}
