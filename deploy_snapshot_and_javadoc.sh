#!/bin/bash -eux
#
# Deploy a jar, source jar, and javadoc jar to Sonatype's snapshot repo.
# Also deploy javadocs to our web site.
#
# Adapted from https://coderwall.com/p/9b_lfq and
# http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/

SLUG=wala/WALA
JDK=oraclejdk8
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
else
  echo "Deploying snapshot..."
  ./gradlew clean uploadArchives
  echo "Snapshot deployed!"

  echo "Uploading javadoc..."

  ./gradlew aggregatedJavadocs

  cd $HOME
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "travis-ci"
  git clone --quiet https://${GH_TOKEN}@github.com/wala/javadoc > /dev/null

  cd javadoc
  git rm -rf --quiet *
  cp -Rf $HOME/build/wala/WALA/build/docs/javadoc/* .
  git add -f .
  git commit -m "Latest javadoc on successful travis build $TRAVIS_BUILD_NUMBER auto-pushed to gh-pages"
  git push > /dev/null

  echo -e "Published Javadoc to gh-pages.\n"
  
fi
