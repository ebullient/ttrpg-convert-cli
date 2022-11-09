#!/usr/bin/env bash
set -eo pipefail

if [[ -z "$CURRENT" ]]; then
  echo "CURRENT env var not defined"
  exit 1
elif [[ -z "$NEXT" ]]; then
  echo "NEXT env var not defined"
  exit 1
fi

# Messy and not maven-y, but whatever.
sed -i -r "s|/$CURRENT|/$NEXT|g" README.md
sed -i -r "s|-$CURRENT|-$NEXT|g" README.md
sed -i -r "s|<revision>.*</revision>|<revision>$NEXT</revision>|" pom.xml
sed -i -r "s/  current-version: .*/  current-version: $NEXT/g" .github/project.yml
sed -i -r "s/  next-version: .*/  next-version: $NEXT/g" .github/project.yml
