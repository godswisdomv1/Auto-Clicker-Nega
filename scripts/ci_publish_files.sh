#!/usr/bin/env bash
# Publishes a folder of files (screenshots and text) to the ci-logs branch under emu/<run id>/ so they can be
# read without downloading artifacts. Never fails the job.
set -uo pipefail

: "${GH_TOKEN:?}" "${GITHUB_REPOSITORY:?}" "${GITHUB_RUN_ID:?}"
src="$(cd "${1:-emu}" 2>/dev/null && pwd)" || exit 0
remote="https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"

for attempt in 1 2 3; do
  work="$(mktemp -d)"
  cd "$work" || exit 0
  git init -q
  git config user.name "nega-ci"
  git config user.email "nega-ci@users.noreply.github.com"
  if git ls-remote --exit-code --heads "$remote" ci-logs >/dev/null 2>&1; then
    git fetch -q --depth=1 "$remote" ci-logs && git checkout -q -B ci-logs FETCH_HEAD
  else
    git checkout -q --orphan ci-logs
  fi
  mkdir -p "emu/${GITHUB_RUN_ID}"
  cp "$src"/* "emu/${GITHUB_RUN_ID}/" 2>/dev/null
  ls emu | sort -n | head -n -4 | while read -r old; do rm -rf "emu/${old}"; done
  git add -A
  git commit -q -m "emulator files: run ${GITHUB_RUN_ID}" || exit 0
  if git push -q "$remote" ci-logs 2>/dev/null; then
    exit 0
  fi
  sleep 3
done
exit 0
