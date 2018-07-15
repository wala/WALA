This document describes some WALA-specific aspects of our new Gradle
build system, plus a few general Gradle features that may be of
particular interest to WALA developers.  However, it is not a Gradle
tutorial.

# Pros and Cons of Switching to Gradle

## Selected Gradle Advantages in Brief

- [more comprehensive management of external
  dependencies](#comprehensive-external-dependencies)
- faster builds using [parallel task
  execution](https://docs.gradle.org/current/userguide/multi_project_builds.html#sec:parallel_execution)
  and [user-scoped
  caching](https://docs.gradle.org/current/userguide/build_cache.html)
  (both enabled by default)
- [trustworthy dependencies for incremental
  builds](#trustworthy-dependencies-for-incremental-builds)
- [composite builds](#composite-builds) for easier integration of WALA
  into larger projects
- [automated Travis CI testing on macOS](#travis-ci-macos)

## Known Shortcomings

The Gradle build system is not yet ready to replace Maven, due to a
few [known shortcomings](https://github.com/liblit/WALA/milestone/1).
Paramount among these is that [Gradle WALA builds do not yet package
up Eclipse plug-ins / features in the proper
manner](https://github.com/liblit/WALA/issues/6).  I have [poked at
this a
bit](https://github.com/liblit/WALA/tree/gradle-artifact-publishing),
but I simply do not understand Eclipse and/or OSGi well enough to get
the job done.  I welcome help from anyone with the right knowledge!

Either Gradle or Maven can be used to build WALA from the command
line.  However, it was not possible to extend this dual-build-system
support to Eclipse.  Working with WALA in Eclipse *requires* [doing
things the Gradle way](#eclipse).  Fortunately, this is rather
seamless; I see no reason why an Eclipse-using WALA developer should
need to avoid this switch.

As noted [below](#classpath-and-project-as-generated-files), Eclipse
`.classpath` and `.project` files are now generated rather than being
repository-tracked source files.  However, a few Maven-run tests
depend on having certain of these files present.  One way to create
them is to [import WALA into
Eclipse](#importing-wala-projects-into-eclipse) before starting your
Maven tests.  If you prefer a non-interactive approach, you can
instead run `./gradlew prepareMavenBuild` before starting your Maven
tests.

# Getting Started

## External Dependencies: Patience is a Virtue

Gradle downloads many packages and supporting Java libraries as
needed.  Your first Gradle build may take a long time.  On a fast
workstation with a University-grade network and no local caches, my
initial run of `./gradlew assemble processTestResources` took five
minutes.  On a decent laptop with residential DSL and no local caches,
the same initial build took twenty minutes.  Fortunately, user- and
project-level Gradle caches will make incremental rebuilds much
faster.  Rerunning `./gradlew assemble processTestResources` with a
warm cache in an already-built tree takes under three seconds.

Maven is the same, really.  You may already have most of what Maven
needs downloaded and cached locally, but your first Maven WALA build
was probably slow as well.  Recent Travis CI runs have showed Gradle
and Maven builds completing in fifteen to twenty minutes, without
significant variation between the two build systems.

<a id="comprehensive-external-dependencies"/>The good news is that the
Gradle build knows about all of its external dependencies and will
download them as needed.  This even includes some complex dependencies
that the Maven build does not automate.  For example, the Gradle build
will automatically gather required Android SDK components:  setting
`$ANDROID_HOME` is not needed.  Gradle builds will also download
`/tmp/DroidBench` when needed to run tests; the Maven build system
required that each developer do this by hand.

## Eclipse

### One-Time Eclipse Configuration

To work with WALA inside Eclipse, first **install Eclipse Buildship**
using either [the Eclipse
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
distribution” choice should be set to “Gradle wrapper”.  You do not
need to select each of WALA’s sub-projects; import only the top-level
WALA source tree, and the rest will follow.

After the lengthy import process completes, **use “Run → After
Importing WALA Into Eclipse” to perform some post-import cleanup and
configuration.** Immediately after importing, you may see some errors
in the Eclipse “Problems” view.  These should all go away after
running the “After Importing WALA Into Eclipse” step.

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

WALA comes preconfigured as an openable IntelliJ IDEA project.  Open
the top-level WALA directory as a project; it should have a
distinctive badge on its folder icon marking it as a directory
containing a recognized IntelliJ IDEA project.  Do *not* open the
top-level WALA `build.gradle` in that directory as a project:  this
will begin an “Import Project from Gradle” process that is *not* the
recommended way to bring WALA up in IntelliJ IDEA.

The first time you open the WALA project, IntelliJ IDEA will
synchronize its project model with the Gradle build configuration,
including downloading some large supporting libraries.  After the
lengthy import process completes, **use “Run → Run… → After Opening
Pristine Project” to perform some post-import configuration.** This
also can take tens of minutes, but is only necessary in a clean,
never-previously-built tree.  Immediately after importing, you may see
some Java compilation errors in the IntelliJ IDEA “Messages” view.
These should all go away after running the “After Opening Pristine
Project” step.  [Be
patient](#external-dependencies-patience-is-a-virtue) during the
initial project open and also during the “After Opening Pristine
Project” step, especially if you have a slow network connection.

If you prefer a non-interactive approach, instead of using “After
Opening Pristine Project” you can run `./gradlew prepareIntelliJIDEA`
either before or after opening WALA in IntelliJ IDEA.

### Benign Warning About Non-Managed Maven Project

Each time you open the WALA project, IntelliJ IDEA may report
“Non-managed pom.xml file found” in its event log.  This arises
because WALA supports both Gradle and Maven, but WALA in IntelliJ IDEA
needs only the Gradle configuration.  You can safely ignore this
notification, permanently disable it using the offered “Disable
notification” link, or even disable the IntelliJ IDEA Maven plugin
entirely if you have no other need for it.

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
that Eclipse-generated artifacts will still appear in the same places
as before, such as `*/bin` and `*/target`.

### Trustworthy Dependencies For Incremental Builds

Gradle has excellent understanding of task and file dependencies.  You
can trust it to perform incremental rebuilds rather than always
rebuilding from scratch.  If you are used to always running `mvn clean
compile` instead of `mvn compile`, or `mvn clean install` instead of
`mvn install`, I recommend that you drop `clean` as a reflexive extra
step and **trust Gradle to do incremental builds correctly.**

### Favorite Build Tasks

Some useful Gradle tasks include:

- `assemble`: build WALA’s main (non-test) code

- `build`: build all WALA code and run all automated tests

- `javadoc`: build all Javadoc documentation

- `publishToMavenLocal`: install WALA’s jar files under `~/.m2`

- `clean`: remove all Gradle-generated artifacts

### Tasks in Specific Sub-Projects

When you run `./gradlew` in the top-level WALA directory, any tasks
you list will be built in all sub-projects.  For example, `./gradlew
assemble` builds all non-test WALA jars in all sub-projects.  If you
want to build tasks only in specific sub-projects, you have two options:

1. Give the fully-qualified name of the sub-project task.  For
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

- [`--tests=...`](https://docs.gradle.org/current/userguide/java_plugin.html#test_filtering):
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

# Travis CI

I use a [Travis CI build
matrix](https://docs.travis-ci.com/user/customizing-the-build/#Build-Matrix)
to perform automated testing in three configurations:

1. Gradle build on Ubuntu 14 (Trusty Tahr)
1. Maven build on Ubuntu 14 (Trusty Tahr)
1. Gradle build on macOS 10.12 (Sierra)

Until we are ready to completely replace Maven with Gradle, it is
important that both keep working.  Therefore, I use Travis CI to build
and test WALA on Ubuntu using both Gradle and Maven.  Every new pull
request must be validated in both of these configurations before I
will accept it onto the `gradle-and-buildship` branch.

<a id="travis-ci-macos"/>The official WALA repository has no macOS CI
testing.  However, [macOS is the main development platform for at
least one WALA
maintainer](https://github.com/liblit/WALA/issues/3#issuecomment-356823287),
so it is great to have Travis CI helping us keep that platform
working.  I will not accept pull requests that introduce regressions
into Gradle macOS builds.  However, I am not using Travis CI to test
Maven macOS builds.  Initial attempts using [the official WALA master
sources](https://github.com/wala/WALA) failed.  As it is my goal to
replace Maven entirely, investigating Maven+macOS failures further is
not a priority.

<!--
LocalWords:  processTestResources pre classpath gradlew mvn
LocalWords:  javadoc buildship issuecomment
-->
