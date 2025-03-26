package com.example.webrtc.openvidu;

import io.openvidu.java.client.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class OpenviduController {

    private final OpenVidu openvidu;
    private final String SECRET = "MY_SECRET";

    public OpenviduController() {
        this.openvidu = new OpenVidu("http://localhost:4443/", SECRET);
    }


    // 현재 존재하는 세션 목록 가져오기
    @GetMapping("/api/sessions")
    public ResponseEntity<List<String>> getSessionList() throws OpenViduJavaClientException, OpenViduHttpException {
        openvidu.fetch(); // 최신 상태 동기화
        List<String> sessionIds = openvidu.getActiveSessions()
                .stream()
                .map(Session::getSessionId)
                .collect(Collectors.toList());

        return ResponseEntity.ok(sessionIds);
    }

    @PostMapping("/api/sessions")
    public ResponseEntity<String> initializeSession(@RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {
        SessionProperties properties = SessionProperties.fromJson(params).build();
        Session session = openvidu.createSession(properties);
        return new ResponseEntity<>(session.getSessionId(), HttpStatus.OK);
    }

    @PostMapping("/api/sessions/{sessionId}/connections")
    public ResponseEntity<String> createConnection(@PathVariable("sessionId") String sessionId,
                                                   @RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {
        Session session = openvidu.getActiveSession(sessionId);
        if (session == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        ConnectionProperties properties = ConnectionProperties.fromJson(params).build();
        Connection connection = session.createConnection(properties);
        return new ResponseEntity<>(connection.getToken(), HttpStatus.OK);
    }

}
