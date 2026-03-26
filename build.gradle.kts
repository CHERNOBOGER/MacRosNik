import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip

plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.2.0"
}

group = "macrosnik"
version = "0.1.0"

val appName = "MacRosNik"
val javaVersion = JavaLanguageVersion.of(21)
val jpackageJavaHome = javaToolchains.launcherFor {
    languageVersion.set(javaVersion)
}.map { it.metadata.installationPath.asFile.absolutePath }
val windowsInstallerType = providers.gradleProperty("windowsInstallerType").orElse("exe")
val jpackageImageOutputDir = layout.buildDirectory.dir("app-image")
val jpackageInstallerOutputDir = layout.buildDirectory.dir("installer")
val jpackageImageDir = layout.buildDirectory.dir("app-image/$appName")
val portableExeDir = layout.buildDirectory.dir("exe/$appName")
val jnativehookJar = configurations.runtimeClasspath.map { runtimeClasspath ->
    runtimeClasspath.files.first { it.name.startsWith("jnativehook-") && it.extension == "jar" }
}

java {
    toolchain {
        languageVersion.set(javaVersion)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
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
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    from({
        zipTree(jnativehookJar.get())
    }) {
        include("com/github/kwhat/jnativehook/lib/windows/**")
        into("native/jnativehook")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("file.encoding", "UTF-8")
}



jlink {
    //moduleName.set("macrosnik")
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    imageName.set(appName)

    launcher {
        name = appName
    }

    jpackage {
        jpackageHome = jpackageJavaHome.get()
        setImageOutputDir(jpackageImageOutputDir.get().asFile)
        setInstallerOutputDir(jpackageInstallerOutputDir.get().asFile)
        installerType = windowsInstallerType.get()
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

tasks.register<Sync>("packageExe") {
    group = "distribution"
    description = "Builds a portable Windows app image with MacRosNik.exe."
    dependsOn("jpackageImage")
    from(jpackageImageDir)
    into(portableExeDir)
}

tasks.register<Zip>("packageExeZip") {
    group = "distribution"
    description = "Creates a zip archive of the portable Windows app image."
    dependsOn("packageExe")
    archiveBaseName.set(appName)
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("windows-portable")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(portableExeDir)
}

tasks.register("packageInstaller") {
    group = "distribution"
    description = "Builds a Windows installer (.exe by default, .msi with -PwindowsInstallerType=msi)."
    dependsOn("jpackage")
}
