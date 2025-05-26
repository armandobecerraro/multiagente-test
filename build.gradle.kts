plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("java")
    id("application")
}

group = "org.smagesci"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // JADE Framework (local dependencies)
    implementation(files("libs/jade/lib/jade.jar"))
    implementation(files("libs/jade/lib/jadeExamples.jar"))

    // Database connectivity
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("mysql:mysql-connector-java:8.0.33")

    // JSON processing (Jackson)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2") // Added for Java Date/Time

    // Logging (SLF4J API + Logback Classic) - REPLACED Log4j2
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.3.11") // Or a more recent compatible version like 1.4.14 for SLF4J 2.0.x

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.11.0") // Added Mockito core
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0") // Added Mockito JUnit 5 integration
}

application {
    mainClass.set("com.smagesci.MainContainerLauncher") // Changed to new MainContainerLauncher
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}