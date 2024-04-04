package de.uni_leipzig.mds.data.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * A factory for creating persistence related clients.
 */
public final class ClientFactory {

    private ClientFactory() {
    }

    public static MongoClient createDBClient(ConnectionString connString) {
        var clientSettings = MongoClientSettings.builder()
                .codecRegistry(createMongoDBCodecRegistry())
                .applyConnectionString(connString)
                .build();

        return MongoClients.create(clientSettings);
    }

    private static CodecRegistry createMongoDBCodecRegistry() {
        var pojoCodecProvider = PojoCodecProvider.builder()
                .register(LaboratoryValue.class, Patient.class, Procedure.class)
                .conventions(List.of(Conventions.ANNOTATION_CONVENTION))
                .build();
        var pojoCodecRegistry = fromProviders(pojoCodecProvider);
        return fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
    }
}
