import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = plugins.getPlugin(KotlinPluginWrapper::class.java).kotlinPluginVersion

val kotlinCoroutinesVersion = "0.22.2"
val vertxVersion = "3.5.0"

plugins {
    kotlin("jvm").version("1.2.21")
    application
    java
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

application {
    version = "1.0.0"
    applicationName = "vertx_rest"
    mainClassName = "de.swirtz.kotlinvertx.rest.ApplicationKt"
}

dependencies {
    compile(kotlin("stdlib", kotlinVersion))
    compile(kotlin("reflect", kotlinVersion))
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    "io.vertx:vertx".let {
        compile("$it-lang-kotlin:$vertxVersion")
        compile("$it-lang-kotlin-coroutines:$vertxVersion")
        compile("$it-web:$vertxVersion")
        compile("$it-mongo-client:$vertxVersion")
        compile("$it-health-check:$vertxVersion")
        compile("$it-web-templ-thymeleaf:$vertxVersion")
    }

    compile("org.slf4j:slf4j-api:1.7.14")
    compile("ch.qos.logback:logback-classic:1.1.3")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.0.pr3")
    compile("de.bwaldvogel:mongo-java-server-h2-backend:1.7.0")
    runtimeClasspath("org.h2:h2:1.0")

    testCompile(kotlin("test", kotlinVersion))
    testCompile(kotlin("test-junit", kotlinVersion))
    testCompile("io.vertx:vertx-unit:$vertxVersion")
    testCompile("org.mockito:mockito-core:2.6.2")
    testCompile("junit:junit:4.11")
}

repositories {
    mavenCentral()
    jcenter()
    listOf("https://www.seasar.org/maven/maven2/", "https://plugins.gradle.org/m2/").forEach {
        maven { setUrl(it) }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<Test> {
        testLogging.showStandardStreams = true
    }

    withType<Jar> {
        manifest {
            attributes["Main-Class"] = application.mainClassName
        }
        from(configurations.runtime.map { if (it.isDirectory) it else zipTree(it) })
    }

}
