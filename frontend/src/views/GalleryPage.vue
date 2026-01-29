<template>
  <div class="bg-background-light min-h-screen text-[#1c140d] pb-24 relative">
    
    <!-- Refined Header -->
    <header class="sticky top-0 z-30 bg-background-light/80 backdrop-blur-md px-4 pt-4 pb-4 transition-colors duration-200 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <button @click="$router.push({ name: 'HomePage' })" class="p-2 -ml-2 rounded-full hover:bg-gray-100 transition-colors">
          <span class="material-symbols-outlined text-[#1c140d]">arrow_back</span>
        </button>
        <h1 class="text-xl font-bold tracking-tight">가족 갤러리</h1>
      </div>
      <div class="flex items-center gap-1">
        <button class="p-2 rounded-full hover:bg-gray-100 transition-colors text-[#1c140d]">
          <span class="material-symbols-outlined">search</span>
        </button>
        <button class="p-2 rounded-full hover:bg-gray-100 transition-colors text-[#1c140d]">
          <span class="material-symbols-outlined">tune</span>
        </button>
      </div>
    </header>

    <main class="space-y-6">
      <!-- Recently Added (Swiper) -->
      <section class="recent-photos-section py-6 bg-[#F0EEE9]">
        <h2 class="px-6 text-lg font-bold text-[#1c140d] mb-4">최근 추가된 사진</h2>
        
        <swiper
          :effect="'creative'"
          :grabCursor="true"
          :centeredSlides="true"
          :slidesPerView="'auto'"
          :loop="true"
          :creativeEffect="{
            prev: {
              shadow: true,
              translate: ['-120%', 0, -500],
              rotate: [0, 0, -15],
              opacity: 0.6,
            },
            next: {
              shadow: true,
              translate: ['120%', 0, -500],
              rotate: [0, 0, 15],
              opacity: 0.6,
            },
          }"
          :modules="modules"
          class="recent-swiper"
        >
          <swiper-slide v-for="(photo, index) in dummyRecentPhotos" :key="index">
            <div class="photo-card relative group">
              <img :src="photo.url" class="rounded-2xl object-cover shadow-lg w-full h-full" />
              <div class="blur-overlay absolute inset-0 rounded-2xl bg-white/10 backdrop-blur-[2px] transition-opacity duration-300 group-[.swiper-slide-active]:opacity-0"></div>
              <div class="absolute bottom-4 left-4 text-white drop-shadow-md opacity-0 group-[.swiper-slide-active]:opacity-100 transition-opacity">
                <p class="text-sm font-bold">{{ photo.date }}</p>
                <p class="text-xs">{{ photo.author }}님이 올림</p>
              </div>
            </div>
          </swiper-slide>
        </swiper>
      </section>

      <!-- Family Albums Grid -->
      <section class="px-4">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-bold leading-tight tracking-tight text-[#1c140d]">가족 앨범</h3>
          <!-- <button class="text-primary text-sm font-bold hover:bg-primary/5 px-2 py-1 rounded-lg transition-colors">모두 보기</button> -->
        </div>
        <div class="grid grid-cols-3 gap-3">
          <!-- Dynamic Albums (from API) -->
          <div 
            v-for="album in albums" 
            :key="album.id" 
            class="flex flex-col gap-2 group cursor-pointer" 
            @click="navigateToAlbum(album.id)"
          >
            <div class="aspect-square rounded-2xl overflow-hidden relative shadow-sm">
              <img 
                class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" 
                :src="album.cover || 'https://via.placeholder.com/150'"
                alt="Album Cover"
              />
              <div class="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors"></div>
            </div>
            <div>
              <p class="text-sm font-bold truncate text-[#1c140d]">{{ album.title }}</p>
              <p class="text-[11px] text-[#9c7349]">{{ album.count }}장</p>
            </div>
          </div>

          <!-- Add Album Placehodler -->
          <div class="flex flex-col gap-2">
            <button class="aspect-square rounded-2xl border-2 border-dashed border-[#9c7349]/30 flex flex-col items-center justify-center gap-1 hover:bg-primary/5 hover:border-primary active:scale-95 transition-all">
              <div class="w-8 h-8 rounded-full bg-[#9c7349]/10 flex items-center justify-center">
                <span class="material-symbols-outlined text-[#9c7349]">add</span>
              </div>
              <p class="text-[11px] font-bold text-[#9c7349]">새 앨범</p>
            </button>
          </div>
        </div>
      </section>

    </main>

    <!-- Floating Action Button -->
    <button @click="triggerFileInput" class="fixed bottom-32 right-6 w-14 h-14 bg-primary text-white rounded-full shadow-lg shadow-primary/30 flex items-center justify-center active:scale-95 transition-transform z-30">
      <span v-if="!isUploading" class="material-symbols-outlined text-3xl">add_photo_alternate</span>
      <span v-else class="material-symbols-outlined text-3xl animate-spin">progress_activity</span>
    </button>
    <input type="file" ref="fileInput" class="hidden" accept="image/*" @change="handleFileUpload" />
    
    <BottomNav />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/layout/BottomNav.vue';
import { useFamilyStore } from '@/stores/family';
import { getPhotos, uploadFile } from '@/services/albumService';

// Swiper Imports
import { Swiper, SwiperSlide } from 'swiper/vue';
import { EffectCreative } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/effect-creative';

const modules = [EffectCreative];

const router = useRouter();
const familyStore = useFamilyStore();
const photos = ref([]);
const fileInput = ref(null);
const isUploading = ref(false);

// Dummy Data for Swiper (Requested)
const dummyRecentPhotos = [
  { url: 'https://images.unsplash.com/photo-1511895426328-dc8714191300', date: '2026.01.28', author: '민수' },
  { url: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb', date: '2026.01.27', author: '영희' },
  { url: 'https://images.unsplash.com/photo-1501785888041-af3ef285b470', date: '2026.01.25', author: '철수' },
  { url: 'https://images.unsplash.com/photo-1472214103451-9374bd1c798e', date: '2026.01.24', author: '지민' },
  { url: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05', date: '2026.01.20', author: '현우' },
];

// Computed Albums for API Data
const albums = computed(() => {
    if (photos.value.length === 0) return [];
    
    // Create a default "All Photos" album
    return [
        { 
            id: 1, 
            title: '전체 사진', 
            count: photos.value.length, 
            cover: photos.value[0]?.storageUrl || photos.value[0]?.imageUrl 
        },
    ];
});

const fetchAlbumPhotos = async () => {
    if (!familyStore.selectedFamily) return;
    try {
        const response = await getPhotos(familyStore.selectedFamily.id);
        console.log("getPhotos response:", response);
        // Checking for different possible structures of response
        if (Array.isArray(response.data)) {
            photos.value = response.data;
        } else if (response.data && Array.isArray(response.data.data)) {
            photos.value = response.data.data;
        } else if (response.data && Array.isArray(response.data.result)) {
            photos.value = response.data.result;
        } else if (response.data && Array.isArray(response.data.content)) {
            photos.value = response.data.content;
        } else {
            console.warn("Unexpected response structure:", response.data);
            photos.value = [];
        }
    } catch (error) {
        console.error("Failed to fetch photos:", error);
    }
};

const triggerFileInput = () => {
    fileInput.value.click();
};

const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file || !familyStore.selectedFamily) return;

    isUploading.value = true;
    try {
        await uploadFile(familyStore.selectedFamily.id, file);
        // Refresh list
        await fetchAlbumPhotos();
        alert('사진이 업로드되었습니다.');
    } catch (error) {
        alert('사진 업로드에 실패했습니다.');
        console.error(error);
    } finally {
        isUploading.value = false;
        event.target.value = ''; // Reset input
    }
};

const navigateToAlbum = (albumId) => {
  router.push({ 
      name: 'AlbumPage', 
      params: { id: albumId } 
  });
};

onMounted(() => {
    if (familyStore.selectedFamily) {
        fetchAlbumPhotos();
    }
});
</script>

<style scoped>
.recent-swiper {
  width: 100%;
  padding-top: 20px;
  padding-bottom: 50px;
}

.swiper-slide {
  width: 260px; /* Card Width */
  height: 340px; /* Card Height */
}

.photo-card {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

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

/* Swiper Active Slide Effects */
.swiper-slide:not(.swiper-slide-active) img {
  filter: blur(4px);
  transition: filter 0.3s ease;
}

.swiper-slide-active img {
  filter: blur(0);
  transform: scale(1.05);
  transition: all 0.3s ease;
}
</style>
