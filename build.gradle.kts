plugins {
    id("java")
}

group = "zealan"
version = "2.0"

repositories {
    mavenLocal()
    mavenCentral()
    flatDir {
        dir(file("libs"))
    }
    maven { url = uri("https://spigotmc.org") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.4.0")
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

    classpath = files(layout.projectDirectory.file("test-server/paper-1.8.8-445.jar"))
    workingDir = layout.projectDirectory.dir("test-server").asFile

    jvmArgs("-Xms2G", "-Xmx4G")
    standardInput = System.`in`
}