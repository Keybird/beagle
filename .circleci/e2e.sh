#!/bin/bash

# The chrome version provided by the e2e docker image
CHROME_VERSION="77.0.3865.120"

# show versions
echo "yarn" $(yarn --version)
echo "npm" $(npm --version)
echo "protractor" $(protractor --version)
google-chrome --version
webdriver-manager version
java -version
curl --version

# Prepare environment to execute java tests
echo "Building Java tests"
mvn clean verify -B -DskipTests || exit 1

echo "Running Java tests"
mvn test -B -P e2e || exit 1

# Download all UI dependencies
yarn

# start selenium server in background
echo "Starting selenium server"
webdriver-manager update --versions.chrome ${CHROME_VERSION}
webdriver-manager start --versions.chrome ${CHROME_VERSION} &
sleep 5 # wait for the driver to fully start
webdriver-manager status

echo "Running UI tests"
protractor --troubleshoot true --baseUrl='http://localhost:8080' src/test/javascript/conf.js || exit 1