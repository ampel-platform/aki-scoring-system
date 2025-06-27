# Expected Results Testdata Spec

The `expected results` must be described in a file in CSV format conforming to the following spec:

- `;` is used as a separator
- strict quotes have to be used
- header needs to be present according to the table below

| X.BCX.S_HC_FALNR | X.BCX.S_HC_LA_ANR | X.BCX.S_HC_RGL_KS |
|------------------|-------------------|-------------------|

- entries have to conform to rules according to the table below

| Column            | Data Type | Value Restrictions                                                                                  |
|-------------------|-----------|-----------------------------------------------------------------------------------------------------|
| X.BCX.S_HC_FALNR  | String    | not empty                                                                                           |
| X.BCX.S_HC_LA_ANR | String    | not empty                                                                                           |
| X.BCX.S_HC_RGL_KS | Enum      | any of [`AKIN Dialy`, `AKIN1`, `AKIN2`, `AKIN3`, `AKIN1 VANC`, `AKIN2 VANC`, `AKIN3 VANC`] or empty |
