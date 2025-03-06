// MongoDB initialization script
print("Starting MongoDB replica set initialization...");
try {
  // Check if replica set is already initialized
  var status = rs.status();
  if (status.ok) {
    print("Replica set is already initialized.");
  } else {
    print("Initializing replica set...");
    rs.initiate({
      _id: "my-replica-set",
      members: [
        { _id: 0, host: "mongo1:30001" },
        { _id: 1, host: "mongo2:30002" },
        { _id: 2, host: "mongo3:30003" }
      ]
    });
    print("Replica set initialization completed.");
  }
} catch (err) {
  print("Error during initialization: " + err);
  print("Initializing replica set...");
  rs.initiate({
    _id: "my-replica-set",
    members: [
      { _id: 0, host: "mongo1:30001" },
      { _id: 1, host: "mongo2:30002" },
      { _id: 2, host: "mongo3:30003" }
    ]
  });
  print("Replica set initialization completed.");
} 