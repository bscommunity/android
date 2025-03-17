plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.kotlin.parcelize)

	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
	alias(libs.plugins.kotlin.compose)
}

android {
	namespace = "com.meninocoiso.beatstarcommunity"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.meninocoiso.bscm"
		// Was previously 24, but was needed to be changed to 26
		// to use the new Date API, since desugar is not working
		minSdk = 26
		targetSdk = 35
		versionCode = 1
		versionName = "0.0.1"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}
	signingConfigs {
		create("release") {
			storeFile = file("keystore.jks")
			storePassword = System.getenv("SIGNING_STORE_PASSWORD")
			keyAlias = System.getenv("SIGNING_KEY_ALIAS")
			keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
		}
	}
	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11

		// Enable core library desugaring
		isCoreLibraryDesugaringEnabled  = false
	}
	kotlinOptions {
		jvmTarget = "11"
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}
	composeOptions {
		kotlinCompilerExtensionVersion = "1.5.1"
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
	buildToolsVersion = "35.0.0"
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)
	implementation(libs.landscapist.coil)
	implementation(libs.landscapist.placeholder)
	implementation(libs.material)
	implementation(libs.androidx.constraintlayout.compose)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.core.splashscreen)
	implementation(libs.desugar.jdk.libs)
    implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	implementation(libs.androidx.room.compiler)
    testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)
	implementation(libs.ktor.client.android)
	implementation(libs.ktor.client.json)
	implementation(libs.ktor.client.serialization)
	implementation(libs.ktor.client.logging)
	implementation(libs.ktor.client.content.negotiation)
	implementation(libs.ktor.serialization.kotlinx.json)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.navigation.compose)

	// Room (local database)
	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	ksp(libs.androidx.room.compiler)

	// Desugaring (new Date API/Java 8)
	/*coreLibraryDesugaring(libs.desugar.jdk.libs)*/

	// Hilt (dependency injection)
	implementation(libs.androidx.hilt.navigation.compose)
	implementation(libs.hilt.android)
	ksp(libs.hilt.compiler)
}
java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations.implementation {
	exclude(group = "com.intellij", module = "annotations")
}
