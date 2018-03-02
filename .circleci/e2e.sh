#!/bin/bash

# show versions
echo "yarn" $(yarn --version)
echo "npm" $(npm --version)
echo "protractor" $(protractor --version)
google-chrome --version
java -version
curl --version

# start selenium server in background
echo "Starting selenium server"
webdriver-manager update
webdriver-manager start &

# Download all dependencies
yarn

# Prepare environment to execute java tests
echo "Building Java tests"
mvn clean verify -B -DskipTests || exit 1

# run test
echo "Running UI tests"
protractor --troubleshoot true --baseUrl='http://localhost:8080' src/test/javascript/conf.js || exit 1

echo "Running Java tests"
mvn test -B -P e2e || exit 1