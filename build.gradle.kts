plugins {
    java
    application
}

group = "org.labs"
version = "1.0-SNAPSHOT"


subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.13.4")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
