#!/bin/sh

gradle buildNativeLambda -Pmicronaut.runtime=lambda
sam local start-api -t sam-native.yaml