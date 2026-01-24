<script setup>
import { useSamsungHealth } from '../composables/useSamsungHealth.js';
// import { useSamsungHealth } from '@/composables/useSamsungHealth.js';
const { heartRate, isLoading, fetchHeartRate } = useSamsungHealth();
</script>

<template>
  <div>
    <button @click="fetchHeartRate" :disabled="isLoading">
      {{ isLoading ? '조회 중...' : '실시간 심박수 가져오기' }}
    </button>
    
    <div v-if="heartRate">
      <p>심박수: {{ heartRate.heart_rate }} BPM</p>
    </div>
  </div>
</template>