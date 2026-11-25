import { vi } from 'vitest';

export class ModalMock {
  static instances: ModalMock[] = [];

  shown = false;

  readonly show = vi.fn(() => {
    this.shown = true;
    this.element.dispatchEvent(new Event('shown.bs.modal'));
  });

  readonly hide = vi.fn(() => {
    this.shown = false;
    this.element.dispatchEvent(new Event('hidden.bs.modal'));
  });

  constructor(readonly element: HTMLElement) {
    ModalMock.instances.push(this);
  }

  static forElement(element: Element): ModalMock {
    const modal = ModalMock.instances.find(instance => instance.element === element);
    if (modal === undefined) {
      throw new Error('no bootstrap modal for element');
    }
    return modal;
  }
}

export function installBootstrapMock(): void {
  (globalThis as unknown as Record<string, unknown>).bootstrap = { Modal: ModalMock };
}
