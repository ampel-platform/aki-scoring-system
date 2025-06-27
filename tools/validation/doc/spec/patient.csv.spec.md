# Patient Testdata Spec

The `procedure` testdata must be described in a file in CSV format conforming to the following spec:

- `;` is used as a separator
- strict quotes have to be used
- header needs to be present according to the table below

| patientId | sex | dateOfBirth | deceased | dischargeTimestamp | cancellationDate | creationDate | updateDate |
|-----------|-----|-------------|----------|--------------------|------------------|--------------|------------|

- entries have to conform to rules according to the table below

| Column             | Data Type | Value Restrictions                                         |
|--------------------|-----------|------------------------------------------------------------|
| patientId          | String    | not empty                                                  |
| sex                | Enum      | any of [`1`, `2`, `3`]                                     |
| dateOfBirth        | Date      | Format `yyyy-MM-dd`                                        |
| deceased           | String    | either ` ` or `X` (`X` resembles the boolean value `true`) |
| dischargeTimestamp | DateTime  | Format `yyyy-MM-dd HH:mm:ss`                               |
| cancellationDate   | Date      | Format `yyyy-MM-dd`                                        |
| creationDate       | Date      | Format `yyyy-MM-dd`                                        |
| updateDate         | Date      | Format `yyyy-MM-dd`                                        |
