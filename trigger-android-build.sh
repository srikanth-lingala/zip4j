#!/bin/sh -f

if [ "$#" -ne 1 ]; then
  echo "Circle CI token missing"
  exit 1
fi

TOKEN="$1"

curl -X POST https://circleci.com/api/v1.1/project/github/srikanth-lingala/zip4j-android-test/build?circle-token=${TOKEN} | tee /tmp/travis-request-output.$$.txt

if ! grep -q 'Build created' /tmp/travis-request-output.$$.txt; then
    echo "CircleCI Android build trigger failed"
    exit 1
fi