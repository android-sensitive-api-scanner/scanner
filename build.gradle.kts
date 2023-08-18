import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.9.0"
}

allprojects {
  apply(plugin = "java")

  repositories {
    google()
    mavenCentral()
  }

  dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    testImplementation(kotlin("test"))
  }

  tasks.test {
    useJUnitPlatform()
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }
}

group = "io.github.porum"
version = "1.1.0"