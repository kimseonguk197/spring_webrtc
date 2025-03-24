package com.example.webrtc.kurento;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kurento.client.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalHandler extends TextWebSocketHandler {

    private final KurentoClient kurento = KurentoClient.create("ws://localhost:8888/kurento");
    private final Map<String, UserSession> users = new ConcurrentHashMap<>();
    private MediaPipeline pipeline;
    private Composite composite;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (pipeline == null) {
            pipeline = kurento.createMediaPipeline();
            composite = new Composite.Builder(pipeline).build();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(message.getPayload());

        String id = node.get("id").asText();

        if ("start".equals(id)) {
            WebRtcEndpoint endpoint = new WebRtcEndpoint.Builder(pipeline).build();
            HubPort port = new HubPort.Builder(composite).build();

            endpoint.connect(port);
            port.connect(endpoint);

            UserSession user = new UserSession(session, endpoint, port);
            users.put(session.getId(), user);

            endpoint.addIceCandidateFoundListener(event -> {
                ObjectNode ice = mapper.createObjectNode();
                ice.put("id", "iceCandidate");
                ice.put("candidate", event.getCandidate().getCandidate());
                ice.put("sdpMid", event.getCandidate().getSdpMid());
                ice.put("sdpMLineIndex", event.getCandidate().getSdpMLineIndex());
                send(session, ice);
            });

            String sdpOffer = node.get("sdpOffer").asText();
            String sdpAnswer = endpoint.processOffer(sdpOffer);

            ObjectNode resp = mapper.createObjectNode();
            resp.put("id", "startResponse");
            resp.put("sdpAnswer", sdpAnswer);
            send(session, resp);

            endpoint.gatherCandidates();
        }

        if ("iceCandidate".equals(id)) {
            IceCandidate candidate = new IceCandidate(
                    node.get("candidate").asText(),
                    node.get("sdpMid").asText(),
                    node.get("sdpMLineIndex").asInt()
            );
            users.get(session.getId()).getEndpoint().addIceCandidate(candidate);
        }
    }

    private void send(WebSocketSession session, ObjectNode message) {
        try {
            session.sendMessage(new TextMessage(message.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
