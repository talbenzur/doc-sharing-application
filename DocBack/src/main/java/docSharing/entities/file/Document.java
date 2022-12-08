package docSharing.entities.file;

import docSharing.controller.request.UpdateRequest;
import docSharing.entities.User;
import docSharing.repository.UpdateLogRepository;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document")
public class Document extends File {
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "content_id", referencedColumnName = "id")
    private Content content;

    @Transient
    private final List<Integer> activeUsers;

    @Transient
    private UpdateLog lastUpdate;


    public Document() {
        super();
        this.activeUsers = new ArrayList<>();
    }

    public Document(User owner, int parentId, String title) {
        super(owner, parentId, title);
        this.content = new Content();
        this.activeUsers = new ArrayList<>();
    }

    public void setContent(String content) {
        this.content.setContent(content);
    }

    public String getContent() {
        return content.getContent();
    }

    public UpdateLog getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(UpdateLog lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void updateLastLog(UpdateLog updateLog) {
        this.lastUpdate.unite(updateLog);
    }

    public void addActiveUser(int userId) {
        if (!this.activeUsers.contains(userId)) {
            this.activeUsers.add(userId);
        }
    }

    public void removeActiveUser(int userId) {
        if (this.activeUsers.contains(userId)) {
            this.activeUsers.remove(userId);
        }
    }

    public List<Integer> getActiveUsers() {
        return this.activeUsers;
    }

    public boolean isContinuousLog(UpdateLog updateLog) {
        return this.lastUpdate != null && this.lastUpdate.isContinuousLog(updateLog);
    }

    public boolean isActiveUser(int userId) {
        return this.activeUsers.contains(userId);
    }

    public UpdateLog updateContent(UpdateRequest updateRequest) {
        switch (updateRequest.getType()) {
            case APPEND:
                this.content.append(updateRequest.getContent(), updateRequest.getStartPosition());
                break;
            case DELETE:
                this.content.delete(updateRequest.getStartPosition(), updateRequest.getEndPosition());
                break;
            case APPEND_RANGE:
                this.content.appendRange(updateRequest.getContent(), updateRequest.getStartPosition(),
                        updateRequest.getEndPosition());
                break;
            case DELETE_RANGE:
                this.content.deleteRange(updateRequest.getStartPosition(), updateRequest.getEndPosition());
                break;
            default:
                throw new IllegalArgumentException("Unsupported update request type!");
        }

        this.getMetadata().setLastUpdated(LocalDateTime.now());
        return new UpdateLog(updateRequest, LocalDateTime.now(), this);
    }

    @Override
    public String toString() {
        return "Document{" +
                "content=" + content +
                '}';
    }
}