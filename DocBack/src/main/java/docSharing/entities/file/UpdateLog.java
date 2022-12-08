package docSharing.entities.file;

import docSharing.controller.request.UpdateRequest;

import javax.persistence.*;
import java.time.LocalDateTime;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Entity
@Table(name = "documents_update_logs")
public class UpdateLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private LocalDateTime timestamp;
    private int userId;
    private UpdateRequest.UpdateType type;
    private String content;
    private int startPosition;
    private int endPosition;

    @ManyToOne
    @JoinColumn(name = "document_id", referencedColumnName = "id")
    private Document document;

    @Transient
    private final int MAX_SECONDS_TO_UNITE = 5;

    public UpdateLog() {
    }

    public UpdateLog(UpdateRequest updateRequest, LocalDateTime timestamp, Document document) {
        this.userId = updateRequest.getUserId();
        this.type = updateRequest.getType();
        this.content = updateRequest.getContent();
        this.startPosition = updateRequest.getStartPosition();
        this.endPosition = updateRequest.getEndPosition();
        this.timestamp = timestamp;
        this.document = document;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getUserId() {
        return userId;
    }

    public UpdateRequest.UpdateType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setType(UpdateRequest.UpdateType type) {
        this.type = type;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public boolean isContinuousLog(UpdateLog updateLog) {
        return (updateLog != null &&
                isSameUser(updateLog) &&
                isMatchingType(updateLog) &&
                isFromLastXSeconds(updateLog, MAX_SECONDS_TO_UNITE) &&
                isContinuousIndex(updateLog));
    }

    public void unite(UpdateLog updateLog) {
        int previousStart = this.getStartPosition();
        int previousEnd = this.getEndPosition();
        int currentStart = updateLog.getStartPosition();
        int currentEnd = updateLog.getEndPosition();

        switch(this.type) {
            case APPEND:
            case APPEND_RANGE:
                appendContent(updateLog);
                this.setStartPosition(min(previousStart, currentStart));
                this.setEndPosition(max(previousEnd, currentEnd));
                break;
            case DELETE:
                this.setEndPosition(min(previousEnd, currentEnd));
                break;
            case DELETE_RANGE:
                this.setStartPosition(min(previousStart, currentEnd));
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Update type: %s is not supported!", updateLog.getType()));
        }

        this.setTimestamp(updateLog.getTimestamp());
    }

    private void appendContent(UpdateLog updateLog) {
        String previousContent = this.getContent();
        int previousStart = this.getStartPosition();
        int currentStart = updateLog.getStartPosition();
        int currentEnd = updateLog.getEndPosition();

        this.setContent(previousContent.substring(0, currentStart - previousStart)
                + updateLog.getContent()
                + previousContent.substring(min(previousContent.length(), currentEnd - previousStart)));
    }

    private boolean isSameUser(UpdateLog updateLog) {
        return this.getUserId() == updateLog.getUserId();
    }

    private boolean isMatchingType(UpdateLog updateLog) {
        switch (this.getType()) {
            case APPEND:
            case APPEND_RANGE:
                return updateLog.getType() == UpdateRequest.UpdateType.APPEND;

            case DELETE:
            case DELETE_RANGE:
                return updateLog.getType() == UpdateRequest.UpdateType.DELETE;

            default:
                throw new IllegalArgumentException(
                        String.format("Update type: %s is not supported!", this.getType()));
        }
    }

    private boolean isFromLastXSeconds(UpdateLog updateLog, int seconds) {
        return this.getTimestamp().isAfter(updateLog.getTimestamp().minusSeconds(seconds));
    }

    private boolean isContinuousIndex(UpdateLog updateLog) {
        int previousStart = this.getStartPosition();
        int previousEnd = this.getEndPosition();
        int currentStart = updateLog.getStartPosition();

        switch (this.getType()) {
            case APPEND:
            case DELETE:
                return currentStart >= previousStart && currentStart <= previousEnd + 1;

            case APPEND_RANGE:
            case DELETE_RANGE:
                return currentStart >= previousStart &&
                        currentStart <= previousStart + this.getContent().length();
            default:
                throw new IllegalArgumentException(
                        String.format("Update type: %s is not supported!", updateLog.getType()));
        }
    }

    @Override
    public String toString() {
        return "UpdateLog{" +
                "timestamp=" + timestamp +
                ", userId=" + userId +
                ", type=" + type +
                ", content='" + content + '\'' +
                ", startPosition=" + startPosition +
                ", endPosition=" + endPosition +
                ", document=" + document +
                '}';
    }
}
