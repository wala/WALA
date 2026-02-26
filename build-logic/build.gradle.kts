plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
  alias(libs.plugins.spotless)
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

private fun Provider<PluginDependency>.asDependency() = map {
  it.run { "$pluginId:$pluginId.gradle.plugin:${version.requiredVersion}" }
}

dependencies {
  implementation(libs.jgit)
  implementation(libs.plugins.eclipse.mavencentral.asDependency())
  implementation(libs.plugins.errorprone.asDependency())
  implementation(libs.plugins.maven.publish.asDependency())
  implementation(libs.plugins.spotless.asDependency())
}

kotlin.jvmToolchain(17)

spotless {
  val ktfmtVersion = libs.versions.ktfmt.get()

  kotlin {
    ktfmt(ktfmtVersion)
    targetExclude("build/")
  }

  kotlinGradle { ktfmt(ktfmtVersion) }

  providers.gradleProperty("spotless.ratchet.from").orNull?.let(::ratchetFrom)
}
