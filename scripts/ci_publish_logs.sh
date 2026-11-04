#!/usr/bin/env bash
# Publishes the last lines of each CI log to the ci-logs branch so a failed run can be read
# without downloading artifacts. Never fails the job.
set -uo pipefail

: "${GH_TOKEN:?}" "${GITHUB_REPOSITORY:?}" "${GITHUB_RUN_ID:?}"
src="$(cd "${1:-logs}" 2>/dev/null && pwd)" || exit 0
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
  mkdir -p "runs/${GITHUB_RUN_ID}"
  for f in "$src"/*; do
    [ -f "$f" ] && tail -n 600 "$f" > "runs/${GITHUB_RUN_ID}/$(basename "$f")"
  done
  printf '{"run":"%s","sha":"%s","status":"%s","ref":"%s"}\n' \
    "$GITHUB_RUN_ID" "${GITHUB_SHA:-}" "${STATUS:-unknown}" "${GITHUB_REF_NAME:-}" > status.json
  cp status.json "runs/${GITHUB_RUN_ID}/status.json"
  ls runs | sort -n | head -n -15 | while read -r old; do rm -rf "runs/${old}"; done
  git add -A
  git commit -q -m "ci logs: run ${GITHUB_RUN_ID} ${STATUS:-unknown}" || exit 0
  if git push -q "$remote" ci-logs 2>/dev/null; then
    exit 0
  fi
  sleep 3
done
exit 0
