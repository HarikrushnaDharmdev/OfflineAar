plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.google.dagger.hilt.android")
    id("com.google.protobuf")
    kotlin("kapt")
}

android {
    namespace = "com.hiren.grpcsync"

    compileSdk {
        version = release(36)
    }

    defaultConfig {
        version = "1.0.0"
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.material3)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Architecture component
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    //Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.databinding.runtime)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    kapt(libs.room.compiler)
    annotationProcessor(libs.room.compiler)

    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // GRPC
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.protobuf.kotlin)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.18.1"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }

    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
                create("java")
            }
        }
    }
}