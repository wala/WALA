This document describes some WALA-specific aspects of our Gradle build
system, plus a few general Gradle features that may be of particular
interest to WALA developers.  However, it is not a Gradle tutorial.

# Getting Started

## External Dependencies: Patience is a Virtue

Gradle downloads many packages and supporting Java libraries as
needed.  Your first Gradle build may take a long time.  On a fast
workstation with a university-grade network and no local caches, my
initial run of `./gradlew assemble processTestResources` took five
minutes.  On a decent laptop with residential DSL and no local caches,
the same initial build took twenty minutes.  Fortunately, user- and
project-level Gradle caches will make incremental rebuilds much
faster.  Rerunning `./gradlew assemble processTestResources` with a
warm cache in an already-built tree takes under three seconds.

## Eclipse

### One-Time Eclipse Configuration

To work with WALA inside Eclipse, first **install Eclipse Buildship
3.1 or later** using either [the Eclipse
Marketplace](http://www.vogella.com/tutorials/EclipseGradle/article.html#installation-via-the-marketplace)
or [the Eclipse update
manager](http://www.vogella.com/tutorials/EclipseGradle/article.html#installation-via-the-eclipse-update-manager).
Buildship integrates Eclipse with Gradle, much like how M2Eclipse
integrates Eclipse with Maven.  Restart Eclipse after installing this
feature.

### Importing WALA Projects Into Eclipse

Once you are running a Buildship-enabled Eclipse, **[use the “Existing
Gradle Project” import
wizard](http://www.vogella.com/tutorials/EclipseGradle/article.html#import-an-existing-gradle-project)
to import WALA into Eclipse.** Select and import the topmost level of
your WALA source tree.  On the “Import Options” page of the import
wizard, leave all settings at their defaults: the “Override workspace
settings” option should be off, and the grayed-out “Gradle
distribution” choice should be set to “Gradle wrapper”.  It is also recommended that you clear the "Gradle user home" dialog box in the **Gradle Preferences** (not the one in the import wizard) prior to importing ([one issue](https://github.com/wala/WALA/issues/731#issuecomment-604465043) was resolved this way). You do not
need to select each of WALA’s sub-projects; import only the top-level
WALA source tree, and the rest will follow.

The first time you import the WALA project, Eclipse will synchronize
its project model with the Gradle build configuration, including
downloading some large supporting libraries.  The “Import Gradle
Project” wizard may spend tens of minutes showing “Importing root
project: Configure project :” with no movement of its progress bar.
This is normal.  [Be
patient](#external-dependencies-patience-is-a-virtue) during the
initial project import, especially if you have a slow network
connection.

Note: a pristine WALA source tree is *not* pre-configured as a group
of Eclipse projects.  Using the standard Eclipse “Existing Projects
into Workspace” import wizard will not work correctly.  You must [use
the “Existing Gradle Project” import
wizard](http://www.vogella.com/tutorials/EclipseGradle/article.html#import-an-existing-gradle-project)
instead.

### `.classpath` and `.project` as Generated Files

You will find no `.classpath` or `.project` files anywhere in [the
Gradle fork of WALA’s git repository](https://github.com/liblit/WALA).
Importing using the “Existing Gradle Project” wizard creates these
Eclipse project configuration files automatically based on the
underlying Gradle configuration.

Therefore, when working with Eclipse + Gradle, you should **treat
`.classpath` and `.project` files as *generated* artifacts,** not as
files to edit directly or through the Eclipse project configuration
GUI.  For example, avoid using the Java Build Path settings dialog to
make changes that are stored in a `.classpath` file:  the modified
`.classpath` file is not git-tracked, so your changes will eventually
be lost or overwritten.

The right way to change the contents of any of a `.classpath` or
`.project` file is to change the Gradle configuration such that the
generated `.classpath` and `.project` files will have the desired
contents, likely by using [Gradle’s `eclipse`
plugin](https://docs.gradle.org/current/userguide/eclipse_plugin.html).
A few WALA sub-projects already use this:  look for `eclipse.project`
in `*/build.gradle` for examples.

## IntelliJ IDEA

### Opening WALA in IntelliJ IDEA

Open the top-level WALA directory as a project; it should have a
distinctive badge on its folder icon marking it as a directory
containing a recognized IntelliJ IDEA project.

The first time you open the WALA project, IntelliJ IDEA will notify
you that “IntelliJ IDEA found a Gradle build script”.  Select the
“Import Gradle Project” option offered by this notification.  IntelliJ
IDEA will synchronize its project model with the Gradle build
configuration, including downloading some large supporting libraries.
This can take tens of minutes, but is only necessary in a clean,
never-previously-built tree.  [Be
patient](#external-dependencies-patience-is-a-virtue) during the
initial project open, especially if you have a slow network
connection.

### Benign Warning About Non-Managed Maven Project

Each time you open the WALA project, IntelliJ IDEA may report
“Non-managed pom.xml file found” in its event log.  This arises
because WALA historically has built using both Gradle and Maven, but
WALA in IntelliJ IDEA needs only the Gradle configuration.  You can
safely ignore this notification, permanently disable it using the
offered “Disable notification” link, or even disable the IntelliJ IDEA
Maven plugin entirely if you have no other need for it.

### Project Configuration as Derived Model

IntelliJ IDEA automatically derives its project models from the Gradle
build configuration, including all information about both internal and
external build dependencies.  However, this synchronization only goes
in one direction: from Gradle to IntelliJ IDEA, not from IntelliJ IDEA
back into Gradle.  If you manipulate the project structure using the
IntelliJ IDEA’s user interface, your changes will likely be
overwritten the next time IntelliJ IDEA scans the Gradle build
configuration.

This particularly applies to settings found in the “Modules” and
“Libraries” sections of the “Project Structure” dialog.  The right way
to change module and library settings is to change the Gradle
configuration such that the *derived* IntelliJ IDEA model is what you
want it to be.

## Gradle Command Line

You do not need to install Gradle separately.  **WALA includes its own
copy of Gradle, available as the `gradlew` script in the top-level
WALA directory.** Use this script for all command-line Gradle actions.
For example, to compile all of WALA’s main (non-test) code and gather
it into jar archives, run `./gradlew assemble`.

In general, most Gradle-generated artifacts will appear somewhere
under `*/build`.  For example the jar archives created by the
`assemble` task can be found as `*/build/libs/*.jar`.  Note, however,
that Eclipse-generated artifacts will appear in Eclipse-specific
places, such as `*/bin` and `*/target`.

### Trustworthy Dependencies For Incremental Builds

Gradle has excellent understanding of task and file dependencies.  You
can trust it to perform incremental rebuilds rather than always
rebuilding from scratch.  If you are used to cleaning your build tree
and rebuilding from scratch after every change, I recommend that you
drop `clean` as a reflexive extra step and **trust Gradle to do
incremental builds correctly.**

### Favorite Build Tasks

Some useful Gradle tasks include:

- `assemble`: build WALA’s main (non-test) code

- `build`: build all WALA code and run all automated tests

- `javadoc`: build all Javadoc documentation

- `publishToMavenLocal`: install WALA’s jar files under `~/.m2`

- `googleJavaFormat`: reformat all Java code to match WALA project
  standards

- `clean`: remove all Gradle-generated artifacts

### Tasks in Specific Sub-Projects

When you run `./gradlew` in the top-level WALA directory, any tasks
you list will be built in all sub-projects.  For example, `./gradlew
assemble` builds all non-test WALA jars in all sub-projects.  If you
want to build tasks only in specific sub-projects, you have two options:

1. Give the fully qualified name of the sub-project task.  For
   example, to assemble only the Dalvik jar, you could run `./gradlew
   :com.ibm.wala.dalvik:assemble`.

1. Run Gradle from within some sub-project directory.  For example, to
   assemble only the Dalvik jar, you could `cd com.ibm.wala.dalvik`
   and then run `../gradlew assemble`.  Note the proper relative path
   to the top-level Gradle script: `../gradle` instead of `./gradlew`.

### Task Name Abbreviation

[Any build task can be
abbreviated](https://docs.gradle.org/current/userguide/command_line_interface.html#_task_name_abbreviation)
by shortening each camel-case-delimited word in its name.  For
example, the `processTestResources` task can probably be abbreviated
as `procTeRes` or even `pTR`.

### Useful Command-Line Flags

Among Gradle’s command-line flags, I have found the following
particularly useful:

- [`--continue`](https://docs.gradle.org/current/userguide/command_line_interface.html#sec:continue_build_on_failure):
  keep building non-dependent sub-tasks even after an initial failure.
  Especially useful in conjunction with the `build` or `test` tasks to
  see multiple test failures rather than giving up after the first
  failure.

- [`-t`,
  `--continuous`](https://docs.gradle.org/current/userguide/command_line_interface.html#_continuous_build):
  keep Gradle process running and re-execute the given tasks whenever
  input files change.  Similar to Eclipse’s behavior of updating the
  build whenever you change and save a file.

- [`--tests=…`](https://docs.gradle.org/current/userguide/java_plugin.html#test_filtering):
  run only the selected tests.  Use in conjunction with the `build` or
  `test` tasks for faster turnaround if you are focusing on getting
  just one or a few failing tests to pass.

- [`--scan`](https://scans.gradle.com/): upload a detailed report of
  the build process to a Gradle-hosted server for further exploration
  and analysis.  The only security here is the obscurity of the
  generated URL for the build report.  If you are not concerned about
  potentially making your build details public, then `--scan` is a
  good way to gain insights into why Gradle did what it did, and how
  long each piece took.

### Composite Builds

Gradle’s [composite
builds](https://docs.gradle.org/current/userguide/composite_builds.html)
allow a Gradle-managed project to recursively include other
Gradle-managed projects, with Gradle managing the entire build process
in a coherent, integrated manner.  Thus, if you use Gradle to build
your WALA-based project, you can easily have it use WALA from your
own, private WALA tree instead of from `~/.m2` or the public Maven
repository.

This is especially useful if you frequently find yourself switching
between multiple different personal or experimental WALA builds.  By
avoiding `~/.m2`, each WALA-based project can be its own composite
build, with its own WALA subtree, and no project interferes with any
other.

<!--
LocalWords:  processTestResources pre classpath gradlew mvn
LocalWords:  javadoc buildship issuecomment
-->
