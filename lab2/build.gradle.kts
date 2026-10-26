plugins {
    java
    application
}

sourceSets {
    main {
        java {
            srcDir("student-java/src")
        }
    }
}

application {
    mainClass = "lab2.Main"
}

