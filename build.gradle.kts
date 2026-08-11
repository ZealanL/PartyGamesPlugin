plugins {
    id("java")
}

group = "zealan"
version = "2.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://spigotmc.org") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }

    ivy {
        url = uri("https://ci.athion.net/job/FastAsyncWorldEdit-Legacy/lastSuccessfulBuild/artifact/target/")

        patternLayout {
            artifact("/[module]-[revision].jar")
        }

        metadataSources { artifact() }
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.4.0")
    compileOnly("ci.athion.net:FastAsyncWorldEdit-bukkit:21.03.26-5ff3a9b-1286-22.3.9")
}


tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

val updatePlugin = tasks.register<Copy>("updatePlugin") {
    dependsOn(tasks.jar)

    from(tasks.jar.flatMap { it.archiveFile })
    into(layout.projectDirectory.dir("test-server/plugins"))
}

tasks.register<JavaExec>("runServer") {
    group = "development"
    description = "Update the plugin and run the server"

    dependsOn(updatePlugin)

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })

    classpath = files(layout.projectDirectory.file("test-server/spigot-1.8.8.jar"))
    workingDir = layout.projectDirectory.dir("test-server").asFile

    jvmArgs("-Xms2G", "-Xmx4G")
    standardInput = System.`in`
}