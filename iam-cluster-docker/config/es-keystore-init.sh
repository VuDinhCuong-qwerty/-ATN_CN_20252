#!/bin/bash
# ES 8.15: bind_password là non-secure setting — để trong elasticsearch.yml, không cần keystore.
# Script này chỉ delegate sang entrypoint chính thức của ES.
set -e
exec /bin/tini -- /usr/local/bin/docker-entrypoint.sh eswrapper
