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

import java.awt.Color;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;



/**
 * The types of annotations that a .lst file can contain: Face, Profile, Skin,
 * Eyes, Nose, and Mouth. Each type has an associated character symbol for
 * printing in the annotations and a color for use in painting the annotation.
 * <p>
 * Features id an Annotation in its most general sense, and explain what an
 * annotations' bounding rectangle marks.
 * 
 * @author bonifantmc
 * 
 */
public enum Feature {
	// String sym, Color c, boolean human, boolean animal, boolean subfeature,
	// boolean square
	// Human Features
	/** Faces are blue and drawn as ovals */
	Face("f", Color.BLUE, FeatureFlags.HUMAN | FeatureFlags.ELLIPSE),
	/** Profiles are read and drawn as rectangles */
	Profile("p", Color.RED, FeatureFlags.HUMAN | FeatureFlags.RECTANGLE),
	//
	// human facial features
	//
	/** Eyes are yellow and drawn as ovals */
	Eyes("i", Color.YELLOW, FeatureFlags.FACE_FEATURES | FeatureFlags.ELLIPSE | FeatureFlags.SUB_FEATURE),
	/** Noses are cyan and drawn as rectangles */
	Nose("n", Color.CYAN, FeatureFlags.FACE_FEATURES | FeatureFlags.RECTANGLE | FeatureFlags.SUB_FEATURE),
	/** Mouths are green and drawn as rectangles */
	Mouth("m", Color.GREEN, FeatureFlags.FACE_FEATURES | FeatureFlags.RECTANGLE | FeatureFlags.SUB_FEATURE),
	/** Ears are Pink and drawn as Rectangles */
	Ear("e", Color.PINK, FeatureFlags.FACE_FEATURES | FeatureFlags.ELLIPSE | FeatureFlags.SUB_FEATURE);

	/**
	 * the color of a feature that's been selected, for display purposes (it's
	 * currently a dark purple, 80,0,80 on the RGB scale
	 */
	public static final Color COLOR_SELECTED = new Color(0x80, 0x00, 0x80);
	/**
	 * This is the character string that is printed when an annotation is
	 * written to a .lst file
	 */
	private String s;

	/** a flag defining traits of the feature */
	int flag = 0;

	/** the color to draw the feature with */
	private Color c;

	/**
	 * @param sym
	 *            the character symbol that each Feature has.
	 * @param color
	 *            the color to draw the Feature as.
	 * @param f
	 *            the flag defining the Feature's feature is
	 *            (rectangle/animal/face... statements).
	 */
	Feature(String sym, Color color, int f) {
		this.s = sym;
		this.c = color;
		this.flag = f;
	}

	/**
	 * @return the character string used in printing annotations
	 */
	public String getIdString() {
		return this.s;
	}

	/**
	 * @return The color that this feature should use for drawing.
	 */
	public Color getColor() {
		return this.c;
	}

	/**
	 * Return an Feature based on the first letter of the submitted string. If
	 * the string matches either the s of an Feature or an Feature's toString()
	 * this method returns that Feature.
	 * <p>
	 * e.g.:"f" and "Face" will return AnnoationType.Face, but "face" will not.
	 * <p>
	 * Anything that does not match the strings will return null by default
	 * 
	 * @param string
	 *            the string to parse to an Feature
	 * @return the Feature specified by the string.
	 */
	public static Feature parseFeature(String string) {
		if (string == null)
			return Face;
		switch (string) {
		case "p":
		case "P":
		case "Profile":
			return Profile;
		case "n":
		case "N":
		case "Nose":
			return Nose;
		case "m":
		case "M":
		case "Mouth":
			return Mouth;
		case "i":
		case "I":
		case "Eyes":
			return Eyes;
		case "e":
		case "E":
		case "Ear":
			return Ear;
		case "f":
		case "F":
		case "Face":
			return Face;
		default:
			return null;
		}
	}

	/** @return list of all characters that might identify a Feature */
	public static List<String> charValues() {
		Feature[] values = values();
		String[] s = new String[values.length * 2];
		for (int i = 0; i < values.length * 2; i += 2) {
			s[i] = values[i / 2].s;
			s[i + 1] = values[i / 2].s.toUpperCase();

		}
		return Arrays.asList(s);
	}


	/** @return list of human features */
	public static Feature[] humanValues() {
		Feature[] v = values();
		List<Feature> l = Arrays.asList(v);
		Iterator<Feature> i = l.iterator();
		Feature f;
		while (i.hasNext()) {
			f = i.next();
			if (!f.isHuman())
				i.remove();
		}
		return (Feature[]) l.toArray();
	}

	/** @return true if the feature is a sub_feature */
	public boolean isSubfeature() {
		return (this.flag & FeatureFlags.SUB_FEATURE) == FeatureFlags.SUB_FEATURE;
	}

	/** @return true if the feature is rectangular ellipse */
	public boolean isRectangle() {
		return (this.flag & FeatureFlags.RECTANGLE) == FeatureFlags.RECTANGLE;
	}

	/** @return true if the feature is human */
	public boolean isHuman() {

		return (this.flag & FeatureFlags.HUMAN) == FeatureFlags.HUMAN;

	}


	/** @return true if the feature contains facial features */
	public boolean isFacialFeatureContaining() {
		return (this.flag & FeatureFlags.FACE_CONTAINING) == FeatureFlags.FACE_CONTAINING;
	}

	/** @return true if the feature is a facial feature */
	public boolean isFacialFeature() {
		return (this.flag & FeatureFlags.FACE_FEATURES) == FeatureFlags.FACE_FEATURES;
	}


	public static Feature[] topLevelValues() {
		return new Feature[] { Face, Profile };
	}
	
	
	/**
	 * Flags indicating metadata about specific features.
	 * 
	 * @author bonifantmc
	 *
	 */
	public static class FeatureFlags {
		//
		// Flags
		//
		/** human features are skin, profile, and face */
		static int HUMAN = 1;
		/** face containing features are face, profile, head, and skin */
		static int FACE_CONTAINING = HUMAN << 1;
		/** face features are eye, nose, ear, and mouth */
		static int FACE_FEATURES = FACE_CONTAINING << 1;
		/** animal features are head, leg, tail, and body */
		/** Features that are displayed as Rectangles */
		static int RECTANGLE = FACE_FEATURES << 1;
		/** Features that are displayed as Ellipses */
		static int ELLIPSE = RECTANGLE << 1;
		/** true if a feature found within other features */
		static int SUB_FEATURE = ELLIPSE << 1;
	}
}
