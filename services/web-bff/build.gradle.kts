plugins { id("reconark.spring-service") }

description = "Backend-for-frontend: OIDC token handler, config-driven route table, UI manifest."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client") // verify starter name per Boot 4.x
}
