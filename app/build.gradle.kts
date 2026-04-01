plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.kotlin.parcelize)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
	alias(libs.plugins.compose.compiler)
}

android {
	namespace = "com.meninocoiso.bscm"
	compileSdk = 36

	androidResources {
		// Enable per-app locale configurations
		generateLocaleConfig = true
	}

	ndkVersion = "28.2.13676358"

	defaultConfig {
		applicationId = "com.meninocoiso.bscm"
		// minSdk was previously 24, but was needed to be changed to 26
		// to use the new Date API, since desugar is not working
		minSdk = 26
		targetSdk = 37
		versionCode = 18
		versionName = "0.3.2-beta"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
			signingConfig = signingConfigs.getByName("release")
			resValue("string", "app_name", "bscm")
		}
		debug {
			// Enable easier debugging
			isDebuggable = true

			applicationIdSuffix = ".debug"
			versionNameSuffix = "-debug"
			resValue("string", "app_name", "bscm debug")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17

		// Enable core library desugaring
		isCoreLibraryDesugaringEnabled = false
	}

	buildFeatures {
		compose = true
		buildConfig = true
		resValues = true
	}

	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.add("-Xlambdas=class")
	}
}

dependencies {
	// Core Android and Kotlin
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.core.splashscreen)
	implementation(libs.androidx.documentfile)
	implementation(libs.androidx.appcompat)

	// Compose UI
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.graphics.shapes)
	implementation(libs.androidx.material3)
	implementation(libs.material.icons)
	implementation(libs.navigation.compose)

	// Image Loading
	implementation(libs.coil.compose)
	implementation(libs.coil.network.okhttp)
	implementation(libs.landscapist.coil)
	implementation(libs.landscapist.placeholder)

	// Dependency Injection
	implementation(libs.androidx.hilt.navigation.compose)
	implementation(libs.hilt.android)
	ksp(libs.hilt.compiler)

	// Network and Serialization
	implementation(libs.ktor.client.android)
	implementation(libs.ktor.client.json)
	implementation(libs.ktor.client.serialization)
	implementation(libs.ktor.client.logging)
	implementation(libs.ktor.client.content.negotiation)
	implementation(libs.ktor.serialization.kotlinx.json)
	implementation(libs.kotlinx.serialization.json)

	// Data Persistence
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	ksp(libs.androidx.room.compiler)

	// Security and Authentication
	implementation(libs.androidx.biometric)
	implementation(libs.tink.android)

	// Utility
	// implementation(libs.desugar.jdk.libs)

	// Testing
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)

	implementation(libs.androidxBrowser)
}