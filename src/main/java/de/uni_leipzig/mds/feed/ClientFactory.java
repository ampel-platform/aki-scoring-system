package de.uni_leipzig.mds.feed;

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

public final class ClientFactory {

    private ClientFactory() {
    }

    public static MongoClient defaultStorageClient(String applicationName, ConnectionString connectionString) {
        var clientSettings = MongoClientSettings.builder()
                .codecRegistry(createCodecRegistry())
                .applicationName(applicationName)
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(clientSettings);
    }

    private static CodecRegistry createCodecRegistry() {
        var pojoCodecProvider = PojoCodecProvider.builder()
                .register(PatientCaseFeedDTO.class, LaboratoryValueDTO.class, PatientDTO.class, ProcedureDTO.class)
                .conventions(List.of(Conventions.ANNOTATION_CONVENTION))
                .build();
        var pojoCodecRegistry = fromProviders(pojoCodecProvider);
        return fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
    }
}
