<template>
  <div class="bg-background-light min-h-screen text-[#1c140d] flex flex-col relative overflow-hidden">
    <!-- Modal Like Header (Pull bar) -->
    <div class="flex flex-col items-center bg-background-light pt-2">
      <div class="h-1.5 w-10 rounded-full bg-[#e8dbce]"></div>
    </div>

    <!-- Top AppBar -->
    <div class="flex items-center bg-background-light p-4 pb-2 justify-between sticky top-0 z-10">
      <div class="flex items-center gap-2">
        <button @click="$router.back()" class="flex items-center justify-center p-2 rounded-full hover:bg-primary/10 transition-colors">
          <span class="material-symbols-outlined text-[#1c140d]">close</span>
        </button>
        <div class="flex flex-col">
          <h2 class="text-[#1c140d] text-xl font-bold leading-tight tracking-[-0.015em]">가족 앨범</h2>
          <p class="text-xs text-[#9c7349] font-medium">{{ photos.length }}개 항목 • 가족 공유</p>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <button class="flex items-center gap-1 text-primary text-base font-bold leading-normal tracking-[0.015em]">
          <span class="material-symbols-outlined text-lg">tune</span>
          <span>필터</span>
        </button>
        <button class="text-primary text-base font-bold leading-normal tracking-[0.015em] shrink-0">편집</button>
      </div>
    </div>

    <!-- Filter Chips Section -->
    <div class="flex gap-2 px-4 py-3 overflow-x-auto hide-scrollbar shrink-0">
      <button class="flex h-9 shrink-0 items-center justify-center gap-x-2 rounded-xl bg-primary text-white px-4">
        <p class="text-sm font-semibold">전체 기간</p>
      </button>
      <button class="flex h-9 shrink-0 items-center justify-center gap-x-2 rounded-xl bg-[#f4ede7] px-4 border border-[#e8dbce]">
        <p class="text-[#1c140d] text-sm font-medium">즐겨찾기</p>
      </button>
      <button class="flex h-9 shrink-0 items-center justify-center gap-x-2 rounded-xl bg-[#f4ede7] px-4 border border-[#e8dbce]">
        <p class="text-[#1c140d] text-sm font-medium">이번 달</p>
      </button>
      <button class="flex h-9 shrink-0 items-center justify-center gap-x-2 rounded-xl bg-[#f4ede7] px-4 border border-[#e8dbce]">
        <p class="text-[#1c140d] text-sm font-medium">가족 행사</p>
      </button>
    </div>

    <!-- 4-Column ImageGrid -->
    <div class="flex-1 overflow-y-auto px-4 pb-24">
      <div v-if="photos.length > 0" class="grid grid-cols-4 gap-1.5">
        <div 
            v-for="photo in photos" 
            :key="photo.photoId || photo.id" 
            class="relative group aspect-square"
        >
          <div 
            class="w-full h-full bg-center bg-no-repeat bg-cover rounded-sm cursor-pointer border-2 border-transparent hover:border-primary transition-colors" 
            :style="{ backgroundImage: `url(${photo.storageUrl || photo.imageUrl})` }"
          ></div>
        </div>
      </div>
      <div v-else class="flex flex-col items-center justify-center h-64 text-gray-500">
          <span class="material-symbols-outlined text-4xl mb-2">image_not_supported</span>
          <p>사진이 없습니다.</p>
      </div>
    </div>

    <!-- Floating Action Bar (Contextual for Editing) -->
    <div class="fixed bottom-0 left-0 right-0 p-4 bg-white/90 backdrop-blur-md border-t border-[#e8dbce]">
      <div class="flex items-center justify-between max-w-lg mx-auto">
        <div class="flex flex-col">
          <p class="text-sm font-bold text-[#1c140d]">선택 항목 없음</p>
          <p class="text-[10px] text-[#9c7349] uppercase tracking-wider font-semibold">길게 눌러 선택</p>
        </div>
        <div class="flex gap-2">
          <button class="flex items-center justify-center p-3 rounded-full bg-[#f4ede7] text-primary">
            <span class="material-symbols-outlined">share</span>
          </button>
          <button class="flex items-center justify-center p-3 rounded-full bg-[#f4ede7] text-primary">
            <span class="material-symbols-outlined">folder_copy</span>
          </button>
          <button class="flex items-center justify-center p-3 rounded-full bg-red-100 text-red-600">
            <span class="material-symbols-outlined">delete</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useFamilyStore } from '@/stores/family';
import { getPhotos } from '@/services/albumService';

const familyStore = useFamilyStore();
const photos = ref([]);

const fetchPhotos = async () => {
    if (!familyStore.selectedFamily) return;
    try {
        const response = await getPhotos(familyStore.selectedFamily.id);
        if (response.data && Array.isArray(response.data.data)) {
            photos.value = response.data.data;
        } else if (Array.isArray(response.data)) {
            photos.value = response.data;
        } else {
             photos.value = [];
        }
    } catch (error) {
        console.error("Failed to fetch album photos:", error);
    }
};
onMounted(() => {
    fetchPhotos();
});
</script>

<style scoped>
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.material-symbols-outlined {
    font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}
</style>
