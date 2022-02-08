#!/usr/bin/env bash

## Author   : Christian Bay
## About    : A small helper script that returns a list of all microservices
##              that have been changed in this branch.
regex="job\/(\w*)\/\S*"

# Production
GITLAB_API_DATA=$(curl -s --header "PRIVATE-TOKEN:$GITLAB_API_ACCESS_TOKEN" "$CI_API_V4_URL/projects/$CI_PROJECT_ID/repository/commits/$CI_COMMIT_SHA")
MR_BRANCH_LAST_COMMIT_SHA=$(echo "$GITLAB_API_DATA" | jq -r '.parent_ids | del(.[] | select(. == "'"$CI_COMMIT_BEFORE_SHA"'")) | .[-1]')

# Test: Use a local commithash and comment the 'Production' section
# MR_BRANCH_LAST_COMMIT_SHA=0686d557afb76933f18cc99105e4c1057f809f6e

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
printf "Deploy following jobs: %s\n" "${uniques[@]}"

for job in "${uniques[@]}";
do
    if ./gradlew -q projects | grep ":job:$job" > /dev/null
    then
        printf "Pushing image for job %s.\n" "$job"
        ./gradlew :job:"$job":jib
    else
        printf "The job %s no longer does exist.\n" "$job"
    fi
done
