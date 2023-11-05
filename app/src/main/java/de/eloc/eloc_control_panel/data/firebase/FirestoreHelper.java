package de.eloc.eloc_control_panel.data.firebase;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.util.HashMap;

import de.eloc.eloc_control_panel.ng3.interfaces.BooleanCallback;
import de.eloc.eloc_control_panel.ng3.interfaces.ProfileCallback;
import de.eloc.eloc_control_panel.ng3.interfaces.VoidCallback;
import de.eloc.eloc_control_panel.ng3.data.UserProfile;

public class FirestoreHelper {

    private final CollectionReference accountsNode;

    private static FirestoreHelper instance;
    public static String FIELD_PROFILE_PICTURE = "profile_picture";
    public static String FIELD_USER_ID = "user_id";

    public static FirestoreHelper getInstance() {
        if (instance == null) {
            instance = new FirestoreHelper();
        }
        return instance;
    }

    private FirestoreHelper() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        accountsNode = firestore.collection("accounts");
    }

    public void updateProfilePicture(String url, String id, VoidCallback callback) {
        updateProfileField(url, FIELD_PROFILE_PICTURE, id, callback);
    }

    private void updateProfileField(String value, String fieldName, String id, VoidCallback callback) {
        final VoidCallback caller = () -> {
            if (callback != null) {
                callback.handler();
            }
        };

        if (id == null) {
            id = "";
        }
        id = id.trim();
        if (id.isEmpty()) {
            caller.handler();
            return;
        }

        if (value == null) {
            value = "";
        }
        final String finalValue = value.trim();
        if (finalValue.isEmpty()) {
            caller.handler();
            return;
        }

        HashMap<String, Object> data = new HashMap<>();
        data.put(fieldName, finalValue);
        accountsNode.document(id).update(data).addOnCompleteListener(task -> caller.handler());
    }

    public void updateProfile(String id, HashMap<String, Object> data, BooleanCallback callback) {
        if (id == null) {
            id = "";
        }
        id = id.trim();
        if (id.isEmpty() || (data == null) || data.isEmpty()) {
            if (callback != null) {
                callback.handler(false);
            }
        } else {
            SetOptions options = SetOptions.merge();
            accountsNode
                    .document(id)
                    .set(data, options)
                    .addOnCompleteListener(task -> {
                        if (callback != null) {
                            callback.handler(task.isSuccessful());
                        }
                    });
        }
    }

    public void hasProfile(String id, BooleanCallback callback) {
        if (id == null) {
            id = "";
        }
        id = id.trim();
        if (id.isEmpty()) {
            if (callback != null) {
                callback.handler(false);
            }
        } else {
            accountsNode.document(id).get(Source.SERVER).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean hasUserId = false;
                    DocumentSnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        Object o = snapshot.get(FIELD_USER_ID);
                        if (o != null) {
                            String userId = o.toString();
                            hasUserId = (!userId.isEmpty());
                        }
                    }
                    if (callback != null) {
                        callback.handler(hasUserId);
                    }
                } else {
                    if (callback != null) {
                        callback.handler(false);
                    }
                }
            });
        }
    }

    public void userIdExists(String userId, BooleanCallback callback) {
        if (userId == null) {
            userId = "";
        }
        userId = userId.trim();
        if (userId.isEmpty()) {
            if (callback != null) {
                callback.handler(false);
            }
        } else {
            accountsNode
                    .whereEqualTo(FIELD_USER_ID, userId)
                    .get(Source.SERVER)
                    .addOnCompleteListener(task -> {
                        boolean exists = false;
                        if (task.isSuccessful()) {
                            QuerySnapshot snapshot = task.getResult();
                            int count = snapshot.size();
                            exists = (count > 0);
                        }
                        if (callback != null) {
                            callback.handler(exists);
                        }
                    });
        }
    }

    public void getProfile(String documentId, final String emailAddress, ProfileCallback callback) {
        if (documentId == null) {
            documentId = "";
        }
        documentId = documentId.trim();
        if (documentId.isEmpty()) {
            if (callback != null) {
                callback.handler(null);
            }
        } else {
            accountsNode
                    .document(documentId)
                    .get(Source.SERVER)
                    .addOnCompleteListener(task -> {
                        UserProfile profile = null;
                        if (task.isSuccessful()) {
                            DocumentSnapshot snapshot = task.getResult();
                            String profilePictureUrl = snapshot.get(FIELD_PROFILE_PICTURE, String.class);
                            String userId = snapshot.get(FIELD_USER_ID, String.class);
                            profile = new UserProfile(userId);
                            profile.setProfilePictureUrl(profilePictureUrl);
                            profile.setEmailAddress(emailAddress);
                        }
                        if (callback != null) {
                            callback.handler(profile);
                        }
                    });
        }
    }

    public void deleteProfile(String documentId, BooleanCallback callback) {
        final BooleanCallback caller = b -> {
            if (callback != null) {
                callback.handler(b);
            }
        };

        if (documentId == null) {
            documentId = "";
        }
        documentId = documentId.trim();
        if (documentId.isEmpty()) {
            caller.handler(false);
        } else {
            accountsNode
                    .document(documentId)
                    .delete()
                    .addOnCompleteListener(task -> caller.handler(task.isSuccessful()));
        }
    }
}
