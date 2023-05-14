#!/usr/bin/env bash

if [ -z "$1" ]; then
  echo "Specify target version"
  exit 1
fi
if [ -z "$JRELEASER_GITHUB_TOKEN" ]; then
  echo "Specify JRELEASER_GITHUB_TOKEN"
  exit 1
fi

export JRELEASER_PROJECT_VERSION=$1

git fetch --all
git checkout ${JRELEASER_PROJECT_VERSION}

./mvnw clean package -Dnative
jreleaser assemble -s archive -scp -od target
jreleaser release -od target -scp -xd uber-jar

