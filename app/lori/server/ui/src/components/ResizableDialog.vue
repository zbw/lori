<script lang="ts">
import { defineComponent, ref, computed, onMounted, onUnmounted } from "vue";

export default defineComponent({
  name: "ResizableDialog",
  props: {
    modelValue: {
      type: Boolean,
      required: true,
    },
    initialWidth: {
      type: Number,
      default: 600,
    },
    initialHeight: {
      type: Number,
      default: 400,
    },
    persistent: {
      type: Boolean,
      default: false,
    },
    retainFocus: {
      type: Boolean,
      default: true,
    },
    scrim: {
      type: Boolean,
      default: true,
    },
  },
  emits: ["update:modelValue", "close"],
  setup(props, { emit }) {
    const width = ref(props.initialWidth);
    const height = ref(props.initialHeight);
    const headerHeight = 64;

    const resizing = ref(false);
    const lastX = ref(0);
    const lastY = ref(0);

    const model = computed({
      get: () => props.modelValue,
      set: (val: boolean) => emit("update:modelValue", val),
    });

    const startResize = (e: MouseEvent) => {
      resizing.value = true;
      lastX.value = e.clientX;
      lastY.value = e.clientY;

      document.addEventListener("mousemove", onResize);
      document.addEventListener("mouseup", stopResize);
    };

    const onResize = (e: MouseEvent) => {
      if (!resizing.value) return;
      const dx = e.clientX - lastX.value;
      const dy = e.clientY - lastY.value;

      width.value = Math.max(300, width.value + dx);
      height.value = Math.max(200, height.value + dy);

      lastX.value = e.clientX;
      lastY.value = e.clientY;
    };

    const stopResize = () => {
      resizing.value = false;
      document.removeEventListener("mousemove", onResize);
      document.removeEventListener("mouseup", stopResize);
    };

    const onEsc = () => {
      emit("close");
    };

    // Cleanup in case dialog is destroyed mid-resize
    onUnmounted(() => {
      document.removeEventListener("mousemove", onResize);
      document.removeEventListener("mouseup", stopResize);
    });

    return {
      model,
      width,
      height,
      headerHeight,
      resizing,
      startResize,
      onEsc,
    };
  },
});
</script>

<style scoped>
.resize-handle {
  width: 16px;
  height: 16px;
  position: absolute;
  bottom: 0;
  right: 0;
  cursor: se-resize;
  background-color: rgba(0, 0, 0, 0.2);
  z-index: 10;
  border-radius: 4px 0 0 0;
}
</style>

<template>
  <v-dialog
    v-model="model"
    :persistent="persistent"
    :retain-focus="retainFocus"
    :scrim="scrim"
    @keydown.esc="onEsc"
    class="resizable-dialog"
  >
    <template #default>
      <v-card
        :style="{
          width: width + 'px',
          height: height + 'px',
          overflow: 'hidden',
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          userSelect: resizing ? 'none' : 'auto',
        }"
      >
        <!-- Optional header -->
        <slot name="header" />

        <!-- Resizable content area -->
        <div
          class="dialog-body"
          :style="{
            overflow: 'auto',
            height: height - headerHeight + 'px',
          }"
        >
          <slot />
        </div>

        <!-- Resize handle -->
        <div class="resize-handle" @mousedown="startResize"></div>
      </v-card>
    </template>
  </v-dialog>
</template>
