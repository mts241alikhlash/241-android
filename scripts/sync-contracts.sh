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
DEST_DIR="${REPO_ROOT}/apps/istiqra/contracts"
mkdir -p "${DEST_DIR}"

for svc in "${SERVICES[@]}"; do
  src="${APPS_DIR}/contracts/${svc}/openapi.json"
  dest="${DEST_DIR}/${svc}.openapi.json"
  python3 - "${src}" "${dest}" <<'PY'
import json, sys

def is_typed_union(node):
    options = node.get("oneOf") if isinstance(node, dict) else None
    return bool(options) and all(
        isinstance(o, dict)
        and o.get("$ref", "").rsplit("/", 1)[-1].endswith(("AnswerPayloadDto", "ContentDto"))
        for o in options
    )

def flatten(node):
    if isinstance(node, dict):
        if is_typed_union(node):
            out = {"type": "object", "additionalProperties": True}
            if "description" in node:
                out["description"] = node["description"]
            return out
        return {k: flatten(v) for k, v in node.items()}
    if isinstance(node, list):
        return [flatten(v) for v in node]
    return node

spec = json.load(open(sys.argv[1]))
json.dump(flatten(spec), open(sys.argv[2], "w"), indent=2, ensure_ascii=False)
PY
  version="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["info"]["version"])' "${dest}")"
  echo "${svc}: ${version}"
done
