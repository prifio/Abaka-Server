plugins {
    id("java")
    kotlin("jvm")
    id("application")
    id("distribution")
}

val ktorVersion = project.property("ktor.version") as String
val logbackVersion = project.property("logback.version") as String

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.xerial:sqlite-jdbc:3.30.1")
    implementation("mysql:mysql-connector-java:8.0.22")
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

tasks.withType<Copy>().named("processResources") {
    from(project(":client").tasks.named("browserDistribution"))
}

/*tasks.register<Copy>("copy_ans") {
    from(layout.projectDirectory.dir("resources/answers.csv"))
    into(layout.buildDirectory.dir("drrr/answers.csv"))
}*/
