#!/usr/bin/env bash

set -x

#if [ -z "$1" ]
#then
#    id=42
#else
#    id=$1
#fi

curl -vvv -H "Content-Type: application/json" --request POST --data  '{"id":"43", "handle":"somehandle", "title":"some fancy title", "publicationYear":"2020", "publicationType": "mono", "actions":[{"permission":"true", "actiontype":"read", "restrictions":[{"restrictiontype":"age", "attributetype":"fromdate", "attributevalues":["18"]}]}]}' http://localhost:8082/api/v1/item
