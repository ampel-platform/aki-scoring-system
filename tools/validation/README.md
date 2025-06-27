# AKI Validation

This is a dedicated tool for validating the [AKI scoring system](https://github.com/AMPEL-CDSS-ORG/aki-service).

## Settings

It's possible to alter settings regarding the validation using environment variables. See the following table for an
overview of available settings and their corresponding environment variables:

| EnvVar                  | Description                                                              |
|-------------------------|--------------------------------------------------------------------------|
| KAFKA_BOOTSTRAP_SERVERS | Comma separated list of Kafka servers used for bootstrapping.            |
| PATIENT_DATA_SOURCE     | Path to a file containing `patient` related testdata in CSV format.      |
| MED_CASE_DATA_SOURCE    | Path to a file containing `medical case` related testdata in CSV format. |
| PROCEDURE_DATA_SOURCE   | Path to a file containing `procedure` related testdata in CSV format.    |
| LABORATORY_DATA_SOURCE  | Path to a file containing `laboratory` related testdata in CSV format.   |
| EXPECTED_RESULTS_SOURCE | Path to a file containing `expected results` in CSV format.              |

> [!IMPORTANT]
> Additional information on the expected format of all testdata files can be found [here](doc/spec/).

## How to run the validation?

> [!CAUTION]
> Do not run this against an instance of the AMPEL platform being in production if you can't gauge the consequences as
> this tool injects testdata into the platform. This might have side effects for downstream systems.

Every release of the scoring system itself is accompanied by a release of this validation tool.
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

__Note:__ You can also simulate a test environment using the [docker-compose.yml](docker-compose.yml) file in this
directory.

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
