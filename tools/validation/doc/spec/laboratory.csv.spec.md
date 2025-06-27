# Laboratory Testdata Spec

The `laboratory` testdata must be described in a file in CSV format conforming to the following spec:

- `;` is used as a separator
- strict quotes have to be used
- header needs to be present according to the table below

| documentId | documentVersion | documentPart | sequenceId | caseId | patientId | orderId | documentCreationTimestamp | originatingSystem | analyte | analyteText | analyteValue | analyteUnit | analyteMeasuringTimestamp |
|------------|-----------------|--------------|------------|--------|-----------|---------|---------------------------|-------------------|---------|-------------|--------------|-------------|---------------------------|

- entries have to conform to rules according to the table below

| Column                    | Data Type | Value Restrictions                                                                    |
|---------------------------|-----------|---------------------------------------------------------------------------------------|
| documentId                | String    | not empty                                                                             |
| documentVersion           | String    | not empty                                                                             |
| documentPart              | String    | not empty                                                                             |
| sequenceId                | String    | not empty                                                                             |
| caseId                    | String    | not empty (need a counterpart in [expected results](./expected-results.csv.spec.md))  |
| patientId                 | String    | not empty                                                                             |
| orderId                   | String    | not empty (needs a counterpart in [expected results](./expected-results.csv.spec.md)) |
| documentCreationTimestamp | DateTime  | Format `yyyy-MM-dd HH:mm:ss`                                                          |
| originatingSystem         | String    | not empty                                                                             |
| analyte                   | String    | not empty                                                                             |
| analyteText               | String    | not empty                                                                             |
| analyteValue              | Float     | not NaN                                                                               |
| analyteUnit               | String    | not empty                                                                             |
| analyteMeasuringTimestamp | DateTime  | Format `yyyy-MM-dd HH:mm:ss`                                                          |
