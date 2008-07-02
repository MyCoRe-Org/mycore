package org.mycore.frontend.iview;

public class MCRIViewTools {
	
	public static float computeScaleFactor(int origWidth, int origHeight, int targetWidth, int targetHeight) {
		float horizSF = (float)targetWidth/origWidth; 
		float vertSF = (float)targetHeight/origHeight;
		if (horizSF<vertSF) 
			return horizSF;
		else
			return vertSF;
	}
	
}