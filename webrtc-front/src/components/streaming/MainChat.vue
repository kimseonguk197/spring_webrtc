<template>
  
<div id="main-container" class="container">
  <div id="session" v-if="session">
    <div id="session-header">
      <h1 id="session-title">{{ mySessionId }}</h1>
      <input class="btn btn-large btn-danger" type="button" id="buttonLeaveSession" @click="leaveSession"
        value="Leave session" />
      <input
        v-if="myRole === 'streamer'"
        class="btn btn-large"
        :class="isScreenSharing ? 'btn-secondary' : 'btn-primary'"
        type="button"
        id="buttonLeaveSession"
        @click="toggleScreenShare"
        :value="isScreenSharing ? '화면 공유 중지' : '화면 공유'"
      />
      <input
        class="btn btn-large"
        :class="isRecording ? 'btn-secondary' : 'btn-success'"
        type="button"
        id="buttonLeaveSession"
        @click="isRecording ? stopRecording() : startRecording()"
        :value="isRecording ? '녹화 중지' : '녹화 시작'"
      />
    </div>
    <!-- Vue에서는 kebab-case로 <user-video>라고 쓰지만, 실제로는 UserVideo.vue 파일에서 정의된 컴포넌트를 사용하는 것 -->
    <div id="main-video" class="col-md-6">
      <user-video :stream-manager="mainStreamManager" />
    </div>
  </div>
