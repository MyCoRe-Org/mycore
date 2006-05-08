// ============================================== 
//  												
// Module-Imaging 1.0, 05-2006  		
// +++++++++++++++++++++++++++++++++++++			
//  												
// Andreas Trappe 	- idea, concept
// Chi Vu Huu		- concept, development
//
// $Revision$ $Date$ 
// ============================================== 

package org.mycore.services.imaging;

/**
 * A class to help benchmark code
 * It simulates a real stop watch
 */
public class Stopwatch {

  private long startTime = -1;
  private long stopTime = -1;
  private boolean running = false;

  public Stopwatch start() {
     startTime = System.currentTimeMillis();
     running = true;
     return this;
  }
  public Stopwatch stop() {
     stopTime = System.currentTimeMillis();
     running = false;
     return this;
  }
  /** returns elapsed time in milliseconds
    * if the watch has never been started then
    * return zero
    */
  public long getElapsedTime() {
     if (startTime == -1) {
        return 0;
     }
     if (running){
     return System.currentTimeMillis() - startTime;
     } else {
     return stopTime-startTime;
     } 
  }

  public Stopwatch reset() {
     startTime = -1;
     stopTime = -1;
     running = false;
     return this;
  }
}