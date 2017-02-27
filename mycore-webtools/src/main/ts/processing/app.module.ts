// modules
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpModule } from '@angular/http';
import { MomentModule } from 'angular2-moment';
import { ModalModule } from 'ng2-bootstrap';

// components
import { AppComponent } from './component/app.component';
import { CollectionComponent } from './component/collection.component';
import { ProcessableComponent } from './component/processable.component';

// pipes
import { JsonStringPipe } from './pipe/json.pipe';

@NgModule( {
    imports: [BrowserModule, HttpModule, MomentModule, ModalModule.forRoot()],
    bootstrap: [AppComponent],
    declarations: [AppComponent, CollectionComponent, ProcessableComponent, JsonStringPipe]
})
export class AppModule {
}
