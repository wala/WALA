#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" == "wala/WALA" ] &&
   [ "$TRAVIS_PULL_REQUEST" == "false" ] &&
   [ "$TRAVIS_BRANCH" == "ms/agg-javadoc-gradle" ]; then

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
