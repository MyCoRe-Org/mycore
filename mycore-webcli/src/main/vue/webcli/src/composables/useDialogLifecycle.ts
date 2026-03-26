import { nextTick, onBeforeUnmount, watch, type Ref } from 'vue';

export function useDialogLifecycle(
  modelValue: Ref<boolean>,
  dialogRef: Ref<HTMLDialogElement | null>,
  onOpenFocus?: () => void
) {
  let previousFocus: HTMLElement | null = null;

  watch(modelValue, async value => {
    const dialog = dialogRef.value;
    if (!dialog) {
      return;
    }
    if (value) {
      previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
      if (typeof dialog.showModal === 'function' && !dialog.open) {
        dialog.showModal();
      } else {
        dialog.setAttribute('open', '');
      }
      await nextTick();
      onOpenFocus?.();
      return;
    }
    if (dialog.open && typeof dialog.close === 'function') {
      dialog.close();
    } else {
      dialog.removeAttribute('open');
    }
    previousFocus?.focus();
  });

  onBeforeUnmount(() => {
    if (dialogRef.value?.open && typeof dialogRef.value.close === 'function') {
      dialogRef.value.close();
    }
  });
}
