<template>
  <section
    v-if="slides.length"
    class="home-carousel"
    role="region"
    aria-label="广告轮播"
    @mouseenter="stopAutoplay"
    @mouseleave="startAutoplay"
  >
    <div class="carousel-track">
      <article
        v-for="(ad, index) in slides"
        :key="ad.id"
        class="carousel-slide"
        :class="{ active: index === currentIndex }"
      >
        <a class="slide-content" :href="ad.targetUrl" target="_blank" rel="noopener noreferrer">
          <img :src="ad.imageUrl" :alt="ad.title" loading="lazy" />
          <div class="slide-overlay">
            <h3>{{ ad.title }}</h3>
          </div>
        </a>
      </article>
    </div>

    <button
      v-if="slides.length > 1"
      type="button"
      class="nav-button prev"
      aria-label="上一张广告"
      @click="goPrevious(true)"
    >
      ‹
    </button>
    <button
      v-if="slides.length > 1"
      type="button"
      class="nav-button next"
      aria-label="下一张广告"
      @click="goNext(true)"
    >
      ›
    </button>

    <div v-if="slides.length > 1" class="indicators" role="tablist">
      <button
        v-for="(ad, index) in slides"
        :key="`indicator-${ad.id}`"
        type="button"
        class="indicator"
        :class="{ active: index === currentIndex }"
        :aria-label="`查看广告 ${index + 1}`"
        :aria-pressed="index === currentIndex"
        role="tab"
        @click="goTo(index)"
      ></button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import type { HomeAd } from "@/types/homeAd";

const props = defineProps<{
  ads: HomeAd[];
  autoplay?: boolean;
  interval?: number;
}>();

const DEFAULT_INTERVAL = 5000;

const sanitizeSlides = (ads: HomeAd[]): HomeAd[] => {
  if (!Array.isArray(ads) || !ads.length) {
    return [];
  }
  const map = new Map<number, HomeAd>();
  ads.forEach((raw) => {
    if (!raw) return;
    const id = Number((raw as any).id ?? raw.id);
    if (!Number.isFinite(id) || map.has(id)) return;
    const title = typeof raw.title === "string" ? raw.title.trim() : "";
    const imageUrl = typeof raw.imageUrl === "string" ? raw.imageUrl.trim() : "";
    const targetUrl = typeof raw.targetUrl === "string" ? raw.targetUrl.trim() : "";
    const orderValue = Number((raw as any).displayOrder ?? raw.displayOrder ?? 0);
    const displayOrder = Number.isFinite(orderValue) ? Math.max(0, Math.floor(orderValue)) : 0;
    if (!title || !imageUrl || !targetUrl) {
      return;
    }
    map.set(id, {
      ...raw,
      id,
      title,
      imageUrl,
      targetUrl,
      displayOrder,
    });
  });
  const slides = Array.from(map.values());
  slides.sort((a, b) => {
    if (a.displayOrder !== b.displayOrder) {
      return a.displayOrder - b.displayOrder;
    }
    const updatedDiff = Date.parse(b.updatedAt) - Date.parse(a.updatedAt);
    if (!Number.isNaN(updatedDiff) && updatedDiff !== 0) {
      return updatedDiff;
    }
    return a.id - b.id;
  });
  return slides;
};

const slides = computed(() => sanitizeSlides(props.ads));
const currentIndex = ref(0);
const intervalMs = computed(() => {
  const custom = Number(props.interval);
  if (Number.isFinite(custom) && custom >= 2000) {
    return Math.floor(custom);
  }
  return DEFAULT_INTERVAL;
});
const autoplayEnabled = computed(() => props.autoplay !== false && slides.value.length > 1);
let timer: ReturnType<typeof setInterval> | null = null;

const stopAutoplay = () => {
  if (timer !== null) {
    clearInterval(timer);
    timer = null;
  }
};

const startAutoplay = () => {
  stopAutoplay();
  if (!autoplayEnabled.value) {
    return;
  }
  timer = setInterval(() => {
    goNext();
  }, intervalMs.value);
};

const goTo = (index: number) => {
  const total = slides.value.length;
  if (!total) return;
  const normalized = ((index % total) + total) % total;
  currentIndex.value = normalized;
  startAutoplay();
};

const goNext = (restart = false) => {
  if (!slides.value.length) return;
  currentIndex.value = (currentIndex.value + 1) % slides.value.length;
  if (restart) {
    startAutoplay();
  }
};

const goPrevious = (restart = false) => {
  if (!slides.value.length) return;
  currentIndex.value = (currentIndex.value - 1 + slides.value.length) % slides.value.length;
  if (restart) {
    startAutoplay();
  }
};

watch(slides, (nextSlides) => {
  if (!nextSlides.length) {
    currentIndex.value = 0;
    stopAutoplay();
    return;
  }
  if (currentIndex.value >= nextSlides.length) {
    currentIndex.value = 0;
  }
  startAutoplay();
});

watch([autoplayEnabled, intervalMs], () => {
  startAutoplay();
});

onMounted(() => {
  startAutoplay();
});

onBeforeUnmount(() => {
  stopAutoplay();
});
</script>

<style scoped>
.home-carousel {
  position: relative;
  overflow: hidden;
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(15, 23, 42, 0.65);
  min-height: 200px;
}

.carousel-track {
  position: relative;
  width: 100%;
  height: 100%;
}

.carousel-slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.6s ease;
  pointer-events: none;
}

.carousel-slide.active {
  opacity: 1;
  pointer-events: auto;
}

.slide-content {
  display: block;
  width: 100%;
  height: 100%;
  position: relative;
  color: inherit;
}

.slide-content img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border-radius: 1.5rem;
}

.slide-overlay {
  position: absolute;
  left: 1.5rem;
  bottom: 1.5rem;
  right: 1.5rem;
  padding: 1rem 1.25rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.75), rgba(30, 41, 59, 0.4));
  color: #e2e8f0;
  backdrop-filter: blur(8px);
}

.slide-overlay h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.nav-button {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  border: none;
  background: rgba(15, 23, 42, 0.6);
  color: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  cursor: pointer;
  transition: background 0.3s, transform 0.3s;
}

.nav-button:hover {
  background: rgba(99, 102, 241, 0.5);
  transform: translateY(-50%) scale(1.05);
}

.nav-button.prev {
  left: 1rem;
}

.nav-button.next {
  right: 1rem;
}

.indicators {
  position: absolute;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 0.5rem;
}

.indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: none;
  background: rgba(148, 163, 184, 0.45);
  padding: 0;
  cursor: pointer;
  transition: transform 0.3s, background 0.3s;
}

.indicator.active {
  background: rgba(99, 102, 241, 0.9);
  transform: scale(1.2);
}

@media (max-width: 640px) {
  .home-carousel {
    border-radius: 1rem;
  }

  .slide-content img {
    border-radius: 1rem;
    min-height: 180px;
  }

  .slide-overlay {
    left: 1rem;
    right: 1rem;
    bottom: 1rem;
    padding: 0.75rem 1rem;
  }

  .slide-overlay h3 {
    font-size: 1.05rem;
  }

  .nav-button {
    width: 2.25rem;
    height: 2.25rem;
  }
}
</style>
