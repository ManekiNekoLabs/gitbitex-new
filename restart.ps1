# Function to check if MongoDB is ready
function Check-Mongo {
    Write-Host "Checking MongoDB connection..."
    try {
        $result = docker exec mongo1 mongo --port 30001 --eval "db.runCommand('ping').ok" --quiet
        return ($result -eq "1")
    } catch {
        Write-Host "MongoDB connection failed: $_"
        return $false
    }
}

# Stop any running containers
Write-Host "Stopping existing containers..."
docker-compose down -v

# Remove any old data
Write-Host "Cleaning up old data..."
if (Test-Path -Path "./data") {
    Remove-Item -Recurse -Force "./data"
}

# Start the containers
Write-Host "Starting containers..."
docker-compose up -d

# Wait for MongoDB to initialize
Write-Host "Waiting for MongoDB to initialize..."
$counter = 0
$maxTries = 30
$mongoReady = $false

while (-not $mongoReady -and $counter -lt $maxTries) {
    Write-Host "Waiting for MongoDB to be ready... ($counter/$maxTries)"
    $mongoReady = Check-Mongo
    if (-not $mongoReady) {
        Start-Sleep -Seconds 10
        $counter++
    }
}

if ($counter -eq $maxTries) {
    Write-Host "MongoDB failed to initialize in time. Check the logs with 'docker-compose logs mongo1'"
    exit 1
}

Write-Host "MongoDB is ready!"

# Check if replica set is initialized
Write-Host "Checking replica set status..."
try {
    $replicaStatus = docker exec mongo1 mongo --port 30001 --eval "rs.status().ok" --quiet
} catch {
    $replicaStatus = "0"
    Write-Host "Error checking replica status: $_"
}

if ($replicaStatus -ne "1") {
    Write-Host "Initializing replica set manually..."
    docker exec mongo1 mongo --port 30001 --eval "rs.initiate({_id: 'my-replica-set', members: [{_id: 0, host: 'mongo1:30001'}, {_id: 1, host: 'mongo2:30002'}, {_id: 2, host: 'mongo3:30003'}]})"
    
    # Wait for replica set to initialize
    Write-Host "Waiting for replica set to initialize..."
    Start-Sleep -Seconds 10
    
    # Check replica set status again
    try {
        $replicaStatus = docker exec mongo1 mongo --port 30001 --eval "rs.status().ok" --quiet
    } catch {
        $replicaStatus = "0"
        Write-Host "Error checking replica status after initialization: $_"
    }
    
    if ($replicaStatus -ne "1") {
        Write-Host "Failed to initialize replica set. Check the logs with 'docker-compose logs mongo1'"
        exit 1
    }
}

Write-Host "Replica set is initialized!"

# Build and run the application
Write-Host "Building the application..."
mvn clean package -Dmaven.test.skip=true

Write-Host "Running the application..."
java -jar target/gitbitex-0.0.1-SNAPSHOT.jar 