<template>
  <label class="lock-toggle-container" :style="containerStyle">
    <input 
      type="checkbox" 
      class="lock-toggle-input" 
      :checked="isChecked" 
      @change="handleChange" 
    />
    <div class="lock-toggle-track">
      <!-- Unlocked Icon (Right side, visible when checked/green) -->
      <!-- Wait, user SVG 1 has d="..." -> This looks like an open lock? 
           SVG 1 path: ...M50,18... (Head) ... 
           Let's just use the SVGs as provided.
           SVG 1 is at left-12 (Right side).
           SVG 2 is at left-1 (Left side).
      -->
      
      <!-- Right Icon (Visible when bg is green/checked, positioned right) -->
      <svg class="lock-icon icon-right" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
        <path d="M50,18A19.9,19.9,0,0,0,30,38v8a8,8,0,0,0-8,8V74a8,8,0,0,0,8,8H70a8,8,0,0,0,8-8V54a8,8,0,0,0-8-8H38V38a12,12,0,0,1,23.6-3,4,4,0,1,0,7.8-2A20.1,20.1,0,0,0,50,18Z" />
      </svg>
      
      <!-- Left Icon (Visible when bg is red/unchecked, positioned left) -->
      <svg class="lock-icon icon-left" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
        <path d="M30,46V38a20,20,0,0,1,40,0v8a8,8,0,0,1,8,8V74a8,8,0,0,1-8,8H30a8,8,0,0,1-8-8V54A8,8,0,0,1,30,46Zm32-8v8H38V38a12,12,0,0,1,24,0Z" fill-rule="evenodd" />
      </svg>
    </div>
  </label>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [Boolean, String, Number],
    default: false
  },
  activeValue: {
    type: [Boolean, String, Number],
    default: true
  },
  inactiveValue: {
    type: [Boolean, String, Number],
    default: false
  },
  scale: {
    type: Number,
    default: 0.6 // Default scaled down to fit in table (approx 58px width)
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isChecked = computed(() => {
  return props.modelValue === props.activeValue
})

const handleChange = (e) => {
  const checked = e.target.checked
  const value = checked ? props.activeValue : props.inactiveValue
  emit('update:modelValue', value)
  emit('change', value)
}

const containerStyle = computed(() => {
  return {
    '--scale': props.scale
  }
})
</script>

<style scoped>
.lock-toggle-container {
  /* Base size from Tailwind w-24 (96px) h-12 (48px) */
  --base-width: 96px;
  --base-height: 48px;
  --base-padding: 4px;
  --circle-size: 40px;
  --translate-x: 48px; /* 96 - 40 - 4 - 4 = 48 */
  
  /* Scaled sizes */
  width: calc(var(--base-width) * var(--scale));
  height: calc(var(--base-height) * var(--scale));
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
  vertical-align: middle;
}

.lock-toggle-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
}

.lock-toggle-track {
  width: 100%;
  height: 100%;
  background-color: #fb7185; /* bg-rose-400 */
  border-radius: 9999px;
  position: relative;
  transition: background-color 0.3s ease;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); /* shadow-md */
}

/* Checked State Background */
.lock-toggle-input:checked + .lock-toggle-track {
  background-color: #10b981; /* bg-emerald-500 */
}

/* The Circle (After pseudo-element) */
.lock-toggle-track::after {
  content: '';
  position: absolute;
  top: calc(var(--base-padding) * var(--scale));
  left: calc(var(--base-padding) * var(--scale));
  width: calc(var(--circle-size) * var(--scale));
  height: calc(var(--circle-size) * var(--scale));
  background-color: #f9fafb; /* bg-gray-50 */
  border-radius: 50%;
  transition: transform 0.3s ease;
  z-index: 2; /* Ensure circle is above SVGs */
}

/* Checked State Circle Position */
.lock-toggle-input:checked + .lock-toggle-track::after {
  transform: translateX(calc(var(--translate-x) * var(--scale)));
}

/* Hover Effect (Scale down slightly) */
.lock-toggle-container:hover .lock-toggle-track::after {
  /* We need to maintain translate if checked */
}
.lock-toggle-input:not(:checked) + .lock-toggle-track:hover::after {
  transform: scale(0.95);
}
.lock-toggle-input:checked + .lock-toggle-track:hover::after {
  transform: translateX(calc(var(--translate-x) * var(--scale))) scale(0.95);
}

/* Icons */
.lock-icon {
  position: absolute;
  top: calc(var(--base-padding) * var(--scale));
  width: calc(var(--circle-size) * var(--scale));
  height: calc(var(--circle-size) * var(--scale));
  fill: #111827; /* stroke-gray-900? User used stroke but SVG has fill path. Let's use fill for path. */
  stroke: #111827;
  stroke-width: 0;
  pointer-events: none;
  z-index: 1;
}

.lock-icon path {
  fill: #111827;
}

.icon-right {
  left: calc(48px * var(--scale)); /* left-12 = 3rem = 48px */
}

.icon-left {
  left: calc(4px * var(--scale)); /* left-1 = 0.25rem = 4px */
}

</style>
