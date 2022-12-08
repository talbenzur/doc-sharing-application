package docSharing.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import docSharing.controller.response.BaseResponse;
import docSharing.entities.file.File;
import docSharing.entities.file.Folder;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class Utils {
    public static String generateUniqueToken() {
        StringBuilder token = new StringBuilder();
        long currentTimeInMillisecond = Instant.now().toEpochMilli();

        return token.append(currentTimeInMillisecond).append("-")
                .append(UUID.randomUUID()).toString();
    }

    public static String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    public static boolean verifyPassword(String passwordFromUser, String PasswordFromDB) {
        BCrypt.Result result = BCrypt.verifyer().verify(passwordFromUser.toCharArray(),
                PasswordFromDB.toCharArray());

        return result.verified;
    }

    public static boolean isUniqueTitleInFolder(Optional<Folder> folder, String title) {
        if (folder.isPresent()) {
            for (File file : folder.get().getSubFiles()) {
                if (file.getMetadata().getTitle().equals(title)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static void validateTitle(Optional<Folder> folder, String title) {
        if (title.equals("")) {
            throw new IllegalArgumentException("Title cannot be empty!");
        }

        if (!Utils.isUniqueTitleInFolder(folder, title)) {
            throw new IllegalArgumentException(String.format("File with title: %s already exists in that folder!", title));
        }
    }

    public static <T> ResponseEntity<BaseResponse<T>> getNoEditPermissionResponse(int userId) {
        return ResponseEntity.badRequest().body(BaseResponse.failure(
                String.format("User: %d does not have edit permission for this document!", userId)));
    }
}
