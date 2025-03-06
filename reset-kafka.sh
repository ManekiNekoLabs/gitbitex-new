#!/bin/bash

# Stop Kafka container
echo "Stopping Kafka container..."
docker stop kafka

# Remove Kafka container
echo "Removing Kafka container..."
docker rm kafka

# Start Kafka container again
echo "Starting Kafka container..."
docker-compose up -d kafka

# Wait for Kafka to start
echo "Waiting for Kafka to start..."
sleep 10

# Create topics
echo "Creating topics..."
docker exec kafka kafka-topics.sh --create --topic matching-engine-command --bootstrap-server localhost:19092 --partitions 1 --replication-factor 1
docker exec kafka kafka-topics.sh --create --topic matching-engine-message --bootstrap-server localhost:19092 --partitions 1 --replication-factor 1

echo "Kafka topics reset complete!" 