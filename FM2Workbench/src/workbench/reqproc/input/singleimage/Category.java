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

import java.util.List;
import java.util.regex.Pattern;

/**
 * Category Attributes mark an Annotation as being something more than just what
 * the Feature id of Annotation indicates. It gives a specific label, like
 * "Larry" or "Subject1". This is little more than an over-glorified string
 * wrapper in honesty.
 * 
 * @author bonifantmc
 *
 */
public class Category {
	/** A Pattern using this class's regex() method to matche Strings */
	static public final Pattern pattern = Pattern.compile(regex());
	/** A constant Category for untagged images */
	static public final Category UNTAGGED = new Category(Annotation.UNTAGGED, 'd');
	/** A constant Category unknown breeds */
	static public final Category BREED_UNKNOWN = new Category("unknown", 'b');
	/**
	 * Whatever this annotation is being marked as ( "Larry", "Pontiac Grand Am"
	 * , "Crumpled Oak Leaf"
	 */
	private String category;

	/**
	 * the choice of default character id/feature tag, default is 'd' for 'id'
	 * for identification
	 */
	final char id;

	/**
	 * Makes a free text attribute
	 * 
	 * @param s
	 *            a string marking what this category is of
	 * @param c
	 *            character mark for the attribute's id
	 */
	public Category(String s, Character c) {
		setIDString(s);

		if (c == null)
			c = 'd';
		this.id = c;
	}

	/**
	 * @return a regular expression for matching Age Attributes
	 */
	static public String regex() {
		return "[A-Za-z]\\[.*\\]";
	}

	/**
	 * Basic parser for getting an Age from a string
	 * 
	 * @param s
	 *            the string to parse
	 * @return Age that matches the input * @throws IllegalArgumentException if
	 *         the String doesn't match anything
	 */
	public static Category getAttribute(String s) {
		if (s.matches(regex()))
			return new Category(s.substring(2, s.length() - 1), s.charAt(0));
		throw new IllegalArgumentException();
	}

	/**
	 * 
	 * @return the formatted String required for writting this Attribute to a
	 *         lst file
	 */
	public String getID() {
		return this.id + "[" + toString() + "]";

	}

	/**
	 * @param category
	 *            value to set category to
	 */
	public void setIDString(String category) {

		if (category != null && category.equals(""))
			throw new IllegalArgumentException();
		this.category = category;
	}

	/**
	 * tells if category is exact same object, or at least same string
	 * 
	 * @return this == o || (o instanceof Category && this.category
	 *         .equals(((Category) o).category));
	 */
	@Override
	public boolean equals(Object o) {
		if (this.category == null)
			return false;
		if (!(o instanceof Category))
			throw new ClassCastException();

		return this.category.toString().equals(((Category) o).category.toString()) && this.id == ((Category) o).id;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * A string representation of the category (ie: this.category, in comparison
	 * to the Id String which is "id[(toString()]"
	 */
	@Override
	public String toString() {
		return this.category;
	}

	/**
	 * Test if a list of Categories contains a given object. Has to be rewritten
	 * from the default list.contains to treat Strings as Categories, because
	 * the default comparison does cati == oj || oj.equals(cati), which will
	 * fail since categories aren't Strings. This switches the object's place so
	 * its cati.equals(oj) in the comparison line.
	 * 
	 * @param list
	 *            the list to iterate over
	 * @param o
	 *            the object to see if the list contains
	 * @return true if the list contains a category representing the object
	 *         given
	 */
	static public boolean contains(List<? extends Category> list, Object o) {
		for (Category cat : list)
			if (cat == o || cat.equals(o))
				return true;
		return false;

	}
}