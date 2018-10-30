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

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Stack;

/**
 * 
 * A snapshot of this Tool at one instance in time.
 * 
 * @author Girish
 * 
 */

class UndoRedo {
	/** stack of previous states */
	private Stack<AnnotationSnapshot> undo = new Stack<>();
	/** stack of states undone that can be redone */
	private Stack<AnnotationSnapshot> redo = new Stack<>();
	/** the image to monitor */
	private EditImage image;

	/**
	 * @param image
	 *            the image this UndoRedo tracks
	 */
	public UndoRedo(EditImage image) {
		this.image = image;
		takeSnapshot();
	}

	/**
	 * take a snapshot of the current state of the EditImage
	 */
	public void takeSnapshot() {
		this.redo.clear();
		this.undo.push(new AnnotationSnapshot(this.image));
	}

	/**
	 * Undo the last action done
	 */
	public void undo() {
		System.out.println("i" + undo.size());
		if (this.undo.size() > 1) {
			System.out.println("j" + undo.size());
			this.redo.push(this.undo.pop());
			this.image.setState(this.undo.peek());
		} else
			this.image.setState(this.undo.peek());
	}

	/** redo the last action undone */
	public void redo() {
		if (this.redo.size() > 1) {
			AnnotationSnapshot newState = this.redo.pop();
			this.image.setState(newState);
			this.redo.push(newState);
		}

	}

	/**
	 * Tracks the state of an EditImage
	 * 
	 * @author bonifantmc
	 *
	 */
	static class AnnotationSnapshot {
		/** this snapshot's list of Annotations at one instance in time */
		final List<Annotation> annotations;
		/** the current Annotation being edited at one instance in time */
		final Annotation snapTempAnnotation;
		/** the current Feature being edited at one instance in time */
		final Feature snapDrawingFeature;
		/** the parent of tempAnnotation at one instance in time */
		final Annotation snapParent;
		/** the start point of tempAnnotation at one instance in time */
		final Point2D.Double snapStart;

		/**
		 * 
		 * @param image
		 *            the image to snapshot
		 */
		private AnnotationSnapshot(EditImage image) {

			this.annotations = Annotation.cloneList(image.getMetaImage().getAnnotations());
			DrawingTool dt = image.attributeMenu.getDrawingTool();
			if (dt != null) {
				this.snapTempAnnotation = dt.getTempAnnotation() == null ? null : dt.getTempAnnotation().clone();
				this.snapDrawingFeature = dt.getDrawingFeature();
				this.snapParent = dt.getParent();
				this.snapStart = dt.getAnchor();
			} else {
				this.snapTempAnnotation = null;
				this.snapDrawingFeature = null;
				this.snapParent = null;
				this.snapStart = null;
			}

		}
	}

	/**
	 * Update the undo/redo stacks for a new MetaImage
	 * 
	 * @param i
	 *            the new MetaImage
	 */
	public void changeImage(MetaImage i) {
		this.undo.clear();
		this.redo.clear();
		if (i != null)
			this.takeSnapshot();
		// TODO WHAT SHOULD IMAGE EDITOR DO DIFFERENTLY
		// image.update();

	}

	/** call undo until the EditImage reverts to it's original state. */
	public void revert() {
		while (this.undo.size() > 1)
			undo();
		undo();
	}
}