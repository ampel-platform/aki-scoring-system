# Procedure Testdata Spec

The `procedure` testdata must be described in a file in CSV format conforming to the following spec:

- `;` is used as a separator
- strict quotes have to be used
- header needs to be present according to the table below

| procedureId | caseId | procedureCode | performedOnTimestamp | cancellationDate | updateDate |
|-------------|--------|---------------|----------------------|------------------|------------|

- entries have to conform to rules according to the table below

| Column               | Data Type | Value Restrictions                                                                   |
|----------------------|-----------|--------------------------------------------------------------------------------------|
| procedureId          | String    | not empty                                                                            |
| caseId               | String    | not empty (need a counterpart in [expected results](./expected-results.csv.spec.md)) |
| procedureCode        | String    | not empty                                                                            |
| performedOnTimestamp | DateTime  | Format `yyyy-MM-dd HH:mm:ss`                                                         |
| cancellationDate     | Date      | Format `yyyy-MM-dd`                                                                  |
| updateDate           | Date      | Format `yyyy-MM-dd`                                                                  |
