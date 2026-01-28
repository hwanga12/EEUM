<template>
  <div class="bg-[var(--bg-page)] min-h-screen flex justify-center items-center">
    <!-- Intro State -->
    <transition name="fade" mode="out-in">
      <div v-if="step === 'intro'" key="intro" class="relative w-full h-full min-h-screen bg-[var(--bg-page)] flex flex-col">
          
        <!-- Header (Intro) -->
        <header class="flex items-center justify-between px-6 py-4 mt-6">
          <button @click="$router.push('/home')" class="w-10 h-10 flex items-center justify-center rounded-full bg-white hover:bg-gray-100 transition-colors shadow-sm">
            <span class="material-symbols-rounded text-slate-700">arrow_back_ios_new</span>
          </button>
          <div class="w-10"></div> 
        </header>

        <main class="flex flex-col items-center px-8 pt-6 flex-1">
          <div class="text-center mb-12">
            <p class="text-[var(--color-primary)] font-semibold mb-1 text-lg">안녕하세요!</p>
            <h1 class="text-3xl font-bold leading-tight text-gray-900">목소리 등록을 <br/>시작해볼까요?</h1>
          </div>

          <!-- Wave Animation Area -->
          <div class="relative flex-1 flex items-center justify-center w-full min-h-[300px]">
            <template v-if="localRecording">
               <div class="absolute w-72 h-72 rounded-full bg-[var(--color-primary)]/10 wave-animation"></div>
               <div class="absolute w-56 h-56 rounded-full bg-[var(--color-primary)]/20 wave-animation" style="animation-delay: 0.5s;"></div>
               <div class="absolute w-40 h-40 rounded-full bg-[var(--color-primary)]/30 wave-animation" style="animation-delay: 1s;"></div>
            </template>
            
            <div class="relative z-10 w-28 h-28 bg-white rounded-full flex items-center justify-center shadow-lg border-4 border-[var(--color-primary)]/20">
              <span class="material-symbols-rounded text-6xl text-[var(--color-primary)] leading-none">family_history</span>
            </div>
          </div>

          <!-- Action Area -->
          <div class="w-full flex flex-col items-center pb-12 mt-auto">
            <p class="text-slate-500 mb-8 text-base font-medium transition-all" :class="{'opacity-0': localRecording}">
               {{ localRecording ? '준비 중...' : '버튼을 눌러 목소리 등록을 시작하세요' }}
            </p>
            
            <button 
              @click="toggleIntroRecording"
              class="group relative w-24 h-24 bg-[var(--color-primary)] rounded-full flex items-center justify-center mic-shadow active:scale-95 transition-transform"
            >
              <span class="material-symbols-rounded text-5xl text-white">
                  {{ localRecording ? 'stop' : 'mic' }}
              </span>
              <span v-if="localRecording" class="absolute inset-0 rounded-full border-4 border-[var(--color-primary)] animate-ping opacity-25"></span>
            </button>

            <!-- Guide Card -->
            <div class="mt-12 px-6 py-5 bg-orange-50 rounded-2xl border border-orange-100 w-full shadow-sm">
              <div class="flex gap-4">
                <span class="material-symbols-rounded text-[var(--color-primary)] text-2xl">info</span>
                <div>
                  <p class="text-xs font-bold text-orange-900 uppercase tracking-wider mb-1">녹음 가이드</p>
                  <p class="text-sm text-slate-600 leading-relaxed break-keep">
                      조용한 환경에서 진행해주세요. 휴대폰을 얼굴에서 약 25cm 정도 거리를 두고 말씀해주세요.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>

      <!-- Recording Step 1 State -->
      <div v-else key="recording" class="relative w-full h-full min-h-screen bg-[var(--bg-page)] flex flex-col">
         <!-- Header -->
        <header class="h-14 flex items-center justify-between px-2 pt-2">
          <button 
            @click="step = 'intro'"
            class="p-2 -ml-2 hover:bg-black/5 rounded-full transition-colors"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-[var(--text-title)]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h1 class="text-lg font-bold text-[var(--text-title)]">목소리 등록</h1>
          <div class="w-10"></div> 
        </header>

        <!-- Progress -->
        <div class="px-6 mt-6">
          <div class="flex justify-between items-end mb-2">
            <span class="text-sm font-medium text-[var(--text-sub)]">등록 단계</span>
            <span class="text-lg font-bold text-[var(--color-primary)]">1 <span class="text-sm font-normal text-gray-400">/ 6</span></span>
          </div>
          <div class="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
            <div class="progress-fill h-full bg-[var(--color-primary)]"></div>
          </div>
        </div>

        <!-- Main Content -->
        <main class="flex-1 flex flex-col items-center justify-center px-8 text-center">
          <p class="text-[var(--text-sub)] mb-8 font-medium">아래 문장을 또박또박 읽어주세요.</p>
          
          <div class="min-h-[160px] flex items-center justify-center">
            <h2 class="text-2xl font-bold leading-tight tracking-tight text-[var(--text-title)] break-keep">
              "안녕하세요, 오늘 기분이 참 좋네요. 잘 지내시나요?"
            </h2>
          </div>

          <!-- Wave Animation -->
          <div class="mt-12 mb-8 flex items-center justify-center gap-2 h-16">
            <div v-for="n in 9" :key="n" class="wave-bar w-1.5 bg-[var(--color-primary)] rounded-full"></div>
          </div>

          <!-- Timer -->
          <div class="flex items-center gap-2 text-[var(--color-primary)] font-bold text-xl">
            <span class="material-icons text-sm animate-pulse">fiber_manual_record</span>
            <span>00:07</span>
            <span class="text-gray-400 font-normal">/ 00:10</span>
          </div>
        </main>

        <!-- Footer -->
        <footer class="p-8 space-y-4 mb-8">
          <div class="bg-orange-50 p-4 rounded-xl flex gap-3 border border-orange-100">
            <span class="material-icons text-[var(--color-primary)] text-xl">info</span>
            <p class="text-xs text-orange-800 text-left leading-relaxed break-keep">
              조용한 환경에서 자연스러운 속도로 말해주세요.
            </p>
          </div>
          
          <button class="w-full bg-[var(--color-primary)] hover:bg-[#d95d41] text-white font-bold py-4 rounded-2xl shadow-lg shadow-orange-500/20 transition-all active:scale-[0.98]">
            완료 및 다음
          </button>
          
          <button class="w-full text-gray-400 font-medium py-2 text-sm hover:text-[var(--color-primary)] transition-colors">
            다시 녹음하기
          </button>
        </footer>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const step = ref('intro'); // 'intro' | 'recording'
