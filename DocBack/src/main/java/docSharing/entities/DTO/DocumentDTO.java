package docSharing.entities.DTO;

import docSharing.entities.file.Document;
import docSharing.entities.file.MetaData;

public class DocumentDTO {
    private int documentId;
    private String url;
    private MetaData metaData;
    private String content;

    public DocumentDTO() {
    }

    public DocumentDTO(int documentId, String url, MetaData metaData) {
        this.documentId = documentId;
        this.url = url;
        this.metaData = metaData;
    }

    public DocumentDTO(Document document, String url) {
        this.documentId = document.getId();
        this.metaData = document.getMetadata();
        this.url = url;
        this.content = document.getContent();
    }

    public int getDocumentId() {
        return documentId;
    }

    public String getUrl() {
        return url;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public String getContent() {
        return content;
    }
}
