# Expected Results Testdata Spec

The `expected results` must be described in a file in CSV format conforming to the following spec:

- `;` is used as a separator
- strict quotes have to be used
- header needs to be present according to the table below

| caseId | orderId | resultAmpel |
|--------|---------|-------------|

- entries have to conform to rules according to the table below

| Column      | Data Type | Value Restrictions                                                                                  |
|-------------|-----------|-----------------------------------------------------------------------------------------------------|
| caseId      | String    | not empty                                                                                           |
| orderId     | String    | not empty                                                                                           |
| resultAmpel | Enum      | any of [`AKIN Dialy`, `AKIN1`, `AKIN2`, `AKIN3`, `AKIN1 VANC`, `AKIN2 VANC`, `AKIN3 VANC`] or empty |
