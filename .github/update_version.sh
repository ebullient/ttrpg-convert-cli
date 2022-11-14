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
sed -E -i "s|/$CURRENT|/$NEXT|g" README.md
sed -E -i "s|-$CURRENT|-$NEXT|g" README.md
sed -E -i "s|<revision>.*</revision>|<revision>$NEXT</revision>|" pom.xml
sed -E -i "s/  current-version: .*/  current-version: $NEXT/g" .github/project.yml
sed -E -i "s/  next-version: .*/  next-version: $NEXT/g" .github/project.yml

if grep '<revision>' pom.xml | grep $NEXT; then
  echo "✅ <revision> in pom.xml updated to $NEXT"
else
  echo "❌ <revision> in pom.xml is $(grep '<revision>' pom.xml)"
  exit 1
fi
