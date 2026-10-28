plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "com.anishop.aninote"
 compileSdk = 35
 val buildCode = providers.environmentVariable("ANI_VERSION_CODE").orElse("2").get().toInt()
 require(buildCode in 2..2100000000)
 defaultConfig {
  applicationId = "com.anishop.aninote.preview"; minSdk = 26; targetSdk = 35
  versionCode = buildCode; versionName = "1.2-preview.$buildCode"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 signingConfigs.getByName("debug") {
  providers.environmentVariable("ANI_KEYSTORE").orNull?.let { storeFile = file(it) }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.9.3")
 implementation("androidx.fragment:fragment-ktx:1.8.5")
 implementation("androidx.biometric:biometric:1.1.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.core:core-ktx:1.15.0")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
