#!/bin/sh -f

# Used from https://github.com/mernst/plume-lib/blob/master/bin/trigger-travis.sh

TRAVIS_URL=travis-ci.org
BRANCH=master
USER=srikanth-lingala
REPO=zip4j-android-test
TOKEN=$(TRAVIS_CI_TOKEN)

## For debugging:
# echo "USER=$USER"
# echo "REPO=$REPO"

body="{
\"request\": {
  \"branch\":\"$BRANCH\"
}}"

# It does not work to put / in place of %2F in the URL below.  I'm not sure why.
curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Travis-API-Version: 3" \
  -H "Authorization: token ${TOKEN}" \
  -d "$body" \
  https://api.${TRAVIS_URL}/repo/${USER}%2F${REPO}/requests \
 | tee /tmp/travis-request-output.$$.txt

if grep -q '"@type": "error"' /tmp/travis-request-output.$$.txt; then
    exit 1
fi
if grep -q 'access denied' /tmp/travis-request-output.$$.txt; then
    exit 1
fi