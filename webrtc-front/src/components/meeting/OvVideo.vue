<template>
	<video autoplay/>
</template>

<script>
export default {
	name: 'OvVideo',

	props: {
		streamManager: Object,
	},

	mounted () {
		if (this.streamManager) {
			this.streamManager.addVideoElement(this.$el);
		}
	},
	watch: {
		streamManager(newVal) {
		if (newVal) {
			newVal.addVideoElement(this.$el);
		}
		},
	},
};
</script>
