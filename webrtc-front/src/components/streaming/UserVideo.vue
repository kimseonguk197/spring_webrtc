<template>
<div v-if="streamManager">
	<ov-video :stream-manager="streamManager"/>
	<div><p>{{ clientData }}</p></div>
</div>
</template>

<script>
import OvVideo from './OvVideo';

export default {
	name: 'UserVideo',

	components: {
		OvVideo,
	},

	props: {
		streamManager: Object,
	},

	computed: {
		clientData () {
			const { clientData } = this.getConnectionData();
			return clientData;
		},
	},

	methods: {
		getConnectionData () {
			const connection = this.streamManager?.stream?.connection;

			if (connection?.data) {
				try {
					return JSON.parse(connection.data);
				} catch (e) {
					console.warn("connection.data 파싱 실패", e);
				}
			}

			return { clientData: "본인 또는 알 수 없음" }; // 기본값
		},
	},
};
</script>
