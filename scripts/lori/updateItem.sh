#!/usr/bin/env bash

if [ -z "$1" ]
then
    id=42
else
    id=$1
fi

JSON_DATA='{"id":"'"$id"'", "handle":"somehandle", "title":"some even more fancy title",'\
'"publicationYear":"2020", "publicationType": "article", "accessState": "open",'\
'"actions":[{"permission":"false", "actiontype":"read",'\
'"restrictions":['\
'{"restrictiontype":"date", "attributetype":"todate", "attributevalues":["2023"]},'\
'{"restrictiontype":"date", "attributetype":"fromdate", "attributevalues":["2017"]},'\
'{"restrictiontype":"group", "attributetype":"groups", "attributevalues":["Leibniz Institute"]},'\
'{"restrictiontype":"parts", "attributetype":"parts", "attributevalues":["Abstract"]}'\
']}]}'
echo "$JSON_DATA"
curl -vvv -H "Content-Type: application/json" --request PUT --data "$JSON_DATA" http://localhost:8082/api/v1/item
