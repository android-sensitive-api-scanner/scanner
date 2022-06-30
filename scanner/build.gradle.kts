plugins {
    kotlin("jvm") version "1.7.0"
    application
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.7.0"
}

dependencies {
    implementation(project(":jadx-core"))
    implementation(project(":mapping-parser"))

    implementation(project(":jadx-dex-input"))

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.beust:jcommander:1.82")
    implementation("ch.qos.logback:logback-classic:1.2.11")
}

application {
    applicationName = "asas"
    mainClass.set("io.github.porum.asas.MainKt")
    applicationDefaultJvmArgs = listOf("-Xms128M", "-XX:MaxRAMPercentage=70.0", "-XX:+UseG1GC")
}


tasks.withType<Jar> {

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "io.github.porum.asas.MainKt"
//        attributes["Class-Path"] = configurations.runtimeClasspath.get().files.map { it.name }.joinToString { " " }
    }

//    configurations.compileClasspath.get().files.filter { it.exists() }.forEach { file: File ->
//        if (file.isDirectory) {
//            from(file)
//        } else {
//            from(zipTree(file.absoluteFile))
//        }
//    }

    configurations.runtimeClasspath.get().files.filter { it.exists() }.forEach { file: File ->
        println(file)
        if (file.isDirectory) {
            from(file)
        } else {
            from(zipTree(file.absoluteFile))
        }
    }
}

tasks.dokkaJavadoc.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
    offlineMode.set(true)
    dokkaSourceSets {
        configureEach {
            // Do not create index pages for empty packages
            skipEmptyPackages.set(true)
            // Disable linking to online kotlin-stdlib documentation
            noStdlibLink.set(false)
            // Disable linking to online Android documentation (only applicable for Android projects)
            noAndroidSdkLink.set(false)

            // Suppress a package
            perPackageOption {
                // will match all .internal packages and sub-packages
                matchingRegex.set(".*\\.internal.*")
                suppress.set(true)
            }
        }
    }
}

val sourceJar by tasks.registering(Jar::class) {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("cmdline") {
            from(components["java"])
            artifact(sourceJar)
            artifact(dokkaJavadocJar)

            groupId = rootProject.group.toString()
            artifactId = "android-sensitive-api-scanner"
            version = rootProject.version.toString()

            pom {
                name.set("android-sensitive-api-scanner")
                description.set("android-sensitive-api-scanner")
                url.set("https://github.com/porum/android-sensitive-api-scanner.git")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("porum")
                        name.set("guobao.sun")
                        email.set("sunguobao12@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/porum/android-sensitive-api-scanner.git")
                }
            }
        }
    }

    val sonatypeUserName: String by project
    val sonatypePassword: String by project

    repositories {
        mavenLocal()
        maven {
            val url = if (rootProject.version.toString().endsWith("-SNAPSHOT")) {
                "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            } else {
                "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            }
            setUrl(url)
            credentials {
                username = sonatypeUserName
                password = sonatypePassword
            }
        }
    }
}

signing {
    sign(extensions.getByType<PublishingExtension>().publications)
}