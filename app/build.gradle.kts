plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.konasl.nagad"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.konasl.nagad"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // 64-bit support
        ndk {
            abiFilters.add("arm64-v8a")
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    
    // RecyclerView for app list
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // VirtualApp core (root-less virtualization)
    implementation("io.va.external:virtual-app:0.0.1")
    
    // OR you can use BlackBox - add JitPack
    // implementation("com.github.android-hacker:BlackBox:1.0.0")
}
