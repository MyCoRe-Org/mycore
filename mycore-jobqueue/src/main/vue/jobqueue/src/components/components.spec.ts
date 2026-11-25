import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it } from 'vitest';

import JobSearch from '@/components/JobSearch.vue';
import Pagination from '@/components/Pagination.vue';
import ShortenedText from '@/components/ShortenedText.vue';
import { FetchMock, translation } from '@/test/helpers/fetchMock';

describe('ShortenedText', () => {
  it('shows short texts completely without info icon', () => {
    const wrapper = mount(ShortenedText, { props: { text: 'short', maxLength: 10 } });

    expect(wrapper.text()).toBe('short');
    expect(wrapper.find('i.fa-info').exists()).toBe(false);
  });

  it('keeps texts with exactly the maximum length', () => {
    const wrapper = mount(ShortenedText, { props: { text: '0123456789', maxLength: 10 } });

    expect(wrapper.text()).toBe('0123456789');
  });

  it('shortens long texts and opens and closes the full text dialog', async () => {
    const wrapper = mount(ShortenedText, { props: { text: '0123456789abc', maxLength: 10 } });

    expect(wrapper.find('span').text()).toBe('0123456789...');
    expect(wrapper.find('.modal').exists()).toBe(false);

    await wrapper.get('i.fa-info').trigger('click');
    expect(wrapper.get('.modal pre').text()).toBe('0123456789abc');

    await wrapper.get('.modal-footer button').trigger('click');
    expect(wrapper.find('.modal').exists()).toBe(false);
  });

  it('follows text changes', async () => {
    const wrapper = mount(ShortenedText, { props: { text: 'short', maxLength: 10 } });

    await wrapper.setProps({ text: 'a much longer text' });

    expect(wrapper.find('span').text()).toBe('a much lon...');
  });
});

describe('Pagination', () => {
  beforeEach(() => {
    new FetchMock();
  });

  function pageNumbers(wrapper: ReturnType<typeof mount>): string[] {
    return wrapper.findAll('.page-item').slice(1, -1).map(page => page.text());
  }

  it('shows at most five pages before and after the current page', () => {
    const wrapper = mount(Pagination, { props: { currentPage: 10, pageCount: 30 } });

    expect(pageNumbers(wrapper)).toEqual(['5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15']);
  });

  it('clamps the page window at the first and the last page', async () => {
    const wrapper = mount(Pagination, { props: { currentPage: 2, pageCount: 4 } });
    expect(pageNumbers(wrapper)).toEqual(['1', '2', '3', '4']);

    await wrapper.setProps({ currentPage: 29, pageCount: 30 });
    expect(pageNumbers(wrapper)).toEqual(['24', '25', '26', '27', '28', '29', '30']);
  });

  it('emits the clicked page and its neighbours', async () => {
    const wrapper = mount(Pagination, { props: { currentPage: 3, pageCount: 5 } });
    const links = wrapper.findAll('.page-link');

    await links[0]!.trigger('click');
    await links[1]!.trigger('click');
    await links.at(-1)!.trigger('click');

    expect(wrapper.emitted('pageClicked')).toEqual([[2], [1], [4]]);
  });

  it('translates the previous and next labels', async () => {
    const wrapper = mount(Pagination, { props: { currentPage: 1, pageCount: 2 } });
    expect(wrapper.find('.page-link').text()).toBe('%jobqueue_job_pagination_previous%');

    await flushPromises();

    expect(wrapper.find('.page-link').text()).toBe(translation('jobqueue.job.pagination.previous'));
    expect(wrapper.findAll('.page-link').at(-1)!.text()).toBe(translation('jobqueue.job.pagination.next'));
  });
});

describe('JobSearch', () => {
  beforeEach(() => {
    new FetchMock();
  });

  it('prefills the search field with the current parameters', () => {
    const wrapper = mount(JobSearch, {
      props: {
        parameter: [
          { name: 'a', value: '1' },
          { name: 'b', value: '2' },
        ],
      },
    });

    expect((wrapper.get('input').element as HTMLInputElement).value).toBe('a:1 b:2');
  });

  it('emits the entered parameters as name value pairs', async () => {
    const wrapper = mount(JobSearch, { props: { parameter: [] } });

    await wrapper.get('input').setValue('object:mir_mods_00000001 path:a:b');
    await wrapper.get('button').trigger('click');

    expect(wrapper.emitted('search')).toEqual([
      [
        {
          parameter: [
            { name: 'object', value: 'mir_mods_00000001' },
            { name: 'path', value: 'a' },
          ],
        },
      ],
    ]);
  });

  it('translates heading and button', async () => {
    const wrapper = mount(JobSearch, { props: { parameter: [] } });
    await flushPromises();

    expect(wrapper.get('h3').text()).toBe(translation('jobqueue.job.search'));
    expect(wrapper.get('button').text()).toBe(translation('jobqueue.job.search.button'));
  });
});
