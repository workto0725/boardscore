plugins { id("com.android.application") }
android {
 namespace = "io.github.workto0725.boardscoreplus"
 compileSdk = 36
 defaultConfig {
  applicationId = "io.github.workto0725.boardscoreplus"
  minSdk = 26
  targetSdk = 36
  versionCode = 1
  versionName = "1.0.0"
 }
 buildTypes { release { isMinifyEnabled = false } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

// AdMob preview uses Google demo inventory. Production monetization remains disabled.
dependencies { implementation("com.google.android.gms:play-services-ads:25.5.0"); implementation("com.google.android.ump:user-messaging-platform:4.0.0") }
