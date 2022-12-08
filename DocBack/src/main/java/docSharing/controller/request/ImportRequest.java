package docSharing.controller.request;

import docSharing.entities.permission.Permission;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public class ImportRequest {
    private String path;
    private int ownerId;
    private int parentId;

    public ImportRequest(String path, int ownerId, int parentId) {
        this.path = path;
        this.ownerId = ownerId;
        this.parentId = parentId;
    }

    public ImportRequest() {
    }

    public String getPath() {
        return path;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getParentId() {
        return parentId;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
}
