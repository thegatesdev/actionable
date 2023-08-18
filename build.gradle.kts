plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.thegatesdev"
version = "2.0"
description = "Data driven actions, conditions and event reacting."
java.sourceCompatibility = JavaVersion.VERSION_17

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    api("io.github.thegatesdev:maple:")
    compileOnly("io.github.thegatesdev:threshold:")
}

tasks {
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to "'1.20'"
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

    shadowJar {
        minimize()
        dependencies {
            include(dependency("io.github.thegatesdev:maple"))
        }
    }

    register<Copy>("pluginJar") {
        from(shadowJar)
        into(buildDir.resolve("pluginJar"))
        rename { "${project.name}.jar" }
    }
}