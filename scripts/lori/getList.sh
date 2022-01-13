#!/usr/bin/env bash

set -x
if [ -z "$1" ]
then
    offset=0
else
    offset=$1
fi
if [ -z "$2" ]
then
    limit=1
else
    limit=$2
fi

curl -vvv http://localhost:8082/api/v1/item/list?offset="$offset"\&limit="$limit"
