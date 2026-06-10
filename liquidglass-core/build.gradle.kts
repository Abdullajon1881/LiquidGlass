plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = providers.gradleProperty("liquidglassGroup").get()
version = providers.gradleProperty("liquidglassVersion").get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        explicitApi()
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            from(components["java"])
            artifactId = "liquidglass-core"
        }
    }
}
