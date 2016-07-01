export class Settings {
  public historySize: number;
  public comHistorySize: number;
  public autoscroll: boolean;
  public continueIfOneFails: boolean;

  constructor(hS: number, cHS: number, aS: boolean, c: boolean){
    this.historySize = hS;
    this.comHistorySize = cHS;
    this.autoscroll = aS;
    this.continueIfOneFails = c;
  }
}
