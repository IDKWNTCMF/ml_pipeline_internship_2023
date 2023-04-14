#!/bin/bash

kotlinc -include-runtime -d main.jar src/main/kotlin/Main.kt src/main/kotlin/Processor.kt
java -jar main.jar $1 $2