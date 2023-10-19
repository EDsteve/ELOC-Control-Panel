package de.eloc.eloc_control_panel.data.firebase;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.ng3.App;
import de.eloc.eloc_control_panel.ng2.interfaces.BooleanCallback;
import de.eloc.eloc_control_panel.ng2.interfaces.StringCallback;

public class AuthHelper {
    private GoogleSignInClient googleSignInClient;

    private static AuthHelper instance;

    public static AuthHelper getInstance() {
        if (instance == null) {
            instance = new AuthHelper();
        }
        return instance;
    }

    private AuthHelper() {
        initGoogleSignIn();
    }

    private void initGoogleSignIn() {
        Context context = App.Companion.getInstance();
        String client_id = context.getString(R.string.default_web_client_id);
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(client_id)
                .build();
        googleSignInClient = GoogleSignIn.getClient(context, signInOptions);
    }

    private FirebaseUser getUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void register(String email, String password, StringCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = getUser();
                if (user != null) {
                    user.sendEmailVerification().addOnCompleteListener(verificationTask -> {
                        if (callback != null) {
                            callback.handler("");
                        }
                    });
                }
            } else {
                Exception exception = task.getException();
                String message = App.Companion.getInstance().getString(R.string.something_went_wrong);
                if (exception != null) {
                    message = exception.getMessage();
                }
                if (callback != null) {
                    callback.handler(message);
                }
            }
        });
    }

    public void signIn(String email, String password, StringCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            String error = "";
            if (!task.isSuccessful()) {
                Context context = App.Companion.getInstance();
                Exception exception = task.getException();
                error = context.getString(R.string.something_went_wrong);
                if ((exception instanceof FirebaseAuthInvalidCredentialsException) || (exception instanceof FirebaseAuthInvalidUserException)) {
                    error = context.getString(R.string.invalid_email_address_or_password);
                } else if (exception != null) {
                    error = exception.getMessage();
                }
            }
            if (callback != null) {
                callback.handler(error);
            }
        });
    }

    public void signInWithGoogle(Intent data, BooleanCallback callback) {
        GoogleSignIn.getSignedInAccountFromIntent(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                GoogleSignInAccount account = task.getResult();
                if (account != null) {
                    String idToken = account.getIdToken();
                    if (idToken != null) {
                        GoogleAuthCredential credential = (GoogleAuthCredential) GoogleAuthProvider.getCredential(idToken, null);
                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(t -> {
                            googleSignInClient.signOut();
                            if (callback != null) {
                                callback.handler(true);
                            }
                        });
                    }
                }
            }
        });
    }

    public Intent getGoogleSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    public void signOut() {
        googleSignInClient.signOut();
        FirebaseAuth.getInstance().signOut();
    }

    public boolean isSignedIn() {
        return (getUser() != null);
    }

    public String getEmailAddress() {
        String emailAddress = "";
        FirebaseUser user = getUser();
        if (user != null) {
            emailAddress = user.getEmail();
        }
        return emailAddress;
    }

    public String getUserId() {
        String id = "no_id";
        FirebaseUser user = getUser();
        if (user != null) {
            id = user.getUid();
        }
        return id;
    }

    public void sendResetLink(String emailAddress, BooleanCallback callback) {
        if (emailAddress == null) {
            emailAddress = "";
        }
        emailAddress = emailAddress.trim();
        if (emailAddress.isEmpty() && (callback != null)) {
            callback.handler(false);
        } else {
            FirebaseAuth.getInstance().sendPasswordResetEmail(emailAddress).addOnCompleteListener(task -> {
                if (callback != null) {
                    callback.handler(task.isSuccessful());
                }
            });
        }
    }

    public void sendVerificationLink(BooleanCallback callback) {
        FirebaseUser user = getUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (callback != null) {
                    callback.handler(task.isSuccessful());
                }
            });
        } else {
            if (callback != null) {
                callback.handler(false);
            }
        }
    }

    private String getDefaultErrorMessage() {
        final Context context = App.Companion.getInstance();
        return context.getString(R.string.something_went_wrong);
    }

    private String getString(int resId) {
        String s = "";
        try {
            s = App.Companion.getInstance().getString(resId);
        } catch (Exception ignore) {

        }
        return s;
    }

    public void reauthenticate(String password, StringCallback callback) {
        StringCallback caller = s -> {
            if (callback != null) {
                callback.handler(s);
            }
        };

        FirebaseUser user = getUser();
        if (user == null) {
            caller.handler(getDefaultErrorMessage());
        } else {
            AuthCredential credential = EmailAuthProvider.getCredential(getEmailAddress(), password);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                String error = "";
                if (!task.isSuccessful()) {
                    error = getString(R.string.invalid_password);
                }
                caller.handler(error);
            });
        }
    }

    public void changeEmailAddress(String newEmailAddress, String password, StringCallback callback) {
        reauthenticate(password, error -> {
            if (error == null) {
                error = "";
            }
            error = error.trim();
            if (error.isEmpty()) {
                changeEmailAfterReauth(newEmailAddress, callback);
            } else {
                if (callback != null) {
                    callback.handler(error);
                }
            }
        });
    }

    private void changeEmailAfterReauth(String newEmailAddress, StringCallback callback) {
        final Context context = App.Companion.getInstance();
        String defaultMessage = context.getString(R.string.something_went_wrong);
        StringCallback caller = s -> {
            if (callback != null) {
                callback.handler(s);
            }
        };
        final FirebaseUser user = getUser();
        if (user == null) {
            caller.handler(defaultMessage);
        } else {
            user
                    .updateEmail(newEmailAddress)
                    .addOnCompleteListener(
                            task -> {
                                String error = "";
                                if (task.isSuccessful()) {
                                    user.sendEmailVerification();
                                } else {
                                    Exception exception = task.getException();
                                    if (exception != null) {
                                        error = exception.getLocalizedMessage();
                                    }
                                    if ((error != null) && error.toLowerCase().contains("invalid")) {
                                        error = context.getString(R.string.invalid_email_address);
                                    }
                                }
                                caller.handler(error);
                            }
                    );
        }
    }

    public void changePassword(String newPassword, String oldPassword, StringCallback callback) {
        reauthenticate(oldPassword, error -> {
            if (error == null) {
                error = "";
            }
            error = error.trim();
            if (error.isEmpty()) {
                changePasswordAfterReauth(newPassword, callback);
            } else {
                if (callback != null) {
                    callback.handler(error);
                }
            }
        });
    }

    private void changePasswordAfterReauth(String newPassword, StringCallback callback) {
        final Context context = App.Companion.getInstance();
        String defaultMessage = context.getString(R.string.something_went_wrong);
        StringCallback caller = s -> {
            if (callback != null) {
                callback.handler(s);
            }
        };
        FirebaseUser user = getUser();
        if (user == null) {
            caller.handler(defaultMessage);
        } else {
            user
                    .updatePassword(newPassword)
                    .addOnCompleteListener(
                            task -> {
                                String error = "";
                                if (!task.isSuccessful()) {
                                    Exception exception = task.getException();
                                    if (exception != null) {
                                        error = exception.getLocalizedMessage();
                                    }
                                }
                                caller.handler(error);
                            }
                    );
        }
    }

    public void deleteAccount(BooleanCallback callback) {
        final BooleanCallback caller = b -> {
            if (callback != null) {
                callback.handler(b);
            }
        };

        FirebaseUser user = getUser();
        if (user == null) {
            caller.handler(false);
        } else {
            user.delete().addOnCompleteListener(task -> caller.handler(task.isSuccessful()));
        }
    }
}
