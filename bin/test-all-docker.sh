#!/bin/bash

export BASE=`dirname $0`/..

function test-ver() {
    sed "s/^FROM openjdk:.*/FROM openjdk:$1/g" $BASE/docker/Dockerfile.test.$2 > $BASE/docker/Dockerfile
    docker build $BASE -f docker/Dockerfile
}


test-ver 8-jdk        ubuntu
test-ver 9-jdk-slim   ubuntu  # non slim version doesn't work due to missing cacerts
test-ver 10-jdk       ubuntu

test-ver 8-jdk-alpine alpine
