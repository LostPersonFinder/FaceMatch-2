/*
Informational Notice:
This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
an agency of the Department of Health and Human Services, United States Government.

- The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.

- The license does not supersede any applicable United States law.

- The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
(FAR) 48 C.F.R. Part52.227-14, Rights in Data—General.
The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
non-commercial products.

LICENSE:

Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
as provided by Federal law.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
•	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.

•	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.

•	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITEDTO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package workbench.reqproc.input.singleimage;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import javax.swing.JLabel;

/***
 * Encapsulates the display of an Image and its Annotations
 * 
 * @author bonifantmc
 *
 */
@SuppressWarnings("serial")
public class ImageLabel extends JLabel {

	/** metadata about the image to display */
	private MetaImage metaImage;

	/** copy of image from MetaImage to play with */
	private BufferedImage image;
	/** how much the image should be scaled */
	double scale;
	/** maximum width of picture to display */
	private int maxWidth = 400;
	/** maximum height of picture to display */
	private int maxHeight = 400;
	private static final int MAX_WIDTH = 300;
	private static final int MAX_HEIGHT = 300;

	/**
	 * 
	 * @param i
	 *            the metadata about the image to display
	 * @param h
	 *            provides access to the images and their directory.
	 */
	public ImageLabel(MetaImage i) {
		this(i, MAX_WIDTH, MAX_HEIGHT);
	}

	ImageLabel(MetaImage i, int w, int h) {
		this.maxHeight = h;
		this.maxWidth = w;
		this.metaImage = i;
		if (metaImage.bufferedImage != null)
			this.image = deepCopy(metaImage.bufferedImage);
		reSetImage();
	}

	/**
	 * (re)load the image for display and adjust the size of ImageLabel
	 */
	private void reSetImage() {
		// prepare for disposal if the image has been set to null
		// void out the different listeners
		if (this.metaImage == null) {
			while (this.getKeyListeners().length > 0)
				this.removeKeyListener(this.getKeyListeners()[0]);
			while (this.getMouseListeners().length > 0)
				this.removeMouseListener(this.getMouseListeners()[0]);
			while (this.getMouseMotionListeners().length > 0)
				this.removeMouseMotionListener(this.getMouseMotionListeners()[0]);

			return;
		}

		double width = this.metaImage.getWidth();
		double height = this.metaImage.getHeight();

		this.scale = scaleMatch(width, height, maxWidth, maxHeight);

		width *= this.scale;
		height *= this.scale;
		this.image = scale(this.image, this.scale);

		Dimension d = new Dimension((int) width, (int) height);
		setSize(d);
		setMaximumSize(d);
		setPreferredSize(d);
		setMinimumSize(d);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponents(g);

		// full icon size
		int w = getWidth();
		int h = getHeight();

		//
		double width = this.metaImage.getWidth() * this.scale;
		double height = this.metaImage.getHeight() * this.scale;

		// fill entire shape, image may not cover all the area, for instance if
		// the image is really small the space for the close, minimize, maximize
		// buttons will force the panel to be larger
		g.setColor(getBackground());
		g.fillRect(0, 0, w, h);

		// draw the image and its annotations.
		g.drawImage(this.image, 0, 0, (int) width, (int) height, this);
		Annotation.paintAnnotations((Graphics2D) g, this.metaImage.getAnnotations(), this.scale);
	}


	///////////////////////////////////////
	// IMAGE SCALING METHODS
	///////////////////////////////////////

	/**
	 * find the scale between that meets both the requested width and height
	 * from the given width and height
	 * 
	 * @param curWid
	 *            current width of the object to scale
	 * @param curHei
	 *            current height of the object to scale
	 * @param reqWid
	 *            the required width of the object to scale
	 * @param reqHei
	 *            the required height of the object to scale
	 * @return scale factor for current width and height to match the requested
	 *         width and height
	 */
	static public double scale(double curWid, double curHei, double reqWid, double reqHei) {
		double scale = 1;
		if (curWid > reqWid)
			scale = reqWid / curWid;
		if (curHei * scale > reqHei && !(curWid * reqHei / curHei > reqWid))
			scale = reqHei / curHei;
		return scale;
	}

	/**
	 * 
	 * @param width
	 *            the current width
	 * @param length
	 *            the current length
	 * @return scaler value required to reduce inputs to fit within required
	 *         Thumbnail Size.
	 */
	double scaleThumbSize(int width, int length) {
		return scale(width, length, (double) MAX_WIDTH, (double) MAX_HEIGHT);
	}

	/**
	 * Scales an image for display
	 * 
	 * @param source
	 *            the image to scale
	 * @param ratio
	 *            how large/small to scale image
	 * @return a copy of the scaled image
	 */
	static public BufferedImage scale(BufferedImage source, double ratio) {
		// if no scale just return a deep copy (don't return the source because
		// these images get written on and we want to keep the source images
		// clean)
		if (ratio == 1) {
			ColorModel cm = source.getColorModel();
			boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
			WritableRaster raster = source.copyData(source.getRaster().createCompatibleWritableRaster());
			return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		}
		int w = (int) (source.getWidth() * ratio);
		int h = (int) (source.getHeight() * ratio);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		BufferedImage bi = gc.createCompatibleImage(w, h);

		Graphics2D g2d = bi.createGraphics();

		AffineTransform at = AffineTransform.getScaleInstance(ratio, ratio);
		g2d.drawRenderedImage(source, at);
		g2d.dispose();
		ge = null;

		return bi;
	}

	/**
	 * Find the scale that makes the current dimensions match as much as
	 * possible the required ones
	 * 
	 * @param curWid
	 *            the width to scale
	 * @param curHei
	 *            the height to scale
	 * @param reqWid
	 *            the required width the scaled width must be less than (or
	 *            equal to)
	 * @param reqHei
	 *            the required height the scaled height must be less than(or
	 *            equal to)
	 * @return the scale that makes the curWid and curHei closest to the reqWid
	 *         and reqHei while maintain their ratio to each other
	 */
	static public double scaleMatch(double curWid, double curHei, double reqWid, double reqHei) {

		double scale = 1;
		if (curWid > reqWid && curHei > reqHei) {
			scale = reqWid / curWid;
			if (curHei * scale > reqHei)
				scale = reqHei / curHei;

		} else {
			scale = curWid / reqWid;
			if (curHei * scale < reqHei)
				scale = reqHei / curHei;
		}
		return scale;
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	public static BufferedImage scale(BufferedImage bufferedImage, int i, int j) {
		double scale = scale(bufferedImage.getWidth(), bufferedImage.getHeight(), i, j);
		return scale(bufferedImage, scale);
	}
}
