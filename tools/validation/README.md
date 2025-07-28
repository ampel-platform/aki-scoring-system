# AKI Validation

This is a dedicated tool for validating the [AKI scoring system](https://github.com/AMPEL-CDSS-ORG/aki-service).

## Settings

It's possible to alter settings regarding the validation using environment variables. See the following table for an
overview of available settings and their corresponding environment variables:

| EnvVar                  | Description                                                              | Default                                                                            |
|-------------------------|--------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| KAFKA_BOOTSTRAP_SERVERS | Comma separated list of Kafka servers used for bootstrapping.            |                                                                                    |
| PATIENT_DATA_SOURCE     | Path to a file containing `patient` related testdata in CSV format.      | [bundled `patient.csv`](src/main/resources/testdata/patients.csv)                  |
| MED_CASE_DATA_SOURCE    | Path to a file containing `medical case` related testdata in CSV format. | [bundled `medical-case.csv`](src/main/resources/testdata/medical-cases.csv)        |
| PROCEDURE_DATA_SOURCE   | Path to a file containing `procedure` related testdata in CSV format.    | [bundled `procedure.csv`](src/main/resources/testdata/procedures.csv)              |
| LABORATORY_DATA_SOURCE  | Path to a file containing `laboratory` related testdata in CSV format.   | [bundled `laboratory.csv`](src/main/resources/testdata/laboratory-evaluations.csv) |
| EXPECTED_RESULTS_SOURCE | Path to a file containing `expected results` in CSV format.              | [bundled `expected-results.csv`](src/main/resources/testdata/expected-results.csv) |

> [!IMPORTANT]
> Additional information on the expected format of all testdata files can be found [here](doc/spec/).

## How to run the validation?

### In a standalone validation platform environment

If you simply want to check whether the scoring system works as expected by running the validation, you can do that by:

1. Download the `docker-compose-validation.yml` asset from
   the [release page](https://github.com/AMPEL-CDSS-ORG/aki-service/releases) of this repository from GitHub
2. Run the validation using
   ```shell
   docker compose -f docker-compose-validation.yml up -d
   ```
3. Wait until the validation service exits (successful validation terminates the service with a status code of 0)
4. Tear down the environment using
   ```shell
   docker compose -f docker-compose-validation.yml down -v
   ```

### In an existing platform environment

> [!CAUTION]
> Do not run this against an instance of the AMPEL platform being in production if you can't gauge the consequences as
> this tool injects testdata into the platform. This might have side effects for downstream systems.

To validate the AKI scoring system using an existing instance of the AMPEL platform that's also running the AKI scoring
system, run the following command (__make sure to substitute placeholders!__):

```shell
docker run \
    --name "aki-validation" \
    --rm \
    --network host \
    -v <PATH_TO_PATIENT_TESTDATA_CSV>:/tmp/NPAT.testdata.csv:ro \
    -v <PATH_TO_MEDICALCASE_TESTDATA_CSV>:/tmp/NFAL.testdata.csv:ro \
    -v <PATH_TO_PROCEDURE_TESTDATA_CSV>:/tmp/NICP.testdata.csv:ro \
    -v <PATH_TO_LABORATORY_TESTDATA_CSV>:/tmp/N2LABOR.testdata.csv:ro \
    -v <PATH_TO_EXPECTED_RESULTS_TESTDATA_CSV>:/tmp/Output.testdata.csv:ro \
    -e KAFKA_BOOTSTRAP_SERVERS="<KAFKA_PLAINTEXT_URL>" \
    -e PATIENT_DATA_SOURCE="/tmp/NPAT.testdata.csv" \
    -e MED_CASE_DATA_SOURCE="/tmp/NFAL.testdata.csv" \
    -e PROCEDURE_DATA_SOURCE="/tmp/NICP.testdata.csv" \
    -e LABORATORY_DATA_SOURCE="/tmp/N2LABOR.testdata.csv" \
    -e EXPECTED_RESULTS_SOURCE="/tmp/Output.testdata.csv" \
    ghcr.io/ampel-cdss-org/aki-service-validation:<VERSION>
```

> [!NOTE]  
> You can completely leave out mounts and environment variables except `KAFKA_BOOTSTRAP_SERVERS`. In this case the
> validation will simply run with bundled testdata.

## How does it work?

The validation is split up in the following steps:

1. Reading testdata

   Testdata is read from the specified CSV files.


2. Filtering testdata

   Testdata that has been read is now filtered in order to remove malformed data entries (e.g. laboratory entries that
   do not resemble a numeric value).


3. Sorting testdata

   This is a crucial step in order to sort testdata by a time value. Sorting is done so that information can be uploaded
   to Kafka in the same order as their corresponding action happened in reality (historical data).


4. Uploading testdata to Kafka

   All testdata entries are now uploaded to Kafka using the correct topic for each type. This is a sequential action and
   deliberately designed to be a bit slow in order to mimic their real life event succession.

   This is going to trigger the scoring system to calculate results and write them back to Kafka.


5. Reading back results from Kafka

   Results produced by the scoring system are read from Kafka during this step. This is done as long there are no new
   results for at least 10 seconds.


6. Evaluating results

   Evaluates whether the results that have been read from Kafka match the expected results. These expected results have
   been defined in one of the input files.

The tool will stop with an exit code other than `0` if the validation failed.
