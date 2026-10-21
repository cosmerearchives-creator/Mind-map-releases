plugins { id("com.android.application") }

android {
 signingConfigs {
  create("release") {
   storeFile = file(System.getenv("ANDROID_SIGNING_KEYSTORE") ?: "missing-keystore.jks")
   storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
   keyAlias = System.getenv("ANDROID_KEY_ALIAS")
   keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
  }
 }
 namespace = "com.cosmere.archives"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.cosmere.archives"
  minSdk = 26
  targetSdk = 34
  versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1)
  versionName = "0.11." + versionCode
 }
 buildTypes {
  getByName("release") {
   isMinifyEnabled = false
   signingConfig = signingConfigs.getByName("release")
  }
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
}
dependencies { implementation("androidx.webkit:webkit:1.12.1") }
