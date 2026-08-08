package com.example.ui;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.data.ChatMessage;
import com.example.repository.ChatRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    public enum ConnectionMode {
        SHARE_ID,
        SCAN_ENTER_ID
    }

    private final ChatRepository repository;

    private final MutableLiveData<String> userTempId = new MutableLiveData<>("");
    private final MutableLiveData<ConnectionMode> connectionMode = new MutableLiveData<>(ConnectionMode.SHARE_ID);
    private final MutableLiveData<String> peerInputId = new MutableLiveData<>("");
    private final MutableLiveData<String> activeRoomId = new MutableLiveData<>(null);
    private final MutableLiveData<String> activePeerId = new MutableLiveData<>(null);
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isCameraScannerOpen = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isConnecting = new MutableLiveData<>(false);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>(null);

    private ListenerRegistration activeMessageListener = null;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ChatRepository(application);
        this.userTempId.setValue(repository.getSessionTempId());

        // Perform Silent Anonymous Auth on ViewModel creation
        repository.performSilentAnonymousAuth();

        repository.getAuthState().observeForever(authState -> {
            if (authState instanceof ChatRepository.AuthState.Authenticated) {
                userTempId.setValue(((ChatRepository.AuthState.Authenticated) authState).tempId);
            } else if (authState instanceof ChatRepository.AuthState.Error) {
                userTempId.setValue(((ChatRepository.AuthState.Error) authState).tempId);
            }
        });
    }

    public LiveData<ChatRepository.AuthState> getAuthState() {
        return repository.getAuthState();
    }

    public LiveData<String> getUserTempId() { return userTempId; }
    public LiveData<ConnectionMode> getConnectionMode() { return connectionMode; }
    public LiveData<String> getPeerInputId() { return peerInputId; }
    public LiveData<String> getActiveRoomId() { return activeRoomId; }
    public LiveData<String> getActivePeerId() { return activePeerId; }
    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<Boolean> getIsCameraScannerOpen() { return isCameraScannerOpen; }
    public LiveData<Boolean> getIsConnecting() { return isConnecting; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    public void setConnectionMode(ConnectionMode mode) {
        connectionMode.setValue(mode);
        if (mode == ConnectionMode.SHARE_ID) {
            isCameraScannerOpen.setValue(false);
        }
    }

    public void updatePeerInputId(String input) {
        if (input == null) input = "";
        String cleaned = input.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (cleaned.length() > 10) cleaned = cleaned.substring(0, 10);
        peerInputId.setValue(cleaned);
    }

    public void toggleCameraScanner(Boolean open) {
        if (open != null) {
            isCameraScannerOpen.setValue(open);
        } else {
            Boolean current = isCameraScannerOpen.getValue();
            isCameraScannerOpen.setValue(current == null || !current);
        }
    }

    public void toggleCameraScanner() {
        toggleCameraScanner(null);
    }

    public void onQrScanned(String scannedText) {
        if (scannedText == null) return;
        String cleaned = scannedText.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (cleaned.length() > 10) cleaned = cleaned.substring(0, 10);
        if (!cleaned.isEmpty()) {
            peerInputId.setValue(cleaned);
            isCameraScannerOpen.setValue(false);
            showToast("Scanned Peer ID: " + cleaned);
            connectToPeer(cleaned);
        }
    }

    public void connectToPeer() {
        String input = peerInputId.getValue();
        connectToPeer(input != null ? input : "");
    }

    /**
     * Deterministic Room Generation Logic in Java ViewModel:
     * Sorts user Temporary ID and target peer Temporary ID to generate identical shared room ID.
     */
    public void connectToPeer(String targetPeerId) {
        if (targetPeerId == null) targetPeerId = "";
        String cleanPeer = targetPeerId.trim().toUpperCase();
        String myId = userTempId.getValue() != null ? userTempId.getValue().trim().toUpperCase() : "";

        if (cleanPeer.isEmpty()) {
            showToast("Please enter or scan a valid Peer Temporary ID");
            return;
        }

        if (cleanPeer.equalsIgnoreCase(myId)) {
            showToast("You cannot connect to your own Temporary ID");
            return;
        }

        isConnecting.setValue(true);

        String roomId = repository.generateDeterministicRoomId(myId, cleanPeer);
        activeRoomId.setValue(roomId);
        activePeerId.setValue(cleanPeer);

        repository.createOrJoinRoom(roomId, myId, cleanPeer);
        repository.sendSystemMessage(roomId, "Connected to room " + roomId);

        startListeningToRoom(roomId);

        isConnecting.setValue(false);
        showToast("Connected to Peer (" + cleanPeer + ")");
    }

    private void startListeningToRoom(String roomId) {
        if (activeMessageListener != null) {
            activeMessageListener.remove();
        }
        activeMessageListener = repository.listenToMessages(roomId, newMessages -> {
            messages.setValue(newMessages != null ? newMessages : new ArrayList<>());
        });
    }

    public void sendMessage(String text, String imageUrl) {
        String roomId = activeRoomId.getValue();
        String senderId = userTempId.getValue();
        if (roomId == null || senderId == null) return;

        repository.sendMessage(roomId, senderId, text, imageUrl, null, null);
    }

    public void sendMessage(String text) {
        sendMessage(text, null);
    }

    public void leaveRoom() {
        String roomId = activeRoomId.getValue();
        if (roomId != null) {
            repository.sendSystemMessage(roomId, "Peer " + userTempId.getValue() + " left the session");
        }
        if (activeMessageListener != null) {
            activeMessageListener.remove();
            activeMessageListener = null;
        }
        activeRoomId.setValue(null);
        activePeerId.setValue(null);
        messages.setValue(new ArrayList<>());
        peerInputId.setValue("");
        showToast("Disconnected from room");
    }

    public void regenerateSessionId() {
        String newId = repository.refreshSessionTempId();
        userTempId.setValue(newId);
        showToast("Generated new Temporary ID: " + newId);
    }

    public void copyIdToClipboard(Context context, String textToCopy) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Temporary Session ID", textToCopy);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied " + textToCopy + " to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }

    public void copyIdToClipboard(Context context) {
        String currentId = userTempId.getValue();
        copyIdToClipboard(context, currentId != null ? currentId : "");
    }

    public void clearToast() {
        toastMessage.setValue(null);
    }

    private void showToast(String msg) {
        toastMessage.setValue(msg);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (activeMessageListener != null) {
            activeMessageListener.remove();
        }
    }
}
