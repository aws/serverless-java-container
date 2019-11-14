#!/bin/bash
DOCKER_IMAGE_NAME=graalvm-lambda-build

if [[ -d "${PWD}/native-image" ]]; then
    rm -rf native-image/*
fi

gradle clean build --info

if [[ $? -ne 0 ]]; then
    echo "Gradle build failed"
    exit 1
fi

mkdir -p ${PWD}/native-image

if [[ "$(docker images -q ${DOCKER_IMAGE_NAME} 2> /dev/null)" == "" ]]; then
    docker build . -t ${DOCKER_IMAGE_NAME}
fi

docker run --rm -v ${PWD}:/func ${DOCKER_IMAGE_NAME}

if [[ -f "${PWD}/native-image/function.zip" ]]; then
    echo "The function is ready to deploy in the ./native-image/function.zip file. Use the sam-native.yaml template to set up your Lambda function and API"
fi
