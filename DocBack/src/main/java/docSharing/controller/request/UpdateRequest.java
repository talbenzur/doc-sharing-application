package docSharing.controller.request;

public class UpdateRequest {
    private int documentId;
    private int userId;
    private UpdateType type;
    private String content;
    private int startPosition;
    private int endPosition;

    public UpdateRequest() {
    }

    public UpdateRequest(UpdateRequestBuilder builder) {
        this.documentId = builder.documentId;
        this.userId = builder.userId;
        this.type = builder.type;
        this.content = builder.content;
        this.startPosition = builder.startPosition;
        this.endPosition = builder.endPosition;
    }

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public UpdateType getType() {
        return type;
    }

    public void setType(UpdateType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int position) {
        this.startPosition = position;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public enum UpdateType{
        DELETE,
        APPEND,
        DELETE_RANGE,
        APPEND_RANGE
    }

    public static class UpdateRequestBuilder {
        private int documentId;
        private int userId;
        private UpdateType type;
        private String content;
        private int startPosition;
        private int endPosition;

        public UpdateRequestBuilder() {
        }

        public UpdateRequestBuilder setDocumentId(int documentId) {
            this.documentId = documentId;
            return this;
        }

        public UpdateRequestBuilder setUserId(int userId) {
            this.userId = userId;
            return this;
        }

        public UpdateRequestBuilder setType(UpdateType type) {
            this.type = type;
            return this;
        }

        public UpdateRequestBuilder setContent(String content) {
            this.content = content;
            return this;
        }

        public UpdateRequestBuilder setStartPosition(int startPosition) {
            this.startPosition = startPosition;
            return this;
        }

        public UpdateRequestBuilder setEndPosition(int endPosition) {
            this.endPosition = endPosition;
            return this;
        }

        public UpdateRequest build() {
            return new UpdateRequest(this);
        }
    }
}