plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.eeum"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.eeum"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.appcompat:appcompat:1.6.1") // 의존성 추가
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ... (기존 코드들 건드리지 마시고 맨 아래에 추가하세요)

// 1. 프론트엔드 경로 설정
val frontendDir = file("${project.projectDir}/../../frontend")

// 2. npm run build 실행 작업
tasks.register<Exec>("buildVueApp") {
    workingDir = frontendDir

    // 수정된 부분: toLowerCase() 대신 ignoreCase = true 옵션 사용
    if (System.getProperty("os.name").contains("windows", ignoreCase = true)) {
        commandLine("npm.cmd", "run", "build")
    } else {
        commandLine("npm", "run", "build")
    }
}

// 3. 빌드 결과물(dist)을 안드로이드 assets로 복사
tasks.register<Copy>("copyVueAssets") {
    dependsOn("buildVueApp")
    from("$frontendDir/dist")
    into("${project.projectDir}/src/main/assets")
}

// 4. 빌드 전 실행 연결
tasks.named("preBuild") {
    dependsOn("copyVueAssets")
}