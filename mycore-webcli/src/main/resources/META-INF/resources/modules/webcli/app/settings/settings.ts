export class Settings {
  public historySize: number;
  public autoscroll: boolean;
  public continueIfOneFails: boolean;

  constructor(hS: number, aS: boolean, c: boolean){
    this.historySize = hS;
    this.autoscroll = aS;
    this.continueIfOneFails = c;
  }
}
