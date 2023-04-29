plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.thegatesdev"
version = "1.2"
description = "Actions and conditions with YAML"
java.sourceCompatibility = JavaVersion.VERSION_17

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")

    api("com.github.thegatesdev:mapletree:-SNAPSHOT")
    compileOnly("io.github.thegatesdev:eventador:1.4.2")
    compileOnly("io.github.thegatesdev:threshold:0.2")
}

tasks{
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to "1.19"
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    shadowJar{
        minimize()
        dependencies{
            include(dependency("com.github.thegatesdev:mapletree"))
        }
    }

    register<Copy>("copyJarToLocalServer") {
        from(jar)
        into("D:\\Coding\\Minecraft\\SERVER\\plugins")
    }
}