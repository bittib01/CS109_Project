package model;

public class User {
    private final String username;
    private final String passwordHash;
    private boolean loggedIn;

    public User(String username, String password) {
        this.username = username;
        this.passwordHash = password;
        this.loggedIn = false;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String passwordHash) {
        return this.passwordHash.equals(passwordHash);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    @Override
    public String toString() {
        return String.format("%s（%s登录）", username, (loggedIn)? "已" : "未");
    }
}