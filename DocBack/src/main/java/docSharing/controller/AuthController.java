package docSharing.controller;

import docSharing.controller.request.UserRequest;
import docSharing.controller.response.BaseResponse;

import docSharing.entities.DTO.UserDTO;
import docSharing.entities.LoginData;
import docSharing.entities.User;
import docSharing.entities.VerificationToken;
import docSharing.service.AuthService;
import docSharing.service.UserService;
import docSharing.utils.InputValidation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLDataException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    private static final Logger logger = LogManager.getLogger(AuthController.class.getName());

    /**
     * Creates a User and saves it to the database (enabled=0).
     * @param userRequest
     * @param request
     * @return The user
     */
    @RequestMapping(method = RequestMethod.POST, path = "/signup")
    public ResponseEntity<BaseResponse<UserDTO>> register(@RequestBody UserRequest userRequest, HttpServletRequest request) {
        logger.info("in register()");

        if (!InputValidation.isValidEmail(userRequest.getEmail())) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("Invalid email address!"));
        }
        if (!InputValidation.isValidName(userRequest.getName())) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("Invalid name!"));
        }
        if (!InputValidation.isValidPassword(userRequest.getPassword())) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("Invalid password!"));
        }

        try {
            UserDTO createdUser = authService.createUser(userRequest);
            authService.publishRegistrationEvent(createdUser, request.getLocale(), request.getContextPath());
            return ResponseEntity.ok(BaseResponse.success(createdUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("Error occurred: " + e.getMessage()));
        }
    }

    /**
     * User logs in to the system with Email and Password
     * @param userRequest
     * @return LoginData: user id and token
     */
    @RequestMapping(method = RequestMethod.POST, path = "/login")
    public  ResponseEntity<BaseResponse<LoginData>> login(@RequestBody UserRequest userRequest) {
        logger.info("in login()");

        if (!authService.isEnabledUser(userRequest)) {
            return ResponseEntity.badRequest().body(BaseResponse.failure("You must confirm your email first!"));
        }

        Optional<LoginData> loginData = authService.login(userRequest);

        logger.info("User with email " + userRequest.getEmail() + " has logged in");

        return loginData.map(value -> ResponseEntity.ok(BaseResponse.success(value))).
                orElseGet(() -> ResponseEntity.badRequest().body(BaseResponse.failure("Failed to log in: Wrong Email or Password")));
    }

    /**
     * Confirm user's registration. (enabled=1)
     * @param request
     * @param token
     * @return string success/failed
     */
    @GetMapping("/registrationConfirm")
    public String confirmRegistration(WebRequest request, @RequestParam("token") String token) {

        Locale locale = request.getLocale();

        VerificationToken verificationToken = authService.getVerificationToken(token);
        if (verificationToken == null) {
            return "redirect:/badUser.html?lang=" + locale.getLanguage();
        }

        User user = verificationToken.getUser();
        Calendar cal = Calendar.getInstance();
        if ((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            return "redirect:/badUser.html?lang=" + locale.getLanguage();
        }

        userService.updateEnabled(user.getId(), true);
        authService.deleteVerificationToken(token);
        return "redirect:/login.html?lang=" + request.getLocale().getLanguage();
    }
}
