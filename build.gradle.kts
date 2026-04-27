import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.GradleException
import java.io.ByteArrayOutputStream

data class GitVersionInfo(
    val projectVersion: String,
    val installerVersion: String,
    val gitDescribe: String?
)

fun gitOutput(rootPath: String, vararg args: String): String? {
    val stdout = ByteArrayOutputStream()
    val stderr = ByteArrayOutputStream()
    val result = exec {
        commandLine("git", "-c", "safe.directory=$rootPath", *args)
        standardOutput = stdout
        errorOutput = stderr
        isIgnoreExitValue = true
    }
    if (result.exitValue != 0) {
        return null
    }
    return stdout.toString().trim().takeIf { it.isNotEmpty() }
}

fun resolveGitVersion(rootPath: String): GitVersionInfo {
    val fallbackBaseVersion = "0.1.0"
    val describe = gitOutput(rootPath, "describe", "--tags", "--dirty", "--always", "--match", "v*")
    val normalizedDescribe = describe?.removeSuffix("-dirty")
    val isDirty = describe?.endsWith("-dirty") == true

    val exactTagMatch = normalizedDescribe?.let {
        Regex("""^v(\d+(?:\.\d+){0,3})$""").matchEntire(it)
    }
    if (exactTagMatch != null) {
        val exactVersion = exactTagMatch.groupValues[1]
        val projectVersion = if (isDirty) "$exactVersion-dirty" else exactVersion
        val installerVersion = if (isDirty) "$exactVersion.0" else exactVersion
        return GitVersionInfo(projectVersion, installerVersion, describe)
    }

    val describedTagMatch = normalizedDescribe?.let {
        Regex("""^v(\d+(?:\.\d+){0,3})-(\d+)-g([0-9a-f]+)$""").matchEntire(it)
    }
    if (describedTagMatch != null) {
        val baseVersion = describedTagMatch.groupValues[1]
        val distance = describedTagMatch.groupValues[2]
        val commitSha = describedTagMatch.groupValues[3]
        val projectVersion = buildString {
            append(baseVersion)
            append("-dev.")
            append(distance)
            append("+g")
            append(commitSha)
            if (isDirty) {
                append(".dirty")
            }
        }
        return GitVersionInfo(projectVersion, "$baseVersion.$distance", describe)
    }

    val commitCount = gitOutput(rootPath, "rev-list", "--count", "HEAD")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val commitSha = gitOutput(rootPath, "rev-parse", "--short", "HEAD") ?: "nogit"
    val projectVersion = buildString {
        append(fallbackBaseVersion)
        append("-dev.")
        append(commitCount)
        append("+g")
        append(commitSha)
        if (isDirty) {
            append(".dirty")
        }
    }
    return GitVersionInfo(projectVersion, "$fallbackBaseVersion.$commitCount", describe)
}

plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.2.0"
}

group = "macrosnik"
val resolvedVersion = resolveGitVersion(rootDir.absolutePath.replace('\\', '/'))
version = resolvedVersion.projectVersion

val appName = "MacRosNik"
val javaVersion = JavaLanguageVersion.of(21)
val installerVersion = resolvedVersion.installerVersion
val jpackageJavaHome = javaToolchains.launcherFor {
    languageVersion.set(javaVersion)
}.map { it.metadata.installationPath.asFile.absolutePath }
val windowsIconFile = layout.projectDirectory.file("src/main/resources/icons/app.ico").asFile
val windowsInstallerType = providers.gradleProperty("windowsInstallerType").orElse("exe")
val installerFileExtension = windowsInstallerType.map { it.lowercase() }
val jpackageImageOutputDir = layout.buildDirectory.dir("app-image")
val jpackageInstallerOutputDir = layout.buildDirectory.dir("installer")
val jpackageImageDir = layout.buildDirectory.dir("app-image/$appName")
val portableExeDir = layout.buildDirectory.dir("exe/$appName")
val releaseAssetsDir = layout.buildDirectory.dir("release-assets")
val releaseInstallerFileName = installerFileExtension.map { "$appName-Setup.$it" }
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
        appVersion = installerVersion
        vendor = "MacrosNik"
        if (windowsIconFile.exists()) {
            icon = windowsIconFile.absolutePath
        }
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
    mustRunAfter("jpackage")
    from(jpackageImageDir)
    into(portableExeDir)
}

tasks.register<Zip>("packageExeZip") {
    group = "distribution"
    description = "Creates a zip archive of the portable Windows app image."
    dependsOn("packageExe")
    archiveFileName.set("$appName.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(portableExeDir)
}

tasks.register("packageInstaller") {
    group = "distribution"
    description = "Builds a Windows installer (.exe by default, .msi with -PwindowsInstallerType=msi)."
    dependsOn("jpackage")
}

tasks.register("prepareReleaseAssets") {
    group = "distribution"
    description = "Collects versionless files for GitHub Releases."
    dependsOn("packageExeZip", "packageInstaller")
    doLast {
        val outputDir = releaseAssetsDir.get().asFile
        delete(outputDir)
        outputDir.mkdirs()

        copy {
            from(tasks.named<Zip>("packageExeZip").flatMap { it.archiveFile })
            into(outputDir)
        }

        val installerFile = jpackageInstallerOutputDir.get().asFile
            .listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension.equals(installerFileExtension.get(), ignoreCase = true) }
            ?.maxByOrNull { it.lastModified() }
            ?: throw GradleException(
                "No installer file with extension .${installerFileExtension.get()} found in ${jpackageInstallerOutputDir.get().asFile}."
            )

        copy {
            from(installerFile)
            into(outputDir)
            rename { releaseInstallerFileName.get() }
        }
    }
}

tasks.register("printVersion") {
    group = "help"
    description = "Prints the resolved project and installer versions."
    doLast {
        println("projectVersion=${project.version}")
        println("installerVersion=$installerVersion")
        println("gitDescribe=${resolvedVersion.gitDescribe ?: "n/a"}")
    }
}
