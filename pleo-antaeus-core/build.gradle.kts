plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    compile(project(":pleo-antaeus-models"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:1.3.0-M1")
    implementation("joda-time:joda-time:2.9.9")
    implementation(kotlin("stdlib-jdk8"))
}