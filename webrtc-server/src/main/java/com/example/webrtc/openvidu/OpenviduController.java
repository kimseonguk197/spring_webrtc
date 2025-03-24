package com.example.webrtc.openvidu;

import io.openvidu.java.client.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/api/openvidu")
@RestController
public class OpenviduController {

    private final OpenVidu openVidu;
    private final String SECRET = "MY_SECRET";

    public OpenviduController() {
        this.openVidu = new OpenVidu("http://localhost:4443/", SECRET);
    }

    // 새 세션 생성
    @PostMapping("/session")
    public ResponseEntity<String> createSession() throws OpenViduJavaClientException, OpenViduHttpException {
        Session session = openVidu.createSession();
        return ResponseEntity.ok(session.getSessionId());
    }

    // 현재 존재하는 세션 목록 가져오기
    @GetMapping("/sessions")
    public ResponseEntity<List<String>> getSessionList() throws OpenViduJavaClientException, OpenViduHttpException {
        openVidu.fetch(); // 최신 상태 동기화
        List<String> sessionIds = openVidu.getActiveSessions()
                .stream()
                .map(Session::getSessionId)
                .collect(Collectors.toList());

        return ResponseEntity.ok(sessionIds);
    }

    // 세션에 연결할 수 있는 token 발급
    @PostMapping("/token")
    public ResponseEntity<String> createToken(@RequestBody Map<String, String> body)
            throws OpenViduJavaClientException, OpenViduHttpException {
        openVidu.fetch();
        String sessionId = body.get("sessionId");
        Session session = openVidu.getActiveSession(sessionId);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found");
        }

        ConnectionProperties properties = new ConnectionProperties.Builder().build();
        Connection connection = session.createConnection(properties);
        return ResponseEntity.ok(connection.getToken());
    }

}
