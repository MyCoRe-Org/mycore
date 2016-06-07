export class Settings {
  public historySize: number;
  public autoscroll: boolean;

  constructor(hS: number, aS: boolean){
    this.historySize = hS;
    this.autoscroll= aS;
  }
}