</div>
</template>
<script>
import { OpenVidu } from 'openvidu-browser'
import axios from 'axios'
import UserVideo from './UserVideo'
export default {
  components: {
    UserVideo,
  },
  data() {
    return {
      // OpenVidu objects
      OV: undefined,
      session: undefined,
      mainStreamManager: undefined,
      publisher: undefined,
      subscribers: [],
      mySessionId: undefined,
      myUserName: undefined,
      isScreenSharing: false, // 화면 공유 상태
      myRole: "streamer", // 기본값
      // 녹화를 위한 변수
      mediaRecorder: null,
      recordedChunks: [],
      isRecording: false,
    }
  },
  created() {
    this.mySessionId = this.$route.params.sessionId
    this.myUserName = this.$route.query.userName
    this.myRole = this.$route.query.role
    axios.defaults.headers.post["Content-Type"] = "application/json";
    this.joinSession();
  },
  methods: {
    joinSession() {
      // --- 1) Get an OpenVidu object ---
      this.OV = new OpenVidu();

      // --- 2) Init a session ---
      this.session = this.OV.initSession();

      // --- 3) Specify the actions when events take place in the session ---

      // On every new Stream received...
      this.session.on("streamCreated", ({ stream }) => {
        const subscriber = this.session.subscribe(stream);
        this.subscribers.push(subscriber);
        
        if (this.myRole === "viewer" && this.mainStreamManager === undefined) {
          this.mainStreamManager = subscriber;
        }
      });

      // On every Stream destroyed...
      this.session.on("streamDestroyed", ({ stream }) => {
        const index = this.subscribers.indexOf(stream.streamManager, 0);
        if (index >= 0) {
          this.subscribers.splice(index, 1);
        }
      });

      // On every asynchronous exception...
      this.session.on("exception", ({ exception }) => {
        console.warn(exception);
      });

      // --- 4) Connect to the session with a valid user token ---

      // Get a token from the OpenVidu deployment
      this.getToken(this.mySessionId).then((token) => {

        // First param is the token. Second param can be retrieved by every user on event
        // 'streamCreated' (property Stream.connection.data), and will be appended to DOM as the user's nickname
        this.session.connect(token, { clientData: this.myUserName })
          .then(() => {
            if (this.myRole === "viewer") return;

            // --- 5) Get your own camera stream with the desired properties ---

            // Init a publisher passing undefined as targetElement (we don't want OpenVidu to insert a video
            // element: we will manage it on our own) and with the desired properties
            let publisher = this.OV.initPublisher(undefined, {
              audioSource: undefined, // The source of audio. If undefined default microphone
              videoSource: undefined, // The source of video. If undefined default webcam
              publishAudio: true, // Whether you want to start publishing with your audio unmuted or not
              publishVideo: true, // Whether you want to start publishing with your video enabled or not
              resolution: "640x480", // The resolution of your video
              frameRate: 30, // The frame rate of your video
              insertMode: "APPEND", // How the video is inserted in the target element 'video-container'
              mirror: false, // Whether to mirror your local video or not
            });

            // Set the main video in the page to display our webcam and store our Publisher
            this.mainStreamManager = publisher;
            console.log("publisher 객체 : ", publisher)  
            this.publisher = publisher;

            // --- 6) Publish your stream ---

            this.session.publish(this.publisher);
          })
          .catch((error) => {
            console.log("There was an error connecting to the session:", error.code, error.message);
          });
      });

      window.addEventListener("beforeunload", this.leaveSession);
    },
    async toggleScreenShare() {
      if (this.isScreenSharing) {
        await this.session.unpublish(this.publisher); // 기존 publish 종료
        this.joinSession();
        this.isScreenSharing = false;

      } else {
        // 시작
        await this.startCombinedStream();
        this.isScreenSharing = true;
      }
    },
    async startCombinedStream() {
      try {
        const webcamStream = await navigator.mediaDevices.getUserMedia({
          video: { width: 640, height: 480 },
          audio: true
        });

        const screenStream = await navigator.mediaDevices.getDisplayMedia({
          video: true
        });

        const canvas = document.createElement("canvas");
        canvas.width = 1280;
        canvas.height = 720;
        const ctx = canvas.getContext("2d");

        const webcamVideo = document.createElement("video");
        webcamVideo.srcObject = webcamStream;
        webcamVideo.muted = true;
        webcamVideo.play();

        const screenVideo = document.createElement("video");
        screenVideo.srcObject = screenStream;
        screenVideo.play();

        const draw = () => {
          ctx.drawImage(screenVideo, 0, 0, canvas.width, canvas.height); // 배경: 화면 공유
          ctx.drawImage(webcamVideo, canvas.width - 320 - 20, canvas.height - 240 - 20, 320, 240); // 오른쪽 아래에 웹캠
          requestAnimationFrame(draw);
        };
        draw();

        const combinedStream = canvas.captureStream(30); // 30fps
        const audioTrack = webcamStream.getAudioTracks()[0];
        combinedStream.addTrack(audioTrack);

        const publisher = await this.OV.initPublisherAsync(undefined, {
          audioSource: false,
          videoSource: combinedStream.getVideoTracks()[0],
          publishAudio: true,
          publishVideo: true,
          resolution: "1280x720",
          frameRate: 30,
          insertMode: "APPEND",
          mirror: false,
        });

        this.mainStreamManager = publisher;

        await this.session.unpublish(this.publisher); // 기존 publish 종료
        this.publisher = publisher;
        await this.session.publish(this.publisher);  // 합쳐진 stream publish

        console.log("🖼️ Combined stream published");
      } catch (err) {
        console.error("화면 + 웹캠 합성 실패:", err);
      }
    },

    leaveSession() {
      if (this.session) this.session.disconnect();

      this.session = undefined;
      this.mainStreamManager = undefined;
      this.publisher = undefined;
      this.subscribers = [];
      this.OV = undefined;

      window.removeEventListener("beforeunload", this.leaveSession);
      this.$router.push({
        path: `/streaming`
      });
      
    },
    
    startRecording() {
      const stream = this.mainStreamManager.stream.getMediaStream(); // 현재 공유 중인 스트림

      this.recordedChunks = [];
      this.mediaRecorder = new MediaRecorder(stream, { mimeType: 'video/webm; codecs=vp9' });

      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.recordedChunks.push(event.data);
        }
      };

      this.mediaRecorder.onstop = () => {
        const blob = new Blob(this.recordedChunks, { type: 'video/webm' });

        // 확장자만 mp4로 바꿔서 다운로드 (실제로는 webm 포맷)
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = `${this.mySessionId}_recording.mp4`; // 확장자만 mp4로
        document.body.appendChild(a);
        a.click();
        URL.revokeObjectURL(url);
      };

      this.mediaRecorder.start();
      this.isRecording = true;
    },


    stopRecording() {
      if (this.mediaRecorder && this.mediaRecorder.state !== "inactive") {
        this.mediaRecorder.stop();
        this.isRecording = false;
      }
    },
    async getToken(mySessionId) {
      const sessionId = await this.createSession(mySessionId);
      return await this.createToken(sessionId);
    },

    async createSession(sessionId) {
      const response = await axios.post('http://localhost:8080/api/sessions', { customSessionId: sessionId }, {
        headers: { 'Content-Type': 'application/json', },
      });
      return response.data; // The sessionId
    },

    async createToken(sessionId) {
      const response = await axios.post('http://localhost:8080/api/sessions/' + sessionId + '/connections', {}, {
        headers: { 'Content-Type': 'application/json', },
      });
      return response.data; // The token
    },
  }
}
</script>
