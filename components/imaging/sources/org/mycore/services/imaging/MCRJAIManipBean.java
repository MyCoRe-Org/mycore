package org.mycore.services.imaging;

import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.apache.log4j.Logger;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;

public class MCRJAIManipBean {
	private static Logger LOGGER = Logger.getLogger(MCRJAIManipBean.class);
	protected double rotAngle = 0.0;
	protected double magFactor = 0.0;
	public double getRotAngle() {
		return rotAngle;
	}
	public void setRotAngle(double rotAngle) {
		this.rotAngle = rotAngle;
	}
	public double getMagFactor() {
		return magFactor;
	}
	public void setMagFactor(double magFactor) {
		this.magFactor = magFactor;
	}
	
	public void manipAndPost(String imagePath, OutputStream out) throws IOException{
		PlanarImage image = readAsPlanarImage(imagePath);
		PlanarImage outImage = manip(image, rotAngle, magFactor);
		Stopwatch sw = new Stopwatch();
		sw.start();
		saveAsJPEG(outImage, out);
		sw.stop();
		LOGGER.info("Time jpeg: " +sw.getElapsedTime());
	}
	
	public void manipAndPost(InputStream imgStream, OutputStream out) throws IOException{
		PlanarImage image = readAsStreamInRAM(imgStream);
		PlanarImage outImage = manip(image, rotAngle, magFactor);
		Stopwatch sw = new Stopwatch();
		sw.start();
		saveAsJPEG(outImage, out);
		sw.stop();
		LOGGER.info("Time jpeg: " +sw.getElapsedTime());
	}
	
	public void saveAsJPEG(RenderedImage image, OutputStream out) throws IOException {
        JPEGEncodeParam param = new JPEGEncodeParam();
        ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", out, param);
        encoder.encode(image);
        out.close();
    }

    public PlanarImage manip(PlanarImage image, double rotAngle, double magFactor) {
        if ((rotAngle <= 0.001) && (magFactor <= 0.0)) {
            return image;
        }

        RenderedOp op = null, op1 = null, op2 = null;

        if (rotAngle > 0.001) {
            op1 = rotate(image, rotAngle);
            if (magFactor <= 0.0) {
                return op1.createInstance();
            } else {
                op2 = scale(op1, magFactor, magFactor);
                return op2.createInstance();
            }
        } else {
            op2 = scale(image, magFactor, magFactor);
            return op2.createInstance();
        }
    }

    public RenderedOp scale(PlanarImage image, double magx, double magy) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add((float)magx);
        pb.add((float)magy);
        pb.add(0f);
        pb.add(0f);
        pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        return JAI.create("scale", pb);
    }

    public RenderedOp rotate(PlanarImage image, double rotAngle) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        Stopwatch sw = new Stopwatch();
        sw.start();
        pb.add((float)image.getWidth()/2);
        sw.stop();
        LOGGER.info("Time getwidth: " + sw.getElapsedTime());
        pb.add((float)image.getHeight()/2);
        pb.add((float)rotAngle);
        pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        return JAI.create("rotate", pb);
    }

    public PlanarImage readAsPlanarImage(String fileName) {
        return JAI.create("fileload", fileName);
    }
    
    public PlanarImage readAsStreamInRAM(InputStream input) {
        MemoryCacheSeekableStream stream = new MemoryCacheSeekableStream(input);
        return JAI.create("stream", stream);
    }
}
