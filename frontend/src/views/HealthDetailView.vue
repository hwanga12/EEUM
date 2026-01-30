<template>
  <div class="min-h-screen bg-background-light flex flex-col items-center">
    <header class="w-full p-4 flex items-center bg-white shadow-sm h-[60px]">
        <button @click="$router.back()" class="p-2 rounded-full hover:bg-gray-100 transition-colors">
            <span class="material-symbols-outlined text-[#1c140d]">arrow_back</span>
        </button>
        <h1 class="ml-2 text-xl font-bold text-[#1c140d]">건강 상세</h1>
    </header>
    
    <div class="flex-1 w-full max-w-md p-6 flex flex-col items-center justify-start space-y-8 mt-4">
      <!-- Heart Rate Card -->
      <div class="w-full bg-white rounded-2xl shadow-md p-6 flex flex-col items-center space-y-4">
        <div class="flex items-center space-x-2 text-[#FF5252]">
           <span class="material-symbols-outlined text-4xl">monitor_heart</span>
           <span class="text-lg font-semibold">최근 심박수</span>
        </div>
        
        <div class="text-center">
            <p v-if="heartRate" class="text-5xl font-bold text-[#1c140d]">
                {{ heartRate }} <span class="text-xl text-gray-500 font-normal">bpm</span>
            </p>
            <p v-else class="text-gray-400 text-lg">
                {{ statusMessage }}
            </p>
        </div>
        
        <p class="text-xs text-gray-400" v-if="lastUpdated">
            마지막 업데이트: {{ lastUpdated }}
        </p>

        <button 
            @click="fetchHeartRate" 
            :disabled="isLoading"
            class="w-full py-3 px-6 bg-[#FF5252] text-white rounded-xl font-medium shadow-sm active:scale-95 transition-transform flex items-center justify-center space-x-2"
        >
            <span v-if="isLoading" class="animate-spin material-symbols-outlined text-sm">progress_activity</span>
            <span>{{ isLoading ? '측정 데이터 가져오는 중...' : '지금 측정하기' }}</span>
        </button>
      </div>

       <div class="w-full bg-white rounded-2xl p-4 shadow-sm">
           <p class="text-sm text-gray-500 leading-relaxed">
               * 삼성 헬스 앱에서 측정된 최신 심박수 데이터를 가져옵니다.<br>
               * 데이터가 보이지 않는다면 삼성 헬스 앱에서 권한을 확인해주세요.
           </p>
       </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const heartRate = ref(null);
const statusMessage = ref('데이터를 불러오는 중...');
const isLoading = ref(false);
const lastUpdated = ref('');

const fetchHeartRate = () => {
    if (window.AndroidBridge && window.AndroidBridge.fetchHeartRate) {
        isLoading.value = true;
        statusMessage.value = '삼성 헬스 데이터 요청 중...';
        // Android Native Method Call
        window.AndroidBridge.fetchHeartRate();
    } else {
        console.warn("AndroidBridge not found");
        statusMessage.value = '앱 환경이 아닙니다.';
        
        // Test Mock Data for Browser
        setTimeout(() => {
            console.log("Mocking data for browser");
            window.onReceiveHealthData(JSON.stringify({
                heart_rate: 75,
                unit: 'bpm',
                start_time: new Date().toISOString() // Mock time
            }));
        }, 1000);
    }
};

// Global callback for Android to call
window.onReceiveHealthData = (dataString) => {
    console.log("Received Health Data from Native:", dataString);
    isLoading.value = false;
    
    if (!dataString || dataString === "null") {
        statusMessage.value = '측정된 데이터가 없습니다.';
        return;
    }

    try {
        const data = JSON.parse(dataString);
        // data structure depends on SDK response, assuming typical Samsung Health structure or simplified logic
        // If the native code sends the raw SDK object, we might need to inspect it. 
        // Based on previous SamsungHealthManager.kt, it sends `Gson().toJson(latestData)`.
        
        // Let's assume 'heart_rate' or similar field exists, or 'count' if valid.
        // If it's the raw HealthData object, we usually look for specific fields.
        // For HeartRate, standard fields usually include 'heart_rate', 'heart_beat_count', 'bpm' etc.
        // Let's handle generic parsing or look for common keys.
        
        if (data.heart_rate) {
             heartRate.value = Math.round(data.heart_rate);
        } else if (data.bpm) {
             heartRate.value = Math.round(data.bpm);
        } else {
            // Fallback: dump the raw JSON to see what we got (for debugging)
             // Or if it's just a number
            if(typeof data === 'number') {
                heartRate.value = data;
            } else {
                 // Try finding any number in the object
                 const val = Object.values(data).find(v => typeof v === 'number');
                 heartRate.value = val ? Math.round(val) : 'N/A';
            }
        }

        const now = new Date();
        lastUpdated.value = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`;
        
    } catch (e) {
        console.error("Failed to parse health data:", e);
        statusMessage.value = '데이터 처리 오류';
    }
};

onMounted(() => {
    // Auto-fetch on mount
    fetchHeartRate();
});

onUnmounted(() => {
    // Cleanup global callback to prevent memory leaks or unwanted calls
    delete window.onReceiveHealthData;
});
</script>

<style scoped>
.material-symbols-outlined {
    font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}
</style>
