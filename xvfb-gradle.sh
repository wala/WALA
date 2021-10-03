#!/bin/sh -efu

exec xvfb-run --auto-servernum ./gradlew "$@"
