import groovy.json.JsonSlurper

plugins {
    java
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    val json = JsonSlurper().parseText(file("src/main/resources/plugin.json").readText()) as Map<*, *>
    project.version = json["version"]!!

    val mindustryVersion = json["minGameVersion"]
    val usefulHash = "a3a201c3b6"

    compileOnly("com.github.anuken.arc:arc-core:v$mindustryVersion")
    compileOnly("com.github.anuken.mindustry:core:v$mindustryVersion")

    implementation("com.github.xzxadixzx.useful-stuffs:bundle:$usefulHash")
    implementation("com.github.xzxadixzx.useful-stuffs:collections:$usefulHash")
    implementation("com.github.xzxadixzx.useful-stuffs:menu:$usefulHash")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
