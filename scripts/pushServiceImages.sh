#!/usr/bin/env bash

## Author   : Christian Bay
## About    : A small helper script that returns a list of all microservices
##              that have been changed in this branch.
set -x
regex="app\/(\w*)\/(server|api)\/\S*"

echo CI_COMMIT_REF_NAME=$CI_COMMIT_REF_NAME
echo CI_COMMIT_BEFORE_SHA=$CI_COMMIT_BEFORE_SHA
echo CI_COMMIT_BRANCH=$CI_COMMIT_BRANCH

changedFiles=$(git diff-tree --no-commit-id --name-only -r origin/master -r $CI_COMMIT_SHA)
changedServices=()
for f in $changedFiles    # unquoted in order to allow the glob to expand
do
    if [[ $f =~ $regex ]]
    then
        changedServices+=("${BASH_REMATCH[1]}")
    fi
done
mapfile -t uniques < <(for v in "${changedServices[@]}"; do echo "$v";done| sort -u)
for service in "${uniques[@]}";
do
	if ! ./gradlew :app:$service:server:jib
    then exit 1
    fi
done
echo "${uniques[@]}"
