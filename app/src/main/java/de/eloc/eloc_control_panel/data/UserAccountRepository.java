package de.eloc.eloc_control_panel.data;

import android.content.Intent;
import android.graphics.Bitmap;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;

import de.eloc.eloc_control_panel.data.firebase.AuthHelper;
import de.eloc.eloc_control_panel.data.firebase.FirestoreHelper;
import de.eloc.eloc_control_panel.data.firebase.StorageHelper;
import de.eloc.eloc_control_panel.ng2.interfaces.BooleanCallback;
import de.eloc.eloc_control_panel.ng2.interfaces.ProfileCallback;
import de.eloc.eloc_control_panel.ng2.interfaces.StringCallback;
import de.eloc.eloc_control_panel.ng2.interfaces.VoidCallback;

public class UserAccountRepository {

    private final FirestoreHelper firestoreHelper;
    private final AuthHelper authHelper;
    private final StorageHelper storageHelper;
    private static UserAccountRepository instance;

    static UserAccountRepository getInstance() {
        if (instance == null) {
            instance = new UserAccountRepository();
        }
        return instance;
    }

    private UserAccountRepository() {
        firestoreHelper = FirestoreHelper.getInstance();
        authHelper = AuthHelper.getInstance();
        storageHelper = StorageHelper.getInstance();
    }

    public String getEmailAddress() {
        return authHelper.getEmailAddress();
    }

    Intent getGoogleSignInIntent() {
        return authHelper.getGoogleSignInIntent();
    }

    void signOut() {
        authHelper.signOut();
    }

    boolean isSignedIn() {
        return authHelper.isSignedIn();
    }

    boolean isUserEmailVerified() {
        boolean verified = false;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            verified = user.isEmailVerified();
        }
        return verified;
    }

    void signInWithGoogle(Intent data, BooleanCallback callback) {
        authHelper.signInWithGoogle(data, callback);
    }

    void register(String email, String password, StringCallback callback) {
        authHelper.register(email, password, callback);
    }

    void updateProfile(HashMap<String, Object> data, BooleanCallback callback) {
        String id = authHelper.getUserId();
        firestoreHelper.updateProfile(id, data, callback);
    }

    void hasProfile(BooleanCallback callback) {
        String id = authHelper.getUserId();
        firestoreHelper.hasProfile(id, callback);
    }

    void userIdExists(String userId, BooleanCallback callback) {
        firestoreHelper.userIdExists(userId, callback);
    }

    void signIn(String emailAddress, String password, StringCallback callback) {
        authHelper.signIn(emailAddress, password, callback);
    }

    void sendPasswordResetLink(String emailAddress, BooleanCallback callback) {
        authHelper.sendResetLink(emailAddress, callback);
    }

    void sendEmailVerificationLink(BooleanCallback callback) {
        authHelper.sendVerificationLink(callback);
    }

    void getProfile(ProfileCallback callback) {
        String id = authHelper.getUserId();
        String emailAddress = getEmailAddress();
        firestoreHelper.getProfile(id, emailAddress, callback);
    }

    void uploadProfilePicture(Bitmap bitmap, StringCallback callback) {
        String id = authHelper.getUserId();
        storageHelper.uploadProfilePicture(id, bitmap, callback);
    }

    void updateProfilePicture(String url, final VoidCallback callback) {
        String id = authHelper.getUserId();
        firestoreHelper.updateProfilePicture(url, id, callback);
    }

    void changeEmailAddress(String emailAddress, String password, StringCallback callback) {
        authHelper.changeEmailAddress(emailAddress, password, callback);
    }

    void changePassword(String newPassword, String oldPassword, StringCallback callback) {
        authHelper.changePassword(newPassword, oldPassword, callback);
    }

    void deleteRemoteFiles(BooleanCallback callback) {
        String id = authHelper.getUserId();
        storageHelper.deleteAccount(id, callback);
    }

    void deleteProfile(BooleanCallback callback) {
        String id = authHelper.getUserId();
        firestoreHelper.deleteProfile(id, callback);
    }

    void deleteAuthAccount(BooleanCallback callback) {
        authHelper.deleteAccount(callback);
    }

    void verifyPassword(String password, StringCallback callback) {
        authHelper.reauthenticate(password, callback);
    }
}
