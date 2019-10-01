#!/bin/sh
echo "Startin GraalVM build"
/usr/lib/graalvm/bin/native-image --no-server -cp /func/build/libs/pet-store-*-all.jar --initialize-at-build-time=com.amazonaws.serverless.proxy.model.ContainerConfig --initialize-at-build-time=reactor.core.publisher.Mono --initialize-at-build-time=reactor.core.publisher.Flux --initialize-at-build-time='com.amazonaws.serverless.proxy.model.ContainerConfig$1'
rm -rf /func/native-image/*

chmod 755 /func/server
mv /func/server /func/native-image/server
cp /func/scripts/bootstrap /func/native-image/bootstrap
cd /func/native-image && zip -j function.zip bootstrap server 
