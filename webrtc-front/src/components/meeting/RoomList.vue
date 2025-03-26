<template>
<div id="main-container" class="container">
  <div id="join" v-if="!session">
    <div id="img-div">
      <!-- <img src="resources/images/openvidu_grey_bg_transp_cropped.png" /> -->
    </div>
    <div id="join-dialog" class="jumbotron vertical-center">
      <h1>화상회의시작하기</h1>
      <div class="form-group">
        <p>
          <label>Participant</label>
          <input v-model="myUserName" class="form-control" type="text" required />
        </p>
        <p>
          <label>Session</label>
          <select v-model="mySessionId" class="form-control">
            <option disabled value="">-- 세션 선택 (또는 아래에 입력) --</option>
            <option v-for="id in sessionList" :key="id" :value="id">{{ id }}</option>
          </select>
        </p>
        <p>
          <label>새 세션 입력</label>
          <input v-model="mySessionId" class="form-control" type="text" placeholder="새 세션 ID 입력" />
        </p>
        <p class="text-center">
          <button class="btn btn-lg btn-success" @click="joinSession()">
            Join!
          </button>
        </p>
      </div>
    </div>
  </div>
</div>
</template>

<script>
import axios from "axios";


export default {
  data() {
    return {
      // Join form
      mySessionId: "SessionA",
      myUserName: "Participant" + Math.floor(Math.random() * 100),
      sessionList: [],
    }
  },
  created() {
    this.fetchSessionList();
    axios.defaults.headers.post["Content-Type"] = "application/json";
  },
  methods: {
    async fetchSessionList() {
      try {
        const response = await axios.get("http://localhost:8080/api/sessions");
        this.sessionList = response.data;
      } catch (error) {
        console.error("세션 목록을 불러오는 데 실패했습니다:", error);
      }
    },
    async joinSession() {
      this.$router.push({
        path: `/room/${this.mySessionId}`,
        query: { userName: this.myUserName }
      });
    },
  }
}
</script>
  