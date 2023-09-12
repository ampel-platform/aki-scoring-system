package de.uni_leipzig.mds;

import de.uni_leipzig.mds.evaluation.Poller;
import de.uni_leipzig.mds.evaluation.PollerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

public class AkiDataPoller implements Poller<AkiData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkiDataPoller.class);

    private static final String AKI_DATA_SQL_QUERY = """
            WITH BASE_CREATININE_VALUES (patient_nr, case_nr, birthdate, vital_state, measurement_time, value_orig, value_numeric)
                     AS (SELECT patient_nr,
                                case_nr,
                                CONVERT(datetime, birthdate) as birthdate,
                                vital_state,
                                measurement_time,
                                value_orig,
                                value_numeric
                         FROM fact_lab_value l
                                  JOIN dim_sample s ON l.sample_id_fk = s.id
                                  JOIN dim_lab_param lp ON l.lab_param_id_fk = lp.id
                                  JOIN dim_sampling sing ON s.sampling_id_fk = sing.id
                                  JOIN dim_patient p ON sing.patient_id_fk = p.id
                                  JOIN fact_patient_admin_encounter fpae ON p.id = fpae.patient_id_fk
                         WHERE param_code = 'LCRE_S'),
                 ENRICHED_CREATININE_VALUES (patient_nr, case_nr, birthdate, vital_state, measurement_time, value_orig,
                                             value_numeric,
                                             creMin48, creMin168)
                     AS (SELECT min48.patient_nr,
                                min48.case_nr,
                                min48.birthdate,
                                min48.vital_state,
                                min48.measurement_time,
                                min48.value_orig,
                                min48.value_numeric,
                                min48.creMin48,
                                min(cv3.value_numeric) as creMin168
                         FROM (SELECT cv1.patient_nr,
                                      cv1.case_nr,
                                      cv1.birthdate,
                                      cv1.vital_state,
                                      cv1.measurement_time,
                                      cv1.value_orig,
                                      cv1.value_numeric,
                                      min(cv2.value_numeric) as creMin48
                               FROM BASE_CREATININE_VALUES cv1
                                        INNER JOIN BASE_CREATININE_VALUES cv2 ON cv2.patient_nr = cv1.patient_nr AND
                                                                                 cv1.measurement_time >= cv2.measurement_time AND
                                                                                 DATEADD(hour, -48, cv1.measurement_time) <=
                                                                                 cv2.measurement_time
                               GROUP BY cv1.patient_nr, cv1.case_nr, cv1.birthdate, cv1.vital_state, cv1.measurement_time,
                                        cv1.value_orig, cv1.value_numeric) as min48
                                  INNER JOIN BASE_CREATININE_VALUES cv3 ON cv3.patient_nr = min48.patient_nr AND
                                                                           min48.measurement_time >= cv3.measurement_time AND
                                                                           DATEADD(hour, -168, min48.measurement_time) <=
                                                                           cv3.measurement_time
                         GROUP BY min48.patient_nr, min48.case_nr, min48.birthdate, min48.vital_state, min48.measurement_time,
                                  min48.value_orig, min48.value_numeric, min48.creMin48),
                 VANCOMYCIN (patient_nr, measurement_time)
                     AS (SELECT patient_nr,
                                measurement_time
                         FROM fact_lab_value l
                                  JOIN dim_sample s ON l.sample_id_fk = s.id
                                  JOIN dim_lab_param lp ON l.lab_param_id_fk = lp.id
                                  JOIN dim_sampling sing ON s.sampling_id_fk = sing.id
                                  JOIN dim_patient p ON sing.patient_id_fk = p.id
                         WHERE param_code IN ('VANC-K_S', 'VANC-S_S', 'VANC-T_S', 'VANC-U_S')),
                 DIALYSIS (patient_nr, execution_time)
                     AS (SELECT patient_nr,
                                CONVERT(datetime, CAST(dTim.day as VARCHAR) + 'T' +
                                                  CAST(fp.procedure_start as VARCHAR(12))) as execution_time
                         FROM fact_procedure fp
                                  JOIN dim_procedure dPro ON fp.procedure_id_fk = dPro.id
                                  JOIN dim_patient dPat ON fp.patient_id_fk = dPat.id
                                  JOIN dim_time dTim ON fp.time_id_fk = dTim.id
                         WHERE ops_code IN ('8-853.71',
                                            '8-854.2',
                                            '8-854.3',
                                            '8-854.4',
                                            '8-854.60',
                                            '8-854.70',
                                            '8-854.71',
                                            '8-854.72',
                                            '8-855.70',
                                            '8-857',
                                            '8-85a.03'))
            SELECT creDia.patient_nr,
                   creDia.case_nr,
                   creDia.birthdate,
                   creDia.vital_state,
                   creDia.measurement_time,
                   creDia.value_orig,
                   creDia.value_numeric,
                   creDia.creMin48,
                   creDia.creMin168,
                   creDia.latest_dialysis_execution_time_last_7_days,
                   --- agg function does not matter -> we just need a value within the column because all the other checks already ran during the join phase
                   max(v.measurement_time) as latest_vancomycin_time_last_7_days
            FROM (SELECT cv.patient_nr,
                         cv.case_nr,
                         cv.birthdate,
                         cv.vital_state,
                         cv.measurement_time,
                         cv.value_orig,
                         cv.value_numeric,
                         cv.creMin48,
                         cv.creMin168,
                         --- agg function does not matter -> we just need a value within the column because all the other checks already ran during the join phase
                         max(d.execution_time) as latest_dialysis_execution_time_last_7_days
                  FROM ENRICHED_CREATININE_VALUES cv
                           LEFT JOIN DIALYSIS d ON d.patient_nr = cv.patient_nr AND
                                                   cv.measurement_time >= d.execution_time AND
                                                   DATEADD(hour, -168, cv.measurement_time) <= d.execution_time
                  GROUP BY cv.patient_nr, cv.case_nr, cv.birthdate, cv.vital_state, cv.measurement_time, cv.value_orig,
                           cv.value_numeric, cv.creMin48, cv.creMin168) AS creDia
                     LEFT JOIN VANCOMYCIN v ON creDia.patient_nr = v.patient_nr AND
                                               creDia.measurement_time >= v.measurement_time AND
                                               DATEADD(hour, -168, creDia.measurement_time) <= v.measurement_time
            GROUP BY creDia.patient_nr, creDia.case_nr, creDia.birthdate, creDia.vital_state, creDia.measurement_time,
                     creDia.value_orig,
                     creDia.value_numeric, creDia.creMin48, creDia.creMin168, creDia.latest_dialysis_execution_time_last_7_days
            ORDER BY creDia.patient_nr, creDia.measurement_time;
            """;

    private final DataSource dataSource;

    public AkiDataPoller(DataSource dataSource) {
        this.dataSource = requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Stream<AkiData> poll() throws PollerException {
        LOGGER.info("running poller...");
        Connection conn;
        try {
            conn = dataSource.getConnection();
        } catch (SQLException e) {
            throw new PollerException("could not establish connection to data source", e);
        }

        try {
            var resultSet = conn.createStatement().executeQuery(AKI_DATA_SQL_QUERY);
            return StreamSupport.stream(new ResultSetIterator(resultSet), false)
                    .onClose(() -> {
                        LOGGER.debug("trying to close connection after stream usage");
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            throw new RuntimeException("could not close connection to data source after stream usage",
                                    e);
                        }
                    });
        } catch (SQLException e) {
            try {
                conn.close();
                throw new PollerException("could not fetch data from the database", e);
            } catch (SQLException ex) {
                LOGGER.warn("could not close connection to data source", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * A {@link Spliterator} for iterating over a {@link ResultSet} as used in a {@link Stream}.
     *
     * @param resultSet The result set over which to iterate.
     */
    private record ResultSetIterator(ResultSet resultSet) implements Spliterator<AkiData> {
        private ResultSetIterator(ResultSet resultSet) {
            this.resultSet = requireNonNull(resultSet, "result set must not be null");
        }

        @Override
        public boolean tryAdvance(Consumer<? super AkiData> action) {
            try {
                if (!resultSet.next()) return false;

                while (!evaluableRecordAtCurrentPosition()) {
                    LOGGER.warn(("result set entry for patient `%s` on case `%s` measured at `%s` lacks creatinine " +
                            "value - thus, dropping it").formatted(
                            resultSet.getString("patient_nr"),
                            resultSet.getString("case_nr"),
                            resultSet.getString("measurement_time")
                    ));
                    if (!resultSet.next()) return false;
                }
                action.accept(createEvaluationRecordForCurrentPosition());
                return true;
            } catch (SQLException e) {
                LOGGER.error("cannot advance in result set", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public Spliterator<AkiData> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return Spliterator.NONNULL | Spliterator.IMMUTABLE;
        }

        private AkiData createEvaluationRecordForCurrentPosition() throws SQLException {
            return AkiData.builder()
                    .patientNumber(resultSet.getString("patient_nr"))
                    .patientCaseNumber(resultSet.getString("case_nr"))
                    .patientBirthdate(resultSet.getTimestamp("birthdate"))
                    .isDeceased(resultSet.getString("vital_state").equals("0"))
                    .analyteMeasurementTime(resultSet.getTimestamp("measurement_time"))
                    .creatinineValue(resultSet.getFloat("value_numeric"))
                    .minCreatinineValue48Hours(resultSet.getFloat("creMin48"))
                    .minCreatinineValue168Hours(resultSet.getFloat("creMin168"))
                    .hadDialysisInLast168Hours(resultSet.getObject(
                            "latest_dialysis_execution_time_last_7_days") != null)
                    .hadVancomycinInLast168Hours(resultSet.getObject(
                            "latest_vancomycin_time_last_7_days") != null)
                    .build();
        }

        private boolean evaluableRecordAtCurrentPosition() throws SQLException {
            return resultSet.getObject("value_numeric") != null;
        }
    }
}
