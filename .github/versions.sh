#!/usr/bin/env bash
set -eo pipefail

CURRENT=$(yq '.release.current-version' .github/project.yml )
SNAPSHOT=$(yq '.release.snapshot-version' .github/project.yml)
ARTIFACT=$(yq '.jitpack.artifact' .github/project.yml)
GROUP=$(yq '.jitpack.group' .github/project.yml)

if [[ "${DRY_RUN}" == "true" ]]; then
  NEXT=299-SNAPSHOT
  echo "🔹 Dry run, use snapshot: $NEXT"
elif [[ -z "${INPUT}" ]] || [[ "${INPUT}" == "project" ]]; then
  NEXT=$(yq '.release.next-version' .github/project.yml)
  echo "🔹 Use project version: $CURRENT --> $NEXT"
else
  TMP=$CURRENT
  major=${TMP%%.*}
  TMP=${TMP#$major.}
  minor=${TMP%%.*}
  TMP=${TMP#$minor.}
  patch=${TMP%.*}
  
  case "$INPUT" in
    major)
      NEXT=$(($major+1)).0.0
      echo "🔹 Bump major version: ${major}.${minor}.${patch} --> $NEXT"
    ;;
    minor)
      NEXT=${major}.$(($minor+1)).0
      echo "🔹 Bump minor version: ${major}.${minor}.${patch} --> $NEXT"
    ;;
    patch)
      NEXT=${major}.${minor}.$(($patch+1))
      echo "🔹 Bump patch version: ${major}.${minor}.${patch} --> $NEXT"
    ;;
    *)
      if [[ ! ${INPUT} =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        printf >&2 'Error : %s is not a valid semver.\n' ${INPUT}
        exit 1
      fi
      NEXT=${INPUT}
      echo "🔹 Use specified version: $CURRENT --> $NEXT"
    ;;
  esac
fi

if [[ "${RETRY}" == "true" ]] || [[ "${DRY_RUN}" == "true" ]]; then
  echo "🔹 Retrying release of $NEXT"
elif git rev-parse "refs/tags/$NEXT" > /dev/null 2>&1; then
  echo "🛑 Tag $NEXT already exists"
  exit 1
fi

echo "current=${CURRENT}" >> $GITHUB_OUTPUT
echo "next=${NEXT}" >> $GITHUB_OUTPUT
echo "snapshot=${SNAPSHOT}" >> $GITHUB_OUTPUT
echo "artifact=${ARTIFACT}" >> $GITHUB_OUTPUT
echo "group=${GROUP}" >> $GITHUB_OUTPUT
