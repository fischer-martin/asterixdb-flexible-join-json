#!/usr/bin/bash

rm flexiblejoin.jar.zip && mvn package && zip -j flexiblejoin.jar.zip target/*.jar && cp flexiblejoin.jar.zip ../../asterixdb-benchmarking/lib/flexiblejoin.jar.zip
