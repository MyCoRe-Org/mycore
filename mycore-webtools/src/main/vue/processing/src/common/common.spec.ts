import { beforeEach, describe, expect, it } from 'vitest';

import { Settings } from './settings';
import { Util } from './util';

describe('Settings', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('stores values with the processables prefix', () => {
    Settings.set('maxNumberFinished', 10);

    expect(localStorage.getItem('mcr.processables.maxNumberFinished')).toBe('10');
    expect(Settings.get('maxNumberFinished')).toBe('10');
  });

  it('returns the default for missing values', () => {
    expect(Settings.get('missing', 50)).toBe(50);
    expect(Settings.get('missing')).toBeUndefined();
  });
});

describe('Util', () => {
  it('copies own properties onto the target', () => {
    const target: Record<string, unknown> = { kept: true, replaced: 1 };

    Util.mixin(Object.assign(Object.create({ inherited: true }), { replaced: 2, added: 'x' }), target);

    expect(target).toEqual({ kept: true, replaced: 2, added: 'x' });
  });

  it('removes an element only when present', () => {
    const array = ['a', 'b', 'c'];

    Util.remove(array, 'b');
    Util.remove(array, 'missing');

    expect(array).toEqual(['a', 'c']);
  });

  it('formats timestamps as month, day and time', () => {
    const timestamp = new Date(2026, 10, 5, 8, 3, 9).getTime();

    expect(Util.formatDate(timestamp)).toBe('11.05 08:03:09');
    expect(Util.formatDate(undefined)).toBe('');
    expect(Util.formatDate(0)).toBe('');
  });

  describe('useJsonString', () => {
    function render(value: unknown): HTMLElement {
      const container = document.createElement('div');
      container.innerHTML = Util.useJsonString(value);
      return container;
    }

    it.each([
      [null, 'json-string-null', 'null'],
      [undefined, 'json-string-null', 'undefined'],
      [true, 'json-string-primitive', 'true'],
      [42, 'json-string-primitive', '42'],
      ['text', 'json-string-primitive', 'text'],
    ])('renders %s', (value, className, text) => {
      const element = render(value).firstElementChild!;

      expect(element.className).toBe(className);
      expect(element.textContent).toBe(text);
    });

    it('renders functions with their name and dates as text', () => {
      const date = new Date(2026, 0, 1);

      expect(render(function namedFunction() {}).textContent).toBe('namedFunction');
      expect(render(date).querySelector('.json-string-date')?.textContent).toBe(date.toString());
    });

    it('renders objects with one row per property', () => {
      const rows = [...render({ a: 1, b: { c: 'd' } }).querySelectorAll(':scope > .json-string-object > .json-string-row')];

      expect(rows.map(row => row.querySelector('.json-string-object-property-name')?.textContent)).toEqual(['a', 'b']);
      expect(rows[1]!.querySelector('.json-string-object-property-value .json-string-object-property-name')?.textContent).toBe('c');
    });

    it('closes each object once after its rows', () => {
      const object = render({ a: 1, b: 2 }).querySelector('.json-string-object')!;

      expect(object.textContent).toBe('{a1b2}');
    });

    it('escapes markup in values, keys, function names and dates', () => {
      const markup = '<script>alert("x")</script>';
      const element = render({ [markup]: markup });

      expect(element.querySelector('script')).toBeNull();
      expect(element.querySelector('.json-string-object-property-name')?.textContent).toBe(markup);
      expect(element.querySelector('.json-string-primitive')?.textContent).toBe(markup);
      expect(Util.escapeHtml(`&<>"'`)).toBe('&amp;&lt;&gt;&quot;&#39;');
    });

    it('renders at most ten array entries', () => {
      const element = render(Array.from({ length: 11 }, (_, index) => index));

      expect(element.querySelectorAll('.json-string-array-entry')).toHaveLength(10);
      expect(element.textContent).toContain('1 more...');
    });
  });
});
