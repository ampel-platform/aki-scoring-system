package de.uni_leipzig.mds.db;

import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import de.uni_leipzig.mds.db.types.MedicalCase;

public interface MedicalCaseWriter {

    ClientNamespacedInsertOneModel createInsertCommand(MedicalCase medicalCase);

}
