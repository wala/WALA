#!/usr/bin/env bash
# Lists all subdirectories containing at least one .java file,
# and checks whether each such directory has a package-info.java file.

find . -type f -name "*.java" \
  | sed 's|/[^/]*$||' \
  | sort -u \
  | while read -r dir; do
      if [ -f "$dir/package-info.java" ]; then
        echo "[YES]  $dir"
      else
        echo "[NO]   $dir"
      fi
    done