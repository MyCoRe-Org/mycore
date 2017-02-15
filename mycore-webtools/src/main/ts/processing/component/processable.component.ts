import { Component, Input } from '@angular/core';
import { Processable } from '../model/model';

@Component( {
    selector: '[processable]',
    templateUrl: 'html/processable.html',
})
export class ProcessableComponent {

    @Input() model: Processable;

    constructor() {

    }

}
