package com.moselo.HomingPigeon.Manager;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moselo.HomingPigeon.Helper.DefaultConstant;
import com.moselo.HomingPigeon.Helper.HomingPigeon;
import com.moselo.HomingPigeon.Listener.HomingPigeonNetworkListener;
import com.moselo.HomingPigeon.Listener.HomingPigeonSocketListener;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.moselo.HomingPigeon.Manager.ConnectionManager.ConnectionStatus.NOT_CONNECTED;

public class ConnectionManager {

    private String TAG = ConnectionManager.class.getSimpleName();
    private static ConnectionManager instance;
    private WebSocketClient webSocketClient;
    private String webSocketEndpoint = "wss://hp-staging.moselo.com:8080/pigeon?access_token=homingpigeon&user_id=";
    //        private String webSocketEndpoint = "ws://echo.websocket.org";
    private URI webSocketUri;
    private ConnectionStatus connectionStatus = NOT_CONNECTED;
    private List<HomingPigeonSocketListener> socketListeners;

    private int reconnectAttempt;
    private final long RECONNECT_DELAY = 500;

    public enum ConnectionStatus {
        CONNECTING, CONNECTED, DISCONNECTED, NOT_CONNECTED
    }

    public static ConnectionManager getInstance() {
        return instance == null ? (instance = new ConnectionManager()) : instance;
    }

    public ConnectionManager() {
        try {
//            webSocketUri = new URI(webSocketEndpoint);
//            initWebSocketClient(webSocketUri);
            initNetworkListener();
            socketListeners = new ArrayList<>();
            reconnectAttempt = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initWebSocketClient(URI webSocketUri) {
        webSocketClient = new WebSocketClient(webSocketUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                connectionStatus = ConnectionStatus.CONNECTED;
                reconnectAttempt = 0;
                if (null != socketListeners && !socketListeners.isEmpty()) {
                    for (HomingPigeonSocketListener listener : socketListeners)
                        listener.onSocketConnected();
                }
            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                String tempMessage = StandardCharsets.UTF_8.decode(bytes).toString();
                try {
                    HashMap response = new ObjectMapper().readValue(tempMessage, HashMap.class);
                    if (null != socketListeners && !socketListeners.isEmpty()) {
                        for (HomingPigeonSocketListener listener : socketListeners)
                            listener.onReceiveNewEmit(response.get("eventName").toString(), tempMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                connectionStatus = ConnectionStatus.DISCONNECTED;
                if (null != socketListeners && !socketListeners.isEmpty()) {
                    for (HomingPigeonSocketListener listener : socketListeners)
                        listener.onSocketDisconnected();
                }
            }

            @Override
            public void onError(Exception ex) {
                connectionStatus = ConnectionStatus.DISCONNECTED;
                if (null != socketListeners && !socketListeners.isEmpty()) {
                    for (HomingPigeonSocketListener listener : socketListeners)
                        listener.onSocketError();
                }
            }

            @Override
            public void reconnect() {
                super.reconnect();
                connectionStatus = ConnectionStatus.CONNECTING;
                if (null != socketListeners && !socketListeners.isEmpty()) {
                    for (HomingPigeonSocketListener listener : socketListeners)
                        listener.onSocketConnecting();
                }
            }
        };
    }

    private void initNetworkListener() {
        HomingPigeonNetworkListener networkListener = () -> {
            if (ConnectionStatus.CONNECTING == connectionStatus ||
                    ConnectionStatus.DISCONNECTED == connectionStatus) {
                reconnect();
            } else if (NOT_CONNECTED == connectionStatus) {
                connect();
            }
        };
        NetworkStateManager.getInstance().addNetworkListener(networkListener);
    }

    public void addSocketListener(HomingPigeonSocketListener listener) {
        socketListeners.add(listener);
    }

    public void removeSocketListener(HomingPigeonSocketListener listener) {
        socketListeners.remove(listener);
    }

    public void removeSocketListenerAt(int index) {
        socketListeners.remove(index);
    }

    public void clearSocketListener() {
        socketListeners.clear();
    }

    public void send(String messageString) {
        if (webSocketClient.isOpen()) {
            webSocketClient.send(messageString.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void connect() {
        if ((ConnectionStatus.DISCONNECTED == connectionStatus || NOT_CONNECTED == connectionStatus) &&
                NetworkStateManager.getInstance().hasNetworkConnection(HomingPigeon.appContext)) {
            try {
                webSocketUri = new URI(webSocketEndpoint+DataManager.getInstance().getActiveUser(HomingPigeon.appContext).getUserID());
                initWebSocketClient(webSocketUri);
                connectionStatus = ConnectionStatus.CONNECTING;
                webSocketClient.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (ConnectionStatus.CONNECTED == connectionStatus ||
                ConnectionStatus.CONNECTING == connectionStatus) {
            try {
                connectionStatus = ConnectionStatus.DISCONNECTED;
                webSocketClient.close();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void reconnect() {
        if (reconnectAttempt < 120) reconnectAttempt++;
        long delay = RECONNECT_DELAY * (long) reconnectAttempt;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (ConnectionStatus.DISCONNECTED == connectionStatus && !ChatManager.getInstance().isFinishChatFlow()) {
                    connectionStatus = ConnectionStatus.CONNECTING;
                    try {
                        webSocketClient.reconnect();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, delay);
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
}
