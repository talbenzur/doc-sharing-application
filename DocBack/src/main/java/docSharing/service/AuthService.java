package docSharing.service;

import docSharing.controller.request.UserRequest;
import docSharing.entities.DTO.UserDTO;
import docSharing.entities.LoginData;
import docSharing.entities.User;
import docSharing.entities.VerificationToken;
import docSharing.events.emailActivation.OnRegistrationCompleteEvent;
import docSharing.repository.UserRepository;
import docSharing.repository.VerificationTokenRepository;
import docSharing.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

import static docSharing.utils.Utils.hashPassword;
import static docSharing.utils.Utils.verifyPassword;

@Service
public class AuthService {
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final VerificationTokenRepository tokenRepository;
    @Autowired
    ApplicationEventPublisher eventPublisher;

    private static final int SCHEDULE = 1000 * 60 * 60;

    static HashMap<Integer, String> usersTokensMap = new HashMap<>();

    private static final Logger logger = LogManager.getLogger(AuthService.class.getName());

    public AuthService(UserRepository userRepository, VerificationTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    /**
     * Create user if email isn't already exist
     * @param userRequest
     * @return the created User
     * @throws IllegalArgumentException
     */
    public UserDTO createUser(UserRequest userRequest) throws IllegalArgumentException {
        logger.info("in createUser()");

        if(userRepository.findByEmail(userRequest.getEmail()).isPresent()){
            throw new IllegalArgumentException(String.format("Email %s already exists!", userRequest.getEmail()));
        }

        logger.debug(userRequest);
        User user = userRepository.save(new User(userRequest.getName(), userRequest.getEmail(),
                hashPassword(userRequest.getPassword())));

        return new UserDTO(user);
    }

    /**
     * Log In user to the system
     * @param userRequest
     * @return LoginData (user id, token) if successes
     */
    public Optional<LoginData> login(UserRequest userRequest) {
        logger.info("in login()");

        Optional<User> user = userRepository.findByEmail(userRequest.getEmail());

        if (user.isPresent() && verifyPassword(userRequest.getPassword(), user.get().getPassword())) {
            Optional<String> token = Optional.of(Utils.generateUniqueToken());
            usersTokensMap.put(user.get().getId(), token.get());
            return Optional.of(new LoginData(user.get().getId(), token.get()));
        }

        return Optional.empty();
    }

    /**
     * Check if user is enabled
     * @param userRequest
     * @return true if user is enabled, otherwise - false
     */
    public boolean isEnabledUser(UserRequest userRequest) {
        logger.info("in isEnabledUser()");

        Optional<User> user = userRepository.findByEmail(userRequest.getEmail());

        return user.isPresent() && user.get().isEnabled();
    }

    /**
     * Check if user is authenticated
     * @param userId
     * @param token
     * @return true if user is authenticated, otherwise - false
     */
    public boolean isAuthenticated(int userId, String token) {
        return usersTokensMap.containsKey(userId) && usersTokensMap.get(userId).equals(token);
    }


    // ------------------ verification token ------------------ //

    /**
     * Publish event on new user registration
     * @param createdUser
     * @param locale
     * @param appUrl
     */
    public void publishRegistrationEvent(UserDTO createdUser, Locale locale, String appUrl) {
        User user = userRepository.getReferenceById(createdUser.getId());
        eventPublisher.publishEvent(new OnRegistrationCompleteEvent(user, locale, appUrl));
    }

    /**
     * Create Verification token and save it to DB
     * @param user
     * @param token
     */
    public void createVerificationToken(User user, String token) {
        VerificationToken myToken = new VerificationToken(token, user);
        tokenRepository.save(myToken);
    }

    /**
     * Get Verification Token object by token string
     * @param VerificationToken
     * @return Verification Token
     */
    public VerificationToken getVerificationToken(String VerificationToken) {
        return tokenRepository.findByToken(VerificationToken);
    }

    /**
     * Delete Verification Token object by token string
     * @param token
     */
    public void deleteVerificationToken(String token) {
        tokenRepository.deleteByToken(token);
    }

    /**
     * Schedule Delete expired verification tokens every period of time
     * Schedule Delete not activated users by expired tokens every period of time
     */
    @Scheduled(fixedRate = SCHEDULE)
    public void scheduleDeleteNotActivatedUsers() {
        logger.info("---------- in scheduleDeleteNotActivatedUsers-------------");
        Calendar cal = Calendar.getInstance();
        List<VerificationToken> expiredTokens = tokenRepository.findAllExpired(new Timestamp(cal.getTime().getTime()));
        logger.debug(expiredTokens);

        for (VerificationToken token: expiredTokens) {
            deleteVerificationToken(token.getToken());
            userRepository.deleteById(token.getUser().getId());
            logger.debug("Verification token for user_id#" + token.getUser().getId() + " and non activated user was deleted");
        }
    }
}
