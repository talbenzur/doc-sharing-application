package docSharing.service;

import docSharing.entities.DTO.DocumentDTO;
import docSharing.entities.file.*;
import docSharing.entities.permission.Authorization;
import docSharing.repository.*;
import docSharing.utils.Utils;
import docSharing.controller.request.UpdateRequest;
import docSharing.entities.User;
import docSharing.entities.permission.Permission;
import docSharing.utils.GMailer;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static docSharing.utils.FilesUtils.*;


@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UpdateLogRepository updateLogRepository;
    private Map<Integer, Document> documentsCache;


    private DocumentService(DocumentRepository documentRepository, FolderRepository folderRepository,
                            UserRepository userRepository, PermissionRepository permissionRepository,
                            UpdateLogRepository updateLogRepository) {
        this.documentRepository = documentRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.updateLogRepository = updateLogRepository;

        this.loadDocumentCache();

    }

    private void loadDocumentCache() {
        this.documentsCache = documentRepository.findAll().stream()
                .collect(Collectors.toMap(File::getId, Function.identity()));
    }

    /**
     * Adds userId to the document's activeUsers list.
     * @param documentId
     * @param userId
     */
    public DocumentDTO join(int documentId, int userId) {
        Document document = documentsCache.get(documentId);
        document.addActiveUser(userId);

        return new DocumentDTO(document, generateUrl(documentId));
    }

    /**
     * Removes userId from the document's activeUsers list.
     * @param documentId
     * @param userId
     */
    public void leave(int documentId, int userId) {
        Document document = documentsCache.get(documentId);
        document.removeActiveUser(userId);

        if (document.getLastUpdate() != null && document.getLastUpdate().getUserId() == userId) {
            updateLogRepository.save(document.getLastUpdate());
        }
    }

    /**
     * Creates a Document and saves it to the database.
     * @param ownerId
     * @param parentId
     * @param title
     * @return The new document
     */
    public DocumentDTO createDocument(int ownerId, int parentId, String title) {
        Optional<User> owner = userRepository.findById(ownerId);
        if (!owner.isPresent()) {
            throw new IllegalArgumentException(String.format("owner ID: %d was not found!", ownerId));
        }

        Optional<Folder> parent = folderRepository.findById(parentId);
        Utils.validateTitle(parent, title);

        Document document = new Document(owner.get(), parentId, title);
        Document saved = documentRepository.save(document);
        this.documentsCache.put(document.getId(), saved);
        addDocumentToParentSubFiles(document);

        Document savedDocument = this.documentsCache.get(document.getId());
        return new DocumentDTO(savedDocument, generateUrl(savedDocument.getId()));
    }

    /**
     * Updates a document and adds a track log to its history.
     * @param updateRequest
     */
    public void update(UpdateRequest updateRequest) {
        Document document = documentsCache.get(updateRequest.getDocumentId());
        UpdateLog updateLog = document.updateContent(updateRequest);

        if (document.isContinuousLog(updateLog)) {
            document.updateLastLog(updateLog);
        } else {
            if (document.getLastUpdate() != null) {
                updateLogRepository.save(document.getLastUpdate());
            }

            document.setLastUpdate(updateLog);
        }

        documentRepository.save(document);
    }

    /**
     * Updates document's parent folder.
     * @param documentId
     * @param parentId
     * @return the updated document representation
     */
    public DocumentDTO setParent(int documentId, int parentId) {
        Document document = this.documentsCache.get(documentId);
        Optional<Folder> parentToBe = folderRepository.findById(parentId);

        Utils.validateTitle(parentToBe, document.getMetadata().getTitle());

        removeDocumentFromParentSubFiles(document);
        document.getMetadata().setParentId(parentId);
        Document savedDocument = documentRepository.save(document);
        addDocumentToParentSubFiles(document);

        return new DocumentDTO(savedDocument, generateUrl(savedDocument.getId()));
    }

    /**
     * Updates document's title.
     * @param documentId
     * @param title
     * @return the updated document representation
     */
    public DocumentDTO setTitle(int documentId, String title) {
        Document document = this.documentsCache.get(documentId);
        Optional<Folder> parent = folderRepository.findById(document.getMetadata().getParentId());

        Utils.validateTitle(parent, title);
        document.setTitle(title);
        Document savedDocument = documentRepository.save(document);

        return new DocumentDTO(savedDocument, generateUrl(savedDocument.getId()));
    }

    /**
     * Sends a share notification email to the specified email address.
     * @param documentId
     * @param email
     * @param permission
     * @return Success status
     */
    public boolean notifyShareByEmail(int documentId, String email, Permission permission) {
        try {
            Document document = this.documentsCache.get(documentId);

            String subject = "Document shared with you: " + document.getMetadata().getTitle();
            String message = String.format("The document owner has invited you to %s the following document: %s",
                    permission.toString(), generateUrl(documentId));
            GMailer.sendMail(email, subject, message);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * @param documentId
     * @return The document's URL
     */
    public String getUrl(int documentId) {
        return generateUrl(documentId);
    }

    /**
     * Imports a document from specified filepath.
     * Will create a new document with the imported title and content.
     * @param path
     * @param ownerId
     * @param parentID
     * @return The imported document
     */
    public DocumentDTO importFile(String path, int ownerId, int parentID) {
        DocumentDTO document = createDocument(ownerId, parentID, getFileName(path));
        updateContent(document.getDocumentId(), readFromFile(path));

        Document savedDocument = this.documentsCache.get(document.getDocumentId());
        return new DocumentDTO(savedDocument, generateUrl(savedDocument.getId()));
    }

    /**
     * Exports a document to a text file.
     * @param documentId
     */
    public void exportFile(int documentId){
        Document document = this.documentsCache.get(documentId);

        String filename = document.getMetadata().getTitle();
        String content = document.getContent();
        String home = System.getProperty("user.home");
        String filePath = home + "\\Downloads\\" + filename + ".txt";

        writeToFile(content, filePath);
    }

    /**
     *
     * @param documentId
     * @return document's metadata
     */
    public MetaData getMetadata(int documentId) {
        Document document = this.documentsCache.get(documentId);
        return document.getMetadata();
    }

    /**
     *
     * @param documentId
     * @return document's active users
     */
    public List<String> getActiveUsers(int documentId) {
        Document document = this.documentsCache.get(documentId);

        List<String> activeUsersName = new ArrayList<>();
        for (Integer userId : document.getActiveUsers()) {
            Optional<User> activeUser = userRepository.findById(userId);
            if (activeUser.isPresent()) {
                activeUsersName.add(activeUser.get().getName());
            }
        }

        return activeUsersName;
    }

    /**
     *
     * @param userId
     * @return all documents associated with userId.
     */
    public List<DocumentDTO> getDocumentsByUser(int userId) {
        List<Authorization> authorizations = this.permissionRepository.findByUser(userId);

        List<DocumentDTO> userDocuments = new ArrayList<>();
        for (Authorization authorization : authorizations) {
            Document document = authorization.getDocument();
            userDocuments.add(new DocumentDTO(document, generateUrl(document.getId())));
        }

        return userDocuments;
    }

    /**
     * Deletes document from the database.
     * @param documentId
     * @return success status
     */
    public boolean delete(int documentId) {
        try {
            Document document = this.documentsCache.get(documentId);
            removeDocumentFromParentSubFiles(document);
            permissionRepository.deleteByDocumentId(documentId);
            updateLogRepository.deleteByDocumentId(documentId);
            documentRepository.delete(document);
            this.documentsCache.remove(documentId);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Adds document to its parent folder sub files.
     * @param document
     */
    private void addDocumentToParentSubFiles(Document document) {
        Optional<Folder> optionalParent = folderRepository.findById(document.getMetadata().getParentId());

        if (optionalParent.isPresent()) {
            optionalParent.get().addSubFile(document);
            folderRepository.save(optionalParent.get());
        }
    }

    /**
     * Removes document from its parent folder sub files.
     * @param document
     */
    private void removeDocumentFromParentSubFiles(Document document) {
        Optional<Folder> optionalParent = folderRepository.findById(document.getMetadata().getParentId());

        if (optionalParent.isPresent()) {
            optionalParent.get().removeSubFile(document);
            folderRepository.save(optionalParent.get());
        }
    }

    /**
     * Sets the content of the document that corresponds to documentId to content.
     * @param documentId
     * @param content
     */
    private void updateContent(int documentId, String content) {
        Document document = this.documentsCache.get(documentId);
        document.setContent(content);
        documentRepository.save(document);
    }

    /**
     * Generates the URL address of the document corresponds to documentId.
     * @param documentId
     * @return the document's URL.
     */
    private String generateUrl(int documentId) {
        Document document = this.documentsCache.get(documentId);

        String url = document.getMetadata().getTitle();
        int parentId = document.getMetadata().getParentId();

        Optional<Folder> parent = folderRepository.findById(parentId);

        while (parent.isPresent()) {
            url = parent.get().getMetadata().getTitle() + FileSystems.getDefault().getSeparator() + url;
            parent = folderRepository.findById(parent.get().getMetadata().getParentId());
        }

        return url;
    }
}