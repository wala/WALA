package com.ibm.wala.gradle

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Property

/** A Gradle extension providing per-project configuration for publishing. */
abstract class WalaPublishingExtension @Inject constructor(private val project: Project) {

  /** The value of the `name` element of the generated POM. */
  abstract val pomName: Property<String>

  init {
    // Every published project has to state its name. A Maven POM tolerates an absent `name`, so an
    // unset `pomName` would otherwise yield a nameless POM without complaint.
    pomName.convention(
        project.provider { error("walaPublishing.pomName is unset for project '${project.path}'") }
    )
  }
}
