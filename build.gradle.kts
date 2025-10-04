import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.kotlinx.kover") version "0.9.2"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    `java-library`
}

group = "io.github.minthem"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

kover {
    reports {
        filters {
            excludes {
                classes("com.example.*.ExcludedByName", "com.example.*.serializables.*\$Companion")
                annotatedBy("*.Generated")
                inheritedFrom("java.lang.AutoCloseable")
            }
        }

        total {

            xml {
                onCheck = true
                xmlFile.set(layout.buildDirectory.file("custom.xml"))
            }

            html {
                onCheck = true
                htmlDir.set(layout.buildDirectory.dir("reports/kover/html"))
                title = "csvparser Coverage Report"
                charset = "UTF-8"
            }

            log {
                onCheck = true
                groupBy = GroupingEntityType.APPLICATION
                coverageUnits = CoverageUnit.BRANCH
                aggregationForGroup = AggregationType.MISSED_COUNT
                header = null
                format = "Full coverage is {value}%"
            }

            verify {
                onCheck = true
                warningInsteadOfFailure = false

                rule("package covered lines") {
                    groupBy = GroupingEntityType.PACKAGE

                    bound {
                        minValue = 85
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}

ktlint {
    version.set("1.5.0")
    verbose.set(true)
    android.set(false)
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
}
