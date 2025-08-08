///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.tinylog:slf4j-tinylog:2.7.0
//DEPS org.tinylog:tinylog-impl:2.7.0
//DEPS io.vertx:vertx-mongo-client:5.0.2


import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;


public class Reproducer {
    static {
        System.setProperty("tinylog.writer.level", "info");
        System.setProperty("tinylog.writer.format", "{date: HH:mm:ss} {level} {file}:{line}\t{message}");
    }
    private static final Logger LOG = LoggerFactory.getLogger(Reproducer.class);

    public static void main(String... args) throws Exception{
        System.out.println(InetAddress.getByName("mongo1.mongo.local")); // sanity check, this should output mongo1.mongo.local/172.28.0.2

        Vertx vertx = Vertx.vertx();

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                vertx.close().toCompletionStage().toCompletableFuture().join()));

        vertx.deployVerticle(ctx -> {

                    LOG.info("Creating mongo client");
                    MongoClient mongo = MongoClient.create(vertx, new JsonObject()
                            .put("connection_string", "mongodb+srv://cluster.mongo.local/test?tls=false&replicaSet=rs0")
                            .put("db_name", "test"));
                    LOG.info("Created mongo client");

                    return mongo.getCollections()
                            // success message can be seen in console when using EVENT_LOOP, but not when using VIRTUAL_THREAD
                            .onSuccess(collectionNames -> LOG.info("Found collections {}", collectionNames))
                            .onFailure(e -> LOG.error("Unable to retrieve collections", e));

                }, new DeploymentOptions().setThreadingModel(ThreadingModel.EVENT_LOOP))
                .onComplete(__ -> vertx.close());
    }
}
