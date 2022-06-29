plugins {
    kotlin("jvm") version "1.7.0"
    id("application")
}

dependencies {
    implementation(project(":jadx-core"))
    implementation(project(":mapping-parser"))

    runtimeOnly(project(":jadx-dex-input"))

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.beust:jcommander:1.82")
    implementation("ch.qos.logback:logback-classic:1.2.11")
}

application {
    applicationName = "asas"
    mainClass.set("io.github.porum.asas.Main")
    applicationDefaultJvmArgs = listOf("-Xms128M", "-XX:MaxRAMPercentage=70.0", "-XX:+UseG1GC")
}
