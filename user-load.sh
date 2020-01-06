#!/bin/bash
#
# Script to load a batch of new users into OAuth for new service access.
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
# $ ./user_load.sh t3 <client>:<secret> THARRISON_ADM 20 users-data.txt false false | tee output.txt
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

ENV=${1?No environment specified}
CLIENT=${2?No client specified}
USER=${3?No user specified}
BATCH=${4?No batch size specified}
FILE=${5?No file specified}
VARY_ROLE=${6?No vary role indicator specified}
DEBUG_CREATION=${7?No debug indicator specified}

# Set the environment-specific hostname for the oauth2 service
if [[ "$ENV" == "t3" ]]; then
  HOST="https://gateway.t3.nomis-api.hmpps.dsd.io"
elif [[ "$ENV" == "t2" ]]; then
  HOST="https://gateway.t2.nomis-api.hmpps.dsd.io"
else
  HOST="https://gateway.$ENV.nomis-api.service.hmpps.dsd.io"
fi

# Check whether the file exists and is readable
if [[ ! -f "$FILE" ]]; then
  echo "Unable to find file $FILE"
  exit 1
fi

# Get token for the client name / secret and store it in the environment variable TOKEN
TOKEN_RESPONSE=$(curl -s -k -d "" -X POST "$HOST/auth/oauth/token?grant_type=client_credentials&username=$USER" -H "Authorization: Basic $(echo -n "$CLIENT" | base64)")
TOKEN=$(echo "$TOKEN_RESPONSE" | jq -er .access_token)
if [[ $? -ne 0 ]]; then
  echo "Failed to read token from credentials response"
  echo "$TOKEN_RESPONSE"
  exit 1
fi

AUTH_TOKEN="Bearer $TOKEN"

addGroup() {
  local user=$1
  local group=$2
  if [[ "$group" != "" && ! "$group" =~ ^[,]*$ ]]; then
    curl -X PUT $HOST/auth/api/authuser/$user/groups/$group -H "Content-Length: 0" -H "Authorization: $AUTH_TOKEN" -H "accept: */*" -H "Content-Type: application/text"
    if [[ $? -ne 0 ]]; then 
      echo "Failed to add $user to group $group"
    fi
  fi
}

cnt=0

while IFS=, read -r user email first last group group2 group3 group4 group5 group6 group7 group8 group9
do

  echo "Processing | $user | $email | $first | $last | $group | $group2 | $group3 | $group4 | $group5 | $group6 | $group7 | $group8 | $group9"

  # Create the user
  curl -X PUT "$HOST/auth/api/authuser/$user" -H "Authorization: $AUTH_TOKEN" -H "accept: */*" -H "Content-Type: application/json" -d "{ \"groupCode\": \"$group\", \"email\": \"$email\", \"firstName\": \"$first\", \"lastName\": \"$last\"}"

  if [[ $? -ne 0 ]]; then
    echo "\033[0;31mFailure to create user ${user}\033[0m"

  else 
    if [[ "$DEBUG_CREATION" == "true" ]]; then
      # Output the user details to confirm it was created
      curl -s "$HOST/auth/api/authuser/$user" -H "Authorization: $AUTH_TOKEN" | jq .
    fi

    if [[ "$VARY_ROLE" == "true" ]]; then
      echo "Adding the ROLE_LICENCE_VARY role for $user"
   
      curl -X PUT $HOST/auth/api/authuser/$user/roles/ROLE_LICENCE_VARY -H "Content-Length: 0" -H "Authorization: $AUTH_TOKEN" -H "accept: */*" -H "Content-Type: application/text"
   
      # Output the roles for the user to confirm
      curl -s $HOST/auth/api/authuser/$user/roles -H "Authorization: $AUTH_TOKEN" | jq .
    fi

    addGroup $user $group2
    addGroup $user $group3
    addGroup $user $group4
    addGroup $user $group5
    addGroup $user $group6
    addGroup $user $group7
    addGroup $user $group8
    addGroup $user $group9
  fi

  # Pause for 5 seconds every BATCH number of records
  cnt=$(($cnt+1))
  n=$(($cnt%$BATCH))
  if [ $n -eq 0 ]
  then
    echo "Record count $cnt - paused for 5 seconds"
    sleep 5
  fi

done < "$FILE"

# End
