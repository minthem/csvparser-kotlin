import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.kotlinx.kover") version "0.9.2"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    `java-library`
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("org.jetbrains.dokka") version "1.9.20"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

group = "io.github.minthem"
version = System.getenv("CI_TAG") ?: "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    configure(KotlinJvm(JavadocJar.Dokka("dokkaHtml")))

    coordinates("io.github.minthem", "csvparser", version.toString())

    pom {
        name.set("csvparser")
        description.set("A simple CSV parser/writer for Kotlin")
        inceptionYear.set("2025")
        url.set("https://github.com/minthem/csvparser")
        licenses {
            license {
                name.set("The MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("minthem")
                name.set("")
                url.set("https://github.com/minthem")
            }
        }
        scm {
            url.set("https://github.com/minthem/csvparser")
            connection.set("scm:git:git://github.com/minthem/csvparser.git")
        }
    }
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
