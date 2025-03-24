<template>
    <div>
      <h2>📝 화상채팅방 목록</h2>
      <ul>
        <li v-for="id in sessions" :key="id">
          <button @click="joinRoom(id)">입장: {{ id }}</button>
        </li>
      </ul>
  
      <button @click="createRoom">➕ 새 방 만들기</button>
    </div>
  </template>
  
  <script>
  import axios from 'axios'
  
  export default {
    data() {
      return {
        sessions: []
      }
    },
    created() {
        this.fetchSessions()
    },
    beforeRouteUpdate(to, from, next) {
        this.fetchSessions()
        next()
    },
    methods: {
      async fetchSessions() {
        const res = await axios.get('http://localhost:8080/api/openvidu/sessions')
        this.sessions = res.data
      },
      async createRoom() {
        const res = await axios.post('http://localhost:8080/api/openvidu/session')
        const sessionId = res.data
        this.$router.push(`/room/${sessionId}`);
      },
      joinRoom(sessionId) {
        this.$router.push(`/room/${sessionId}`);
      }
    }
  }
  </script>
  