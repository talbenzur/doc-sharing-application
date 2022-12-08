package docSharing.entities;


public class LoginData {
    private Integer userId;
    private String token;

    public LoginData(Integer userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }
}
