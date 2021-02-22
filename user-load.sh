#!/bin/bash
#
# Script to load a batch of new users into OAuth for new service access.
# You may need to create a new client in OAuth specifically for this work.
# The client should have the ROLE_SYSTEM_USER and ROLE_MAINTAIN_OAUTH_USERS roles.
#
# Parameters:
#       1. ENV - [t3|t2|preprod|prod]
#       2. CLIENT - the client Id and secret, colon-separated
#       3. USER - the name of the client used to authenticate
#       4  BATCH - the size of batches (with 5 second pauses between them)
#       5. FILE - the name of the file containing the user data
#       6. DEBUG_CREATION - [true|false] - true if should then output the details of the user after creation
#
# Example:
#
# $ ./user-load.sh t3 <client>:<secret> THARRISON_ADM 20 users-data.txt false | tee output.txt
#
# File format for users:
#
#  A comma-separated file containing fields
#        1) email address
#        2) First name
#        3) Last name
#        4+) group codes (one per column)
#
# Make sure there is a new line at the end of the file, otherwise it will not load.
#

ENV=${1?No environment specified}
CLIENT=${2?No client specified}
USER=${3?No user specified}
BATCH=${4?No batch size specified}
FILE=${5?No file specified}
DEBUG_CREATION=${6?No debug indicator specified}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
. "${DIR}"/token-functions.sh

HOST=$(calculateHostname "$ENV")
checkFile "$FILE"
AUTH_TOKEN_HEADER=$(authenticate "$CLIENT" "$USER")

addGroup() {
  local user=$1
  local group=$2
  if [[ "$group" != "" && ! "$group" =~ ^[,]*$ ]]; then
    if [[ $(curl -sS -X PUT "$HOST/auth/api/authuser/$user/groups/$group" -H "Content-Length: 0" -H "$AUTH_TOKEN_HEADER") -ne 0 ]]; then
        echo "Failed to add $user to group $group"
    fi
  fi
}

cnt=0

# email first last group group2 group3 group4 group5 group6 group7 group8 group9
while IFS=, read -r -a row; do
  user="${row[0]}"
  # To uppercase
  user=$(echo "$user" | tr '[:lower:]' '[:upper:]')
  if [[ "$user" == "EMAIL" || -z "$user" ]]; then
    continue
  fi

  echo "Processing ${row[*]}"

  printf -v groups '\"%s\",' "${row[@]:3}"

  # Create the user
  if ! output=$(curl -sS -X POST "$HOST/auth/api/authuser/create" -H "$AUTH_TOKEN_HEADER" -H "Content-Type: application/json" \
    -d "{ \"groupCodes\": [${groups%,}], \"email\": \"${row[0]}\", \"firstName\": \"${row[1]}\", \"lastName\": \"${row[2]}\"}"); then

    echo "Failure to create user ${user}"
  else
    if [[ $output =~ "error_description" ]]; then
      #Check if the username was returned. If so, try to add the groups.
      username=$(echo "$output" | jq  -r '.username')

      if [[ "$username" == "null" ]]; then
        echo "Failure to create user ${user} due to $output"
      else
        echo "existing user: ${user} found. Adding groups to this user."
        for group in "${row[@]:3}"; do
          addGroup "$username" "$group"
        done
      fi

    else
      if [[ "$DEBUG_CREATION" == "true" ]]; then
        # Output the user details to confirm it was created
        curl -sS "$HOST/auth/api/authuser/$user" -H "$AUTH_TOKEN_HEADER" | jq .
      fi
    fi
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
