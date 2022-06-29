plugins {
    kotlin("jvm") version "1.7.0"
}

dependencies {
//    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}