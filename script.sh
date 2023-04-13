#!/bin/bash

gradle init

bash ./gradlew run --args="$1 $2"