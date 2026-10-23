Contributing to WALA
====================

WALA welcomes contributions of all kinds and sizes. This includes
everything from simple bug reports to large features.

Workflow
--------

We love GitHub issues!

For small feature requests, an issue first proposing it for discussion
or demo implementation in a PR suffice.

For big features, please open an issue so that we can agree on the
direction, and hopefully avoid investing a lot of time on a feature
that might need reworking.

Small pull requests for things like typos, bug fixes, etc are always welcome.

Additional Checks
-----------------

Beyond tests, other checks run as part of `./gradlew check` and
`./gradlew build`, including:

1. Compilation with the Java compiler from [Eclipse JDT
   Core](https://www.eclipse.org/jdt/core/), which runs additional
   Lint checks

1. Checking that all Java and Kotlin code is formatted according to
   [Google Java Format](https://github.com/google/google-java-format)
   and [ktfmt](https://facebook.github.io/ktfmt/) standards,
   respectively.

If your code fails check 2, you can run `./gradlew spotlessApply` to
automatically format it.  The CI job runs `./gradlew build` and will
fail if any of these additional checks fail.

Agent Skills
------------

Some agent skills that help with Gradle are fetched from the Gradle
project, so that they are available to whichever agent you happen to use.
They are not tracked by Git.  What is tracked is `apm.yml`, which
declares where the skills come from, and `apm.lock.yaml`, which pins the
exact upstream commit of each skill and the hash of every deployed file.

The skills are managed by [APM](https://aka.ms/apm) (the Agent Package
Manager), which reads those two files, fetches what the lock file pins,
and deploys each skill.  Install the `apm` command line tool first:

    brew install apm

After you clone the repository, fetch the skills:

    ./refresh-agent-skills.sh

The skills are installed in "project" scope: APM deploys the canonical
copy of each skill into `.agents/skills/`, and the refresh script links
it into `.claude/skills/`, so that there is only ever one copy to
maintain and every agent sees the same skills.  Agents like Junie load
skills directly from `.agent/` and `.agents/`, so they need no links.
Those directories are ignored by Git.

The fetch also happens on its own in a devcontainer.  Elsewhere it
happens after a checkout, once the Git hooks are installed:

    ./gradlew installGitHooks

`apm.lock.yaml` is written by APM, and it is the only thing that should
write it.

To move the pin to a newer upstream release, change the `ref` in
`apm.yml` to the release you want, and let APM re-resolve it:

    apm update

That rewrites `apm.lock.yaml` to record the new commit and the new
hashes.  Run `./refresh-agent-skills.sh` once afterwards, so that the
skills on disk are the ones that the new pin calls for.  Commit the
resulting changes to `apm.yml` and `apm.lock.yaml`; those two files are
the whole change.

To give the skills to another agent, add its skills directory to the
`AGENT_SKILL_DIRS` list in `refresh-agent-skills.sh` and re-run the
script.  APM has a target for most agents; when it does not, the script
links the canonical copy into that agent's directory.

On Windows, where symbolic links generally are not available, the
refresh script makes directory junctions instead of them.  Nothing is
tracked, so there is nothing to commit either way; the junctions are
simply left in place, like the symbolic links are elsewhere.

These skills come from the Gradle project and are licensed under the
Apache License, Version 2.0.  Each skill records its license in the
front matter of its `SKILL.md` file.  You may obtain a copy of the
license at <https://www.apache.org/licenses/LICENSE-2.0>.

DOs and DON'Ts
--------------

* DO format your Java and Kotlin code using [Google Java
  Format](https://github.com/google/google-java-format) and
  [ktfmt](https://facebook.github.io/ktfmt/), respectively.  A CI job
  will fail if your code is not formatted in this way.

* DO include tests when adding new features. When fixing bugs, start
  with adding a test that highlights how the current behavior is
  broken.

* DO keep the discussions focused. When a new or related topic comes
  up it's often better to create new issue than to side track the
  discussion.

* DO make liberal use of Javadoc and comments to document code.

* DO use the `com.ibm.wala.util.debug.Assertions` class liberally. All
  calls to `assert()` must be guarded by
  `Assertions.verifyAssertions`. Use the `productionAssert`
  entrypoints for assertions that should be enabled in production, and
  thus not guarded.

* DO make code deterministic.  Construct `HashMap`s and `HashSet`s
  using `com.ibm.wala.util.collections.HashMapFactory.make()` and
  `com.ibm.wala.util.collections.HashSetFactory.make()` respectively.
  Avoid use of `System.identityHashCode()` and finalizers.

* DON'T submit PRs that alter licensing related files or headers. If
  you believe there's a problem with them, file an issue and we'll be
  happy to discuss it.

Guiding Principles
------------------

* We allow anyone to participate in our projects. Tasks can be carried
  out by anyone that demonstrates the capability to complete them.

* Always be respectful of one another. Assume the best in others and
  act with empathy at all times

* Collaborate closely with individuals maintaining the project or
  experienced users. Getting ideas out in the open and seeing a
  proposal before it's a pull request helps reduce redundancy and
  ensures we're all connected to the decision making process
