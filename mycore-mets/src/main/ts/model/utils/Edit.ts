namespace org.mycore.mets.model {
    export class Edit<T> {
        public type: string;
        public previousValue: T;
        public currentValue: T;
        public of: any;
    }
}

