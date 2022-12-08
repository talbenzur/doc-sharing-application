package docSharing.controller;

import docSharing.controller.request.AccessRequest;
import docSharing.controller.request.UpdateRequest;
import docSharing.controller.response.BaseResponse;
import docSharing.entities.DTO.DocumentDTO;
import docSharing.entities.file.DocOperation;
import docSharing.entities.file.MetaData;
import docSharing.service.DocumentService;
import docSharing.service.PermissionService;
import docSharing.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
@ComponentScan
public class DocumentEditController {
    @Autowired
    private DocumentService documentService;
    @Autowired
    private PermissionService permissionService;

    private static final Logger logger = LogManager.getLogger(DocumentEditController.class.getName());

    public DocumentEditController() {
    }

    /**
     * Checks whether the user is authorized to the file.
     * Inserts the user into the document - on active user (service- join function)
     *
     * @param accessRequest (documentId, userId)
     * @return DocumentDTO if success- else error message
     */
    @MessageMapping("/join")
    @SendTo("/topic/join")
    public ResponseEntity<BaseResponse<DocumentDTO>> join(AccessRequest accessRequest) {
        logger.info("in join()");

        if(!permissionService.isAuthorized(accessRequest.getDocumentId(), accessRequest.getUserId(), DocOperation.JOIN)) {
            logger.warn("User is not authorized");
            return Utils.getNoEditPermissionResponse(accessRequest.getUserId());
        }

        try {
            return ResponseEntity.ok(BaseResponse.success(
                    documentService.join(accessRequest.getDocumentId(), accessRequest.getUserId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure(e.getMessage()));
        }
    }

    /**
     * Removes the user from the document.
     *
     * @param accessRequest ( documentId, userId).
     */
    @MessageMapping("/leave")
    public void leave(AccessRequest accessRequest) {
        logger.info("in leave()");

        try {
            documentService.leave(accessRequest.getDocumentId(), accessRequest.getUserId());
        } catch (Exception e) {
            logger.error(String.format("Failed to leave user #%d from document #%d",
                    accessRequest.getUserId(), accessRequest.getDocumentId()));
        }
    }

    /**
     * get update request and update the content on DB.
     * @param updateRequest (documentId, userId, type, content, startPosition, endPosition)
     *
     * @return the updateRequest
     */
    @MessageMapping("/update")
    @SendTo("/topic/updates")
    public UpdateRequest update(UpdateRequest updateRequest) {
        logger.info("in update() - update message: " + updateRequest.getContent());

        if (!permissionService.isAuthorized(updateRequest.getDocumentId(), updateRequest.getUserId(), DocOperation.UPDATE)) {
            return null;
        }

        try {
            documentService.update(updateRequest);
        } catch (Exception e) {
            logger.error("Error occurred while trying to update: " + e.getMessage());
        }

        return updateRequest;
    }

    /**
     * @param documentId
     * @return the metadata of document (title, owner, parentId, created, last update)
     */
    @MessageMapping("/metadata")
    @SendTo("/topic/metadata")
    public MetaData getMetaData(int documentId) {
        return documentService.getMetadata(documentId);
    }

    /**
     *
     * @param documentId
     * @return list of all active user of document by document id.
     */
    @MessageMapping("/activeUsers")
    @SendTo("/topic/activeUsers")
    public List<String> getActiveUsers(int documentId) {
        try {
            return documentService.getActiveUsers(documentId);
        } catch (Exception e) {
            logger.error("Error occurred while trying to retrieve active users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * print message when docket connection is open
     * @param name
     */
    @MessageMapping("/hello")
    public void greet(String name){
        System.out.println("on connection name: " + name);
    }
}