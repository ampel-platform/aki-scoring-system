# Medical Case Testdata Spec

The `medical case` testdata must be described in a file in CSV format conforming to the following spec:

- `;` is used as a separator
- strict quotes have to be used
- header needs to be present according to the table below

| caseId | patientId | status | creationDate | cancellationDate | updateDate |
|--------|-----------|--------|--------------|------------------|------------|

- entries have to conform to rules according to the table below

| Column           | Data Type | Value Restrictions                                                                   |
|------------------|-----------|--------------------------------------------------------------------------------------|
| caseId           | String    | not empty (need a counterpart in [expected results](./expected-results.csv.spec.md)) |
| patientId        | String    | not empty                                                                            |
| status           | Enum      | any of [`E`, `I`, `P`]                                                               |
| creationDate     | Date      | Format `yyyy-MM-dd`                                                                  |
| cancellationDate | Date      | Format `yyyy-MM-dd`                                                                  |
| updateDate       | Date      | Format `yyyy-MM-dd`                                                                  |
