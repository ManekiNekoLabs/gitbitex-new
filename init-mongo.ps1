# Stop and remove existing containers
Write-Host "Stopping and removing existing containers..."
docker-compose down -v

# Start MongoDB containers
Write-Host "Starting MongoDB containers..."
docker-compose up -d mongo1 mongo2 mongo3

# Wait for MongoDB to be ready
Write-Host "Waiting for MongoDB to be ready..."
$counter = 0
$maxTries = 30
$ready = $false

while (-not $ready -and $counter -lt $maxTries) {
    Write-Host "Attempt $counter of $maxTries..."
    try {
        $result = docker exec mongo1 mongo --port 30001 --eval "db.stats().ok" --quiet
        if ($result -eq "1") {
            $ready = $true
            Write-Host "MongoDB is ready!"
        } else {
            Write-Host "MongoDB not ready yet. Waiting..."
            Start-Sleep -Seconds 5
            $counter++
        }
    } catch {
        Write-Host "Error connecting to MongoDB: $_"
        Start-Sleep -Seconds 5
        $counter++
    }
}

if (-not $ready) {
    Write-Host "Failed to connect to MongoDB after $maxTries attempts. Exiting."
    exit 1
}

# Initialize replica set
Write-Host "Initializing replica set..."
$initCommand = @"
rs.initiate({
  _id: 'my-replica-set',
  members: [
    { _id: 0, host: 'mongo1:30001' },
    { _id: 1, host: 'mongo2:30002' },
    { _id: 2, host: 'mongo3:30003' }
  ]
});
"@

docker exec mongo1 mongo --port 30001 --eval "$initCommand"

# Wait for replica set to initialize
Write-Host "Waiting for replica set to initialize..."
Start-Sleep -Seconds 10

# Check replica set status
Write-Host "Checking replica set status..."
$statusCommand = "rs.status().ok"
$status = docker exec mongo1 mongo --port 30001 --eval "$statusCommand" --quiet

if ($status -eq "1") {
    Write-Host "Replica set initialized successfully!"
    
    # Start remaining services
    Write-Host "Starting remaining services..."
    docker-compose up -d
    
    Write-Host "All services started. MongoDB replica set is ready."
} else {
    Write-Host "Failed to initialize replica set. Status: $status"
    exit 1
} 