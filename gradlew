#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME\n\nPlease set the JAVA_HOME environment variable in your environment to point to the Java installation directory." "$ERROR_CODE_GENERIC"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.\n\nPlease set the JAVA_HOME environment variable in your environment to point to the Java installation directory." "$ERROR_CODE_GENERIC"
fi

# Determine the script directory.
SCRIPT_DIR=$(dirname "$0")

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

# Set Gradle home to the script directory.
GRADLE_HOME="$SCRIPT_DIR"

# Set Gradle wrapper jar path.
GRADLE_WRAPPER_JAR="$GRADLE_HOME/gradle/wrapper/gradle-wrapper.jar"

# Execute Gradle.
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -jar "$GRADLE_WRAPPER_JAR" "$@"

