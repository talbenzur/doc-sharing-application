package docSharing.service;

import docSharing.entities.User;
import docSharing.entities.file.Document;
import docSharing.entities.file.File;
import docSharing.entities.file.Folder;
import docSharing.repository.DocumentRepository;
import docSharing.repository.FolderRepository;
import docSharing.repository.PermissionRepository;
import docSharing.repository.UserRepository;
import docSharing.utils.Utils;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
public class FolderService {
    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;


    private FolderService(FolderRepository folderRepository, DocumentRepository documentRepository,
                          UserRepository userRepository, PermissionRepository permissionRepository) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    /**
     * Creates a Folder and saves it to the database.
     * @param ownerId
     * @param parentId
     * @param title
     * @return The new document
     */
    public Folder createFolder(int ownerId, int parentId, String title) {
        Optional<User> owner = userRepository.findById(ownerId);
        if (!owner.isPresent()) {
            throw new IllegalArgumentException(String.format("owner ID: %d was not found!", ownerId));
        }

        Optional<Folder> parent = folderRepository.findById(parentId);
        if (!Utils.isUniqueTitleInFolder(parent, title)) {
            throw new IllegalArgumentException(String.format("title: %s already exists in folder!", title));
        }

        Folder folder = new Folder(owner.get(), parentId, title);

        Folder savedFolder = folderRepository.save(folder);
        addFolderToParentSubFiles(folder);

        return savedFolder;
    }

    /**
     * Updates folder's parent folder.
     * @param folderId
     * @param parentId
     * @return the updated folder
     */
    public Folder setParent(int folderId, int parentId) {
        Folder folder = folderRepository.getReferenceById(folderId);

        validateUniqueTitle(parentId, folder.getMetadata().getTitle());

        removeFolderFromParentSubFiles(folder);
        folder.getMetadata().setParentId(parentId);
        Folder savedFolder = folderRepository.save(folder);
        addFolderToParentSubFiles(folder);

        return savedFolder;
    }

    /**
     * Updates folder's title.
     * @param folderId
     * @param title
     * @return the updated folder
     */
    public Folder setTitle(int folderId, String title) {
        Folder folder = folderRepository.getReferenceById(folderId);

        validateUniqueTitle(folder.getMetadata().getParentId(), title);

        folder.setTitle(title);
        return folderRepository.save(folder);
    }

    /**
     * Deletes folder from the database.
     * @param folderId
     * @return success status
     */
    public boolean delete(int folderId) {
        boolean success = true;

        Optional<Folder> folder = folderRepository.findById(folderId);
        if (!folder.isPresent()) {
            return false;
        }

        try {
            removeFolderFromParentSubFiles(folder.get());
            deleteSubFiles(folder.get());
            folderRepository.delete(folder.get());
        } catch (Exception e) {
            success = false;
        }

        return success;
    }

    /**
     * Adds folder to its parent folder sub files.
     * @param folder
     */
    private void addFolderToParentSubFiles(Folder folder) {
        Optional<Folder> optionalParent = folderRepository.findById(folder.getMetadata().getParentId());

        if (optionalParent.isPresent()) {
            optionalParent.get().addSubFile(folder);
            folderRepository.save(optionalParent.get());
        }
    }

    /**
     * Removes folder from its parent folder sub files.
     * @param folder
     */
    private void removeFolderFromParentSubFiles(Folder folder) {
        Optional<Folder> optionalParent = folderRepository.findById(folder.getMetadata().getParentId());

        if (optionalParent.isPresent()) {
            optionalParent.get().removeSubFile(folder);
            folderRepository.save(optionalParent.get());
        }
    }

    /**
     * Asserts that there is no file with the same title in the folder.
     * Throws IllegalArgumentException otherwise.
     * @param folder_id
     * @param title
     */
    private void validateUniqueTitle(int folder_id, String title) {
        Optional<Folder> folder = folderRepository.findById(folder_id);
        if (!Utils.isUniqueTitleInFolder(folder, title)) {
            throw new IllegalArgumentException(String.format("File with title: %s already exists in that folder!", title));
        }
    }

    /**
     * Deletes all folder's sub files.
     * @param folder
     */
    @Transactional
    @Modifying
    private void deleteSubFiles(Folder folder) {
        for (File file : folder.getSubFiles()) {
            if (file instanceof Folder) {
                deleteSubFiles((Folder) file);
                folderRepository.delete((Folder) file);
            } else if (file instanceof Document) {
                permissionRepository.deleteByDocumentId(file.getId());
                documentRepository.delete((Document) file);
            }
        }
    }
}
