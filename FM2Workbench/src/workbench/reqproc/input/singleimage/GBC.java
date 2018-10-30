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
/*----------------------------------------------------------------*/
/*
GBC - A convenience class to tame the GridBagLayout

Copyright (C) 2002 Cay S. Horstmann (http://horstmann.com)

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package workbench.reqproc.input.singleimage;

import java.awt.GridBagConstraints;

/**
 * This class simplifies the use of the GridBagConstraints class.
 */
@SuppressWarnings("serial")
public class GBC extends GridBagConstraints {
	/**
	 * Constructs a GBC with a given gridx and gridy position and all other grid
	 * bag constraint values set to the default.
	 * 
	 * @param gridx
	 *            the gridx position
	 * @param gridy
	 *            the gridy position
	 */
	public GBC(int gridx, int gridy) {
		this.gridx = gridx;
		this.gridy = gridy;
	}

	/**
	 * Sets the cell spans.
	 * 
	 * @param gridwidth
	 *            the cell span in x-direction
	 * @param gridheight
	 *            the cell span in y-direction
	 * @return this object for further modification
	 */
	public GBC setSpan(int gridwidth, int gridheight) {
		this.gridwidth = gridwidth;
		this.gridheight = gridheight;
		return this;
	}

	/**
	 * Sets the anchor.
	 * 
	 * @param anchor
	 *            the anchor value
	 * @return this object for further modification
	 */
	public GBC setAnchor(int anchor) {
		this.anchor = anchor;
		return this;
	}

	/**
	 * Sets the fill direction.
	 * 
	 * @param fill
	 *            the fill direction
	 * @return this object for further modification
	 */
	public GBC setFill(int fill) {
		this.fill = fill;
		return this;
	}

	/**
	 * Sets the cell weights.
	 * 
	 * @param weightx
	 *            the cell weight in x-direction
	 * @param weighty
	 *            the cell weight in y-direction
	 * @return this object for further modification
	 */
	public GBC setWeight(double weightx, double weighty) {
		this.weightx = weightx;
		this.weighty = weighty;
		return this;
	}

	/**
	 * Sets the insets of this cell.
	 * 
	 * @param distance
	 *            the spacing to use in all directions
	 * @return this object for further modification
	 */
	public GBC setInsets(int distance) {
		this.insets = new java.awt.Insets(distance, distance, distance, distance);
		return this;
	}

	/**
	 * Sets the insets of this cell.
	 * 
	 * @param top
	 *            the spacing to use on top
	 * @param left
	 *            the spacing to use to the left
	 * @param bottom
	 *            the spacing to use on the bottom
	 * @param right
	 *            the spacing to use to the right
	 * @return this object for further modification
	 */
	public GBC setInsets(int top, int left, int bottom, int right) {
		this.insets = new java.awt.Insets(top, left, bottom, right);
		return this;
	}

	/**
	 * Sets the internal padding
	 * 
	 * @param ipadx
	 *            the internal padding in x-direction
	 * @param ipady
	 *            the internal padding in y-direction
	 * @return this object for further modification
	 */
	public GBC setIpad(int ipadx, int ipady) {
		this.ipadx = ipadx;
		this.ipady = ipady;
		return this;
	}

	public GBC setLocation(int x, int y) {
		this.gridx = x;
		this.gridy = y;
		return this;
	}

	public GBC setX(int x) {
		this.gridx = x;
		return this;
	}

	public GBC setY(int y) {
		this.gridy = y;
		return this;
	}

}