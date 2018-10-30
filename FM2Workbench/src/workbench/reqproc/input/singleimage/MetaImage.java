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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * MetaImages can be obtained from reading a lst file or looking at a directory.
 * Those taken from directories have a file name, memory size, width and length.
 * If the image is taken from a lst file it will have those fields in addition
 * to an ArrayList of Annotations
 * 
 * Both have the flags inList and found to check that the image is from a lst
 * file or found in a given directory. inList will result in green or red
 * borders for image thumbnails. found will result in the image displaying in
 * the search, if not found, then a red "X" is displayed in its place.
 * 
 * @author bonifantmc
 * 
 */
public class MetaImage implements Nameable, ListDataListener, Comparable<MetaImage> {

	// Object variables
	/** file name */
	private String name;

	/** List of annotations to an image */
	private final List<Annotation> annotations = new ArrayList<>();

	/** The image group this MetaImage is part of */
	private String tag;

	/** Size of image in bytes */
	long memSize;

	public BufferedImage bufferedImage;

	public double matchValue;

	/** listeners attached to this image */
	Vector<MetaImageChangeListener> listeners = new Vector<>();

	/**
	 * only used for generating test data in generateImage method
	 * 
	 * @param uri
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public MetaImage(String uri) throws MalformedURLException, IOException {
		this.name = uri;
		this.bufferedImage = ImageIO.read(new URL(uri));
	}

	/**
	 * @return tab deliminated string of all marks/annotations of this image.
	 */
	String marksString() {
		return Annotation.toString(annotations);
	}

	/**
	 * Creates a String matching the same structure as the annotations that is
	 * "<filename> (\t MARK)*"
	 */
	@Override
	public String toString() {
		return "\nURL:\t" + this.getName() + "\nTag:\t" + tag + "\nDistance:\t" + this.matchValue + "\nRegion:"
				+ marksString();
	}

