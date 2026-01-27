import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// [추가된 핵심 코드] 요청 인터셉터: 모든 API 요청 전에 실행됩니다.
apiClient.interceptors.request.use(
  (config) => {
    // 1. localStorage에서 저장된 토큰을 꺼냅니다.
    const token = localStorage.getItem('accessToken');
    
    // 2. 토큰이 있다면 Bearer 방식으로 헤더에 추가합니다.
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const getUserProfile = () => {
  return apiClient.get('/users/profile/me');
};

export const updateUserProfile = (formData) => {
  return apiClient.put('/users/profile', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const joinFamilyWithCode = (inviteCode) => {
  return apiClient.post('/families/join', inviteCode, {
    headers: {
      'Content-Type': 'text/plain',
    },
    transformRequest: [(data) => data],
  });
};

export default apiClient;