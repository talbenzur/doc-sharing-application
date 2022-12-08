package docSharing.controller.request;
import docSharing.entities.permission.Permission;

import java.util.List;


public class ShareRequest {
    private int documentID;
    private int ownerID;
    private List<String> emails;
    private Permission permission;
    private boolean notify;


    public ShareRequest() {
    }

    public ShareRequest(int documentID, int ownerID, List<String> emails, Permission permission, boolean notify) {
        this.documentID = documentID;
        this.ownerID = ownerID;
        this.emails = emails;
        this.permission = permission;
        this.notify = notify;
    }

    public int getDocumentID() {
        return documentID;
    }

    public int getOwnerID() {
        return ownerID;
    }

    public List<String> getEmails() {
        return emails;
    }

    public Permission getPermission() {
        return permission;
    }

    public boolean isNotify() {
        return notify;
    }
}