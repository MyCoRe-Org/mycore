import type { CommandGroup } from '@/types';

export function makeCommandGroups(): CommandGroup[] {
  return [
    {
      name: 'Basic commands',
      commands: [
        {
          command: 'process resource {0}',
          help: 'Execute the commands listed in the resource file {0}.',
        },
        {
          command: 'skip on error',
          help: 'Skip execution of failed command in case of error',
        },
      ],
    },
    {
      name: 'Transformations',
      commands: [
        {
          command: 'xslt transform {0}',
          help: 'Run an XSLT transformation.',
        },
        {
          command: 'validate xslt {0}',
          help: 'Validate an XSLT transformation.',
        },
      ],
    },
  ];
}
