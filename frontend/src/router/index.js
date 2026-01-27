// src/router/index.js

import { createRouter, createWebHashHistory } from 'vue-router';
import MyProfileView from '../views/MyProfileView.vue';
import MyProfileEdit from '../views/MyProfileEdit.vue';
import VoiceSample from '../views/VoiceSample.vue';
import LoginView from '../views/Login.vue'; // 1. 여기서 'LoginView'로 가져옴

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView // 2. [수정] 위에서 가져온 이름인 'LoginView'로 변경! (Login -> LoginView)
  },
  {
    path: '/',
    redirect: '/login', // [추천] 바로 테스트할 수 있게 일단 로그인으로 보내버리세요
  },
  // ... 생략
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

export default router;