plugins { id("com.android.application") }

android {
 namespace = "com.cosmere.archives"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.cosmere.archives"
  minSdk = 26
  targetSdk = 34
  versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1)
  versionName = "0.11." + versionCode
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
}
dependencies { implementation("androidx.webkit:webkit:1.12.1") }
