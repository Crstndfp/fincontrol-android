#!/bin/sh

# Gradle start up script for POSIX
DIRNAME="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$DIRNAME"
APP_NAME="Gradle"

# Use default JVM options if none were specified
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
else
    JAVACMD="java"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
