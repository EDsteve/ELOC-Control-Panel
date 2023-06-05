package de.eloc.eloc_control_panel.data;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;

import de.eloc.eloc_control_panel.ng2.interfaces.BooleanCallback;
import de.eloc.eloc_control_panel.ng2.interfaces.StringCallback;
import de.eloc.eloc_control_panel.ng2.interfaces.VoidCallback;

public class UserAccountViewModel extends AndroidViewModel {

    private final UserAccountRepository repository;
    private final MutableLiveData<UserProfile> userProfileLiveData = new MutableLiveData<>();

    public UserAccountViewModel(@NonNull Application application) {
        super(application);
        repository = UserAccountRepository.getInstance();
    }

    public String getEmailAddress() {
        return repository.getEmailAddress();
    }

    public Intent getGoogleSignInIntent() {
        return repository.getGoogleSignInIntent();
    }

    public void signOut() {
        repository.signOut();
    }

    public boolean isEmailVerified() {
        return repository.isUserEmailVerified();
    }

    public void signInWithGoogle(Intent data, BooleanCallback callback) {
        repository.signInWithGoogle(data, callback);
    }

    public void register(String email, String password, StringCallback callback) {
        repository.register(email, password, callback);
    }

    public boolean isSignedIn() {
        return repository.isSignedIn();
    }

    public void updateProfile(HashMap<String, Object> data, BooleanCallback callback) {
        repository.updateProfile(data, callback);
    }

    public void hasProfile(BooleanCallback callback) {
        repository.hasProfile(callback);
    }

    public void userIdExists(String userId, BooleanCallback callback) {
        repository.userIdExists(userId, callback);
    }

    public void signIn(String emailAddress, String password, StringCallback callback) {
        repository.signIn(emailAddress, password, callback);
    }

    public void sendPasswordResetLink(String emailAddress, BooleanCallback callback) {
        repository.sendPasswordResetLink(emailAddress, callback);
    }

    public void sendEmailVerificationLink(BooleanCallback callback) {
        repository.sendEmailVerificationLink(callback);
    }

    public void getProfile() {
        repository.getProfile(userProfileLiveData::setValue);
    }

    public LiveData<UserProfile> watchProfile() {
        return userProfileLiveData;
    }

    public void uploadProfilePicture(Bitmap bitmap, StringCallback callback) {
        repository.uploadProfilePicture(bitmap, callback);
    }

    public void updateProfilePicture(String url, VoidCallback callback) {
        repository.updateProfilePicture(url, callback);
    }

    public void changeEmailAddress(String emailAddress, String password, StringCallback callback) {
        repository.changeEmailAddress(emailAddress, password, callback);
    }

    public void changePassword(String newPassword, String oldPassword, StringCallback callback) {
        repository.changePassword(newPassword, oldPassword, callback);
    }

    public void deleteRemoteFiles(BooleanCallback callback) {
        repository.deleteRemoteFiles(callback);
    }

    public void deleteProfile(BooleanCallback callback) {
        repository.deleteProfile(callback);
    }

    public void deleteAuthAccount(BooleanCallback callback) {
        repository.deleteAuthAccount(callback);
    }

    public void verifyPassword(String password, StringCallback callback) {
        repository.verifyPassword(password, callback);
    }
}
