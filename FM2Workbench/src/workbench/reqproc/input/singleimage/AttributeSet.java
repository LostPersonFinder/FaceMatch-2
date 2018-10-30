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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;

/**
 *
 *         An attribute, is a list of possible features. In annotations they
 *         appear as a character id, followed by a list of selections comma
 *         separated.
 *         <p>
 *         For instance: w[blood,scar,bruise]. could indicate a wound with
 *         blood, scars, and bruising.
 *         <p>
 *         Or: g[male] could indicate a male gendered object.
 *
 *
 *
 */
public class AttributeSet {

	/** an array of possible attributes (ex: {"blood", "scars", "bruising") */
	final private List<String> tags;
	/**
	 * an array of valid states that attributes can be selected in (ex:
	 * {{"blood"}, {"blood", "scars"}, {"scars", "bruising"}})
	 */
	final private String[][] invalidSelections;
	/**
	 * a character id to identify what the attribute is marking (ex: w for wound
	 * or g for gender)
	 */
	char id;

	/**
	 * a string labeling what this AttributeSet is identifying (eg: gender,
	 * wounds, eye color, etc)
	 */
	String characteristic = "Label";

	/**
	 * the number of states that can be selected, anything less than 1 indicates
	 * there is no limit
	 */
	int validLength = -1;

	/**
	 * @param t
	 *            the tags
	 * @param inv
	 *            the invalid selection states
	 * @param i
	 *            the id
	 * @param n
	 *            the name for what this AttributeSet characterizes
	 * @param j
	 *            the length of a valid selection state
	 */
	public AttributeSet(String[] t, String[][] inv, char i, String n, int j) {
		this.id = i;
		this.tags = Collections.unmodifiableList(Arrays.asList(t));
		this.invalidSelections = inv;
		this.characteristic = n;
		this.validLength = j;
	}

	/**
	 * @param s
	 *            string to check if it's a valid attribute
	 * @return true if the attribute is contained in this set.
	 */
	public boolean contains(String s) {
		for (String is : this.tags)
			if (is.equals(s))
				return true;
		return false;
	}

	/**
	 * Checks if a given selection is valid.
	 * 
	 * @param selection
	 *            the state to check
	 * @return true if the state is among the valid selections
	 */
	public boolean isValidSelection(String... selection) {
		// easiest check first, must meet length requirement
		if (this.validLength > 0 && selection.length > this.validLength)
			return false;
		// next easiest check, must actually be a list of states for this
		// attribute
		for (String select : selection)
			if (!contains(select))
				return false;

		// hardest check, must be a valid state for this attribute
		for (String[] vs : this.invalidSelections)
			if (contains(selection, vs))
				return false;
		return true;
	}

	/**
	 * Checks if vs is a subset of selection
	 * 
	 * @param selection
	 *            the selection
	 * @param vs
	 *            a subset to look for
	 * @return true iff vs is a subset of selection
	 */
	private boolean contains(String[] selection, String[] vs) {
		if (vs.length > selection.length)
			return false;
		HashSet<String> s = new HashSet<String>();
		for (String select : selection)
			s.add(select);
		for (String select : vs)
			if (!s.contains(select))
				return false;

		return true;
	}

	/**
	 * @return a regex that can parse the String
	 */
	public String regex() {
		StringBuilder bldr = new StringBuilder();
		bldr.append(this.id);
		bldr.append("\\[(");

		StringJoiner options = new StringJoiner("|");
		for (String selection : this.tags)
			options.add(selection);
		bldr.append("((");
		bldr.append(options.toString());
		bldr.append("),)*");
		bldr.append("(");
		bldr.append(options.toString());
		bldr.append("))\\]");
		return bldr.toString();
	}

	/**
	 * Parses a File into an AttributeSet,
	 * 
	 * A file detailing an AttributeSet can have any number of commented lines
	 * at the start (lines starting with '#').
	 * <p>
	 * After comments the first line must be a single character, the id. (eg: w)
	 * <p>
	 * Then the second line must be a comma separated list of possible tags (eg:
	 * blood,guts,bruises)
	 * <p>
	 * Each of the remaining lines is a colon separated list detailing a valid
	 * state (eg: blood:scar, is the state that accepts scar and blood).
	 * 
	 * @param path
	 *            location of a text file encapsulating the attribute
	 * @return the Attribute
	 */
	public static AttributeSet load(String path) {
		File f = new File(path);
		if (!f.exists() || !f.isFile())
			throw new IllegalArgumentException(f.getAbsolutePath() + " is not a file to read");

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String id = br.readLine();
			while (id.charAt(0) == '#')// skip comments at the start
				id = br.readLine();
			String name = br.readLine();
			String length = br.readLine();
			String tags = br.readLine();
			ArrayList<String> arr = new ArrayList<String>();
			String invalidStates;
			while ((invalidStates = br.readLine()) != null)
				arr.add(invalidStates);

			return parse(name, id, tags, arr.toArray(new String[] {}), length);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * @param n
	 *            the name of the Attribute set
	 * @param i
	 *            the character id
	 * @param t
	 *            the tags, each state is separated by a comma
	 * @param inv
	 *            the invalid states that cannot be allowed
	 * @param length
	 *            the length a selection can be
	 * @return the Attribute these create.
	 */
	public static AttributeSet parse(String n, String i, String t, String[] inv, String length) {
		i = i.trim();
		if (i.length() != 1)
			throw new IllegalArgumentException(i + " is not a character");
		char id = i.charAt(0);

		String[] tags = t.split(",");
		String[][] validStates = new String[inv.length][];
		for (int j = 0; j < inv.length; j++)
			validStates[j] = inv[j].split(":");
		return new AttributeSet(tags, validStates, id, n, Integer.parseInt(length));
	}

	@Override
	public String toString() {
		return this.characteristic;
	}

	/** @return a List of the tags this AttributeSet contains */
	public List<String> values() {
		return this.tags;
	}

	/** @return the valid length of AttributeSet Selection */
	public int getValidLength() {
		return this.validLength;
	}
}
