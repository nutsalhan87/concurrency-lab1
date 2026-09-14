plugins {
    id("java")
    application
}

group = "org.labs"
version = "1.0"

application {
    mainClass.set("org.labs.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}