plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "macrosnik"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

javafx {
    version = "25"
    modules = listOf("javafx.controls")
}

application {
    mainClass.set("macrosnik.app.MainApp")
}

tasks.test {
    useJUnitPlatform()
}
