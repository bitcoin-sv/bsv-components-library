#!/bin/bash

# This script is meant to be used by the Tests under src/test.
# If you want to run it from the command line, you need to run it from te Root folder of the Project:
# > ./src/test/resources/stopFDBForTesting.sh

# Path locations:

DOCKER_COMPOSE_FOLDER="installation"
DOCKER_COMPOSE_FILE="${DOCKER_COMPOSE_FOLDER}/docker-compose.yml"

# Output info:

echo "Docker-compose file used:  $DOCKER_COMPOSE_FILE"

# we Stop the Container...

docker-compose -f $DOCKER_COMPOSE_FILE down
sleep 5.0
echo "FDB Stopped."