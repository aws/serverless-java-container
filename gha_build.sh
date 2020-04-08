#!/usr/bin/env bash

WORKING_DIR=$(pwd)
FRAMEWORK=$1
RUN_ARCHETYPE=$2
RUN_SAMPLES=$3
EXTRA_PARAMS=${*:4}

echo "Starting build script for ${FRAMEWORK} with params ${EXTRA_PARAMS}"

if [[ -z ${FRAMEWORK} ]] ; then
    echo "Missing framework parameter"
    exit 1
fi

function install {
    # we skip tests for core because we assume they will be run in a separate branch of the workflow
    cd ${WORKING_DIR}/aws-serverless-java-container-core && mvn -q clean install -DskipTests
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
    cd ${WORKING_DIR}/aws-serverless-java-container-$1 && mvn -q clean install  ${@:2}
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
}

function archetype {
    ARCHETYPE_NAME=aws-serverless-$1-archetype
    PROJ_NAME=$1-archetype-test
    cd ${WORKING_DIR}/${ARCHETYPE_NAME} && mvn -q clean install
    ARCHETYPE_TEST_DIR=${WORKING_DIR}/$1_archetype_test
    mkdir -p ${ARCHETYPE_TEST_DIR}
    cd ${ARCHETYPE_TEST_DIR} && mvn archetype:generate -DgroupId=my.service -DartifactId=${PROJ_NAME} -Dversion=1.0-SNAPSHOT \
       -DarchetypeGroupId=com.amazonaws.serverless.archetypes \
       -DarchetypeArtifactId=${ARCHETYPE_NAME} \
       -DarchetypeCatalog=local \
       -DinteractiveMode=false
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
    cd ${ARCHETYPE_TEST_DIR}/${PROJ_NAME} && mvn -q clean package -Pshaded-jar
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
    cd ${ARCHETYPE_TEST_DIR}/${PROJ_NAME} && mvn -q clean package
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
    cd ${ARCHETYPE_TEST_DIR}/${PROJ_NAME} && gradle -q wrapper
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
    cd ${ARCHETYPE_TEST_DIR}/${PROJ_NAME} && ./gradlew wrapper --gradle-version 5.0
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
    cd ${ARCHETYPE_TEST_DIR}/${PROJ_NAME} && ./gradlew -q clean build
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
}

function sample {
    # force to pet store for now. In the future we may loop over all samples
    SAMPLE_FOLDER=${WORKING_DIR}/samples/$1/pet-store
    cd ${SAMPLE_FOLDER} && mvn -q clean package
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
    cd ${SAMPLE_FOLDER} && gradle -q wrapper
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
    cd ${SAMPLE_FOLDER} && ./gradlew wrapper --gradle-version 5.0
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
    cd ${SAMPLE_FOLDER} && ./gradlew -q clean build
    if [[ "$?" -ne 0 ]]; then
        exit 1
    fi
}

# set up the master pom otherwise we won't be able to find new dependencies
cd ${WORKING_DIR}/ && mvn -q --non-recursive clean install

case $1 in
    # special case for spring since we include both spring and springboot 1.x in one package
    spring)
        install ${FRAMEWORK} ${EXTRA_PARAMS}
        if [[ "$RUN_ARCHETYPE" = true ]] ; then
            archetype ${FRAMEWORK}
            archetype springboot
        fi
        if [[ "$RUN_SAMPLES" = true ]] ; then
            sample ${FRAMEWORK}
            sample springboot
        fi
        break
        ;;
    *)
        install ${FRAMEWORK} ${EXTRA_PARAMS}
        if [[ "$RUN_ARCHETYPE" = true ]] ; then
            archetype ${FRAMEWORK}
        fi
        if [[ "$RUN_SAMPLES" = true ]] ; then
            sample ${FRAMEWORK}
        fi
        ;;
esac