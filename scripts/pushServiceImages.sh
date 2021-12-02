#!/usr/bin/env bash

## Author   : Christian Bay
## About    : A small helper script that returns a list of all microservices
##              that have been changed in this branch.
set -x
regex="app\/(\w*)\/(server|api)\/\S*"
changedFiles=$(git diff-tree --no-commit-id --name-only -r origin/$CI_MERGE_REQUEST_TARGET_BRANCH_NAME -r $CI_COMMIT_SHA)
changedServices=()
echo CI_MERGE_REQUEST_TARGET_BRANCH_NAME=${CI_MERGE_REQUEST_TARGET_BRANCH_NAME}
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
