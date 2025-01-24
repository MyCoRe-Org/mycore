import { ref } from 'vue';

export function useModal() {
  const modals = ref(new Map());

  const open = (modalId: string) => {
    modals.value.set(modalId, true);
  };

  const close = (modalId: string) => {
    modals.value.set(modalId, false);
  };

  const isVisible = (modalId: string) => {
    return modals.value.get(modalId) || false;
  };

  return {
    open,
    close,
    isVisible,
  };
}
