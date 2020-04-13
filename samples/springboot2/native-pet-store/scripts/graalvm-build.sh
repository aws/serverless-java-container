#!/bin/sh
echo "Starting GraalVM build"
cd /func && mvn clean package

if [[ $? -ne 0 ]]; then
    echo "Maven build failed"
    exit 1
fi

mkdir /func/target/lambda

# $(find ./target -maxdepth 1 -type f -not -name "*.jar*")
GRAAL_EXECUTABLE=com.amazonaws.serverless.sample.springboot2.application
mv /func/target/$GRAAL_EXECUTABLE /func/target/lambda/$GRAAL_EXECUTABLE
chmod +x /func/target/lambda/$GRAAL_EXECUTABLE

cp /func/scripts/bootstrap /func/target/lambda