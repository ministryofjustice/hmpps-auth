#!/bin/bash
#
# Script to amend user emails
# You may need to create a new client in OAuth specifically for this work.
# The client should have the ROLE_MAINTAIN_OAUTH_USERS roles.
#
# Parameters:
#       1. ENV - [t3|t2|preprod|prod]
#       2. CLIENT - the client Id and secret, colon-separated
#       3. USER - the name of the client used to authenticate
#       4  BATCH - the size of batches (with 5 second pauses between them)
#       5. FILE - the name of the file containing the user data
#
# Example:
#
# $ ./amend-email.sh t3 <client>:<secret> THARRISON_ADM 20 users-data.csv | tee output.txt
#
# File format for users:
#
# A comma-separated file containing fields
#        1) username
#        2) email address
#
# Make sure there is a new line at the end of the file, otherwise it will not load.
#

ENV=${1?No environment specified}
CLIENT=${2?No client specified}
USER=${3?No user specified}
BATCH=${4?No batch size specified}
FILE=${5?No file specified}

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
. "${DIR}"/token-functions.sh

HOST=$(calculateHostname "$ENV")
checkFile "$FILE"
AUTH_TOKEN_HEADER=$(authenticate "$CLIENT" "$USER")

cnt=0

# username email
while IFS=, read -r -a row; do
  # grab uppercase username from first field and ignore empty rows
  user=$(echo "${row[0]}" | tr '[:lower:]' '[:upper:]')
  if [[ "$user" == "USER" || -z "$user" ]]; then
    continue
  fi
  email="${row[1]}"

  echo "Amending user $user with new email of $email"

  # Amend user email address
  if ! output=$(curl -sS -X POST "$HOST/auth/api/authuser/${user}" -H "$AUTH_TOKEN_HEADER" -H "Content-Type: application/json" \
    -d "{ \"email\": \"${email}\" }"); then

    echo "Failed to amend email for $user to $email"
  elif [[ $output =~ "error_description" ]]; then
    echo "Failed to amend email for $user to $email due to $output"
  fi

  # Pause for 5 seconds every BATCH number of records
  cnt=$((cnt + 1))
  n=$((cnt % BATCH))
  if [[ $n -eq 0 ]]; then
    echo "Record count $cnt - paused for 5 seconds"
    sleep 5
  fi

done <"$FILE"

# End
