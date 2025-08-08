# Reproducer

## Overview
Set up a mongo cluster + DNS server with docker-compose.
The DNS server is configured with an SRV record which points to the 3 mongo instances.

Spin up another container that connects to mongo via `mongo+srv://` connection string and does a query.

1. Spin up mongo cluster + DNS server
```sh
docker compose up -d
```

Confirm that the DNS server responds accordingly (172.28.0.10 = dns server docker ip)
```sh
dig @172.28.0.10 SRV _mongodb._tcp.cluster.mongo.local # should output mongo{1..3}.mongo.local
dig @172.28.0.10 google.com # should output a public ip
```

2. Configure mongo cluster + add some data
```sh
docker exec -it mongo1 mongosh

test> rs.initiate({_id: "rs0", members: [{ _id: 0, host: "mongo1.mongo.local:27017" },{ _id: 1, host: "mongo2.mongo.local:27017" },{ _id: 2, host: "mongo3.mongo.local:27017" }]})

// output shuld be { ok: 1 }

// add some data (This sometimes failed with MongoServerError[NotWritablePrimary]: not primary. If it does, just run the insert again)
rs0 [direct: primary] test> db.reproducer.insertOne({'sample':'data'})

// sanity check
rs0 [direct: primary] test> db.getCollectionNames()
// should output [ 'reproducer' ]

```
Exit the container.

3. Run the reproducer
```sh
docker run --rm -it --network vertx-mongo-reproducer_mongo_net --dns 172.28.0.10 -v ./Reproducer.java:/Reproducer.java jbangdev/jbang run /Reproducer.java
```

When using `EVENT_LOOP` `Found collections [reproducer]` is printed, but when using `VIRTUAL_THREAD` it is not.