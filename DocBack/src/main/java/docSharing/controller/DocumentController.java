package docSharing.controller;

import docSharing.controller.request.ShareRequest;
import docSharing.controller.response.BaseResponse;
import docSharing.entities.DTO.DocumentDTO;
import docSharing.entities.DTO.UserDTO;
import docSharing.entities.file.DocOperation;
import docSharing.entities.permission.Permission;
import docSharing.service.AuthService;
import docSharing.service.DocumentService;
import docSharing.service.PermissionService;
import docSharing.service.UserService;
import docSharing.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RestController
@CrossOrigin
@RequestMapping("/document")
public class DocumentController {
    @Autowired
    private DocumentService documentService;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;
    @Autowired
    private PermissionService permissionService;
    private static final Logger logger = LogManager.getLogger(DocumentController.class.getName());

    public DocumentController() {
    }

    /**
     * Creates a Document and saves it to the database.
     * @param token
     * @param ownerId
     * @param parentId
     * @param title
     * @return the new document's representation
     */
    @RequestMapping(method = RequestMethod.POST, path="/create")
    public ResponseEntity<BaseResponse<DocumentDTO>> create(@RequestHeader String token, @RequestHeader int ownerId,
                                                         @RequestParam int parentId, @RequestParam String title) {
        logger.info("in create()");

        if (!authService.isAuthenticated(ownerId, token)) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("User is not logged-in!"));
        }

        try {
            DocumentDTO document = documentService.createDocument(ownerId, parentId, title);

            if (document != null) {
                permissionService.addPermission(document.getDocumentId(), ownerId, Permission.OWNER);
                return ResponseEntity.ok(BaseResponse.success(document));
            } else {
                return ResponseEntity.badRequest().body(BaseResponse.failure("Error occurred while trying to create a document"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }

    /**
     * Updates permissions for users specified in shareRequest body.
     * Sends notification email if shareRequest.notify is true.
     * @param token
     * @param shareRequest
     * @return Void
     */
    @RequestMapping(method = RequestMethod.PATCH, path="/share")
    public ResponseEntity<BaseResponse<Void>> share(@RequestHeader String token, @RequestBody ShareRequest shareRequest) {
        logger.info("in share()");

        if (!authService.isAuthenticated(shareRequest.getOwnerID(), token)) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("User is not logged-in!"));
        }

        if (!permissionService.isAuthorized(shareRequest.getDocumentID(), shareRequest.getOwnerID(), DocOperation.SHARE)) {
            return Utils.getNoEditPermissionResponse(shareRequest.getOwnerID());
        }

        try {
            if (shareToUsersList(retrieveShareRequestUsers(shareRequest), shareRequest)) {
                return ResponseEntity.ok(BaseResponse.noContent(true, "Share succeed for all users"));
            } else {
                return ResponseEntity.badRequest().body(BaseResponse.failure("Share by email failed for some users!"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }

    /**
     * Returns document's URL address.
     * @param documentId
     * @return URL address
     */
    @RequestMapping(method = RequestMethod.GET, path="/getUrl")
    public ResponseEntity<BaseResponse<String>> getUrl(@RequestHeader int documentId) {
        logger.info("in getUrl()");

        try {
            return ResponseEntity.ok(BaseResponse.success(documentService.getUrl(documentId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }

    /**
     * Updates document's parent folder.
     * User must be logged in and must have edit permissions.
     * @param documentId
     * @param userId
     * @param token
     * @param parentId
     * @return The updated document
     */
    @RequestMapping(method = RequestMethod.PATCH, path="/setParent")
    public ResponseEntity<BaseResponse<DocumentDTO>> setParent(@RequestHeader int documentId, @RequestHeader int userId,
                                                            @RequestHeader String token, @RequestParam int parentId) {
        logger.info("in setParent()");

        if (!authService.isAuthenticated(userId, token)) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("User is not logged-in!"));
        }

        if (!permissionService.isAuthorized(documentId, userId, DocOperation.SET_PARENT)) {
            return Utils.getNoEditPermissionResponse(userId);
        }

        try {
            return ResponseEntity.ok(BaseResponse.success(documentService.setParent(documentId, parentId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }
    /**
     * Updates document's title.
     * The title must be unique in the containing folder and must not be empty.
     * User must be logged in and must have edit permissions.
     * @param documentId
     * @param userId
     * @param token
     * @param title
     * @return The updated document
     */
    @RequestMapping(method = RequestMethod.PATCH, path="/setTitle")
    public ResponseEntity<BaseResponse<DocumentDTO>> setTitle(@RequestHeader int documentId, @RequestHeader int userId,
                                                           @RequestHeader String token, @RequestParam String title) {
        logger.info("in setTitle()");

        if (!authService.isAuthenticated(userId, token)) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("User is not logged-in!"));
        }

        if (!permissionService.isAuthorized(documentId, userId, DocOperation.SET_TITLE)) {
            return Utils.getNoEditPermissionResponse(userId);
        }

        try {
            return ResponseEntity.ok(BaseResponse.success(documentService.setTitle(documentId, title)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }

    /**
     * Deletes document from the database.
     * User must be logged in and must have edit permissions.
     * @param documentId
     * @param token
     * @param userId
     */
    @RequestMapping(method = RequestMethod.DELETE, path="/delete")
    public ResponseEntity<BaseResponse<Void>> delete(@RequestHeader int documentId, @RequestHeader String token,
                                                     @RequestHeader int userId) {

        logger.info("in delete()");

        if (!authService.isAuthenticated(userId, token)) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("User is not logged-in!"));
        }

        if (!permissionService.isAuthorized(documentId, userId, DocOperation.DELETE)) {
            return Utils.getNoEditPermissionResponse(userId);
        }

        try {
            if (documentService.delete(documentId)) {
                return ResponseEntity.ok(BaseResponse.noContent(true, "document was successfully deleted"));
            } else {
                return ResponseEntity.badRequest().body(BaseResponse.failure("Document deletion failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }

    /**
     * Imports a document from specified filepath.
     * Will create a new document with the imported title and content.
     * @param token
     * @param ownerId
     * @param filePath
     * @param parentId
     * @return The imported document
     */
    @RequestMapping(method = RequestMethod.POST, path="/import")
    public ResponseEntity<BaseResponse<DocumentDTO>> importFile(@RequestHeader String token, @RequestHeader int ownerId,
                                                             @RequestParam String filePath, @RequestParam int parentId) {

        if (!authService.isAuthenticated(ownerId, token)) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("User is not logged-in!"));
        }

        try {
            return ResponseEntity.ok(BaseResponse.success(documentService.importFile(filePath, ownerId, parentId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }

    /**
     * Exports a document to a text file.
     * @param documentId
     * @param token
     * @param userId
     * @return Void
     */
    @RequestMapping(method = RequestMethod.GET, path="/export")
    public ResponseEntity<BaseResponse<Void>> exportFile(@RequestHeader int documentId, @RequestHeader String token,
                                                         @RequestHeader int userId) {

        if (!authService.isAuthenticated(userId, token)) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("User is not logged-in!"));
        }

        if (!permissionService.isAuthorized(documentId, userId, DocOperation.EXPORT)) {
            return Utils.getNoEditPermissionResponse(userId);
        }

        try {
            documentService.exportFile(documentId);
            return ResponseEntity.ok(BaseResponse.noContent(true, "Document was exported successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }

    /**
     *
     * @param userId
     * @return list of documents that the user corresponding to userId has permissions to.
     */
    @RequestMapping(method = RequestMethod.GET, path="/getDocumentsByUser")
    public ResponseEntity<BaseResponse<List<DocumentDTO>>> getDocumentsByUser(@RequestHeader int userId) {
        logger.info("in getDocumentsByUser()");

        try {
            return ResponseEntity.ok(BaseResponse.success(documentService.getDocumentsByUser(userId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }

    /**
     * @param shareRequest
     * @return List of Users corresponding to the shareRequest's emails list
     */
    private List<UserDTO> retrieveShareRequestUsers(ShareRequest shareRequest) {
        List<UserDTO> users = new ArrayList<>();

        for (String email : shareRequest.getEmails()) {
            Optional<UserDTO> user = userService.getByEmail(email);
            if (!user.isPresent()) {
                logger.warn("Shared via email failed - user: " + email + " does not exist!");
                continue;
            }

            users.add(user.get());
        }

        return users;
    }

    /**
     * Updates permissions to a list of Users.
     * Sends notification email if shareRequest.notify is true.
     * @param users
     * @param shareRequest
     * @return success status
     */
    private boolean shareToUsersList(List<UserDTO> users, ShareRequest shareRequest) {
        boolean allSucceed = true;

        for (UserDTO user : users) {
            try {
                permissionService.updatePermission
                        (shareRequest.getDocumentID(), user.getId(), shareRequest.getPermission());

                if (shareRequest.isNotify()) {
                    documentService.notifyShareByEmail
                            (shareRequest.getDocumentID(), user.getEmail(), shareRequest.getPermission());
                }
            } catch (Exception e) {
                allSucceed = false;
                logger.warn(e.getMessage());
            }
        }

        return allSucceed;
    }
}