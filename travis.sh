#!/usr/bin/env bash

IFS=$'\n'

echo "STARTING TRAVIS BUILD in $(pwd)"
cd $TRAVIS_BUILD_DIR
mvn install
echo "COMPLETED MAIN FRAMEWORK INSTALL"

echo "STARTING SAMPLES BUILD"
for SAMPLE in $(find samples -type d -maxdepth 2 -mindepth 2);
do
    echo "BUILDING SAMPLE '$SAMPLE'"
    cd $SAMPLE
    mvn clean package
    gradle clean build

    SAM_FILE="$TRAVIS_BUILD_DIR/$SAMPLE/sam.yaml"
    if [[ -f "$SAM_FILE" ]]; then
        TARGET_ZIP=$(cat $SAM_FILE | grep CodeUri | sed -e 's/^.*:\ //g')
        if [[ ! -f "./$TARGET_ZIP" ]]; then
            echo "COULD NOT FIND TARGET ZIP FILE $TARGET_ZIP for $SAMPLE"
            exit 1
        fi
    else
        echo "COULD NOT FIND SAM FILE: '$TRAVIS_BUILD_DIR/$SAMPLE/sam.yaml'"
        exit 1
    fi

    cd $TRAVIS_BUILD_DIR
    echo "'$SAMPLE' BUILT SUCCESSFULLY"
done
echo "COMPLETED SAMPLES BUILD"

cd $TRAVIS_BUILD_DIR
rm -rf tmp
echo "STARTING ARCHETYPE BUILD"
for ARCH in $(find . -name "*archetype" -type d);
do
    echo "TESTING ARCHETYPE '$ARCH'"
    ARCH_NAME=$(basename $ARCH)
    TEST_PROJ="TEST-$ARCH_NAME"
    mkdir tmp && cd tmp
    mvn archetype:generate -DgroupId=my.service -DartifactId=$TEST_PROJ -Dversion=1.0-SNAPSHOT \
       -DarchetypeGroupId=com.amazonaws.serverless.archetypes \
       -DarchetypeArtifactId=$ARCH_NAME \
       -DarchetypeCatalog=local \
       -DinteractiveMode=false
    cd ${TEST_PROJ}
    mvn clean package -Pshaded-jar
    mvn clean package
    if [[ -f "$TRAVIS_BUILD_DIR/tmp/$TEST_PROJ/build.gradle" ]]; then
        gradle clean build
    else
        echo "GRADLE BUILD FILE NOT FOUND"
    fi

    SAM_FILE="$TRAVIS_BUILD_DIR/tmp/$TEST_PROJ/sam.yaml"
    if [[ -f "$SAM_FILE" ]]; then
        TARGET_ZIP=$(cat $SAM_FILE | grep CodeUri | sed -e 's/^.*:\ //g')
        if [[ ! -f "./$TARGET_ZIP" ]]; then
            echo "COULD NOT FIND TARGET ZIP FILE $TARGET_ZIP for $ARCH"
            exit 1
        fi
    else
        echo "COULD NOT FIND SAM FILE: '$TRAVIS_BUILD_DIR/$SAMPLE/sam.yaml'"
        exit 1
    fi


    echo "'$ARCH' BUILT SUCCESSFULLY"
    cd $TRAVIS_BUILD_DIR
    rm -rf tmp
done
echo "COMPLETED ARCHETYPE BUILD"