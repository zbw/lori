#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
#set -o xtrace
#shellcheck disable=SC1091

# Back up and generate runtime config
cp "${SRV_DIR}/config.dct" "${SRV_DIR}/.config.dct.bak"
gomplate -f ./gomplates/config.dct.tmpl > "${SRV_DIR}/config.dct"

gomplate -f ./gomplates/start-server.exp.tmpl | expect