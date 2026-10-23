#!/bin/bash -eu
#
# Installs the agent skills that this repository depends on, using APM
# (the Agent Package Manager).
#
# Usage:
#
#   ./refresh-agent-skills.sh [--if-missing]
#
# APM reads apm.yml and apm.lock.yaml, fetches what the lock file pins, and
# deploys each skill into .agents/skills/.  The lock file is the single
# source of truth for what gets installed, and it is the only thing that
# should write it.
#
# APM deploys the skills to the cross-tool .agents/skills/ directory.  This
# script links that canonical copy into each agent's own skills directory, so
# that there is only ever one copy of a skill to maintain and every agent
# sees the same skills.  None of those directories is tracked by Git.  See
# CONTRIBUTING.md for the details.
#
# With --if-missing, the install is skipped when the skills already on disk
# are the ones that the lock file pins.  The Git hook uses that, so that
# checking out a branch does not fetch the skills all over again.

# The canonical directory that APM deploys the skills into.
readonly SKILLS_DIR=".agents/skills"

# The skills directories of the agents that get the skills.  Add an agent
# here to give it the skills as well.
readonly AGENT_SKILL_DIRS=(.claude/skills)

case "${1:-}" in
  "" | --if-missing) ;;
  -h | --help)
    echo "usage: $0 [--if-missing]"
    exit 0
    ;;
  *)
    echo "error: unknown option: $1" >&2
    echo "usage: $0 [--if-missing]" >&2
    exit 2
    ;;
esac
if [ "$#" -gt 1 ]; then
  echo "error: too many arguments" >&2
  echo "usage: $0 [--if-missing]" >&2
  exit 2
fi

if ! command -v apm > /dev/null 2>&1; then
  echo "error: this script requires the apm command line tool" >&2
  echo "install it with: brew install apm" >&2
  exit 1
fi

cd "$(git rev-parse --show-toplevel)"
repo_root="$(pwd)"

if [ ! -f "apm.yml" ]; then
  echo "error: apm.yml not found; run this script from a checkout of the repository" >&2
  exit 1
fi

# Tells whether the skills on disk are the ones that the lock file pins.
# `apm audit --ci` compares what is on disk against the file hashes that
# the lock file records, so a clean audit means that the skills on disk are
# the ones that the lock file pins.
skills_match_the_lock_file() {
  apm audit --ci > /dev/null 2>&1
}

if [ "${1:-}" = "--if-missing" ] && skills_match_the_lock_file; then
  echo "The agent skills are already installed; nothing to fetch."
else
  echo "Installing the agent skills into ${SKILLS_DIR}/"
  apm install
  if ! skills_match_the_lock_file; then
    echo "error: the installed agent skills do not match apm.lock.yaml" >&2
    exit 1
  fi
  echo "The agent skills are installed."
fi

# APM deploys the skills into the canonical directory, but it knows about
# only the agents that APM has a target for.  Link them into the other
# agents here.
shopt -s nullglob
for skill_dir in "${SKILLS_DIR}"/*/; do
  skill="$(basename "${skill_dir}")"
  if [ ! -f "${SKILLS_DIR}/${skill}/SKILL.md" ]; then
    continue
  fi
  for agent_dir in "${AGENT_SKILL_DIRS[@]}"; do
    link="${agent_dir}/${skill}"
    mkdir -p "${agent_dir}"
    if [ -L "${link}" ]; then
      rm -rf "${link}"
    elif [ -e "${link}" ]; then
      # Not ours to replace: that would be somebody's own skill.
      echo "warning: ${link} is not a link; leaving it alone" >&2
      continue
    fi
    if [ "${OS:-}" = "Windows_NT" ]; then
      # Windows has no symbolic links without extra privileges, so a
      # directory junction is used there, as the `skills` tool does.
      cmd //c mklink //J "$(cygpath -w "${repo_root}/${link}")" \
        "$(cygpath -w "${repo_root}/${SKILLS_DIR}/${skill}")" > /dev/null
    else
      ln -s "../../${SKILLS_DIR}/${skill}" "${link}"
    fi
  done
done
