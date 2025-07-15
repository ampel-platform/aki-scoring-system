package de.uni_leipzig.mds.db;

import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import de.uni_leipzig.mds.db.types.Patient;

public interface PatientWriter {

    ClientNamespacedInsertOneModel createInsertCommand(Patient patient);

}
