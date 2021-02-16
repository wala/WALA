#!/bin/bash -eux
#
# Execute various steps after a successful Travis build on master.
# * Deploy a jar, source jar, and javadoc jar to Sonatype's snapshot repo.
# * Deploy javadocs to our web site.
# * Trigger Travis for dependent projects
#
# Adapted from https://coderwall.com/p/9b_lfq and
# http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/

SLUG=wala/WALA
JDK=openjdk8
BRANCH=master
OSNAME=linux

if [ "$TRAVIS_REPO_SLUG" != "$SLUG" ]; then
  echo "Skipping snapshot deployment: wrong repository. Expected '$SLUG' but was '$TRAVIS_REPO_SLUG'."
elif [ "$TRAVIS_JDK_VERSION" != "$JDK" ]; then
  echo "Skipping snapshot deployment: wrong JDK. Expected '$JDK' but was '$TRAVIS_JDK_VERSION'."
elif [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo "Skipping snapshot deployment: was pull request."
elif [ "$TRAVIS_BRANCH" != "$BRANCH" ]; then
  echo "Skipping snapshot deployment: wrong branch. Expected '$BRANCH' but was '$TRAVIS_BRANCH'."
elif [ "$TRAVIS_OS_NAME" != "$OSNAME" ]; then
  echo "Skipping snapshot deployment: wrong OS. Expected '$OSNAME' but was '$TRAVIS_OS_NAME'."
elif [ "$TRAVIS_EVENT_TYPE" == "cron" ]; then
  echo "Skipping snapshot deployment; running cron job."
else
  # echo "Deploying snapshot..."
  # ./gradlew clean publishAllPublicationsToMavenRepository
  # echo "Snapshot deployed!"

  # echo "Uploading javadoc..."

  # ./gradlew aggregatedJavadocs

  # (
  # cd build/docs/javadoc
  # git init
  # git add .
  # git commit -m "Latest javadoc on successful travis build $TRAVIS_BUILD_NUMBER auto-pushed to gh-pages"

  # {
  #   set +x
  #   git config --global user.email "travis@travis-ci.org"
  #   git config --global user.name "travis-ci"
  #   git remote add origin "https://${GH_TOKEN}@github.com/wala/javadoc"
  #   set -x
  # }

  # # we can force push here since we don't care about maintaining javadoc history for every commit
  # git push origin master --force

  # echo -e "Published Javadoc to gh-pages.\n"
  # )

  {
    set +x
    if [ -n "$AUTH_TOKEN" ]; then
      ./dependent-projects-trigger.sh "$AUTH_TOKEN"
    fi
    set -x
  }
fi
