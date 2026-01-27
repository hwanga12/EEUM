<script setup>
import { onMounted } from 'vue'
import { useUserStore } from './stores/user'

const userStore = useUserStore()

onMounted(async () => {
  // 1. 주소창에서 accessToken이 있는지 확인 (백엔드 성공 핸들러가 보낸 것)
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('accessToken');

  if (token) {
    // 2. 토큰이 있다면 저장소에 저장
    localStorage.setItem('accessToken', token);
    
    // 3. 주소창 지저분하지 않게 파라미터 제거
    window.history.replaceState({}, document.title, window.location.pathname);
    
    console.log("새로운 토큰 저장 성공!");
  }

  // 4. 이제 토큰이 있는 상태에서 유저 정보를 가져옴
  // (만약 토큰이 없으면 fetchUser 내부에서 에러 처리가 될 겁니다)
  await userStore.fetchUser();
});
</script>

<template>
  <router-view />
</template>

<style>
/* 전체 화면 스타일링 */
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}
</style>