const localRecording = ref(false);

const toggleIntroRecording = () => {
    if (localRecording.value) {
        // Stop logic if needed
        localRecording.value = false;
    } else {
        // Start simulation
        localRecording.value = true;
        // After small delay, transition to recording step
        setTimeout(() => {
            localRecording.value = false;
            step.value = 'recording';
        }, 800);
    }
};
</script>

<style scoped>
.wave-animation {
    animation: pulse-wave 2s infinite ease-in-out;
}
@keyframes pulse-wave {
    0% { transform: scale(0.8); opacity: 0.4; }
    50% { transform: scale(1.1); opacity: 0.1; }
    100% { transform: scale(0.8); opacity: 0.4; }
}
.mic-shadow {
    box-shadow: 0 10px 25px -5px rgba(231, 111, 81, 0.4); /* Primary color shadow approx */
}

.wave-bar {
  animation: wave 1.2s ease-in-out infinite;
}

@keyframes wave {
  0%, 100% { height: 12px; opacity: 0.5; }
  50% { height: 48px; opacity: 1; }
}

.wave-bar:nth-child(1) { animation-delay: 0.0s; }
.wave-bar:nth-child(2) { animation-delay: 0.1s; }
.wave-bar:nth-child(3) { animation-delay: 0.2s; }
.wave-bar:nth-child(4) { animation-delay: 0.3s; }
.wave-bar:nth-child(5) { animation-delay: 0.4s; }
.wave-bar:nth-child(6) { animation-delay: 0.5s; }
.wave-bar:nth-child(7) { animation-delay: 0.6s; }
.wave-bar:nth-child(8) { animation-delay: 0.7s; }
.wave-bar:nth-child(9) { animation-delay: 0.8s; }

.progress-fill {
  width: 16.66%;
  transition: width 0.5s ease-in-out;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

