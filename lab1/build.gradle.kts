plugins {
    java
    application
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "solution.Main"
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
}

tasks.test {
    useJUnitPlatform()
}

fun registerRun(taskName: String, className: String, taskGroup: String, taskDescription: String) =
    tasks.register<JavaExec>(taskName) {
        group = taskGroup
        description = taskDescription
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set(className)
    }

// Этап 0
registerRun("stage0Bench", "solution.stage0.Stage0Main", "lab1 stages", "Этап 0: однопоточный baseline")

// Этап 1
registerRun("stage1EmptyLock", "solution.stage1.Stage1EmptyMain", "lab1 stages", "Этап 1: пустой лок")
registerRun("stage1SyncLock", "solution.stage1.Stage1SyncMain", "lab1 stages", "Этап 1: единый общий synchronized-лок")

// Этап 2
registerRun("stage2Bench", "solution.stage2.Stage2Main", "lab1 stages", "Этап 2: шардированный лок — замер пропускной способности")
registerRun("stage2ConsistencyTest", "solution.stage2.InconsistencyStage2Test", "lab1 tests", "Этап 2: стресс-тест согласованности снимков")

// Этап 3
registerRun("stage3Bench", "solution.stage3.Stage3Main", "lab1 stages", "Этап 3: thread-local состояние — замер пропускной способности")
registerRun("stage3ConsistencyTest", "solution.stage3.InconsistencyStage3Test", "lab1 tests", "Этап 3: стресс-тест согласованности снимков")

// Этап 4
registerRun("stage4Bench", "solution.stage4.Stage4Main", "lab1 stages", "Этап 4: двойная буферизация — замер пропускной способности")
registerRun("stage4ConsistencyTest", "solution.stage4.Stage4InconsistencyTest", "lab1 tests", "Этап 4: стресс-тест согласованности (с шагом 3)")
registerRun("stage4ConsistencyTestNoStep3", "solution.stage4.Stage4InconsistencyNoStep3Test", "lab1 tests", "Этап 4: стресс-тест согласованности (без шага 3, ожидается поломка)")

val benchOrder = listOf("stage0Bench", "stage1EmptyLock", "stage1SyncLock", "stage2Bench", "stage3Bench", "stage4Bench")
for (i in 1 until benchOrder.size) {
    tasks.named(benchOrder[i]) { mustRunAfter(benchOrder[i - 1]) }
}

tasks.register("runAllStages") {
    group = "lab1 stages"
    description = "Запускает замеры пропускной способности всех этапов (каждый — в своём JVM-процессе, строго по очереди)"
    dependsOn(benchOrder)
}

val consistencyOrder = listOf("stage2ConsistencyTest", "stage3ConsistencyTest", "stage4ConsistencyTest", "stage4ConsistencyTestNoStep3")
for (i in 1 until consistencyOrder.size) {
    tasks.named(consistencyOrder[i]) { mustRunAfter(consistencyOrder[i - 1]) }
}

tasks.register("runAllConsistencyTests") {
    group = "lab1 tests"
    description = "Запускает все стресс-тесты согласованности снимков (строго по очереди)"
    dependsOn(consistencyOrder)
}