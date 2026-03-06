plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.2.0"
}

group = "macrosnik"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("com.github.kwhat:jnativehook:2.2.2")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

javafx {
    version = "21.0.5"
    modules = listOf("javafx.controls")
}

application {
    mainModule.set("macrosnik")
    mainClass.set("macrosnik.app.MainApp")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}



jlink {
    //moduleName.set("macrosnik")
    imageName.set("MacRosNik")

    launcher {
        name = "MacRosNik"
    }

    jpackage {
        installerType = "msi"
        appVersion = project.version.toString()
        vendor = "MacrosNik"
        installerOptions = listOf(
            "--win-upgrade-uuid", "a94a57ec-4c96-4b95-8b92-dd012041f832",
            "--win-per-user-install",
            "--win-dir-chooser",
            "--win-menu",
            "--win-shortcut"
        )
    }
}

