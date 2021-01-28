#!/bin/bash

# This script is meant to be used by the Tests under src/test.
# If you want to run it from the command line, you need to run it from te Root folder of the Project:
# > ./src/test/resources/startFDBForTesting.sh

# Path locations:

DOCKER_COMPOSE_FOLDER="installation"
DOCKER_COMPOSE_FILE="${DOCKER_COMPOSE_FOLDER}/docker-compose.yml"
CLUSTER_FILE="$DOCKER_COMPOSE_FOLDER/fdb.cluster"

# Output info:

echo "Docker-compose file used:  $DOCKER_COMPOSE_FILE"
echo "FDB Cluster file used:  $CLUSTER_FILE"
echo "Starting FDB in Docker Container..."

# we Start the Container...

docker-compose -f $DOCKER_COMPOSE_FILE up -d
sleep 5.0
echo "FDB Started."

# We init the DB...

echo "Initializing DB..."
fdbcli -C $CLUSTER_FILE --exec "configure new single memory"
fdbcli -C $CLUSTER_FILE --exec "writemode on; clearrange \"\" \xFF"