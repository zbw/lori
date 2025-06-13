<template>
  <div id="redoc-container" style="min-height: 100vh;" />
</template>

<script lang="ts">
import { defineComponent, onMounted, nextTick } from 'vue'
import openapiSpec from '@/generated-sources/openapijson/openapi.json'

export default defineComponent({
  name: 'RestApi',

  setup() {
    onMounted(async () => {
      await nextTick() // Ensure DOM is fully rendered

      const el = document.getElementById('redoc-container')

      if (el && window.Redoc) {
        window.Redoc.init(openapiSpec, {
          scrollYOffset: 50,
        }, el)
      } else {
        console.error('Redoc or #redoc-container not found')
      }
    })
  },
})
</script>
