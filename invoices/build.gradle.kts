plugins {
    id("lnd-manageJ.java-library-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation(project(":model"))
    implementation(project(":caching"))
    implementation(project(":grpc-adapter"))
    implementation(project(":payments"))
    testFixturesApi(testFixtures(project(":model")))
    integrationTestRuntimeOnly("com.h2database:h2")
    integrationTestImplementation(project(":payments"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    integrationTestImplementation(testFixtures(project(":model")))
}

pitest {
    testStrengthThreshold.set(95)
}
