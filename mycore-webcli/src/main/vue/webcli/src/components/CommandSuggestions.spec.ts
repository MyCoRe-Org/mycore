import { mount, type VueWrapper } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import CommandSuggestions from '@/components/CommandSuggestions.vue';

describe('CommandSuggestions', () => {
  let wrapper: VueWrapper | null = null;

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
  });

  it('emits activation and selection events from pointer interaction', async () => {
    wrapper = mount(CommandSuggestions, {
      props: {
        id: 'suggestions',
        highlightedIndex: 0,
        suggestionLimit: 2,
        totalCount: 2,
        suggestions: [
          {
            command: 'xslt transform',
            help: 'Run an XSLT transformation.',
            groupName: 'Transformations',
          },
        ],
      },
    });

    await wrapper.get('.webcli-suggestion').trigger('mouseenter');
    await wrapper.get('.webcli-suggestion').trigger('pointerdown');

    expect(wrapper.emitted('activate')).toEqual([[0]]);
    expect(wrapper.emitted('highlight')).toEqual([[0]]);
    expect(wrapper.emitted('select')).toEqual([['xslt transform']]);
  });

  it('shows a truncation message when only the top matches are visible', () => {
    wrapper = mount(CommandSuggestions, {
      props: {
        id: 'suggestions',
        highlightedIndex: null,
        suggestionLimit: 2,
        totalCount: 4,
        suggestions: [
          {
            command: 'xslt transform 1',
            help: 'Run an XSLT transformation.',
            groupName: 'Transformations',
          },
          {
            command: 'xslt transform 2',
            help: 'Run another XSLT transformation.',
            groupName: 'Transformations',
          },
        ],
      },
    });

    expect(wrapper.get('.webcli-suggestions-meta').text()).toContain('Showing top 2 of 4 matches');
  });

  it('scrolls the active suggestion into view when the highlighted index changes', async () => {
    const scrollIntoView = vi.fn();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: scrollIntoView,
    });

    wrapper = mount(CommandSuggestions, {
      attachTo: document.body,
      props: {
        id: 'suggestions',
        highlightedIndex: null,
        suggestionLimit: 2,
        totalCount: 2,
        suggestions: [
          {
            command: 'xslt transform 1',
            help: 'Run an XSLT transformation.',
            groupName: 'Transformations',
          },
          {
            command: 'xslt transform 2',
            help: 'Run another XSLT transformation.',
            groupName: 'Transformations',
          },
        ],
      },
    });

    await wrapper.setProps({ highlightedIndex: 1 });
    await nextTick();

    expect(scrollIntoView).toHaveBeenCalled();
  });

  it('renders suggestions without an active item before navigation starts', () => {
    wrapper = mount(CommandSuggestions, {
      props: {
        id: 'suggestions',
        highlightedIndex: null,
        suggestionLimit: 2,
        totalCount: 1,
        suggestions: [
          {
            command: 'xslt transform',
            help: 'Run an XSLT transformation.',
            groupName: 'Transformations',
          },
        ],
      },
    });

    const suggestion = wrapper.get('.webcli-suggestion');
    expect(suggestion.classes()).not.toContain('is-highlighted');
    expect(suggestion.attributes('aria-selected')).toBe('false');
  });
});
