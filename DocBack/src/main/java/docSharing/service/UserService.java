package docSharing.service;

import docSharing.entities.DTO.UserDTO;
import docSharing.entities.User;
import docSharing.repository.PermissionRepository;
import docSharing.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static docSharing.utils.Utils.hashPassword;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    private static final Logger logger = LogManager.getLogger(UserService.class.getName());

    public UserService(UserRepository userRepository, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    /**
     * Get User by email
     * @param email
     * @return the User if exists
     */
    public Optional<UserDTO> getByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (!user.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(new UserDTO(user.get()));
    }

    /**
     * Get User id by Email
     * @param email
     * @return the id
     */
    public Integer getUserIdByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);

        if (!user.isPresent()) {
            return null;
        }

        return user.get().getId();
    }

    /**
     * Delete user by Id
     * @param id
     * @return true if user was deleted, otherwise - false
     */
    public boolean deleteUser(int id) {
        int lines = userRepository.deleteById(id);
        logger.debug("lines deleted: " + lines);

        if (lines == 1) {
            logger.debug("User #" + id + " deleted: ");
            return true;
        }
        return false;
    }

    /**
     * Update User's name
     * @param id
     * @param name
     * @return the updated User
     */
    public Optional<UserDTO> updateName(int id, String name) {
        int lines = userRepository.updateUserNameById(id, name);
        logger.debug("lines updated: " + lines);

        return getUpdatedUser(id, lines);
    }

    /**
     * Update user's email
     * @param id
     * @param email
     * @return the updated User
     */
    public Optional<UserDTO> updateEmail(int id, String email) {
        int lines = userRepository.updateUserEmailById(id, email);
        logger.debug("lines updated: " + lines);

        return getUpdatedUser(id, lines);
    }

    /**
     * Update user's password
     * @param id
     * @param password
     * @return the updated User
     */
    public Optional<UserDTO> updatePassword(int id, String password) {
        int lines = userRepository.updateUserPasswordById(id,hashPassword(password));
        logger.debug("lines updated: " + lines);

        return getUpdatedUser(id, lines);
    }

    /**
     * Update user's enabled field
     * @param id
     * @param enabled
     * @return the updated User
     */
    public Optional<UserDTO> updateEnabled(int id, Boolean enabled) {
        int lines = userRepository.updateUserEnabledById(id, enabled);
        logger.debug("lines updated: " + lines);

        return getUpdatedUser(id, lines);
    }

    /**
     * Get updated user
     * @param id
     * @param lines
     * @return the User if exists
     */
    private Optional<UserDTO> getUpdatedUser(int id, int lines) {
        if (lines == 1) {
            Optional<User> user = userRepository.findById(id);
            logger.debug("User #" + id + " updated: " + user.get());
            UserDTO userDTO = new UserDTO(user.get());
            return Optional.of(userDTO);
        }

        return Optional.empty();
    }
}
