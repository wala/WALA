#!/bin/sh -eu

# Validate ".gitignore" patterns.  All files created during testing should already be ignored, and
# no git-tracked file should be modified during testing.  In other words, "git status" should still
# report a clean tree after testing is done.

if [ -n "$(git status --porcelain)" ]; then
  echo 'warning: source tree is unclean after testing; .gitignore patterns may need to be improved'
  git status
  # false
fi
