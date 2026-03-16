plugins {
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

// Disable bootJar for library
tasks.bootJar {
    enabled = false
}

// Remove plain classifier for jar
tasks.jar {
    archiveClassifier = ""
}

group = "io.github.vduzh"
version = "0.1.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "Keycloak JWT Authorities Converter"
                description = "Spring Security converter that extracts granted authorities from Keycloak JWT tokens"
                url = "https://github.com/vduzh/keycloak-jwt-authorities-converter"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "vduzh"
                        name = "vduzh"
                        url = "https://github.com/vduzh"
                    }
                }

                scm {
                    url = "https://github.com/vduzh/keycloak-jwt-authorities-converter"
                    connection = "scm:git:git://github.com/vduzh/keycloak-jwt-authorities-converter.git"
                    developerConnection = "scm:git:ssh://github.com/vduzh/keycloak-jwt-authorities-converter.git"
                }
            }

            versionMapping {
                usage("java-api") {
                    fromResolutionResult()
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
    repositories {
        maven {
            name = "nexus"
            val path = if (version.toString().endsWith("SNAPSHOT")) "maven-snapshots" else "maven-releases"
            url = uri(System.getenv("NEXUS_URL") ?: "http://localhost:8081/repository/$path")
            isAllowInsecureProtocol = true
            credentials {
                username = System.getenv("NEXUS_USERNAME") ?: "admin"
                password = System.getenv("NEXUS_PASSWORD") ?: "admin123"
            }
        }
        maven {
            name = "central"
            url = uri("https://central.sonatype.com/repository/maven-releases/")
            credentials {
                username = System.getenv("MAVEN_CENTRAL_USERNAME") ?: ""
                password = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: ""
            }
        }
    }
}

signing {
    isRequired = false
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        isRequired = gradle.taskGraph.hasTask(":publishMavenJavaPublicationToCentralRepository")
    }
    sign(publishing.publications["mavenJava"])
}
