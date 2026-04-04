<template>
  <button
    type="button"
    class="app-back-button"
    :aria-label="label"
    :title="label"
    @click="$emit('click')"
  >
    <span class="app-back-button__box">
      <svg
        class="app-back-button__elem"
        viewBox="0 0 24 24"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path
          d="M14.7 5.3a1 1 0 0 1 0 1.4L10.41 11H20a1 1 0 1 1 0 2h-9.59l4.3 4.3a1 1 0 1 1-1.42 1.4l-6-6a1 1 0 0 1 0-1.4l6-6a1 1 0 0 1 1.41 0Z"
        />
      </svg>
      <svg
        class="app-back-button__elem"
        viewBox="0 0 24 24"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path
          d="M14.7 5.3a1 1 0 0 1 0 1.4L10.41 11H20a1 1 0 1 1 0 2h-9.59l4.3 4.3a1 1 0 1 1-1.42 1.4l-6-6a1 1 0 0 1 0-1.4l6-6a1 1 0 0 1 1.41 0Z"
        />
      </svg>
    </span>
  </button>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    default: '返回'
  }
})

defineEmits(['click'])
</script>

<style scoped>
.app-back-button {
  display: block;
  position: relative;
  width: 56px;
  height: 56px;
  margin: 0;
  overflow: hidden;
  outline: none;
  background-color: transparent;
  cursor: pointer;
  border: 0;
  flex: 0 0 auto;
}

.app-back-button::before,
.app-back-button::after {
  content: "";
  position: absolute;
  border-radius: 50%;
  inset: 7px;
}

.app-back-button::before {
  border: 4px solid #d8dce6;
  transition:
    opacity 0.4s cubic-bezier(0.77, 0, 0.175, 1) 80ms,
    transform 0.5s cubic-bezier(0.455, 0.03, 0.515, 0.955) 80ms;
}

.app-back-button::after {
  border: 4px solid #6d8df7;
  transform: scale(1.3);
  transition:
    opacity 0.4s cubic-bezier(0.165, 0.84, 0.44, 1),
    transform 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0;
}

.app-back-button:hover::before,
.app-back-button:focus-visible::before {
  opacity: 0;
  transform: scale(0.7);
  transition:
    opacity 0.4s cubic-bezier(0.165, 0.84, 0.44, 1),
    transform 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.app-back-button:hover::after,
.app-back-button:focus-visible::after {
  opacity: 1;
  transform: scale(1);
  transition:
    opacity 0.4s cubic-bezier(0.77, 0, 0.175, 1) 80ms,
    transform 0.5s cubic-bezier(0.455, 0.03, 0.515, 0.955) 80ms;
}

.app-back-button__box {
  display: flex;
  position: absolute;
  top: 0;
  left: 0;
}

.app-back-button__elem {
  display: block;
  width: 20px;
  height: 20px;
  margin: 18px;
  fill: #8f99ab;
}

.app-back-button:hover .app-back-button__box,
.app-back-button:focus-visible .app-back-button__box {
  transition: 0.4s;
  transform: translateX(-56px);
}
</style>

