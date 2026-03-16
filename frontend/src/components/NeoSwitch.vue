<template>
  <div class="neo-toggle-container">
    <input 
      type="checkbox" 
      class="neo-toggle-input" 
      :checked="modelValue" 
      @change="handleChange" 
    />
    <div class="neo-toggle">
      <div class="neo-track">
        <div class="neo-background-layer"></div>
        <div class="neo-grid-layer"></div>
        <div class="neo-track-highlight"></div>
        <div class="neo-spectrum-analyzer">
          <div class="neo-spectrum-bar"></div>
          <div class="neo-spectrum-bar"></div>
          <div class="neo-spectrum-bar"></div>
          <div class="neo-spectrum-bar"></div>
          <div class="neo-spectrum-bar"></div>
        </div>
      </div>
      <div class="neo-thumb">
        <div class="neo-thumb-ring"></div>
        <div class="neo-thumb-core">
          <div class="neo-thumb-icon"></div>
        </div>
        <div class="neo-thumb-wave"></div>
        <div class="neo-thumb-pulse"></div>
      </div>
      <div class="neo-interaction-feedback">
        <div class="neo-ripple"></div>
        <div class="neo-progress-arc"></div>
      </div>
    </div>
    <!-- 移除文字状态显示，如果需要可以取消注释 -->
    <!--
    <div class="neo-status">
      <div class="neo-status-indicator">
        <div class="neo-status-dot"></div>
        <div class="neo-status-text"></div>
      </div>
    </div>
    -->
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  activeValue: {
    type: [Boolean, String, Number],
    default: true
  },
  inactiveValue: {
    type: [Boolean, String, Number],
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const handleChange = (e) => {
  const checked = e.target.checked
  const value = checked ? props.activeValue : props.inactiveValue
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style scoped>
.neo-toggle-container {
  --toggle-width: 60px; /* Reduced size slightly for UI fit */
  --toggle-height: 30px;
  --toggle-bg: #181c20;
  --toggle-off-color: #475057;
  --toggle-on-color: #36f9c7;
  --toggle-transition: 0.4s cubic-bezier(0.25, 1, 0.5, 1);

  position: relative;
  display: inline-flex;
  flex-direction: column;
  font-family: "Segoe UI", Tahoma, sans-serif;
  user-select: none;
  vertical-align: middle;
}

.neo-toggle-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.neo-toggle {
  position: relative;
  width: var(--toggle-width);
  height: var(--toggle-height);
  display: block;
  cursor: pointer;
  transform: translateZ(0);
  perspective: 500px;
}

/* Track styles */
.neo-track {
  position: absolute;
  inset: 0;
  border-radius: calc(var(--toggle-height) / 2);
  overflow: hidden;
  transform-style: preserve-3d;
  transform: translateZ(-1px);
  transition: transform var(--toggle-transition);
  box-shadow: 
    0 2px 10px rgba(0, 0, 0, 0.5), 
    inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.neo-background-layer {
  position: absolute;
  inset: 0;
  background: var(--toggle-bg);
  background-image: linear-gradient(
    -45deg, 
    rgba(20, 20, 20, 0.8) 0%, 
    rgba(30, 30, 30, 0.3) 50%, 
    rgba(20, 20, 20, 0.8) 100%
  );
  opacity: 1;
  transition: all var(--toggle-transition);
}

.neo-grid-layer {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(
      to right, 
      rgba(71, 80, 87, 0.05) 1px, 
      transparent 1px 
    ), 
    linear-gradient(to bottom, rgba(71, 80, 87, 0.05) 1px, transparent 1px);
  background-size: 5px 5px;
  opacity: 0;
  transition: opacity var(--toggle-transition);
}

.neo-track-highlight {
  position: absolute;
  inset: 1px;
  border-radius: calc(var(--toggle-height) / 2);
  background: linear-gradient(90deg, transparent, rgba(54, 249, 199, 0));
  opacity: 0;
  transition: all var(--toggle-transition);
}

/* Spectrum analyzer */
.neo-spectrum-analyzer {
  position: absolute;
  bottom: 6px;
  right: 10px;
  height: 10px;
  display: flex;
  align-items: flex-end;
  gap: 2px;
  opacity: 0;
  transition: opacity var(--toggle-transition);
}

.neo-spectrum-bar {
  width: 2px;
  height: 3px;
  background-color: var(--toggle-on-color);
  opacity: 0.8;
}

/* Thumb styles */
.neo-thumb {
  position: absolute;
  top: 3px; /* Adjusted for smaller height */
  left: 3px;
  width: 24px; /* Adjusted */
  height: 24px; /* Adjusted */
  border-radius: 50%;
  transform-style: preserve-3d;
  transition: transform var(--toggle-transition);
  z-index: 1;
}

.neo-thumb-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: var(--toggle-off-color);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  transition: all var(--toggle-transition);
}

.neo-thumb-core {
  position: absolute;
  inset: 4px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), transparent);
  transition: all var(--toggle-transition);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.neo-thumb-icon {
  position: relative;
  width: 10px;
  height: 10px;
  transition: all var(--toggle-transition);
}

.neo-thumb-wave {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 10px;
  height: 2px;
  background: var(--toggle-off-color);
  transform: translate(-50%, -50%);
  transition: all var(--toggle-transition);
}

.neo-thumb-pulse {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid var(--toggle-off-color);
  transform: scale(0);
  opacity: 0;
  transition: all var(--toggle-transition);
}

/* Interaction feedback */
.neo-interaction-feedback {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.neo-ripple {
  position: absolute;
  top: 50%;
  left: 30%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: radial-gradient(
    circle, 
    var(--toggle-on-color) 0%, 
    transparent 70% 
  );
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: all 0.4s ease-out;
}

.neo-progress-arc {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2px solid transparent;
  border-top-color: var(--toggle-on-color);
  transform: translate(-50%, -50%) scale(0) rotate(0deg);
  opacity: 0;
  transition: 
    opacity 0.3s ease, 
    transform 0.5s ease;
}

/* Active states */

/* ON state */
.neo-toggle-input:checked + .neo-toggle .neo-thumb {
  transform: translateX(calc(var(--toggle-width) - 30px)); /* Adjusted */
}

.neo-toggle-input:checked + .neo-toggle .neo-thumb-ring {
  background-color: var(--toggle-on-color);
  border-color: rgba(54, 249, 199, 0.3);
  box-shadow: 0 0 15px rgba(54, 249, 199, 0.5);
}

.neo-toggle-input:checked + .neo-toggle .neo-thumb-wave {
  height: 6px;
  width: 6px;
  border-radius: 50%;
  background: transparent;
  border: 1px solid #fff;
}

.neo-toggle-input:checked + .neo-toggle .neo-thumb-pulse {
  transform: scale(1.2);
  opacity: 0.3;
  animation: neo-pulse 1.5s infinite;
}

.neo-toggle-input:checked + .neo-toggle .neo-track-highlight {
  background: linear-gradient(90deg, transparent, rgba(54, 249, 199, 0.2));
  opacity: 1;
}

.neo-toggle-input:checked + .neo-toggle .neo-grid-layer {
  opacity: 1;
}

.neo-toggle-input:checked + .neo-toggle .neo-spectrum-analyzer {
  opacity: 1;
}

.neo-toggle-input:checked + .neo-toggle .neo-spectrum-bar:nth-child(1) {
  animation: neo-spectrum 0.9s infinite;
}

.neo-toggle-input:checked + .neo-toggle .neo-spectrum-bar:nth-child(2) {
  animation: neo-spectrum 0.8s 0.1s infinite;
}

.neo-toggle-input:checked + .neo-toggle .neo-spectrum-bar:nth-child(3) {
  animation: neo-spectrum 1.1s 0.2s infinite;
}

.neo-toggle-input:checked + .neo-toggle .neo-spectrum-bar:nth-child(4) {
  animation: neo-spectrum 0.7s 0.1s infinite;
}

.neo-toggle-input:checked + .neo-toggle .neo-spectrum-bar:nth-child(5) {
  animation: neo-spectrum 0.9s 0.15s infinite;
}

/* Hover effects */
.neo-toggle:hover .neo-thumb-ring {
  transform: scale(1.05);
}

.neo-toggle-input:not(:checked) + .neo-toggle:hover .neo-thumb-wave::before,
.neo-toggle-input:not(:checked) + .neo-toggle:hover .neo-thumb-wave::after {
  opacity: 1;
}

/* Animations */
@keyframes neo-pulse {
  0% { 
    transform: scale(1); 
    opacity: 0.5; 
  }
  50% { 
    transform: scale(1.5); 
    opacity: 0.2; 
  }
  100% { 
    transform: scale(1); 
    opacity: 0.5; 
  }
}

@keyframes neo-spectrum {
  0% { 
    height: 3px; 
  }
  50% { 
    height: 8px; 
  }
  100% { 
    height: 3px; 
  }
}
</style>
