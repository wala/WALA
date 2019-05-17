#!/bin/bash -eux

# specify new version as version number, optionally followed by
# '-SNAPSHOT' for a snapshot version

if [ "$#" != 2 ]; then
  echo "Usage: $0 <old-version> <new-version>"
  exit 1
fi

replace() {
  pattern=$1
  replacement=$2
  shift 2

  find . \( "$@" \) -print0 |
    xargs -0 -n 1 perl -pi -e "s/$pattern/$replacement/g"
}

if command -v mvn 2>/dev/null; then
  mvn clean -q
fi

oldVersionRegex=${1//./\\.}
newVersion=$2
replace "$oldVersionRegex" "$newVersion" -name gradle.properties -o -name pom.xml

oldBundleRegex=${oldVersionRegex/%-SNAPSHOT/\\.qualifier}
newBundle=${newVersion/%-SNAPSHOT/.qualifier}
replace "$oldBundleRegex" "$newBundle" -name feature.xml -o -name MANIFEST.MF
