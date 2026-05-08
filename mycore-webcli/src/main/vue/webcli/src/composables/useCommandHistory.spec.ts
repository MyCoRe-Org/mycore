import { beforeEach, describe, expect, it } from 'vitest';

import { useCommandHistory } from '@/composables/useCommandHistory';
import { setupDomTestEnvironment } from '@/test/helpers/domMocks';

describe('useCommandHistory', () => {
  beforeEach(() => {
    setupDomTestEnvironment();
  });

  it('avoids storing consecutive duplicate commands', () => {
    const history = useCommandHistory(() => 10);

    history.addEntry('process a');
    history.addEntry('process a');
    history.addEntry('process b');

    expect(history.entries.value).toEqual(['process a', 'process b']);
  });

  it('trims history to the configured size', () => {
    const history = useCommandHistory(() => 2);

    history.addEntry('process a');
    history.addEntry('process b');
    history.addEntry('process c');

    expect(history.entries.value).toEqual(['process b', 'process c']);
  });

  it('treats the current input as a temporary draft while browsing and removes it when finalized', () => {
    const history = useCommandHistory(() => 10);
    history.addEntry('process a');
    history.addEntry('process b');

    expect(history.browseUp('draft command')).toBe('process b');
    expect(history.entries.value).toEqual(['process a', 'process b', 'draft command']);
    expect(history.browseDown()).toBe('draft command');

    history.finalizeExecution();
    expect(history.entries.value).toEqual(['process a', 'process b']);
  });

  it('clears persisted history', () => {
    const history = useCommandHistory(() => 10);
    history.addEntry('process a');

    history.clear();

    expect(history.entries.value).toEqual([]);
    expect(window.localStorage.getItem('commandHistory')).toBeNull();
  });
});
