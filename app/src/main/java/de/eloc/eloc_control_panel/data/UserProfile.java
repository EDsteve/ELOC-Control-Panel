package de.eloc.eloc_control_panel.data;

public class UserProfile {
    private final String userId;

    private String profilePictureUrl, emailAddress;

    public UserProfile(String userId) {
        this.userId = userId;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
