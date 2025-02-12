plugins {
    id("java")
    id("lnd-manageJ.cpd")
    id("lnd-manageJ.errorprone")
    id("lnd-manageJ.checkstyle")
    id("lnd-manageJ.tests")
    id("lnd-manageJ.mutationtests")
    id("lnd-manageJ.integration-tests")
    id("lnd-manageJ.pmd")
    id("lnd-manageJ.jacoco")
    id("org.springframework.boot")
    id("java-test-fixtures")
    id("lnd-manageJ.spotbugs")
    id("lnd-manageJ.versions")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    consistentResolution {
        useCompileClasspathVersions()
    }
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Werror")
}

dependencies {
    implementation(platform("de.cotto.lndmanagej:platform"))
    testFixturesImplementation(platform("de.cotto.lndmanagej:platform"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.commons:commons-lang3")
    implementation("com.google.code.findbugs:jsr305")
    implementation("com.ryantenney.metrics:metrics-spring")
    implementation("com.google.guava:guava")
}
