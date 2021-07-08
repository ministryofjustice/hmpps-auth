#!/usr/bin/env bash
set -e

if ! echo "$BASH_VERSION" | grep -E "^[45]" &>/dev/null; then
  echo "Found bash version: $BASH_VERSION"
  echo "Ensure you are using bash version 4 or 5"
  exit 1
fi

# Set via env vars for auth
#ENV=
#USER=
#CLIENTID=(clientID with rotate permissions)
#CLIENTSECRET=

usage() {
  echo "$0 usage: " && grep " .)\ #" "$0"
  exit 1
}

while getopts ":cke:u:n" arg; do
  case "${arg}" in
  e) # Environment to run against
    ENV="${OPTARG}"
    ;;
  u) # User to run script against
    USER="${OPTARG}"
    ;;
  c) # Specify whether to create the kubernetes secret
    create_secret=true
    ;;
  k) # Specify whether to create the kubernetes secret
    create_keys_in_secret=true
    ;;
  n) # Stop after creating secret i.e. don't restart deployment or delete old secret
    stop_after_creation=true
    ;;
  *)
    usage
    ;;
  esac
done
shift $((OPTIND - 1))

BASE_CLIENT_ID=${1?: Usage: $0 baseClientId}

# Test mandatory env vars
enforce_var_set() {
  if [[ ! -v $1 ]]; then
    echo "$1 environment variable not set."
    exit 1
  fi
}

enforce_var_set ENV
enforce_var_set USER
enforce_var_set CLIENTID
enforce_var_set CLIENTSECRET
enforce_var_set BASE_CLIENT_ID

# Run against cloudplatforms k8s cluster by default, otherwise set KUBE_CONTEXT
kubectl config use-context "${KUBE_CONTEXT:-live-1.cloud-platform.service.justice.gov.uk}"

CLIENT="${CLIENTID}:${CLIENTSECRET}"

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
. "${DIR}"/token-functions.sh

HOST=$(calculateHostname "${ENV}")
AUTH_TOKEN_HEADER=$(authenticate "${CLIENT}" "${USER}")

HTTPIE_SESSION="./.httpie_session_auth.json"
HTTPIE_OPTS=("--body" "--check-status" "--timeout=4.5" "--session-read-only=${HTTPIE_SESSION}")

# Setup httpie session, enable preview API features
if ! OUTPUT=$(http --check-status --ignore-stdin --session=${HTTPIE_SESSION} "${HOST}/auth/api/client/${CLIENTID}" "${AUTH_TOKEN_HEADER}"); then
  echo "Unable to talk to HMPPS AUTH API, check credentials are set correctly and permissions granted."
  echo "$OUTPUT"
  exit 1
fi

hmpps_auth() {
  http "${HTTPIE_OPTS[@]}" "$@"
}

echo "Working on env \"${ENV}\""
echo "Talking to host \"${HOST}\""

# Fetch clientID data
echo "Fetching deployment data for clientID \"${BASE_CLIENT_ID}\""
clientInfo_json=$(hmpps_auth GET "${HOST}/auth/api/client/${BASE_CLIENT_ID}")

namespace=$(echo "${clientInfo_json}" | jq -r .clientDeployment.namespace)
deployment=$(echo "${clientInfo_json}" | jq -r .clientDeployment.deployment)
secretName=$(echo "${clientInfo_json}" | jq -r .clientDeployment.secretName)
clientIdKey=$(echo "${clientInfo_json}" | jq -r .clientDeployment.clientIdKey)
secretKey=$(echo "${clientInfo_json}" | jq -r .clientDeployment.secretKey)

# Check if $deployment exists and is readable
if ! kubectl -n "${namespace}" get deployment "${deployment}" &>/dev/null; then
  echo "Unable to find deployment \"${deployment}\" in namespace \"${namespace}\""
  exit 1
fi

# Check if $secretName exists and is readable
if ! kubectl -n "${namespace}" get secrets "${secretName}" -o json &>/dev/null; then
  echo "Unable to find k8s secret with name \"${secretName}\" for namespace \"${namespace}\""
  [[ -z "${create_secret}" ]] && echo "If this is not expected to exist then use the -c argument to create" && exit 1
  echo "Attempting creation of secret"
  if ! kubectl -n "${namespace}" create secret generic "${secretName}"; then
    echo "Failed to create secret \"${secretName}\""
    exit 1
  fi
fi

# Check if $clientIdKey exists and is readable
if ! kubectl -n "${namespace}" get secrets "${secretName}" -o json | jq -e "select(.data[\"${clientIdKey}\"] != null)" &>/dev/null; then
  echo "Unable to find k8s secret with key \"${clientIdKey}\" in \"${secretName}\" for namespace \"${namespace}\""
  [[ -z "${create_keys_in_secret}" ]] && echo "If this is not expected to exist then use the -k argument to create" && exit 1
fi

# Check if $secretKey exists and is readable
if ! kubectl -n "${namespace}" get secrets "${secretName}" -o json | jq -e "select(.data[\"${secretKey}\"] != null)" &>/dev/null; then
  echo "Unable to find k8s secret with key \"${secretKey}\" in \"${secretName}\" for namespace \"${namespace}\""
  [[ -z "${create_keys_in_secret}" ]] && exit 1
fi

# Duplicate clientID get new secret
results_json=$(hmpps_auth PUT "${HOST}/auth/api/client/${BASE_CLIENT_ID}")

new_clientID_name=$(echo "${results_json}" | jq -r .clientId)
new_clientID_b64name=$(echo "${results_json}" | jq -r .base64ClientId)
new_clientID_b64secret=$(echo "${results_json}" | jq -r .base64ClientSecret)

echo "New clientID created '${new_clientID_name}'"

# Save current clientID for delete at the end.
currentClientID=$(kubectl -n "${namespace}" get secrets "${secretName}" -o json | jq -r ".data[\"${clientIdKey}\"] | @base64d")

# Update k8s secret with new clientID and secret
echo "Updating k8s secret \"${secretName}\" with new clientID and secret."
kubectl -n "${namespace}" get secrets "${secretName}" -o json |
  jq ".data[\"${clientIdKey}\"]=\"$(echo -n "$new_clientID_b64name")\"" |
  jq ".data[\"${secretKey}\"]=\"$(echo -n "$new_clientID_b64secret")\"" |
  kubectl -n "${namespace}" apply -f -

[[ ! -z "${stop_after_creation}" ]] && echo "Stopping after update" && exit 0

# Restart the app deployment
echo "Restarting deployment \"${deployment}\""
kubectl -n "${namespace}" rollout restart deployment "${deployment}"

# Wait for restart to complete
kubectl -n "${namespace}" rollout status deployment "${deployment}"

### Delete the old secret no longer used
echo "Deleting old clientID ${currentClientID}"
hmpps_auth DELETE "${HOST}/auth/api/client/${currentClientID}"
