# AGENTS.md

This file provides guidance to coding agents working with code in this
repository.

## Build and Test

Always use the bundled wrapper, `./gradlew`. Gradle itself needs a Java 21 or
newer JVM. Compilation and tests use the toolchain named by the
`com.ibm.wala.jdk-version` property (17 by default in `gradle.properties`), and
Gradle downloads that JDK if needed. CI runs the full build on JDKs 17, 21, 25
and 27, so pass `-Pcom.ibm.wala.jdk-version=<N>` to reproduce a JDK-specific
failure.

- `./gradlew assemble`: compile all main code.
- `./gradlew build`: compile everything, run all tests and all checks. This is
  what CI runs, along with `javadoc`, `aggregatedJavadocs` and
  `checkGitCleanliness`.
- `./gradlew :core:test --tests 'com.ibm.wala.core.tests.callGraph.CPATest'`:
  run one test class. Append `.methodName` to run a single test.
- `./gradlew spotlessApply`: format Java (Google Java Format) and Kotlin
  (ktfmt). `spotlessCheck` runs as part of `check`.
- `./gradlew :core:jmh`: run the JMH benchmarks in `core/src/jmh`.

Subproject paths follow the directory tree (`:util`, `:shrike`, `:core`,
`:cast`, `:cast:java:ecj`, `:cast:js:rhino`, `:dalvik`, `:scandroid`, `:ide:jdt`
and so on), as listed in `settings.gradle.kts`.

The full build is slow: the JavaScript call-graph tests in `:cast:js:rhino` and
the core call-graph tests take many minutes. Prefer running the affected
subproject's tests while iterating. `:core` tests run with the
`com.ibm.wala.junit.profile=short` system property; tests check it with
`WalaTestCase.useShortProfile()` to skip or shrink long-running cases. On Linux,
CI runs Gradle under `xvfb-run`, so tests that need a display may fail in a
headless shell without it (see `xvfb-gradle.sh`).

## Checks Beyond Tests

`check` (and so `build`) fails on more than test failures:

- `javac` runs with `-Werror` and Error Prone. Many Error Prone checks are
  promoted to errors; the list and the deliberately disabled checks (for example
  `MissingSummary` and `ReferenceEquality`) are in
  `build-logic/src/main/kotlin/com/ibm/wala/gradle/java.gradle.kts`.
- NullAway runs in JSpecify mode on `@NullMarked` code in subprojects that apply
  the `com.ibm.wala.gradle.NullAway` plugin (currently `util`). Use
  `NullabilityUtil.castToNonNull` for casts NullAway can't verify.
- Every source set is also compiled with the Eclipse compiler (ECJ), which adds
  its own lint warnings as errors.
- `lintMarkdown` lints Markdown files.
- `checkGitCleanliness` fails if the build leaves tracked files modified or
  untracked files behind.

Build configuration shared by all subprojects lives in the convention plugins
under `build-logic/src/main/kotlin/com/ibm/wala/gradle/`. Dependency versions
are in the version catalog, `gradle/libs.versions.toml`.

## Architecture

WALA is layered, and each layer depends only on those below it.

- `util`: general data structures (`collections`, `intset`), graphs and graph
  algorithms (`graph`), the iterative fixed-point solver framework
  (`com.ibm.wala.fixpoint`, `com.ibm.wala.fixedpoint`), graph-based dataflow
  (`com.ibm.wala.dataflow.graph`), and progress monitoring and cancellation
  (`MonitorUtil`).
- `shrike`: reading, writing and instrumenting JVM bytecode.
- `core`: analysis of JVM bytecode.
  - `classLoader` and `ipa.cha` load classes into an `AnalysisScope` and build
    the `ClassHierarchy`.
  - `ssa` holds the SSA register-transfer IR. `IR`s are built per method and
    `Context` by an `IRFactory` and cached by `AnalysisCache`.
  - `ipa.callgraph` and `ipa.callgraph.propagation` build call graphs and
    points-to analysis together. A `PropagationCallGraphBuilder` turns each
    reachable node's IR into constraints over `PointerKey`s and `InstanceKey`s
    and solves them with the `util` fixed-point solver, adding new call-graph
    nodes as targets are discovered. `ContextSelector`, `InstanceKeyFactory` and
    `SSAContextInterpreter` are the main policy hooks. `ipa.callgraph.impl.Util`
    has factory methods for the standard builders (`makeZeroCFABuilder`,
    `makeZeroOneCFABuilder` and so on).
  - `dataflow.IFDS` (the IFDS/IDE tabulation solvers), `ipa.slicer`,
    `ipa.modref`, `demandpa` and `escape` build on the call graph and pointer
    analysis.
- `cast`: the Common AST framework for source-language front ends. A front end
  parses source into `CAstEntity` trees (`cast.tree`), optionally rewrites them
  (`cast.tree.rewrite`, for example constant folding), and `AstTranslator`
  (`cast.ir.translator`) translates them into the same SSA IR that `core` uses,
  with extra instructions for dynamic languages. `cast.ipa.callgraph` extends
  the propagation builder for those instructions, for example lexical scoping.
  - `cast:java` (with `cast:java:ecj`) is the Java source front end.
  - `cast:js` is the JavaScript front end, with the Rhino parser in
    `cast:js:rhino`, Node.js support in `cast:js:nodejs` and HTML parsing in
    `cast:js:nu_validator`.
- `dalvik` analyzes Android Dalvik bytecode, and `scandroid` builds Android
  security analyses on it.
- `ide` holds the Eclipse plug-ins.

The Python front end is not in this repository. It lives in
<https://github.com/wala/ML>.

## Tests

Many test suites are written once against an abstract front end and run for each
concrete one. The abstract classes live in a subproject's `src/testFixtures`
(for example `cast/js/src/testFixtures/.../TestSimpleCallGraphShape.java`), and
the concrete subclasses live in the front-end subproject (for example
`cast/js/rhino/src/test/.../TestSimpleCallGraphShapeRhino.java`). To run such a
test, target the concrete class in its subproject. `WalaTestCase` is a shared
test fixture in `core/src/testFixtures`; `CallGraphTestUtil` and `TestConstants`
are in `core/src/main`, under `com.ibm.wala.core.tests`.

Java test programs that `core` tests analyze are in `core/src/testSubjects`,
compiled into the test data that `TestConstants.WALA_TESTDATA` names. JavaScript
test inputs are resources under `cast/js/src/test/resources` (and
`cast/js/nodejs/src/test/resources` for Node.js).

## Conventions

- Construct hash maps and sets with `HashMapFactory.make()` and
  `HashSetFactory.make()` from `com.ibm.wala.util.collections`, so analysis
  results are deterministic. Avoid `System.identityHashCode()` and finalizers.
- Use `com.ibm.wala.util.debug.Assertions` for analysis invariants, for example
  `Assertions.UNREACHABLE()` and `Assertions.productionAssertion(...)`.
- Don't change license files or license headers.
- Agent skills for Gradle are managed with APM (`apm.yml`, `apm.lock.yaml`). Run
  `./refresh-agent-skills.sh` to install them for your agent. See
  `CONTRIBUTING.md`.
