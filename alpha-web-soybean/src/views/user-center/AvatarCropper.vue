<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue';
import { message } from 'ant-design-vue';
import {
  MinusOutlined,
  PlusOutlined,
  RotateLeftOutlined,
  RotateRightOutlined,
  UploadOutlined
} from '@ant-design/icons-vue';
import { VueCropper } from 'vue-cropper/dist/vue-cropper.es.js';
import 'vue-cropper/dist/index.css';

defineProps<{ open: boolean }>();
const emit = defineEmits<{
  'update:open': [open: boolean];
  confirm: [file: File];
}>();

interface CropperInstance {
  changeScale: (scale: number) => void;
  rotateLeft: () => void;
  rotateRight: () => void;
  getCropData: (callback: (dataUrl: string) => void) => void;
  getCropBlob: (callback: (blob: Blob) => void) => void;
}

const cropper = ref<CropperInstance>();
const source = ref('');
const previewUrl = ref('');
const fileInput = ref<HTMLInputElement>();
let sourceObjectUrl: string | undefined;

const options = {
  outputSize: 0.9,
  outputType: 'png',
  info: true,
  autoCrop: true,
  autoCropWidth: 240,
  autoCropHeight: 240,
  fixed: true,
  fixedNumber: [1, 1],
  centerBox: true,
  canMoveBox: true,
  original: false,
  maxImgSize: 2000
};

function close() {
  emit('update:open', false);
}
function resetSource() {
  if (sourceObjectUrl) URL.revokeObjectURL(sourceObjectUrl);
  sourceObjectUrl = undefined;
  source.value = '';
  previewUrl.value = '';
  if (fileInput.value) fileInput.value.value = '';
}
onBeforeUnmount(resetSource);
function selectImage() {
  fileInput.value?.click();
}
function onFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  if (!['image/png', 'image/jpeg', 'image/gif', 'image/webp'].includes(file.type)) {
    message.error('请选择 PNG、JPG、GIF 或 WebP 图片');
    return;
  }
  resetSource();
  sourceObjectUrl = URL.createObjectURL(file);
  source.value = sourceObjectUrl;
}
function updatePreview() {
  cropper.value?.getCropData(dataUrl => {
    previewUrl.value = dataUrl;
  });
}
function exportImage() {
  if (!cropper.value || !source.value) {
    message.info('请先选择头像图片');
    return;
  }
  cropper.value.getCropBlob(blob => {
    emit('confirm', new File([blob], `avatar-${Date.now()}.png`, { type: 'image/png' }));
    close();
  });
}
</script>

<template>
  <AModal :open="open" title="裁剪头像" :width="760" :footer="null" @cancel="close" @after-close="resetSource">
    <div class="avatar-cropper-layout">
      <div class="avatar-cropper-canvas">
        <VueCropper
          v-if="source"
          ref="cropper"
          :img="source"
          :output-size="options.outputSize"
          :output-type="options.outputType"
          :info="options.info"
          :auto-crop="options.autoCrop"
          :auto-crop-width="options.autoCropWidth"
          :auto-crop-height="options.autoCropHeight"
          :fixed="options.fixed"
          :fixed-number="options.fixedNumber"
          :center-box="options.centerBox"
          :can-move-box="options.canMoveBox"
          :original="options.original"
          :max-img-size="options.maxImgSize"
          @real-time="updatePreview"
        />
        <div v-else class="avatar-cropper-empty">请选择一张头像图片</div>
      </div>
      <div class="avatar-cropper-preview-panel">
        <div class="avatar-cropper-preview">
          <img v-if="previewUrl" :src="previewUrl" alt="头像预览" />
        </div>
        <span>预览</span>
      </div>
    </div>
    <div class="avatar-cropper-toolbar">
      <input
        ref="fileInput"
        class="avatar-cropper-input"
        type="file"
        accept=".png,.jpg,.jpeg,.gif,.webp"
        @change="onFileChange"
      />
      <AButton @click="selectImage">
        <UploadOutlined />
        选择图片
      </AButton>
      <AButton :disabled="!source" @click="cropper?.changeScale(1)"><PlusOutlined /></AButton>
      <AButton :disabled="!source" @click="cropper?.changeScale(-1)"><MinusOutlined /></AButton>
      <AButton :disabled="!source" @click="cropper?.rotateLeft()"><RotateLeftOutlined /></AButton>
      <AButton :disabled="!source" @click="cropper?.rotateRight()"><RotateRightOutlined /></AButton>
      <span class="avatar-cropper-toolbar-spacer" />
      <AButton @click="close">取消</AButton>
      <AButton type="primary" :disabled="!source" @click="exportImage">确定上传</AButton>
    </div>
  </AModal>
</template>

<style scoped>
.avatar-cropper-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
  gap: 24px;
  min-height: 360px;
}
.avatar-cropper-canvas {
  height: 360px;
  overflow: hidden;
  background: var(--alpha-canvas);
}
.avatar-cropper-empty {
  display: grid;
  height: 100%;
  color: var(--alpha-text-secondary);
  place-items: center;
}
.avatar-cropper-preview-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--alpha-text-secondary);
}
.avatar-cropper-preview {
  width: 112px;
  height: 112px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--alpha-border-soft);
}
.avatar-cropper-preview img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-cropper-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 18px;
}
.avatar-cropper-toolbar-spacer {
  flex: 1;
}
.avatar-cropper-input {
  display: none;
}
@media (max-width: 640px) {
  .avatar-cropper-layout {
    grid-template-columns: 1fr;
    gap: 12px;
    min-height: 0;
  }
  .avatar-cropper-canvas {
    height: 220px;
  }
  .avatar-cropper-preview-panel {
    flex-direction: row;
  }
  .avatar-cropper-preview {
    width: 76px;
    height: 76px;
  }
  .avatar-cropper-toolbar-spacer {
    display: none;
    width: 100%;
    flex-basis: 100%;
  }
}
</style>
