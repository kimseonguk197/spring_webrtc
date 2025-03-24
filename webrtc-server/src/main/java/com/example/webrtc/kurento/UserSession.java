package com.example.webrtc.kurento;

import org.kurento.client.HubPort;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.socket.WebSocketSession;

public class UserSession {
    private final WebSocketSession session;
    private final WebRtcEndpoint endpoint;
    private final HubPort port;

    public UserSession(WebSocketSession session, WebRtcEndpoint endpoint, HubPort port) {
        this.session = session;
        this.endpoint = endpoint;
        this.port = port;
    }

    public WebRtcEndpoint getEndpoint() {
        return endpoint;
    }
}
