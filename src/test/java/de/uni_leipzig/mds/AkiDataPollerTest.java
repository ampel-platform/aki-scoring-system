package de.uni_leipzig.mds;

import de.uni_leipzig.mds.evaluation.PollerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AkiDataPollerTest {

    @Mock
    private ResultSet mockResultSet;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Connection mockConnection;

    @Mock
    private DataSource mockDataSource;

    @InjectMocks
    private AkiDataPoller dataPoller;

    @Test
    public void stream_should_be_empty_if_result_set_is_empty() throws SQLException, PollerException {
        doReturn(mockConnection).when(mockDataSource).getConnection();
        when(mockConnection.createStatement().executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        var polledDataStream = dataPoller.poll();
        assertTrue(polledDataStream.toList().isEmpty());
    }

    // TODO: Move this to an integration test later on. Mocking just for a log message is not the right thing to do!
    @Test
    public void stream_should_not_contain_entries_without_creatinine_value() throws SQLException, PollerException {
        doReturn(mockConnection).when(mockDataSource).getConnection();
        when(mockConnection.createStatement().executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getObject("value_numeric")).thenReturn(null);
        when(mockResultSet.getString(anyString())).thenReturn(null);

        var polledDataStream = dataPoller.poll();
        assertTrue(polledDataStream.toList().isEmpty());
    }
}
