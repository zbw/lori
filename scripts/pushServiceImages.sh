#!/bin/bash

## Author   : Christian Bay
## About    : A small helper script to find all microservices
##              that have been changed and push their image to the registry.
regex="app\/(\w*)\/(server|api)\/\S*"

# Production
GITLAB_API_DATA=$(curl -s --header "PRIVATE-TOKEN:$GITLAB_API_ACCESS_TOKEN" "$CI_API_V4_URL/projects/$CI_PROJECT_ID/repository/commits/$CI_COMMIT_SHA")
echo "Gitlab data: $GITLAB_API_DATA"
MR_BRANCH_LAST_COMMIT_SHA=$(echo "$GITLAB_API_DATA" | jq -r '.parent_ids | del(.[] | select(. == "'"$CI_COMMIT_BEFORE_SHA"'")) | .[-1]')

# Test: Use a local commithash and comment the 'Production' section
# MR_BRANCH_LAST_COMMIT_SHA=b7180e350fc839bf29df1cd81a983a95aa38b609

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
printf "Deploy following microservices: %s\n" "${uniques[@]}"

for service in "${uniques[@]}";
do
    if ./gradlew -q projects | grep ":app:$service:server" > /dev/null
    then
        printf "Pushing image for service %s.\n" "$service"
        ./gradlew :app:"$service":server:jib
    else
        printf "The service %s no longer does exist.\n" "$service"
    fi
done