	/**
	 * Hashes images based on their file name
	 * 
	 * @return the index
	 */
	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}

	/**
	 * MetaImages are equal when they share the same index location in FMList.ls
	 * when FMList.ls is first loaded. This means they have the same hash, since
	 * their hash is their index
	 * 
	 * @param o
	 *            the object being compared to this.
	 * @return whether this and the object indicated are equal
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof MetaImage && this.hashCode() == o.hashCode();
	}

	/**
	 * Checks if this image matches the set FMList.glob.
	 * 
	 * @param mask
	 *            the glob pattern to mask with
	 * @return True if the image matches the glob, the glob is null, or the glob
	 *         is an empty string.
	 */
	@Override
	public boolean matchesGlob(String mask) {
		if (mask == null || mask.equals(""))
			mask = "**";
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + mask);
		return matcher.matches(Paths.get(this.getName()));

	}

	/**
	 * Using a Regular Expressions extract the wildcard group requested.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<Comparable> getGroups(Pattern p) {
		Matcher m = p.matcher(getName());
		if (!m.matches())
			return new ArrayList<>(Arrays.asList(new Comparable[] { getName() }));
		ArrayList<Comparable> ret = new ArrayList<>();
		for (int i = 1; i < m.groupCount() + 1; i++) {
			if (m.group(i) == null)
				continue;
			else if (m.group(i).matches("-?\\d+"))
				ret.add(new Integer(Integer.parseInt(m.group(i))));
			else if (m.group(i).matches("-?\\d+\\.\\d+"))
				ret.add(new Double(Double.parseDouble(m.group(i))));
			else
				ret.add(m.group(i));
		}
		return ret;
	}

	/**
	 * Creates a human readable representation of the memory size recorded in
	 * the MetaImage. The unit prefixes are in accordance with SI metric units
	 * (1kB = 1000B etc.)
	 * 
	 * 
	 * @return a human readable representation of this.memSize
	 */
	public String humanReadableByteCount() {
		int unit = 1000;
		if (this.memSize < unit)
			return this.memSize + " B";
		int exp = (int) (Math.log(this.memSize) / Math.log(unit));
		String pre = "kMGTPE".charAt(exp - 1) + "B";
		return String.format("%.1f %s", this.memSize / Math.pow(unit, exp), pre);
	}

	/**
	 * @return A string representation of what this MetaImage's tooltip should
	 *         entail if its to be displayed by a renderer
	 */
	public String toolTip() {
		StringBuilder tip = new StringBuilder();
		tip.append("<html>Name: ");
		tip.append(getName());
		tip.append("<br>File Size: ");
		tip.append(humanReadableByteCount());
		tip.append("<br>Dimension: ");
		tip.append(getWidth());
		tip.append("X");
		tip.append(getHeight());
		tip.append("<br>Annotations: <br>");
		tip.append(marksString().replaceAll("}", "}<br>"));
		tip.append("<br>Last Modified: ");
		tip.append("</html>");
		return tip.toString();
	}

	/**
	 * print all fields this MetaImage has
	 */
	public void printFields() {
		try {
			for (Field fd : this.getClass().getDeclaredFields()) {
				System.out.println(fd.getName() + ": " + fd.get(this));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** @return the height of this image */
	public int getHeight() {
		return this.bufferedImage.getHeight();
	}

	/** @return the width of this image */
	public int getWidth() {
		return this.bufferedImage.getWidth();
	}

	/** @return the Annotations for this image */
	public List<Annotation> getAnnotations() {
		return this.annotations;
	}

	/**
	 * @param annotations
	 *            the new annotations for this image
	 */
	public void setAnnotations(List<Annotation> annotations) {
		this.annotations.clear();
		this.annotations.addAll(annotations);
		this.fireListeners();
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * @param name
	 *            the new name for this image
	 */
	public void setName(String name) {
		this.name = name;
		this.fireListeners();
	}

	/**
	 * Iterate through the list and find the Annotation that matches the given
	 * string
	 * 
	 * @param string
	 *            a string representation of the annotation being sought
	 * @return the sought annotation if the image has a copy of it (here a copy
	 *         means they cover the same area and have the same feature type.
	 *         Categories and excess information like gender are ignored). OR
	 *         null if no such element exists
	 *
	 */
	public Annotation getAnnotationByString(String string) {
		Annotation note = Annotation.parseAnnotation(string, null);
		int buffer = 20;
		for (Annotation a : getAnnotations()) {
			System.out.println(a + " vs " + string);
			// within buffer required because values slightly change for
			// annotations TODO check out FaceMatch code and see why that is
			if (withinBuffer(a.x, note.x, buffer) && withinBuffer(a.y, note.y, buffer)
					&& withinBuffer(a.width, note.width, buffer) && withinBuffer(a.height, note.height, buffer)
					&& a.getId().equals(note.getId())) {
				return a;
			}
		}
		throw new NoSuchElementException(note.toString());
	}

	/**
	 * 
	 * @param x
	 *            test value
	 * @param y
	 *            target value
	 * @param z
	 *            buffer value
	 * @return true if x is within z of y
	 */
	private static boolean withinBuffer(double x, double y, int z) {
		return Math.abs(x - y) <= z;
	}

	/**
	 * @return the name of the group this image is set in (it matches all other
	 *         images in that group)
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * @param imageGroupId
	 *            the name of the group the image fits in, all other images in
	 *            that group match this and share this id
	 */
	public void setTag(String tag) {
		this.tag = tag;
		this.fireListeners();
	}

	/**
	 * 
	 * @param pattern
	 *            the regex pattern to filter this image's name by
	 * @return a dash separated list of what the pattern matched in this image's
	 *         name
	 */
	public String getGroupsString(Pattern pattern) {
		Matcher m = pattern.matcher(getName());
		int i = 0;
		StringBuilder ret = new StringBuilder();

		if (m.matches()) {
			while (++i <= m.groupCount())
				ret.append("-").append(m.group(i));
			if (ret.length() > 1)
				return ret.toString().substring(1, ret.toString().length());
			return ret.toString();
		}
		return "Failed Pattern Match";
	}

	/***
	 * 
	 * @author bonifantmc
	 *
	 */
	public interface MetaImageChangeListener {

		/** do something when MetaImage Changes */
		public void onChange();

	}

	/**
	 * @param l
	 *            the listener to add
	 */
	public void addListener(MetaImageChangeListener l) {
		if (!this.listeners.contains(l))
			this.listeners.addElement(l);
	}

	/**
	 * @param l
	 *            the listener to remove
	 */
	public void removeListener(MetaImageChangeListener l) {
		while (this.listeners.contains(l))
			this.listeners.remove(l);
	}

	/** activate listeners */
	synchronized void fireListeners() {
		for (MetaImageChangeListener l : this.listeners)
			l.onChange();
	}

	@Override
	public void contentsChanged(ListDataEvent e) {
		fireListeners();
	}

	@Override
	public void intervalAdded(ListDataEvent e) {
		fireListeners();
	}

	@Override
	public void intervalRemoved(ListDataEvent e) {
		fireListeners();
	}

	public MetaImage clone() {
		MetaImage result;
		try {
			result = new MetaImage(this.name);
			result.setAnnotations(Annotation.cloneList(this.annotations));
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public int compareTo(MetaImage o) {
		return Double.compare(this.matchValue, o.matchValue);
	}

}