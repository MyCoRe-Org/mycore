
export interface JobQueueList {
    queue: Array<JobQueueBase>;
}

export interface JobQueueBase {
    name: string;
}


export interface JobQueueDetailed {
    name: string;
    job: Array<Job>;
}

export interface Job {
    id: number;
    status: "FINISHED" | "NEW" | "RUNNING" | "ERROR" | "MAX_TRIES";

    date:  Array<JobDate>

    parameter: Array<JobParameter>

    tries?: number;

    exception?: string;
}

export interface JobDate {
    type: "added" | "start" | "finished",
    value: number
}

export interface JobParameter {
    name: string;
    value: string;
}