package docSharing.entities.file;

import docSharing.controller.request.UpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateLogTests {
    private UpdateLog updateLog;

    @BeforeEach
    void beforeEach() {
        this.updateLog = createArbitraryUpdateLog();
    }

    @Test
    @DisplayName("isContinuousLog() returns false when user is different")
    void isContinuousLog_DifferentUser_ReturnsFalse() {
        int userId1 = 1;
        int userId2 = 2;
        UpdateLog arbitraryLog = createArbitraryUpdateLog();
        this.updateLog.setUserId(userId1);
        arbitraryLog.setUserId(userId2);

        assertFalse(this.updateLog.isContinuousLog(arbitraryLog),
                "isContinuousLog() should return false for different users");
    }

    @Test
    @DisplayName("isContinuousLog() returns false when type is different")
    void isContinuousLog_DifferentType_ReturnsFalse() {
        UpdateRequest.UpdateType type1 = UpdateRequest.UpdateType.APPEND;
        UpdateRequest.UpdateType type2 = UpdateRequest.UpdateType.DELETE;

        UpdateLog arbitraryLog = createArbitraryUpdateLog();
        this.updateLog.setType(type1);
        arbitraryLog.setType(type2);

        assertFalse(this.updateLog.isContinuousLog(arbitraryLog),
                "isContinuousLog() should return false for different types");
    }

    @Test
    @DisplayName("isContinuousLog() returns false when more than 5 seconds passed")
    void isContinuousLog_DistantTimeStamp_ReturnsFalse() {
        LocalDateTime timestamp1 = LocalDateTime.now();
        LocalDateTime timestamp2 = timestamp1.plusSeconds(5);

        UpdateLog arbitraryLog = createArbitraryUpdateLog();
        this.updateLog.setTimestamp(timestamp1);
        arbitraryLog.setTimestamp(timestamp2);

        assertFalse(this.updateLog.isContinuousLog(arbitraryLog),
                "isContinuousLog() should return false for distant timestamps");
    }

    @Test
    @DisplayName("isContinuousLog() returns false when indexes are not continuous")
    void isContinuousLog_NonContinuousIndexes_ReturnsFalse() {
        UpdateLog arbitraryLog = createArbitraryUpdateLog();
        this.updateLog.setStartPosition(20);
        this.updateLog.setEndPosition(22);
        arbitraryLog.setStartPosition(24);
        arbitraryLog.setEndPosition(25);

        assertFalse(this.updateLog.isContinuousLog(arbitraryLog),
                "isContinuousLog() should return false when indexes are not continuous");
    }

    @Test
    @DisplayName("isContinuousLog() returns true when logs are continuous")
    void isContinuousLog_ContinuousLogs_ReturnsTrue() {
        UpdateLog arbitraryLog = createArbitraryUpdateLog();
        this.updateLog.setStartPosition(20);
        this.updateLog.setEndPosition(22);
        arbitraryLog.setStartPosition(23);
        arbitraryLog.setEndPosition(24);

        this.updateLog.setType(UpdateRequest.UpdateType.APPEND);
        arbitraryLog.setType(UpdateRequest.UpdateType.APPEND);

        int userId = 1;
        this.updateLog.setUserId(userId);
        arbitraryLog.setUserId(userId);

        LocalDateTime timestamp1 = LocalDateTime.now();
        LocalDateTime timestamp2 = timestamp1.plusSeconds(3);
        this.updateLog.setTimestamp(timestamp1);
        arbitraryLog.setTimestamp(timestamp2);

        assertTrue(this.updateLog.isContinuousLog(arbitraryLog),
                "isContinuousLog() should return true when logs are continuous");
    }

    @Test
    @DisplayName("unite() updates log's content for append UpdateLogs")
    void unite_AppendLogs_UpdatesContent() {
        List<UpdateLog> continuousLogs = getContinuousLogs(UpdateRequest.UpdateType.APPEND,
                "Lior", "Mathan");
        continuousLogs.get(0).unite(continuousLogs.get(1));

        assertEquals("LiorMathan", continuousLogs.get(0).getContent(),
                "isContinuousLog() should update content to be: LiorMathan");
    }

    @Test
    @DisplayName("unite() updates log's indexes for append UpdateLogs")
    void unite_AppendLogs_UpdatesIndexes() {
        List<UpdateLog> continuousLogs = getContinuousLogs(UpdateRequest.UpdateType.APPEND,
                "Lior", "Mathan");
        continuousLogs.get(0).unite(continuousLogs.get(1));

        assertEquals(20, continuousLogs.get(0).getStartPosition(),
                "isContinuousLog() should update startPosition to be 20");

        assertEquals(30, continuousLogs.get(0).getEndPosition(),
                "isContinuousLog() should update EndPosition to be 30");
    }

    @Test
    @DisplayName("unite() updates log's indexes for delete UpdateLogs")
    void unite_DeleteLogs_UpdatesIndexes() {
        List<UpdateLog> continuousLogs = getContinuousLogs(UpdateRequest.UpdateType.DELETE,
                "Lior", "Mathan");
        continuousLogs.get(0).setStartPosition(20);
        continuousLogs.get(0).setEndPosition(18);
        continuousLogs.get(1).setStartPosition(18);
        continuousLogs.get(1).setEndPosition(17);

        continuousLogs.get(0).unite(continuousLogs.get(1));

        assertEquals(20, continuousLogs.get(0).getStartPosition(),
                "isContinuousLog() should update startPosition to be 20");

        assertEquals(17, continuousLogs.get(0).getEndPosition(),
                "isContinuousLog() should update EndPosition to be 17");
    }

    List<UpdateLog> getContinuousLogs(UpdateRequest.UpdateType type, String content1, String content2) {
        UpdateLog log1 = createArbitraryUpdateLog();
        UpdateLog log2 = createArbitraryUpdateLog();

        log1.setStartPosition(20);
        log1.setEndPosition(20 + content1.length());
        log2.setStartPosition(20 + content1.length());
        log2.setEndPosition(20 + content1.length() + content2.length());

        log1.setType(type);
        log2.setType(type);

        int userId = 1;
        log1.setUserId(userId);
        log2.setUserId(userId);

        LocalDateTime timestamp1 = LocalDateTime.now();
        LocalDateTime timestamp2 = timestamp1.plusSeconds(3);
        log1.setTimestamp(timestamp1);
        log2.setTimestamp(timestamp2);

        log1.setContent(content1);
        log2.setContent(content2);

        assertTrue(log1.isContinuousLog(log2));

        List<UpdateLog> logs = new ArrayList();
        logs.add(log1);
        logs.add(log2);

        return logs;
    }

    UpdateLog createArbitraryUpdateLog() {
        UpdateRequest updateRequest = new UpdateRequest.UpdateRequestBuilder()
                .setUserId(1).setType(UpdateRequest.UpdateType.APPEND)
                .setStartPosition(10).setEndPosition(12).build();
        return new UpdateLog(updateRequest, LocalDateTime.now(), null);
    }
}
