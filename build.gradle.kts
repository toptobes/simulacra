import org.gradle.kotlin.dsl.application

plugins {
    id("java")
    application
}

group = "org.datastax.simulacra"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("org.datastax.simulacra.Main")
    applicationDefaultJvmArgs = listOf("--enable-preview")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.2")
    implementation("com.fasterxml.jackson.module:jackson-module-jsonSchema:2.12.2")
    implementation("com.kjetland:mbknor-jackson-jsonschema_2.12:1.0.39")
    implementation("com.theokanning.openai-gpt3-java:service:0.14.0")
    implementation("com.datastax.oss:java-driver-core:4.17.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview")
}
