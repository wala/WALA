#!/bin/bash -eu
#
# Fetches the agent skills that this repository pins in skills-lock.json.
#
# Usage:
#
#   ./refresh-agent-skills.sh [--if-missing]
#
# The `skills` command line tool restores the skills that the lock file
# pins, at the release that it pins, so the lock file is the single source
# of truth for what gets installed.
#
# The canonical copy of each skill lands in .agents/skills/, and this
# script links it into each agent's own skills directory.  Neither is
# tracked by Git; only the pin in skills-lock.json is.  See CONTRIBUTING.md
# for the details.
#
# With --if-missing, the fetch is skipped when the skills are already
# installed at the pinned revision.  The Git hook uses that, so that
# checking out a branch does not fetch the skills all over again.

# The version of the `skills` command line tool to use.  Pin it, so that a
# refresh cannot silently pick up a release with different behavior.
readonly SKILLS_CLI_VERSION="1.7.4"

# The canonical directory that the skills are installed into.
readonly SKILLS_DIR=".agents/skills"

# The skills directories of the agents that get the skills.  Add an agent
# here to give it the skills as well.
readonly AGENT_SKILL_DIRS=(.claude/skills)

# Records the revision that the skills on disk were fetched at, so that
# --if-missing can tell whether they are up to date.
readonly INSTALLED_REVISION_FILE="${SKILLS_DIR}/.installed-revision"

# The file that pins the agent skills.
readonly LOCK_FILE="skills-lock.json"

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

if ! command -v node > /dev/null 2>&1; then
  echo "error: this script requires node" >&2
  exit 1
fi

cd "$(git rev-parse --show-toplevel)"
repo_root="$(pwd)"

if [ ! -f "${LOCK_FILE}" ]; then
  echo "error: ${LOCK_FILE} not found; run this script from a checkout of the repository" >&2
  exit 1
fi

# The revision that the lock file pins, and the skills that it pins.  The
# lock file is read with node, which is needed anyway to run the `skills`
# command line tool, so this script needs no JSON processor of its own.
lock_summary="$(
  node -e '
    const fs = require("fs");
    const lock = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const skills = Object.entries(lock.skills);
    const refs = [...new Set(skills.map(([, s]) => s.ref).filter(Boolean))];
    if (refs.length !== 1) {
      console.error("error: " + process.argv[1] + " must pin exactly one revision");
      process.exit(1);
    }
    console.log(refs[0]);
    console.log(skills.map(([name]) => name).join(" "));
  ' "${LOCK_FILE}"
)"
{
  read -r revision
  read -r -a skills
} <<< "${lock_summary}"

# Tells whether the skills on disk are the ones that the lock file pins.
installed_at_pinned_revision() {
  if [ ! -f "${INSTALLED_REVISION_FILE}" ]; then
    return 1
  fi
  if [ "$(cat "${INSTALLED_REVISION_FILE}")" != "${revision}" ]; then
    return 1
  fi
  for skill in "${skills[@]}"; do
    if [ ! -f "${SKILLS_DIR}/${skill}/SKILL.md" ]; then
      return 1
    fi
  done
  return 0
}

if [ "${1:-}" = "--if-missing" ] && installed_at_pinned_revision; then
  echo "The agent skills are already installed at ${revision}; nothing to fetch."
else
  echo "Installing the agent skills at ${revision} into ${SKILLS_DIR}/"
  npx --yes "skills@${SKILLS_CLI_VERSION}" experimental_install
  printf '%s\n' "${revision}" > "${INSTALLED_REVISION_FILE}"
  if ! installed_at_pinned_revision; then
    echo "error: the agent skills at ${revision} are not installed" >&2
    exit 1
  fi
  echo "The agent skills are installed at ${revision}."
fi

# The `skills` command line tool installs the skills into the canonical
# directory, but it links them only into the agents that read that
# directory directly.  Link them into the other agents here.
shopt -s nullglob
for skill_dir in "${SKILLS_DIR}"/*/; do
  skill="$(basename "${skill_dir}")"
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
