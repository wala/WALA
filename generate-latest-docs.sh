#!/bin/bash -eux

# Copyright 2020 The Error Prone Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Adapted from https://github.com/google/error-prone/blob/1ca81097e39957e55867732642687f3fd1160f70/util/generate-latest-docs.sh

echo -e "Publishing docs...\n"

JAVADOC_DIR=$HOME/wala-javadoc

git clone --quiet --filter=tree:0 https://x-access-token:"${GITHUB_TOKEN}"@github.com/wala/javadoc "$JAVADOC_DIR" > /dev/null
(
  cd "$JAVADOC_DIR"
  rm -rf ./*
)

./gradlew aggregatedJavadocs
rsync -a build/docs/javadoc/ "${JAVADOC_DIR}"

cd "$JAVADOC_DIR"
git add --all .
git config --global user.name "$GITHUB_ACTOR"
git config --global user.email "$GITHUB_ACTOR@users.noreply.github.com"
if git commit -m "Latest docs on successful build $GITHUB_RUN_NUMBER auto-pushed to gh-pages"; then
    git push -fq origin master > /dev/null
    echo "Published docs to gh-pages."
else
    echo "No javadoc changes to publish."
fi
