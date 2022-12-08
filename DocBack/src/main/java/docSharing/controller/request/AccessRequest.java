package docSharing.controller.request;

public class AccessRequest {
    private int documentId;
    private int userId;

    public AccessRequest() {
    }

    public AccessRequest(int documentId, int userId) {
        this.documentId = documentId;
        this.userId = userId;
    }

    public int getDocumentId() {
        return documentId;
    }

    public int getUserId() {
        return userId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
