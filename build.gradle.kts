plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.github.ssquadteam"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    compileOnly("com.github.ssquadteam:TaleLib")
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    archiveBaseName.set("VideoMaps")

    from(sourceSets.main.get().output)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
