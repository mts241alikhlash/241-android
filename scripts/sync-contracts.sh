#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <path-241-apps>" >&2
  exit 1
fi

APPS_DIR="$1"
SERVICES=(exam identity student)

for svc in "${SERVICES[@]}"; do
  src="${APPS_DIR}/contracts/${svc}/openapi.json"
  if [[ ! -f "${src}" ]]; then
    echo "Missing contract file: ${src}" >&2
    exit 1
  fi
done

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST_DIR="${REPO_ROOT}/apps/exam-android/contracts"
mkdir -p "${DEST_DIR}"

for svc in "${SERVICES[@]}"; do
  src="${APPS_DIR}/contracts/${svc}/openapi.json"
  dest="${DEST_DIR}/${svc}.openapi.json"
  cp "${src}" "${dest}"
  version="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["info"]["version"])' "${dest}")"
  echo "${svc}: ${version}"
done
