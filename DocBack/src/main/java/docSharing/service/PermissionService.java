package docSharing.service;

import docSharing.entities.User;
import docSharing.entities.file.DocOperation;
import docSharing.entities.file.Document;
import docSharing.entities.permission.Authorization;
import docSharing.entities.permission.Permission;
import docSharing.repository.DocumentRepository;
import docSharing.repository.PermissionRepository;
import docSharing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

      private PermissionService(PermissionRepository permissionRepository, UserRepository userRepository,
                              DocumentRepository documentRepository) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * add permission to user to specific document by document id
     * @param documentId
     * @param userId
     * @param permission
     */
    public void addPermission(int documentId, int userId, Permission permission) {
        User user = userRepository.getReferenceById(userId);
        Document document = documentRepository.getReferenceById(documentId);
        Authorization authorization = new Authorization(document, user, permission);
        permissionRepository.save(authorization);
    }

    /**
     * remove permission to user to specific document by document id
     * @param documentId
     * @param userId
     */
    public void deletePermission(int documentId, int userId) {
        List<Authorization> authorizations = permissionRepository.findByDocumentAndUser(documentId, userId);
        if (!authorizations.isEmpty()) {
            permissionRepository.delete(authorizations.get(0));
        }
    }

    /**
     * add/change permission to user to specific document by document id
     * @param documentId
     * @param userId
     * @param permission
     */
    public void updatePermission(int documentId, int userId, Permission permission) {
        if (isOwner(userId, documentId)) {
            throw new IllegalArgumentException("Cannot change owner's permissions!");
        }

        List<Authorization> authorizations = permissionRepository.findByDocumentAndUser(documentId, userId);
        if (!authorizations.isEmpty()) {
            authorizations.get(0).setPermission(permission);
            permissionRepository.save(authorizations.get(0));
        } else {
            addPermission(documentId, userId, permission);
        }
    }

    /**
     * Checks if the user is authorized for the required operation.
     * This method compares between Permission enum's ordinals.
     * @param documentId
     * @param userId
     * @param operation
     * @return true if the user has permissions for the required operation, else false.
     */
    public boolean isAuthorized(int documentId, int userId, DocOperation operation) {
        List<Authorization> authorizations = permissionRepository.findByDocumentAndUser(documentId, userId);
        if (authorizations.isEmpty()) {
            return false;
        }

        return (operation.getPermission() == null ||
                authorizations.get(0).getPermission().ordinal() <= operation.getPermission().ordinal());
    }

    /**
     * check if user is owner of the document
     * @param userId
     * @param documentId
     * @return true- is the user is owner /false.
     */
    private boolean isOwner(int userId, int documentId) {
        Document document = documentRepository.getReferenceById(documentId);
        return document.getMetadata().getOwner().getId() == userId;
    }
}
