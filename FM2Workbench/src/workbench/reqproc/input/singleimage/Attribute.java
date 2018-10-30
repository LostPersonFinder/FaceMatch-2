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

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every Annotation can have Attributes. The Attribute is a special type of
 * Annotation that modifies an existing Annotation by giving additional
 * information about it.
 * <p>
 * For instance annotations with skin in them may have an Attribute to describe
 * the general tone of the skin.
 * 
 * 
 * @author bonifantmc
 * @author glingappa@mail.nih.gov for gender and age support
 */
public class Attribute {
	/** rules defining Gender */
	public final static AttributeSet Gender = AttributeSet.load("Attributes/Gender.txt");
	/** rules defining Wounds */
	public final static AttributeSet Wounds = AttributeSet.load("Attributes/Wounds.txt");
	/** rules defining Skin Tone */
	public final static AttributeSet SkinTone = AttributeSet.load("Attributes/SkinTone.txt");
	/** rules defining Occlusions */
	public final static AttributeSet Occlusions = AttributeSet.load("Attributes/Occlusions.txt");
	/** rules defining Age */
	public final static AttributeSet Age = AttributeSet.load("Attributes/Age.txt");
	/** rules defining Kind */
	public final static AttributeSet Kind = AttributeSet.load("Attributes/Kind.txt");

	/** String for any Attribute that has not been set */
	public final static String UNMARKED = "unmarked";

	/** a list of Attributes used */
	private List<String> state = new ArrayList<>();
	/** the rules defining this Attribute */
	final private AttributeSet rules;

	/**
	 * 
	 * @param rules
	 *            the rules defining this attribute
	 * @param defaultState
	 *            the default state of an attribute (usually "Unmarked")
	 */
	public Attribute(AttributeSet rules, String defaultState) {
		this.rules = rules;
		this.state.add(defaultState);
	}

	/**
	 * @param rules
	 *            the rules defining the Attribute
	 */
	public Attribute(AttributeSet rules) {
		this(rules, UNMARKED);
	}

	/**
	 * @return the regex for this Attribute's rule set.
	 */
	public String regex() {
		return this.rules.regex();
	}

	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner(",", this.rules.id + "[", "]");
		for (String i : this.state)
			sj.add(i);
		return sj.toString();
	}

	/**
	 * @param values
	 *            the values to set the state to
	 * @return true if the state could be set (ie was valid) otherwise return
	 *         false
	 */
	public boolean setState(String... values) {
		if (values.length == 0)
			values = new String[] { Attribute.UNMARKED };
		if (this.rules.isValidSelection(values)) {
			this.state.clear();
			for (String s : values)
				this.state.add(s);
			return true;
		}
		return false;
	}

	/**
	 * @param s
	 *            the string to parse
	 * @return the attribute created
	 */
	public boolean parseSet(String s) {
		Pattern p = Pattern.compile(this.regex());
		Matcher m = p.matcher(s);
		if (m.matches())
			return this.setState(m.group(1).split(","));
		return false;

	}

	/** @return true if the only state included is UNMARKED */
	public boolean isUnmarked() {
		return this.state.size() == 1 && this.state.get(0).equals(UNMARKED);
	}

	/**
	 * @return an unmodifiable list of Attribute state/values
	 */
	public String[] values() {
		return this.state.toArray(new String[] {});
	}

	/** @return the rules of this Attribute */
	public AttributeSet getRules() {
		return this.rules;
	}

	/**
	 * @return a score for sorting this Attribute, the score is based on the
	 *         index the attribute's state is at in the rules' list of values,
	 *         higher scores are from closer to the top of the list, multiple
	 *         items also yields larger scores.
	 */
	public int score() {
		int score = 0;
		for (String s : this.state)
			score += (this.rules.values().size() - this.rules.values().indexOf(s));
		return score;
	}

	/**
	 * @return a List of all the states the Attribute is in currently.
	 */
	public List<String> getState() {
		return this.state;
	}
}