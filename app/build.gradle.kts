import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.app_aeroclima"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.app_aeroclima"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val checkWxKey = localProperties.getProperty("CHECKWX_API_KEY") ?: ""
        buildConfigField("String", "CHECKWX_API_KEY", "\"$checkWxKey\"")

        val geminiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    
    // Sirve para usar "by viewModels()" en el MainActivity
    implementation("androidx.activity:activity-ktx:1.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // bcrypt para la encriptacion de la constrasena
    implementation("org.mindrot:jbcrypt:0.4")

    // Retrofit para las llamadas a la API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Moshi para convertir JSON a objetos Kotlin
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")

    // implementaciones de google
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // dependencias para gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}