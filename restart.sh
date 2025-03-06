#!/bin/bash

# Function to check if MongoDB is ready
check_mongo() {
  echo "Checking MongoDB connection..."
  docker exec mongo1 mongo --port 30001 --eval "db.runCommand('ping').ok" --quiet
  return $?
}

# Stop any running containers
echo "Stopping existing containers..."
docker-compose down -v

# Remove any old data
echo "Cleaning up old data..."
rm -rf ./data

# Start the containers
echo "Starting containers..."
docker-compose up -d

# Wait for MongoDB to initialize
echo "Waiting for MongoDB to initialize..."
COUNTER=0
MAX_TRIES=30
until check_mongo || [ $COUNTER -eq $MAX_TRIES ]; do
  echo "Waiting for MongoDB to be ready... ($COUNTER/$MAX_TRIES)"
  sleep 10
  ((COUNTER++))
done

if [ $COUNTER -eq $MAX_TRIES ]; then
  echo "MongoDB failed to initialize in time. Check the logs with 'docker-compose logs mongo1'"
  exit 1
fi

echo "MongoDB is ready!"

# Check if replica set is initialized
echo "Checking replica set status..."
REPLICA_STATUS=$(docker exec mongo1 mongo --port 30001 --eval "rs.status().ok" --quiet || echo "0")
if [ "$REPLICA_STATUS" != "1" ]; then
  echo "Initializing replica set manually..."
  docker exec mongo1 mongo --port 30001 --eval "rs.initiate({_id: 'my-replica-set', members: [{_id: 0, host: 'mongo1:30001'}, {_id: 1, host: 'mongo2:30002'}, {_id: 2, host: 'mongo3:30003'}]})"
  
  # Wait for replica set to initialize
  echo "Waiting for replica set to initialize..."
  sleep 10
  
  # Check replica set status again
  REPLICA_STATUS=$(docker exec mongo1 mongo --port 30001 --eval "rs.status().ok" --quiet || echo "0")
  if [ "$REPLICA_STATUS" != "1" ]; then
    echo "Failed to initialize replica set. Check the logs with 'docker-compose logs mongo1'"
    exit 1
  fi
fi

echo "Replica set is initialized!"

# Build and run the application
echo "Building the application..."
mvn clean package -Dmaven.test.skip=true

echo "Running the application..."
java -jar target/gitbitex-0.0.1-SNAPSHOT.jar

echo "Application started!" 