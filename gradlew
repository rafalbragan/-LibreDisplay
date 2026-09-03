#!/usr/bin/env sh
#
# Gradle start up script for UN*X
#

# Attempt to set APP_HOME
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$(dirname "$PRG")"/$link"
    fi
done
SAVED=$(pwd)
cd "$(dirname "$PRG")/" >/dev/null
APP_HOME=$(pwd -P)
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

MAX_FD="maximum"

warn() {
    echo "$*"
}

die() {
    echo
    echo "$*"
    echo
    exit 1
}

case $( uname ) in
    CYGWIN*) cygwin=true ;;
    Darwin*) darwin=true ;;
    MINGW*) msys=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"

exec "$JAVACMD" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"

