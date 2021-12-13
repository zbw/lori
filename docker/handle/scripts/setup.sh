#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
#set -o xtrace
#shellcheck disable=SC1091

gomplate -f ./gomplates/setup-server.exp.tmpl | expect
