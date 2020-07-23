plugins {
    kotlin("jvm") version "1.4-M3"
}

group = "com.corundumstudio.socketio"
version = "1.7.19-SNAPSHOT"

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    val nettyVersion = "4.1.50.Final"
    implementation("io.netty:netty-buffer:$nettyVersion")
    implementation("io.netty:netty-common:$nettyVersion")
    implementation("io.netty:netty-transport:$nettyVersion")
    implementation("io.netty:netty-handler:$nettyVersion")
    implementation("io.netty:netty-codec-http:$nettyVersion")
    implementation("io.netty:netty-codec:$nettyVersion")
    implementation("io.netty:netty-transport-native-epoll:$nettyVersion")

    implementation("org.slf4j:slf4j-api:1.7.21")

    val jacksonVersion = "2.10.4"
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    val springVersion = "3.1.4.RELEASE"
    compileOnly("org.springframework:spring-beans:$springVersion")
    compileOnly("org.springframework:spring-core:$springVersion") {
        exclude(group = "commons-logging", module = "commons-logging")
    }

    compileOnly("org.redisson:redisson:3.13.1")
    compileOnly("com.hazelcast:hazelcast-client:3.12.7")

    testImplementation("junit:junit:4.11")
    testImplementation("org.jmockit:jmockit:1.39")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}