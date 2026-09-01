plugins {
    id("org.springframework.boot")
    id("com.vaadin")
}

dependencies {
    implementation(project(":backend"))
    implementation("com.vaadin:vaadin-spring-boot-starter")
    developmentOnly("com.vaadin:vaadin-dev")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

springBoot {
    mainClass.set("it.brunasti.mitire.Application")
}
