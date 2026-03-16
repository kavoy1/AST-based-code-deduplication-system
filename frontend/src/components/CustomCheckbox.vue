<template>
  <label class="simple-checkbox-container" :style="customStyle">
    <input 
      type="checkbox" 
      :checked="modelValue" 
      @change="handleChange" 
    />
    <div class="checkmark"></div>
  </label>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: '17px'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const handleChange = (e) => {
  const checked = e.target.checked
  emit('update:modelValue', checked)
  emit('change', checked)
}

const customStyle = computed(() => {
  return {
    '--size': props.size
  }
})
</script>

<style scoped>
.simple-checkbox-container input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.simple-checkbox-container {
  position: relative;
  cursor: pointer;
  font-size: var(--size);
  width: 2em; /* 2 * 17px = 34px by default */
  height: 2em;
  user-select: none;
  border: 5px solid white; /* Consider making color variable? User said white */
  border-radius: 4px; /* Optional: slight radius looks better usually, but user didn't specify. Let's stick to user CSS which has no radius, meaning square box. */
  /* User CSS: border: 5px solid white; */
  display: inline-block; /* changed from block to inline-block */
  vertical-align: middle;
  background-color: transparent;
  box-sizing: border-box; /* Important for border */
}

.checkmark {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  /* background-color: #ccc; User didn't specify background for unchecked. */
}

/* The square inside */
.checkmark:after {
  content: '';
  position: absolute;
  top: 25%;
  left: 25%;
  background-color: white;
  width: 50%;
  height: 50%;
  transform: scale(0);
  transition: .1s ease;
}

/* When checked, scale up the inner square */
.simple-checkbox-container input:checked ~ .checkmark:after {
  transform: scale(1);
}

/* Hover effect? User didn't provide. */
</style>
