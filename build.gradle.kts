import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val guavaVersion by extra("19.0")
val pircbotxVersion by extra("2.1")
val okHttpVersion by extra("4.12.0")
val jsonVersion by extra("20231013")
val socketIoVersion by extra("2.1.0")
val kotlinXCoroutinesVersion by extra("1.5.2")

plugins {
  id("org.springframework.boot") version "3.2.2"
  id("io.spring.dependency-management") version "1.1.4"
  kotlin("jvm") version "1.9.22"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
}

group = "com.github.veccvs"

version = "0.0.1-SNAPSHOT"

java { sourceCompatibility = JavaVersion.VERSION_21 }

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }

repositories { mavenCentral() }

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.liquibase:liquibase-core")
  compileOnly("org.projectlombok:lombok")
  runtimeOnly("org.postgresql:postgresql")
  annotationProcessor("org.projectlombok:lombok")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  implementation("org.pircbotx:pircbotx:${pircbotxVersion}")
  implementation("com.google.guava:guava:${guavaVersion}")
  implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
  implementation("org.json:json:${jsonVersion}")
  implementation("io.socket:socket.io-client:${socketIoVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlinXCoroutinesVersion}")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs += "-Xjsr305=strict"
    jvmTarget = "21"
  }
}

tasks.withType<Test> { useJUnitPlatform() }
