<script lang="ts">
import { computed, defineComponent, PropType, ref } from "vue";
import { BookmarkRest, RightRest } from "@/generated-sources/openapi";
import RightsEditDialog from "@/components/RightsEditDialog.vue";

/**
 * Wrapper for the rights coordinating copy mechanism and business logic between different instances.
 */
export default defineComponent({
  components: { RightsEditDialog },
  inheritAttrs: false,
  props: {
    rightId: {
      type: String,
      required: false,
    },
    index: {
      type: Number,
      required: true,
    },
    isNewRight: {
      type: Boolean,
      required: true,
    },
    isNewTemplate: {
      type: Boolean,
      required: true,
    },
    isExceptionTemplate: {
      type: Boolean,
      required: false,
    },
    handle: {
      type: String,
      required: false,
    },
    reinitCounter: {
      type: Number,
      required: false,
    },
    initialBookmark: {
      type: Object as PropType<BookmarkRest>,
      required: false,
    },
    initialRight: {
      type: Object as PropType<RightRest>,
      required: false,
    },
    isTabEntry: {
      type: Boolean,
      default: false,
    },
    licenceUrl: {
      type: String,
      required: false,
    },
  },
  emits: {
    addSuccessful: (right: RightRest) => true,
    addTemplateSuccessful: (right: RightRest) => true,
    deleteSuccessful: (index: number, rightId: string | undefined) => true,
    deleteTemplateSuccessful: (templateName: string) => true,
    editRightClosed: () => true,
    hasFormChanged: (existingRightHasChanges: boolean, rightId: string) => true,
    updateSuccessful: (right: RightRest, index: number) => true,
    updateTemplateSuccessful: (templateName: string) => true,
  },
  setup(props, { emit, attrs }) {
    const copyInstance = ref(false);
    const exceptionInstance = ref(false);

    const closeExceptionView = () => {
      exceptionInstance.value = false;
    };

    const exception = ref({} as RightRest);
    const exceptionValue = computed(() => exception.value);

    const addNewExceptionToOriginal = (excTemplate: RightRest) => {
      exception.value = Object.assign({}, excTemplate);
    };

    const displayExceptionView = () => {
      exceptionInstance.value = true;
    };

    const rightToCopy = ref({} as RightRest);
    const displayCopyView = (right: RightRest) => {
      rightToCopy.value = right;
      copyInstance.value = true;
    };

    const copiedSuccessfulTo = ref("");
    const copySuccessful = (handle: String) => {
      copiedSuccessfulTo.value = handle;
    };

    const closeCopyView = () => {
      rightToCopy.value = Object.assign({} as RightRest);
      copyInstance.value = false;
    };

    const onAddSuccessful = (newRight: RightRest) => {
      emit("addSuccessful", newRight);
    };

    const onAddTemplateSuccessful = (newRight: RightRest) => {
      emit("addTemplateSuccessful", newRight);
    };

    const onDeleteSuccessful = (index: number, rightId: string | undefined) => {
      emit("deleteSuccessful", index, rightId);
    };

    const onDeleteTemplateSuccessful = (templateName: string) => {
      emit("deleteTemplateSuccessful", templateName);
    };

    const onEditRightClosed = () => {
      emit("editRightClosed");
    };

    const onHasFormChanged = (
      existingRightHasChanges: boolean,
      rightId: string,
    ) => {
      emit("hasFormChanged", existingRightHasChanges, rightId);
    };

    const onUpdateSuccessful = (newRight: RightRest, index: number) => {
      emit("updateSuccessful", newRight, index);
    };

    const onUpdateTemplateSuccessful = (templateName: string) => {
      emit("updateTemplateSuccessful", templateName);
    };

    return {
      attrs,
      copyInstance,
      copiedSuccessfulTo,
      exceptionValue,
      exceptionInstance,
      rightToCopy,
      addNewExceptionToOriginal,
      closeCopyView,
      closeExceptionView,
      copySuccessful,
      displayCopyView,
      displayExceptionView,
      onAddSuccessful,
      onAddTemplateSuccessful,
      onDeleteSuccessful,
      onDeleteTemplateSuccessful,
      onEditRightClosed,
      onHasFormChanged,
      onUpdateSuccessful,
      onUpdateTemplateSuccessful,
    };
  },
});
</script>

<style>
.relative {
  width: 100%;
  height: 100%;
}

/* make wrapper inherit the overlay's max-height (v-overlay__content has inline max-height) */
.rights-wrapper {
  width: 100%;
  max-height: inherit; /* ← inherit the parent's max-height: 850px */
  box-sizing: border-box;
}

.rights-scroll {
  max-height: inherit; /* inherits same value */
  overflow: auto; /* this will show scrollbars when content is taller */
  box-sizing: border-box;
}

.rights-overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  background: white;
  border: 2px solid #bcd4ff;
  border-radius: 0.5rem;
  max-height: inherit;
  overflow: auto;
  box-sizing: border-box;
  padding: 1rem;
}
</style>

<template>
  <div class="rights-wrapper" v-bind="attrs">
    <!-- default view -->
    <!-- v-show is important because otherwise the overlays would be rendered twice -->
    <div v-show="!exceptionInstance && !copyInstance" class="rights-scroll">
      <RightsEditDialog
        v-bind="attrs"
        :rightId="rightId"
        :index="index"
        :isNewRight="isNewRight"
        :isNewTemplate="isNewTemplate"
        :isExceptionTemplate="isExceptionTemplate"
        :handle="handle"
        :reinitCounter="reinitCounter"
        :initialBookmark="initialBookmark"
        :initialRight="initialRight"
        :isTabEntry="isTabEntry"
        :licenceUrl="licenceUrl"
        :exceptionTemplate="exceptionValue"
        :copySuccessfulTo="copiedSuccessfulTo"
        v-on:createException="displayExceptionView"
        v-on:addSuccessful="onAddSuccessful"
        v-on:addTemplateSuccessful="onAddTemplateSuccessful"
        v-on:editRightClosed="onEditRightClosed"
        v-on:hasFormChanged="onHasFormChanged"
        v-on:updateSuccessful="onUpdateSuccessful"
        v-on:updateTemplateSuccessful="onUpdateTemplateSuccessful"
        v-on:copyRight="displayCopyView"
      ></RightsEditDialog>
    </div>

    <!-- Overlay copy -->
    <div v-if="copyInstance" class="rights-scroll">
      <RightsEditDialog
        v-bind="attrs"
        :isNewRight="true"
        :isNewTemplate="false"
        :initialRight="rightToCopy"
        :index="0"
        :isCopy="true"
        v-on:editRightClosed="closeCopyView"
        v-on:copySuccessful="copySuccessful"
      ></RightsEditDialog>
    </div>

    <!-- Overlay exception -->
    <div v-if="exceptionInstance" class="rights-scroll">
      <RightsEditDialog
        v-bind="attrs"
        :isNewRight="false"
        :isNewTemplate="true"
        :index="0"
        :isExceptionTemplate="true"
        v-on:editRightClosed="closeExceptionView"
        v-on:addTemplateSuccessful="addNewExceptionToOriginal"
      ></RightsEditDialog>
    </div>
  </div>
</template>
