#!/bin/sh

./docker-build.sh
sam local start-api -t sam-native.yaml