package com.ibm.wala.gradle.cast

tasks.withType<CppCompile> {
  notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/13485")
}
