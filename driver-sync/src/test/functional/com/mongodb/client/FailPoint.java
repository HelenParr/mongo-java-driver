/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mongodb.client;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterConnectionMode;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.conversions.Bson;

import java.util.Collections;

import static com.mongodb.client.Fixture.getMongoClientSettingsBuilder;

public final class FailPoint implements AutoCloseable {
    private final BsonDocument failPointDocument;
    private final MongoClient client;
    private final boolean close;

    private FailPoint(final BsonDocument failPointDocument, final MongoClient client, final boolean close) {
        this.failPointDocument = failPointDocument.toBsonDocument();
        this.client = client;
        this.close = close;
    }

    /**
     * @param configureFailPointDoc A document representing {@code configureFailPoint} command to be issued as is via
     * {@link com.mongodb.client.MongoDatabase#runCommand(Bson)}.
     */
    public static FailPoint enable(final BsonDocument configureFailPointDoc, final ServerAddress serverAddress) {
        MongoClientSettings clientSettings = getMongoClientSettingsBuilder()
                .applyToClusterSettings(builder -> builder
                        .mode(ClusterConnectionMode.SINGLE)
                        .hosts(Collections.singletonList(serverAddress)))
                .build();
        MongoClient client = MongoClients.create(clientSettings);
        return enable(configureFailPointDoc, client, true);
    }

    /**
     * @see #enable(BsonDocument, ServerAddress)
     */
    public static FailPoint enable(final BsonDocument configureFailPointDoc, final MongoClient client) {
        return enable(configureFailPointDoc, client, false);
    }

    private static FailPoint enable(final BsonDocument configureFailPointDoc, final MongoClient client, final boolean close) {
        FailPoint result = new FailPoint(configureFailPointDoc, client, close);
        client.getDatabase("admin").runCommand(configureFailPointDoc);
        return result;
    }

    @Override
    public void close() {
        try {
            client.getDatabase("admin").runCommand(new BsonDocument()
                    .append("configureFailPoint", failPointDocument.getString("configureFailPoint"))
                    .append("mode", new BsonString("off")));
        } finally {
            if (close) {
                client.close();
            }
        }
    }
}
