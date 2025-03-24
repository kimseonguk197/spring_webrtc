<template>
  <div>
    <button @click="joinSession">참여하기</button>

    <button @click="startScreenShare">🖥 화면 공유</button>
    <div class="video-wrapper">
      <div>
        <h3>📷 내 화면</h3>
        <div id="publisher-video" class="video-box"></div>
      </div>
      <div>
        <h3>👀 다른 참가자 화면</h3>
        <div id="subscribers-video" class="video-box"></div>
      </div>
    </div>
  </div>
</template>
<script>
import { OpenVidu } from 'openvidu-browser'
import axios from 'axios'

export default {
  data() {
    return {
      session: null,
      screenSession: null
    }
  },
  beforeUnmount() {
    // 세션이 없으면 오픈비두에서 알아서 세션을 삭제
    if (this.session) {
      this.session.disconnect()
      this.session = null
    }

    if (this.screenSession) {
      this.screenSession.disconnect()
      this.screenSession = null
    }
  },
  methods: {
    async joinSession() {
      if (this.session) {
        console.warn('이미 세션에 연결됨')
        return
      }

      // session초기화
      const OV = new OpenVidu()
      this.session = OV.initSession()

      const sessionId = this.$route.params.sessionId
      const tokenResponse = await axios.post('http://localhost:8080/api/openvidu/token', {
        sessionId
      })
      const token = tokenResponse.data
      this.session.on('streamCreated', (event) => {
        const subscriberContainer = document.createElement('div')
        subscriberContainer.className = 'subscriber-box'
        document.getElementById('subscribers-video').appendChild(subscriberContainer)
        const subscriber = this.session.subscribe(event.stream, subscriberContainer)
        console.log('🔗 다른 사용자 구독:', subscriber)
      })

      // token안의 값의 형태 =>  ws://localhost:4443?sessionId=ses_HrbfYPP2IK&token=tok_Qph8NaNuMdq2YYfH
      // 세션이 연결되는 순간 streamCreated한건씩 호출
      await this.session.connect(token)

      const publisher = OV.initPublisher('publisher-video', {
        audioSource: undefined,
        videoSource: undefined,
        publishAudio: true,
        publishVideo: true,
        resolution: '640x480',
        frameRate: 30,
        insertMode: 'APPEND',
        mirror: false
      })

      this.session.publish(publisher);
    },
    async startScreenShare() {
      const sessionId = this.$route.params.sessionId

      // 새 OpenVidu 인스턴스
      const screenOV = new OpenVidu()
      this.screenSession = screenOV.initSession()

      // 새로운 token 발급 (같은 sessionId지만 다른 사용자로 인식됨)
      const tokenResponse = await axios.post('http://localhost:8080/api/openvidu/token', {
        sessionId
      })
      const screenToken = tokenResponse.data

      // 연결
      await this.screenSession.connect(screenToken)

      // 화면 공유 퍼블리셔 생성
      const screenPublisher = await screenOV.initPublisherAsync(undefined, {
        videoSource: 'screen',
        publishAudio: false,
        publishVideo: true,
        mirror: false
      })

      // publish 화면 공유 스트림
      this.screenSession.publish(screenPublisher)
      console.log('🖥 별도 connection으로 화면 공유 시작됨')

      // 내 화면에 추가하고 싶으면 이 부분도
      const screenContainer = document.createElement('div')
      screenContainer.className = 'subscriber-box'
      document.getElementById('subscribers-video').appendChild(screenContainer)
      screenPublisher.addVideoElement(screenContainer)

      console.log('🖥 화면 공유 시작됨')
    }
  }
}
</script>

<style scoped>
.video-wrapper {
  display: flex;
  gap: 40px;
  margin-top: 20px;
}

.video-box {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 400px;
  min-height: 300px;
  border: 2px solid #ccc;
  padding: 10px;
}

.subscriber-box {
  border: 1px solid #888;
  padding: 4px;
}
video {
  width: 300px;
  height: auto;
}
</style>
