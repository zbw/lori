#!/usr/bin/env bash

## Author   : Christian Bay
## About    : A small helper script that returns a list of all microservices
##              that have been changed in this branch.
set -x
regex="app\/(\w*)\/(server|api)\/\S*"

GITLAB_API_DATA=$(curl -s --header "PRIVATE-TOKEN:$GITLAB_API_ACCESS_TOKEN" "$CI_API_V4_URL/projects/$CI_PROJECT_ID/repository/commits/$CI_COMMIT_SHA")
MR_BRANCH_LAST_COMMIT_SHA=$(echo $GITLAB_API_DATA | jq -r '.parent_ids | del(.[] | select(. == "'$CI_COMMIT_BEFORE_SHA'")) | .[-1]')

echo $GITLAB_API_DATA
echo "MR_BRANCH_LAST_COMMIT_SHA: $MR_BRANCH_LAST_COMMIT_SHA"

changedFiles=$(git diff-tree --no-commit-id --name-only -r "$MR_BRANCH_LAST_COMMIT_SHA")
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
    if ! ./gradlew :app:"$service":server:jib
    then exit 1
    fi
done
echo "${uniques[@]}"
