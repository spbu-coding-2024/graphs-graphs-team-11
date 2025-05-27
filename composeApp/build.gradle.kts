import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    id("org.jetbrains.dokka") version "1.9.0"
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation("org.neo4j.driver:neo4j-java-driver:5.10.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                implementation("org.xerial:sqlite-jdbc:3.36.0.3")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        val desktopTest by getting {
            dependencies {
                // JUnit 5
                implementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
            }
        }
    }
}

// Объявляем таск Dokka внутри блока tasks
tasks.named<DokkaTask>("dokkaHtml") {
    // Куда выводить HTML
    outputDirectory.set(buildDir.resolve("dokka/html"))

    dokkaSourceSets {
        named("commonMain") {
            displayName.set("Общее API")
        }
        named("desktopMain") {
            displayName.set("Desktop API")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "org.spb.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.spb.project"
            packageVersion = "1.0.0"
        }
    }
}