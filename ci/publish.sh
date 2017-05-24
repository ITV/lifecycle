#!/bin/bash

#exit non-zero return code if a simple command exits with non-zero return code
set -e

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

eval "$sbt_cmd clean compile test"

if [[ $TRAVIS_PULL_REQUEST == "false" && $TRAVIS_BRANCH == "master" ]]; then
  openssl aes-256-cbc -K $encrypted_580469430836_key -iv $encrypted_580469430836_iv -in ci/private.asc.enc -out ci/private.asc -d
  eval "$sbt_cmd clean +publishSigned"
fi;
