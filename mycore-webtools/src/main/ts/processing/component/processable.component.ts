import { Component, Input } from '@angular/core';
import { Processable } from '../model/model';

@Component( {
    selector: '[processable]',
    templateUrl: 'html/processable.html',
    styleUrls:  ['css/processable.css']
})
export class ProcessableComponent {

    @Input() model: Processable;

    constructor() {
    }

    getProgress() {
        return this.model.progress + "%";
    }

}