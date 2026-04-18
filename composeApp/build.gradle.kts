plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
    
    /*
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    */
    
    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.activity:activity-compose:1.8.2")

                // Wear OS specific dependencies
                implementation("androidx.wear.compose:compose-material:1.3.1")
                implementation("androidx.wear.compose:compose-foundation:1.3.1")
                implementation("androidx.wear.compose:compose-navigation:1.3.1")
                implementation("com.google.android.gms:play-services-wearable:18.1.0")
                
                // New dependencies for background and carousel support
                implementation("androidx.wear:wear-ongoing:1.0.0")
                implementation("androidx.wear.tiles:tiles:1.2.0")
                implementation("androidx.wear.tiles:tiles-material:1.2.0")
                implementation("androidx.wear:wear:1.3.0")
            }
        }

        /*
        iosMain {
            dependencies {
            }
        }
        */
    }
}

android {
    namespace = "com.example.runapp"
    compileSdk = 34

    flavorDimensions += "version"
    productFlavors {
        create("mobile") {
            dimension = "version"
            applicationIdSuffix = ".mobile"
        }
        create("wear") {
            dimension = "version"
            applicationIdSuffix = ".wear"
        }
    }

    defaultConfig {
        applicationId = "com.example.runapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageVersion = "1.0.0"
        }
    }
}
