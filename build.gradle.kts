plugins {
    java
}

group = "dev.angopiccDev"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Compiled against the oldest API we support. Everything used here is long-standing
    // API, so the same jar runs on 1.21.4 through the current 26.x releases.
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    // Java 21 bytecode: loads on 1.21.x servers (Java 21) and 26.x servers (Java 25).
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
