plugins {
    id("lnd-manageJ.java-conventions")
    id("test-report-aggregation")
    id("jacoco-report-aggregation")
}

dependencies {
    runtimeOnly(project(":web"))
    runtimeOnly(project(":ui"))
    runtimeOnly(project(":forwarding-history"))
    runtimeOnly(project(":invoices"))
    runtimeOnly(project(":payments"))
    runtimeOnly(project(":statistics"))
    runtimeOnly(project(":selfpayments"))
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-core")
    runtimeOnly("com.h2database:h2")
    integrationTestImplementation("com.ryantenney.metrics:metrics-spring")
    integrationTestImplementation("io.grpc:grpc-stub")
    integrationTestImplementation(project(":backend"))
    integrationTestImplementation(project(":grpc-adapter"))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rules.forEach {rule ->
            rule.limits.forEach {limit ->
                if (limit.counter == "INSTRUCTION") {
                    limit.minimum = 0.12.toBigDecimal()
                }
                if (limit.counter == "CLASS") {
                    limit.minimum = 0.6.toBigDecimal()
                }
                if (limit.counter == "METHOD") {
                    limit.minimum = 0.11.toBigDecimal()
                }
            }
        }
    }
}

pitest {
    testStrengthThreshold.set(0)
}

tasks.bootJar {
    archiveClassifier.set("boot")
    requiresUnpack("**/ortools-darwin*.jar", "**/ortools-win32*.jar", "**/ortools-linux*.jar")
}

tasks.check {
    dependsOn(tasks.testAggregateTestReport)
    dependsOn(tasks.testCodeCoverageReport)
}
