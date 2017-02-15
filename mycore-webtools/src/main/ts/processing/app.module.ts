// modules
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpModule } from '@angular/http';
import { MomentModule } from 'angular2-moment';

// components
import { AppComponent } from './component/app.component';
import { CollectionComponent } from './component/collection.component';
import { ProcessableComponent } from './component/processable.component';

@NgModule( {
    imports: [BrowserModule, HttpModule, MomentModule],
    bootstrap: [AppComponent],
    declarations: [AppComponent, CollectionComponent, ProcessableComponent]
})
export class AppModule {
}
