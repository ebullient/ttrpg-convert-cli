#!/usr/bin/env bash

if [ -z "$1" ]; then
  echo "Specify target version"
  exit 1
fi

export JRELEASER_PROJECT_VERSION=$1
export JRELEASER_GITHUB_TOKEN=`cat ~/.config/env/gh-cli-token`

git fetch --all
git checkout ${JRELEASER_PROJECT_VERSION}

mvn clean package -Dnative
jreleaser assemble -s archive -scp -od target
jreleaser release -od target -scp -xd uber-jar

