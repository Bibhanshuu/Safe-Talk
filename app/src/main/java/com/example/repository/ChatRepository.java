package com.example.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.data.ChatMessage;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ChatRepository {

    private static final String TAG = "ChatRepositoryJava";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private final MutableLiveData<AuthState> authState = new MutableLiveData<>(new AuthState.Initializing());
    private String sessionTempId;
    private final Map<String, List<ChatMessage>> localRoomMessages = new HashMap<>();

    public interface MessageListener {
        void onMessagesUpdated(List<ChatMessage> messages);
    }

    public static abstract class AuthState {
        public static class Initializing extends AuthState {}
        public static class Authenticated extends AuthState {
            public final String uid;
            public final String tempId;
            public Authenticated(String uid, String tempId) {
                this.uid = uid;
                this.tempId = tempId;
            }
        }
        public static class Error extends AuthState {
            public final String message;
            public final String tempId;
            public Error(String message, String tempId) {
                this.message = message;
                this.tempId = tempId;
            }
        }
    }

    public ChatRepository(Context context) {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.sessionTempId = generateShortTempId();
    }

    public LiveData<AuthState> getAuthState() {
        return authState;
    }

    public String getSessionTempId() {
        return sessionTempId;
    }

    public String generateShortTempId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
    }

    public String refreshSessionTempId() {
        this.sessionTempId = generateShortTempId();
        FirebaseUser currentUser = auth.getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "local_" + UUID.randomUUID();
        authState.postValue(new AuthState.Authenticated(uid, sessionTempId));
        return sessionTempId;
    }

    /**
     * Silent Anonymous Authentication flow in Java.
     */
    public void performSilentAnonymousAuth() {
        authState.postValue(new AuthState.Initializing());
        try {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "Already authenticated anonymously in Java: " + currentUser.getUid());
                authState.postValue(new AuthState.Authenticated(currentUser.getUid(), sessionTempId));
            } else {
                auth.signInAnonymously()
                        .addOnSuccessListener(result -> {
                            FirebaseUser user = result.getUser();
                            String uid = user != null ? user.getUid() : UUID.randomUUID().toString();
                            Log.d(TAG, "Silent anonymous sign-in success in Java! UID: " + uid);
                            authState.postValue(new AuthState.Authenticated(uid, sessionTempId));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Silent anonymous sign-in failed: " + e.getLocalizedMessage());
                            authState.postValue(new AuthState.Error(
                                    e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Network/Auth Error",
                                    sessionTempId
                            ));
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase Auth not initialized: " + e.getLocalizedMessage());
            authState.postValue(new AuthState.Error(e.getLocalizedMessage(), sessionTempId));
        }
    }

    /**
     * Deterministic Room Generation Logic in Java:
     * Takes user's Temporary ID and peer's Temporary ID, sorts them lexicographically,
     * and combines them to form a unique, shared Firestore room ID.
     */
    public String generateDeterministicRoomId(String myTempId, String peerTempId) {
        String clean1 = myTempId.trim().toUpperCase(Locale.ROOT);
        String clean2 = peerTempId.trim().toUpperCase(Locale.ROOT);
        List<String> sorted = new ArrayList<>();
        sorted.add(clean1);
        sorted.add(clean2);
        Collections.sort(sorted);
        return "room_" + sorted.get(0) + "_" + sorted.get(1);
    }

    public void createOrJoinRoom(String roomId, String myTempId, String peerTempId) {
        try {
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("roomId", roomId);
            List<String> participants = new ArrayList<>();
            participants.add(myTempId);
            participants.add(peerTempId);
            roomData.put("participants", participants);
            roomData.put("createdAt", FieldValue.serverTimestamp());
            roomData.put("lastActive", FieldValue.serverTimestamp());

            firestore.collection("rooms").document(roomId).set(roomData);
        } catch (Exception e) {
            Log.w(TAG, "Could not sync room metadata: " + e.getLocalizedMessage());
        }
    }

    public ListenerRegistration listenToMessages(String roomId, MessageListener listener) {
        try {
            Query query = firestore.collection("rooms")
                    .document(roomId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING);

            return query.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.w(TAG, "Snapshot error: " + error.getLocalizedMessage());
                    List<ChatMessage> local = localRoomMessages.get(roomId);
                    listener.onMessagesUpdated(local != null ? local : new ArrayList<>());
                    return;
                }

                if (snapshot != null) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    List<ChatMessage> messages = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String text = doc.getString("text") != null ? doc.getString("text") : "";
                        String senderTempId = doc.getString("senderTempId") != null ? doc.getString("senderTempId") : "";
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        Boolean isSystem = doc.getBoolean("isSystemMessage");
                        String imageUrl = doc.getString("imageUrl");
                        String reaction = doc.getString("reaction");

                        Date date = timestamp != null ? timestamp.toDate() : new Date();

                        ChatMessage msg = new ChatMessage(
                                doc.getId(),
                                senderTempId,
                                text,
                                timestamp,
                                timeFormat.format(date),
                                isSystem != null && isSystem,
                                imageUrl,
                                reaction
                        );
                        messages.add(msg);
                    }

                    localRoomMessages.put(roomId, new ArrayList<>(messages));
                    listener.onMessagesUpdated(messages);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Listener exception: " + e.getLocalizedMessage());
            List<ChatMessage> local = localRoomMessages.get(roomId);
            listener.onMessagesUpdated(local != null ? local : new ArrayList<>());
            return null;
        }
    }

    public void sendMessage(String roomId, String senderTempId, String text, String imageUrl, Runnable onSuccess, Runnable onError) {
        if ((text == null || text.trim().isEmpty()) && imageUrl == null) return;

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderTempId", senderTempId);
        messageData.put("text", text != null ? text.trim() : "");
        messageData.put("timestamp", FieldValue.serverTimestamp());
        messageData.put("isSystemMessage", false);
        messageData.put("imageUrl", imageUrl);
        messageData.put("reaction", null);

        List<ChatMessage> localList = localRoomMessages.computeIfAbsent(roomId, k -> new ArrayList<>());
        ChatMessage localMsg = new ChatMessage(
                UUID.randomUUID().toString(),
                senderTempId,
                text != null ? text.trim() : "",
                Timestamp.now(),
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()),
                false,
                imageUrl,
                null
        );

        try {
            firestore.collection("rooms")
                    .document(roomId)
                    .collection("messages")
                    .add(messageData)
                    .addOnSuccessListener(ref -> {
                        Map<String, Object> update = new HashMap<>();
                        update.put("lastMessage", text);
                        update.put("lastMessageTime", FieldValue.serverTimestamp());
                        firestore.collection("rooms").document(roomId).update(update);
                        if (onSuccess != null) onSuccess.run();
                    })
                    .addOnFailureListener(e -> {
                        localList.add(localMsg);
                        if (onSuccess != null) onSuccess.run();
                    });
        } catch (Exception e) {
            localList.add(localMsg);
            if (onSuccess != null) onSuccess.run();
        }
    }

    public void sendSystemMessage(String roomId, String systemText) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderTempId", "SYSTEM");
        messageData.put("text", systemText);
        messageData.put("timestamp", FieldValue.serverTimestamp());
        messageData.put("isSystemMessage", true);

        try {
            firestore.collection("rooms")
                    .document(roomId)
                    .collection("messages")
                    .add(messageData);
        } catch (Exception e) {
            List<ChatMessage> localList = localRoomMessages.computeIfAbsent(roomId, k -> new ArrayList<>());
            localList.add(new ChatMessage(
                    UUID.randomUUID().toString(),
                    "SYSTEM",
                    systemText,
                    Timestamp.now(),
                    new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()),
                    true,
                    null,
                    null
            ));
        }
    }
}
