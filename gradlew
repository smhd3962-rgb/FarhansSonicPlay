#!/bin/sh

# Setup Gradle wrapper jar if absent on clean checkout
WRAPPER_JAR_PATH="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR_PATH" ]; then
    echo "Downloading Gradle wrapper jar..."
    curl -sLo "$WRAPPER_JAR_PATH" "https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar" || \
    curl -sLo "$WRAPPER_JAR_PATH" "https://repo.maven.apache.org/maven2/org/gradle/gradle-tooling-api/8.9/gradle-tooling-api-8.9.jar"
fi

exec gradle "$@" || java -cp "$WRAPPER_JAR_PATH" org.gradle.wrapper.GradleWrapperMain "$@"
