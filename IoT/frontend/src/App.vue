<script setup>
import { onMounted, onUnmounted } from 'vue';
import { useAlertStore } from './stores/alert';
import { requestFullscreen } from './utils/fullscreen';
import AlertOverlay from './components/AlertOverlay.vue';
import MainView from './views/MainView.vue';

const alertStore = useAlertStore();

// "Always Fullscreen" - Request on first interaction
const enableAutoFullscreen = async () => {
  const success = await requestFullscreen();
  if (success) {
    console.log("🚀 Fullscreen enabled on first interaction");
    window.removeEventListener('click', enableAutoFullscreen);
    window.removeEventListener('touchstart', enableAutoFullscreen);
  }
};

onMounted(() => {
  alertStore.connect();
  window.addEventListener('click', enableAutoFullscreen);
  window.addEventListener('touchstart', enableAutoFullscreen);
});

onUnmounted(() => {
  alertStore.disconnect();
  window.removeEventListener('click', enableAutoFullscreen);
  window.removeEventListener('touchstart', enableAutoFullscreen);
});
</script>

<template>
  <AlertOverlay />
  <MainView />
</template>

<style>
/* Global Reset or specific styles if needed */
html, body, #app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}
</style>
