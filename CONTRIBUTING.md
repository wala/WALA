Contributing to WALA
====================

WALA welcomes contributions of all kinds and sizes. This includes everything from from simple bug reports to large features.

Workflow
--------

We love GitHub issues!

For small feature requests, an issue first proposing it for discussion or demo implementation in a PR suffice.

For big features, please open an issue so that we can agree on the direction, and hopefully avoid investing a lot of time on a feature that might need reworking.

Small pull requests for things like typos, bug fixes, etc are always welcome.

Additional Checks
-----------------

Beyond tests, other checks run as part of `./gradlew check` and `./gradlew build`, including:

1. Compilation with the Java compiler from [Eclipse JDT Core](https://www.eclipse.org/jdt/core/),
which runs additional lint checks
2. Checking that all code is formatted according to [Google Java
  Format](https://github.com/google/google-java-format) standards

If your code fails check 2, you can run `./gradlew googleJavaFormat` to automatically format
it.  The CI job runs `./gradlew build` and will fail if any of these additional
checks fail.


DOs and DON'Ts
--------------

* DO format your code using [Google Java Format](https://github.com/google/google-java-format).  You can do so by running `./gradlew googleJavaFormat`.  A CI job will fail if your code is not formatted in this way.
* DO include tests when adding new features. When fixing bugs, start with adding a test that highlights how the current behavior is broken.
* DO keep the discussions focused. When a new or related topic comes up it's often better to create new issue than to side track the discussion.
* DO make liberal use of Javadoc and comments to document code.
* DO use the `com.ibm.wala.util.debug.Assertions` class liberally. All calls to `assert()` must be guarded by `Assertions.verifyAssertions`. Use the `productionAssert` entrypoints for assertions that should be enabled in production, and thus not guarded.
* DO make code deterministic.  Construct `HashMap`s and `HashSet`s using `com.ibm.wala.util.collections.HashMapFactory.make()` and ``com.ibm.wala.util.collections.HashSetFactory.make()` respectively.  Avoid use of `System.identityHashCode()` and finalizers.  

* DON'T write to `System.out` or `System.err`. Use the `com.ibm.wala.util.debug.Trace` facility to write debugging and trace messages.
* DON'T submit PRs that alter licensing related files or headers. If you believe there's a problem with them, file an issue and we'll be happy to discuss it.


Guiding Principles
------------------

* We allow anyone to participate in our projects. Tasks can be carried out by anyone that demonstrates the capability to complete them.
* Always be respectful of one another. Assume the best in others and act with empathy at all times
* Collaborate closely with individuals maintaining the project or experienced users. Getting ideas out in the open and seeing a proposal before it's a pull request helps reduce redundancy and ensures we're all connected to the decision making process
