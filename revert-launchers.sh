#!/bin/bash -eu

cd "$(dirname "$0")"

# be selective; revert only launchers we think we know about
launchers=(com.ibm.wala.*/launchers/*.launch com.ibm.wala.*/.launchConfigurations/*.launch)

# if no launchers have changed, then there's nothing for us to do
if git diff --quiet "${launchers[@]}"; then
  exit
fi

# create a backup in case we revert something that we should have left alone
git stash save --quiet 'safety snapshot before reverting launcher changes made by Eclipse Buildship import'
git stash apply --quiet

# revert launch configurations that Buildship import has mangled
# <https://github.com/eclipse/buildship/issues/653>
git checkout -- "${launchers[@]}"
