import { flushPromises, type VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import type { Job, JobQueueDetailed, JobQueueList } from '@/api/JobQueue';
import router from '@/router';
import { FetchMock, jsonResponse, translation } from '@/test/helpers/fetchMock';
import { mountAppAt, type MountedApp } from '@/test/helpers/mountApp';

const QUEUES: JobQueueList = { queue: [{ name: 'mail' }, { name: 'derivate' }] };

const ADDED = Date.UTC(2026, 0, 2, 3, 4, 5);
const STARTED = Date.UTC(2026, 0, 2, 3, 5, 0);
const FINISHED = Date.UTC(2026, 0, 2, 3, 6, 0);
const LONG_EXCEPTION = 'java.lang.IllegalStateException: '.padEnd(120, 'x');

function job(id: number, overrides: Partial<Job> = {}): Job {
  return {
    id,
    status: 'FINISHED',
    date: [
      { type: 'added', value: ADDED },
      { type: 'start', value: STARTED },
      { type: 'finished', value: FINISHED },
    ],
    parameter: [{ name: 'object', value: `mir_mods_0000000${id}` }],
    tries: 1,
    ...overrides,
  };
}

function queue(jobs: Job[]): JobQueueDetailed {
  return { name: 'mail', job: jobs };
}

function apiUrls(fetchMock: FetchMock): string[] {
  return fetchMock.apiRequests().map(call => call.url);
}

describe('jobqueue app', () => {
  let fetchMock: FetchMock;
  let app: MountedApp | null = null;

  async function mountAt(path: string): Promise<VueWrapper> {
    app = await mountAppAt(path);
    return app.wrapper;
  }

  beforeEach(async () => {
    fetchMock = new FetchMock();
    await router.replace('/');
  });

  afterEach(() => {
    if (app !== null) {
      expect(app.warnings).toEqual([]);
      expect(app.errors).toEqual([]);
      app.wrapper.unmount();
      app = null;
    }
  });

  describe('queue list', () => {
    it('loads the queues with the jwt authorization header and links each queue', async () => {
      fetchMock.on('api/jobqueue/', () => jsonResponse(QUEUES));

      const wrapper = await mountAt('/');

      const [request] = fetchMock.apiRequests();
      expect(request?.url).toBe('http://localhost:3000/mir/api/jobqueue/');
      expect(new Headers(request?.init?.headers).get('authorization')).toBe('Bearer test-token');
      const links = wrapper.findAll('.jobqueue-list-item a');
      expect(links.map(link => link.text())).toEqual(['mail', 'derivate']);
      expect(links.map(link => link.attributes('href'))).toEqual([
        '/mir/jobqueue/mail/ALL',
        '/mir/jobqueue/derivate/ALL',
      ]);
      expect(wrapper.find('.spinner-border').exists()).toBe(false);
    });

    it('shows a spinner until the queues are loaded', async () => {
      let resolveQueues: (response: Response) => void = () => {};
      fetchMock.on('api/jobqueue/', () => new Promise<Response>(resolve => (resolveQueues = resolve)));

      const wrapper = await mountAt('/');
      expect(wrapper.find('.spinner-border').exists()).toBe(true);

      resolveQueues(jsonResponse(QUEUES));
      await flushPromises();
      expect(wrapper.find('.spinner-border').exists()).toBe(false);
      expect(wrapper.findAll('.jobqueue-list-item')).toHaveLength(2);
    });

    it('shows an error when the server rejects the request', async () => {
      fetchMock.on('api/jobqueue/', () => new Response('', { status: 500 }));

      const wrapper = await mountAt('/');

      expect(wrapper.find('.alert-danger').exists()).toBe(true);
      expect(wrapper.find('.jobqueue-list').exists()).toBe(false);
    });

    it('shows an error when the request fails', async () => {
      fetchMock.on('api/jobqueue/', () => Promise.reject(new TypeError('Failed to fetch')));

      const wrapper = await mountAt('/');

      expect(wrapper.find('.alert-danger').exists()).toBe(true);
    });

    it('shows an error and skips the api request when the jwt login fails', async () => {
      fetchMock.on('rsc/jwt', () => jsonResponse({ login_success: false }));

      const wrapper = await mountAt('/');

      expect(wrapper.find('.alert-danger').exists()).toBe(true);
      expect(fetchMock.apiRequests()).toEqual([]);
    });

    it('shows the not found page when the job queue api is not available', async () => {
      fetchMock.on('api/jobqueue/', () => new Response('', { status: 404 }));

      const wrapper = await mountAt('/');

      expect(router.currentRoute.value.name).toBe('404');
      expect(wrapper.get('.jumbotron p').text()).toBe(translation('jobqueue.404'));
    });

    it('navigates to the job table of a queue', async () => {
      fetchMock
        .on('api/jobqueue/', () => jsonResponse(QUEUES))
        .on('api/jobqueue/mail', () => jsonResponse(queue([job(1)]), { headers: { 'X-Total-Count': '1' } }));

      const wrapper = await mountAt('/');
      await wrapper.get('.jobqueue-list-item a').trigger('click');
      await flushPromises();

      expect(router.currentRoute.value.fullPath).toBe('/mail/ALL');
      expect(wrapper.findAll('tbody tr')).toHaveLength(1);
    });
  });

  describe('job table', () => {
    function serveJobs(jobs: Job[], total = jobs.length): void {
      fetchMock.on('api/jobqueue/mail?', () =>
        jsonResponse(queue(jobs), { headers: { 'X-Total-Count': String(total) } })
      );
    }

    it('requests all jobs of the queue and renders one row per job', async () => {
      serveJobs([job(1), job(2, { status: 'ERROR', tries: 3, exception: 'boom' })]);

      const wrapper = await mountAt('/mail/ALL');

      expect(apiUrls(fetchMock)).toEqual(['http://localhost:3000/mir/api/jobqueue/mail?limit=50']);
      const rows = wrapper.findAll('tbody tr');
      expect(rows).toHaveLength(2);
      const cells = rows[1]!.findAll('th, td').map(cell => cell.text());
      expect(cells).toEqual([
        '2',
        'ERROR',
        new Date(ADDED).toLocaleString(),
        new Date(STARTED).toLocaleString(),
        new Date(FINISHED).toLocaleString(),
        'boom',
        '3',
        'object:mir_mods_00000002',
      ]);
    });

    it('leaves date cells empty for dates the job does not have yet', async () => {
      serveJobs([job(1, { status: 'NEW', date: [{ type: 'added', value: ADDED }] })]);

      const wrapper = await mountAt('/mail/ALL');

      const cells = wrapper.findAll('tbody tr td').map(cell => cell.text());
      expect(cells.slice(1, 4)).toEqual([new Date(ADDED).toLocaleString(), '', '']);
    });

    it('filters by status for every status tab except ALL', async () => {
      serveJobs([job(1, { status: 'ERROR' })]);

      await mountAt('/mail/ERROR');

      expect(apiUrls(fetchMock)).toEqual(['http://localhost:3000/mir/api/jobqueue/mail?status=ERROR&limit=50']);
    });

    it('links the status tabs and marks the current one as active', async () => {
      serveJobs([job(1)]);

      const wrapper = await mountAt('/mail/FINISHED');

      const tabs = wrapper.findAll('.nav-tabs .nav-link');
      expect(tabs.map(tab => tab.attributes('href'))).toEqual(
        ['ALL', 'NEW', 'PROCESSING', 'FINISHED', 'ERROR', 'MAX_TRIES'].map(status => `/mir/jobqueue/mail/${status}`)
      );
      expect(tabs.filter(tab => tab.classes('active')).map(tab => tab.attributes('href'))).toEqual([
        '/mir/jobqueue/mail/FINISHED',
      ]);
    });

    it('reloads the jobs when another status tab is selected', async () => {
      serveJobs([job(1)]);
      const wrapper = await mountAt('/mail/ALL');

      await wrapper.findAll('.nav-tabs .nav-link')[4]!.trigger('click');
      await flushPromises();

      expect(router.currentRoute.value.fullPath).toBe('/mail/ERROR');
      expect(apiUrls(fetchMock)).toEqual([
        'http://localhost:3000/mir/api/jobqueue/mail?limit=50',
        'http://localhost:3000/mir/api/jobqueue/mail?status=ERROR&limit=50',
      ]);
    });

    it('renders the breadcrumb with the queue and the translated status', async () => {
      serveJobs([job(1)]);

      const wrapper = await mountAt('/mail/NEW');

      const crumbs = wrapper.findAll('.breadcrumb-item a');
      expect(crumbs.map(crumb => crumb.text())).toEqual([
        'Job Queue',
        'mail',
        translation('jobqueue.job.status.NEW'),
      ]);
      expect(crumbs.map(crumb => crumb.attributes('href'))).toEqual([
        '/mir/jobqueue/',
        '/mir/jobqueue/mail/ALL',
        '/mir/jobqueue/mail/NEW',
      ]);
    });

    it('translates the table headers in the current language', async () => {
      serveJobs([job(1)]);

      const wrapper = await mountAt('/mail/ALL');

      expect(wrapper.findAll('thead th').map(header => header.text())).toEqual(
        [
          'jobqueue.job.jobId',
          'jobqueue.job.status',
          'jobqueue.job.date.added',
          'jobqueue.job.date.start',
          'jobqueue.job.date.finished',
          'jobqueue.job.exception',
          'jobqueue.job.tries',
          'jobqueue.job.parameter',
        ].map(translation)
      );
      expect(fetchMock.requests('rsc/locale/translate/de/').length).toBeGreaterThan(0);
      expect(fetchMock.requests('rsc/locale/translate/').every(call => call.url.includes('/translate/de/'))).toBe(
        true
      );
    });

    it('shows placeholders until the translations arrive', async () => {
      fetchMock.on('rsc/locale/translate/', () => new Promise<Response>(() => {}));
      serveJobs([job(1)]);

      const wrapper = await mountAt('/mail/ALL');

      expect(wrapper.find('thead th').text()).toBe('%jobqueue_job_jobId%');
    });

    it('shortens long exceptions and shows the full text on demand', async () => {
      serveJobs([job(1, { status: 'ERROR', exception: LONG_EXCEPTION })]);

      const wrapper = await mountAt('/mail/ALL');

      const exceptionCell = wrapper.findAll('tbody td')[4]!;
      expect(exceptionCell.text()).toBe(`${LONG_EXCEPTION.substring(0, 50)}...`);
      await exceptionCell.get('i.fa-info').trigger('click');
      expect(wrapper.get('.modal pre').text()).toBe(LONG_EXCEPTION);
    });

    it('joins multiple job parameters', async () => {
      serveJobs([
        job(1, {
          parameter: [
            { name: 'a', value: '1' },
            { name: 'b', value: '2' },
          ],
        }),
      ]);

      const wrapper = await mountAt('/mail/ALL');

      expect(wrapper.findAll('tbody td')[6]!.text()).toBe('a:1\n b:2');
    });

    it('shows an error when the jobs cannot be loaded', async () => {
      fetchMock.on('api/jobqueue/mail?', () => Promise.reject(new TypeError('Failed to fetch')));

      const wrapper = await mountAt('/mail/ALL');

      expect(wrapper.get('.alert-danger').text()).toBe('TypeError: Failed to fetch');
      expect(wrapper.find('table').exists()).toBe(false);
    });

    it('redirects to the not found page for an unknown queue', async () => {
      fetchMock.on('api/jobqueue/unknown?', () => new Response('', { status: 404 }));

      const wrapper = await mountAt('/unknown/ALL');

      expect(router.currentRoute.value.name).toBe('404');
      expect(wrapper.get('.jumbotron p').text()).toBe(translation('jobqueue.404'));
    });

    it('shows an error without redirect for other server errors', async () => {
      fetchMock.on('api/jobqueue/mail?', () => new Response('', { status: 500 }));

      const wrapper = await mountAt('/mail/ALL');

      expect(router.currentRoute.value.fullPath).toBe('/mail/ALL');
      expect(wrapper.find('.alert-danger').exists()).toBe(true);
    });

    describe('pagination', () => {
      it('is hidden when all jobs fit on one page', async () => {
        serveJobs([job(1)], 50);

        const wrapper = await mountAt('/mail/ALL');

        expect(wrapper.find('.jobqueue-pagination').exists()).toBe(false);
      });

      it('is hidden when the server sends no total count', async () => {
        fetchMock.on('api/jobqueue/mail?', () => jsonResponse(queue([job(1), job(2)])));

        const wrapper = await mountAt('/mail/ALL');

        expect(wrapper.find('.jobqueue-pagination').exists()).toBe(false);
        expect(wrapper.findAll('tbody tr')).toHaveLength(2);
        expect(wrapper.find('.alert-danger').exists()).toBe(false);
      });

      it('is shown above and below the table when there are more jobs', async () => {
        serveJobs([job(1)], 120);

        const wrapper = await mountAt('/mail/ALL');

        const paginations = wrapper.findAll('.jobqueue-pagination');
        expect(paginations).toHaveLength(2);
        const pages = paginations[0]!.findAll('.page-item');
        expect(pages.map(page => page.text())).toEqual([
          translation('jobqueue.job.pagination.previous'),
          '1',
          '2',
          '3',
          translation('jobqueue.job.pagination.next'),
        ]);
        expect(pages[0]!.classes()).toContain('disabled');
        expect(pages[1]!.classes()).toContain('active');
        expect(pages[4]!.classes()).not.toContain('disabled');
      });

      it('loads the selected page with the matching offset', async () => {
        serveJobs([job(1)], 120);
        const wrapper = await mountAt('/mail/ALL');

        await wrapper.findAll('.jobqueue-pagination .page-link')[2]!.trigger('click');
        await flushPromises();

        expect(router.currentRoute.value.query).toEqual({ offset: '50' });
        expect(apiUrls(fetchMock).at(-1)).toBe('http://localhost:3000/mir/api/jobqueue/mail?limit=50&offset=50');
        const pages = wrapper.findAll('.jobqueue-pagination')[0]!.findAll('.page-item');
        expect(pages[2]!.classes()).toContain('active');
        expect(pages[0]!.classes()).not.toContain('disabled');
      });

      it('disables next on the last page and moves back with previous', async () => {
        serveJobs([job(1)], 120);
        const wrapper = await mountAt('/mail/ALL?offset=100');

        const pages = wrapper.findAll('.jobqueue-pagination')[0]!.findAll('.page-item');
        expect(pages[3]!.classes()).toContain('active');
        expect(pages[4]!.classes()).toContain('disabled');

        await pages[0]!.get('a').trigger('click');
        await flushPromises();

        expect(router.currentRoute.value.query).toEqual({ offset: '50' });
      });

      it('keeps the parameter filter when changing the page', async () => {
        serveJobs([job(1)], 120);
        const wrapper = await mountAt('/mail/ALL?parameter=object:mir_mods_00000001');

        await wrapper.findAll('.jobqueue-pagination .page-link')[2]!.trigger('click');
        await flushPromises();

        expect(router.currentRoute.value.query).toEqual({ offset: '50', parameter: 'object:mir_mods_00000001' });
      });
    });

    describe('parameter search', () => {
      it('is offered with an empty field when no parameter filter is set', async () => {
        serveJobs([job(1)]);
        const wrapper = await mountAt('/mail/ALL');

        expect((wrapper.get('#parameterName').element as HTMLInputElement).value).toBe('');
        await wrapper.get('#parameterName').setValue('object:mir_mods_00000001');
        await wrapper.get('.jobqueue-search-wrapper button').trigger('click');
        await flushPromises();

        expect(router.currentRoute.value.query).toEqual({ offset: '0', parameter: ['object:mir_mods_00000001'] });
      });

      it('sends the parameter filters of the route to the api', async () => {
        serveJobs([job(1)]);

        const wrapper = await mountAt('/mail/ALL?parameter=object:mir_mods_00000001&parameter=type:mail');

        expect(apiUrls(fetchMock)).toEqual([
          'http://localhost:3000/mir/api/jobqueue/mail?limit=50&parameter=object:mir_mods_00000001&parameter=type:mail',
        ]);
        expect((wrapper.get('#parameterName').element as HTMLInputElement).value).toBe(
          'object:mir_mods_00000001 type:mail'
        );
      });

      it('puts a new search into the route and starts at the first page', async () => {
        serveJobs([job(1)], 120);
        const wrapper = await mountAt('/mail/ERROR?parameter=object:old&offset=50');

        await wrapper.get('#parameterName').setValue('object:new type:mail incomplete');
        await wrapper.get('.jobqueue-search-wrapper button').trigger('click');
        await flushPromises();

        expect(router.currentRoute.value.path).toBe('/mail/ERROR');
        expect(router.currentRoute.value.query).toEqual({ offset: '0', parameter: ['object:new', 'type:mail'] });
        expect(apiUrls(fetchMock).at(-1)).toBe(
          'http://localhost:3000/mir/api/jobqueue/mail?status=ERROR&limit=50&parameter=object:new&parameter=type:mail&offset=0'
        );
      });
    });
  });
});
