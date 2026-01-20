rootProject.name = "MacRosNik"
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

toolchainManagement {
    jvm {
        javaRepositories {
            repository("foojay") {
                resolverClass.set(
                    org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java
                )
            }
        }
    }
}
