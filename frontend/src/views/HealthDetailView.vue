<template>
  <div class="min-h-screen bg-background-light flex flex-col items-center justify-center">
    <header class="absolute top-0 left-0 w-full p-4 flex items-center">
        <button @click="$router.back()" class="p-2 rounded-full hover:bg-gray-100 transition-colors">
            <span class="material-symbols-outlined text-[#1c140d]">arrow_back</span>
        </button>
        <h1 class="ml-2 text-xl font-bold text-[#1c140d]">건강 상세 페이지</h1>
    </header>
    <div class="text-center">
      <h2 class="text-2xl font-bold text-gray-500">건강상세페이지</h2>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.material-symbols-outlined {
    font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}
</style>
