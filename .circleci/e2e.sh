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

# small delay
sleep 5

# Download all dependencies
yarn

# run test
echo "Running UI tests"
protractor --troubleshoot true --baseUrl='http://localhost:8080' src/test/javascript/conf.js || exit 1

echo "Building Java tests"
mvn clean verify -B -DskipTests

echo "Running Java tests"
mvn test -P e2e