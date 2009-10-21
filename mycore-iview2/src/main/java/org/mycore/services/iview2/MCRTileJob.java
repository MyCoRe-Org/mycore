package org.mycore.services.iview2;

import java.util.Date;

public class MCRTileJob {
	/*
	public enum JobState {
		NEW ('n'),
	    PROCESS ('p'),
	    FIN ('f');

	    private char status;
	    
	    JobState(char status) {
	        this.status = status;
	    }
	    
	    public char toChar() {
			return status;
	    }
	}*/
	
	  private long id;
	  private String derivate;
	  private String path;
	  private char status;
	  private Date added;
	  private Date start;
	  private Date finished;
	  private long tiles;
	  private long width;
	  private long height;
	  private long zoomLevel;
	  
	public Date getAdded() {
		return added;
	}
	public void setAdded(Date added) {
		this.added = added;
	}
	public String getDerivate() {
		return derivate;
	}
	public void setDerivate(String derivate) {
		this.derivate = derivate;
	}
	public Date getFinished() {
		return finished;
	}
	public void setFinished(Date finished) {
		this.finished = finished;
	}
	public long getHeight() {
		return height;
	}
	public void setHeight(long height) {
		this.height = height;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Date getStart() {
		return start;
	}
	public void setStart(Date start) {
		this.start = start;
	}
	public char getStatus() {
		return status;
	}
	public void setStatus(/*MCRTileJob.*/MCRJobState status) {
		this.status = status.toChar();
	}
	private void setStatus(char status) {
		this.status = status;
	}
	public long getTiles() {
		return tiles;
	}
	public void setTiles(long tiles) {
		this.tiles = tiles;
	}
	public long getWidth() {
		return width;
	}
	public void setWidth(long width) {
		this.width = width;
	}
	public long getZoomLevel() {
		return zoomLevel;
	}
	public void setZoomLevel(long zoomLevel) {
		this.zoomLevel = zoomLevel;
	}
  
}
