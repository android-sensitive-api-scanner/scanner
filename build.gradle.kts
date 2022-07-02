import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        implementation("org.slf4j:slf4j-api:1.7.36")
        compileOnly("org.jetbrains:annotations:23.0.0")

//        testImplementation(kotlin("test"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

group = "io.github.porum"
version = "1.0.1"