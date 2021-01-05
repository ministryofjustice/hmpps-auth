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
#       4  BATCH - the size of batches (with 30 second pauses between them)
#       5. FILE - the name of the file containing the user data
#       6. VARY_ROLE - [true|false] - true if all users in this load file should receive the ROLE_LICENCE_VARY role
#       7. DEBUG_CREATION - [true|false] - true if should then output the details of the user after creation
#
# Example:
#
# $ ./user-load.sh t3 <client>:<secret> THARRISON_ADM 20 users-data.txt false false | tee output.txt
#
# File format for users:
#
#  A comma-separated file containing fields
#        1) username
#        2) email address
#        3) First name
#        4) Last name
#        5) group code
#
# Make sure there is a new line at the end of the file, otherwise it will not load.
#

ENV=${1?No environment specified}
CLIENT=${2?No client specified}
USER=${3?No user specified}
BATCH=${4?No batch size specified}
FILE=${5?No file specified}
VARY_ROLE=${6?No vary role indicator specified}
DEBUG_CREATION=${7?No debug indicator specified}

# Set the environment-specific hostname for the oauth2 service
if [[ "$ENV" == "t3" ]]; then
  HOST="https://sign-in-dev.hmpps.service.justice.gov.uk"
elif [[ "$ENV" == "t2" ]]; then
  HOST="https://sign-in-stage.hmpps.service.justice.gov.uk"
elif [[ "$ENV" == "preprod" ]]; then
  HOST="https://sign-in-preprod.hmpps.service.justice.gov.uk"
elif [[ "$ENV" == "prod" ]]; then
  HOST="https://sign-in.hmpps.service.justice.gov.uk"
elif [[ "$ENV" =~ localhost* ]]; then
  HOST="http://$ENV"
fi

# Check whether the file exists and is readable
if [[ ! -f "$FILE" ]]; then
  echo "Unable to find file $FILE"
  exit 1
fi

# Get token for the client name / secret and store it in the environment variable TOKEN
if echo | base64 -w0 >/dev/null 2>&1; then
  AUTH=$(echo -n "$CLIENT" | base64 -w0)
else
  AUTH=$(echo -n "$CLIENT" | base64)
fi

if ! TOKEN_RESPONSE=$(curl -sS -d "" -X POST "$HOST/auth/oauth/token?grant_type=client_credentials&username=$USER" -H "Authorization: Basic $AUTH"); then
  echo "Failed to read token from credentials response"
  echo "$TOKEN_RESPONSE"
  exit 1
fi
TOKEN=$(echo "$TOKEN_RESPONSE" | jq -er .access_token)

AUTH_TOKEN_HEADER="Authorization: Bearer $TOKEN"

cnt=0

# email first last group group2 group3 group4 group5 group6 group7 group8 group9
while IFS=, read -r -a row; do
  user="${row[0]}"
  # To uppercase
  user=$(echo $user | tr 'a-z' 'A-Z')
  if [[ "$user" == "EMAIL" || -z "$user" ]]; then
    continue
  fi

  echo "Processing ${row[*]}"

  printf -v groups '\"%s\",' "${row[@]:3}"

  # Create the user
  if ! output=$(curl -sS -X POST "$HOST/auth/api/authuser/create" -H "$AUTH_TOKEN_HEADER" -H "Content-Type: application/json" \
    -d "{ \"groupCodes\": [${groups%,}], \"email\": \"${row[0]}\", \"firstName\": \"${row[1]}\", \"lastName\": \"${row[2]}\"}"); then

    echo "\033[0;31mFailure to create user ${user}\033[0m"
  else
    if [[ $output =~ "error_description" ]]; then
      echo "\033[0;31mFailure to create user ${user}\033[0m due to $output"
    else
      if [[ "$DEBUG_CREATION" == "true" ]]; then
        # Output the user details to confirm it was created
        curl -sS "$HOST/auth/api/authuser/$user" -H "$AUTH_TOKEN_HEADER" | jq .
      fi

      if [[ "$VARY_ROLE" == "true" ]]; then
        echo "Adding the ROLE_LICENCE_VARY role for $user"

        curl -sS -X PUT "$HOST/auth/api/authuser/$user/roles/ROLE_LICENCE_VARY" -H "Content-Length: 0" -H "$AUTH_TOKEN_HEADER"

        # Output the roles for the user to confirm
        curl -sS "$HOST/auth/api/authuser/$user/roles" -H "$AUTH_TOKEN_HEADER" | jq .
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
