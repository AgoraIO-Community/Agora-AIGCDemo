import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 导入 local.properties 文件
val localProperties = Properties()
localProperties.load(project.rootProject.file("local.properties").inputStream())

android {
    namespace = project.rootProject.extra["namespace"] as String
    compileSdk = project.rootProject.extra["compile_sdk_version"] as Int

    defaultConfig {
        applicationId = project.rootProject.extra["application_id"] as String
        minSdk = project.rootProject.extra["min_sdk_version"] as Int
        targetSdk = project.rootProject.extra["target_skd_version"] as Int
        versionCode = project.rootProject.extra["version_code"] as Int
        versionName = project.rootProject.extra["version_name"] as String

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "APP_ID", "\"${localProperties.getProperty("APP_ID", "")}\"")
        buildConfigField(
            "String",
            "APP_CERTIFICATE",
            "\"${localProperties.getProperty("APP_CERTIFICATE", "")}\""
        )

        buildConfigField(
            "String",
            "KEY",
            "\"${localProperties.getProperty("KEY", "")}\""
        )

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            // 设置签名配置的属性
            keyAlias = "key0"
            keyPassword = "123456"
            storeFile = file("./keystore/testkey.jks")
            storePassword = "123456"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    applicationVariants.all {
        outputs.all {
            val now = Date()
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
            val formatTime = sdf.format(now)
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "${project.rootProject.name}-${defaultConfig.versionName}-${buildType.name}-$formatTime.apk"
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    viewBinding {
        enable = true
    }

    packagingOptions {
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/FastDoubleParser-LICENSE")
        exclude("META-INF/FastDoubleParser-NOTICE")
        exclude("META-INF/bigint-LICENS")
        exclude("META-INF/io.netty.versions.properties")
        exclude("org/apache/commons/codec/language/bm/*")
        exclude("org/apache/commons/codec/language/*")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("com.github.li-xiaojun:XPopup:2.7.5")
    implementation("io.agora:authentication:2.0.0")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("com.alibaba:fastjson:1.2.83")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.fragment:fragment-ktx:1.5.6")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("pub.devrel:easypermissions:3.0.0")

    implementation("io.agora.rtc:agora-special-voice:4.2.6.8")

    implementation("io.github.winskyan:Agora-AIGCService:1.2.0-alpha-24") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
        exclude(group = "io.github.winskyan", module = "Agora-LoggingService")
    }
    implementation("io.github.winskyan:Agora-LoggingService:1.0.7")

    implementation("com.fasterxml.jackson.core:jackson-core:2.11.3")
}
