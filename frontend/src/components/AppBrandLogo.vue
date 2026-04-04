<template>
  <div class="app-brand-logo" :class="`app-brand-logo--${variant}`" aria-hidden="true">
    <img class="app-brand-logo__image" :src="logoSrc" alt="" />
  </div>
</template>

<script setup>
import logoSrc from '../assets/brand/ast-logo-source.jpg'

defineProps({
  variant: {
    type: String,
    default: 'sidebar'
  }
})
</script>

<style scoped>
.app-brand-logo {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  isolation: isolate;
}

.app-brand-logo::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(145deg, rgba(12, 35, 94, 0.96), rgba(43, 216, 235, 0.92));
  opacity: 0.12;
}

.app-brand-logo::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: inherit;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: inset 0 0 0 1px rgba(15, 32, 72, 0.05);
}

.app-brand-logo__image {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transform: scale(1.2);
  clip-path: circle(49% at 50% 50%);
  filter: saturate(1.04) contrast(1.04);
}

.app-brand-logo--rail {
  width: 42px;
  height: 42px;
  border-radius: 18px;
}

.app-brand-logo--sidebar {
  width: 42px;
  height: 42px;
  border-radius: 15px;
}

.app-brand-logo--auth {
  width: 72px;
  height: 72px;
  border-radius: 26px;
}
</style>
