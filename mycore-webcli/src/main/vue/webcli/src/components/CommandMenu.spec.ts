import { mount, type VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import CommandMenu from '@/components/CommandMenu.vue';
import { makeCommandGroups } from '@/test/helpers/commandFixtures';
import { cleanupDomTestEnvironment, setupDomTestEnvironment } from '@/test/helpers/domMocks';

describe('CommandMenu', () => {
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    setupDomTestEnvironment();
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    cleanupDomTestEnvironment();
  });

  it('supports keyboard navigation', async () => {
    wrapper = mount(CommandMenu, {
      attachTo: document.body,
      props: {
        groups: makeCommandGroups(),
      },
    });
    await nextTick();

    const submenuGroup = wrapper.find('.dropdown-submenu');
    const groupElement = submenuGroup.element as HTMLElement;
    groupElement.getBoundingClientRect = () => ({
      top: 100,
      right: 200,
      bottom: 130,
      left: 0,
      width: 200,
      height: 30,
      x: 0,
      y: 100,
      toJSON: () => undefined,
    });

    const toggle = wrapper.get('.dropdown-toggle');
    await toggle.trigger('keydown', { key: 'ArrowDown', preventDefault: vi.fn() });
    await nextTick();

    const groupButton = wrapper.find('[data-group-button]');
    expect(document.activeElement).toBe(groupButton.element);

    await groupButton.trigger('keydown', { key: 'ArrowRight', preventDefault: vi.fn() });
    await nextTick();

    const commandButton = wrapper.find('[data-command-button="0"]');
    expect(document.activeElement).toBe(commandButton.element);
    const flyout = submenuGroup.find('.dropdown-menu').element as HTMLElement;
    expect(flyout.style.left).toBe('200px');

    await commandButton.trigger('keydown', { key: 'Escape', preventDefault: vi.fn() });
    await nextTick();

    expect(document.activeElement).toBe(toggle.element);
  });

  it('keeps only one submenu open when mouse hover and keyboard navigation are mixed', async () => {
    wrapper = mount(CommandMenu, {
      attachTo: document.body,
      props: {
        groups: makeCommandGroups(),
      },
    });
    await nextTick();

    const submenuGroups = wrapper.findAll('.dropdown-submenu');
    expect(submenuGroups).toHaveLength(2);

    submenuGroups.forEach((submenuGroup, index) => {
      const groupElement = submenuGroup.element as HTMLElement;
      groupElement.getBoundingClientRect = () => ({
        top: 100 + index * 30,
        right: 200,
        bottom: 130 + index * 30,
        left: 0,
        width: 200,
        height: 30,
        x: 0,
        y: 100 + index * 30,
        toJSON: () => undefined,
      });
    });

    const toggle = wrapper.get('.dropdown-toggle');
    await toggle.trigger('click');
    await nextTick();

    await submenuGroups[0].trigger('mouseenter');
    await nextTick();

    expect(submenuGroups[0].classes()).toContain('show');
    expect(submenuGroups[1].classes()).not.toContain('show');

    const firstGroupButton = submenuGroups[0].get('[data-group-button]');
    await firstGroupButton.trigger('keydown', { key: 'ArrowDown', preventDefault: vi.fn() });
    await nextTick();

    expect(submenuGroups[0].classes()).not.toContain('show');
    expect(submenuGroups[1].classes()).toContain('show');
    expect(wrapper.findAll('.dropdown-submenu').filter(group => group.classes().includes('show'))).toHaveLength(1);
  });

  it('positions the flyout submenu on hover so it is visible in the browser', async () => {
    wrapper = mount(CommandMenu, {
      attachTo: document.body,
      props: {
        groups: makeCommandGroups(),
      },
    });
    await nextTick();

    const submenuGroup = wrapper.find('.dropdown-submenu');
    const groupElement = submenuGroup.element as HTMLElement;
    groupElement.getBoundingClientRect = () => ({
      top: 100,
      right: 200,
      bottom: 130,
      left: 0,
      width: 200,
      height: 30,
      x: 0,
      y: 100,
      toJSON: () => undefined,
    });

    await submenuGroup.trigger('mouseenter');

    const flyout = submenuGroup.find('.dropdown-menu').element as HTMLElement;
    expect(flyout.style.top).toBe('100px');
    expect(flyout.style.left).toBe('200px');
    expect(flyout.style.maxHeight).toContain('px');
  });

  it('shifts the flyout submenu up when there is not enough space below', async () => {
    wrapper = mount(CommandMenu, {
      attachTo: document.body,
      props: {
        groups: [
          {
            name: 'Large group',
            commands: [
              { command: 'one', help: '1' },
              { command: 'two', help: '2' },
              { command: 'three', help: '3' },
            ],
          },
        ],
      },
    });
    await nextTick();

    const originalInnerHeight = window.innerHeight;
    Object.defineProperty(window, 'innerHeight', {
      configurable: true,
      value: 500,
      writable: true,
    });

    const submenuGroup = wrapper.find('.dropdown-submenu');
    const groupElement = submenuGroup.element as HTMLElement;
    groupElement.getBoundingClientRect = () => ({
      top: 480,
      right: 200,
      bottom: 530,
      left: 0,
      width: 200,
      height: 50,
      x: 0,
      y: 480,
      toJSON: () => undefined,
    });

    await submenuGroup.trigger('mouseenter');

    const flyout = submenuGroup.find('.dropdown-menu').element as HTMLElement;
    expect(Number.parseInt(flyout.style.top, 10)).toBe(384);
    expect(flyout.style.left).toBe('200px');
    expect(Number.parseInt(flyout.style.maxHeight, 10)).toBe(106);

    Object.defineProperty(window, 'innerHeight', {
      configurable: true,
      value: originalInnerHeight,
      writable: true,
    });
  });

  it('repositions the flyout submenu to stay within the viewport horizontally', async () => {
    wrapper = mount(CommandMenu, {
      attachTo: document.body,
      props: {
        groups: makeCommandGroups(),
      },
    });
    await nextTick();

    const originalInnerWidth = window.innerWidth;
    Object.defineProperty(window, 'innerWidth', {
      configurable: true,
      value: 420,
      writable: true,
    });

    const submenuGroup = wrapper.find('.dropdown-submenu');
    const groupElement = submenuGroup.element as HTMLElement;
    groupElement.getBoundingClientRect = () => ({
      top: 100,
      right: 390,
      bottom: 130,
      left: 220,
      width: 170,
      height: 30,
      x: 220,
      y: 100,
      toJSON: () => undefined,
    });

    const flyout = submenuGroup.find('.dropdown-menu').element as HTMLElement;
    Object.defineProperty(flyout, 'offsetWidth', {
      configurable: true,
      value: 180,
    });

    await submenuGroup.trigger('mouseenter');

    expect(flyout.style.left).toBe('40px');

    Object.defineProperty(window, 'innerWidth', {
      configurable: true,
      value: originalInnerWidth,
      writable: true,
    });
  });
});
