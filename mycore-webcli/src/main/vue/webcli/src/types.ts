export interface CommandEntry {
  command: string;
  help: string;
}

export interface CommandGroup {
  name: string;
  commands: CommandEntry[];
}

export interface SearchableCommand {
  command: string;
  groupName: string;
  help: string;
}

export interface LogEntry {
  exception: string | null;
  logLevel: string;
  message: string;
  time: number;
}

export interface Settings {
  historySize: number;
  comHistorySize: number;
  suggestionLimit: number;
  autoscroll: boolean;
  continueIfOneFails: boolean;
}

export type TransportEvent =
  | { type: 'commandList'; value: CommandGroup[] }
  | { type: 'log'; value: LogEntry }
  | { type: 'queue'; value: string[]; size: number }
  | { type: 'currentCommand'; value: string }
  | { type: 'continueIfOneFails'; value: boolean }
  | { type: 'noPermission' };
