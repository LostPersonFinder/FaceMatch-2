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
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import workbench.reqproc.input.singleimage.UndoRedo.AnnotationSnapshot;

/**
 * Opens a specified image to its fullest size possible according to screen
 * dimensions in a separate JFrame from the ILB.
 * <p>
 * EditImage windows cannot be resized.
 * <p>
 * EditImage uses the {@link DrawingTool} as a MouseListener and KeyListener for
 * editing image Annotations.
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
public class EditImage extends JPanel {
	/** metadata about the image to display */
	private MetaImage metaImage;

	/** tracks changes in the EditImage */
	final private UndoRedo ur;
	/** menu/display area for user to enter parameters */
	final AttributeMenu attributeMenu;

	// display
	/** the canvas the image is displayed in */
	final private ImageLabel il;

	/** Remove all annotations from the image */
	final Action clear = new Clear();

	/** action for undoing the last action the user took */
	final Action undo = new Undo();

	final JPanel content = new JPanel();

	/**
	 * 
	 * @param operation
	 * @param i
	 *            the image to display
	 * @param h
	 *            handles interactions with the images
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public EditImage(String uri, String operation) throws MalformedURLException, IOException {
		super();
		System.out.println(operation);

		// set important variables
		this.metaImage = new MetaImage(uri);

		// instantiate components of the EditImage
		this.il = new ImageLabel(metaImage);

		// build display
		content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
		content.add(getImageLabel());
		switch (operation) {
		case workbench.control.FMMenuCommands.INGEST_SINGLE_RT:
			attributeMenu = new IngestAttributeMenu(this);
			break;
		case workbench.control.FMMenuCommands.REMOVE_SINGLE_RT:
			attributeMenu = new RemoveAttributeMenu(this);

			break;
		case workbench.control.FMMenuCommands.QUERY_SINGLE_RT:
			attributeMenu = new QueryAttributeMenu(this);

			break;
		case workbench.control.FMMenuCommands.FF_SINGLE_RT:
			attributeMenu = new FaceFindAttributeMenu(this);

			break;
		default:
			attributeMenu = null;
			break;
		}
		content.add(attributeMenu);
		this.ur = new UndoRedo(this);
		this.add(content);

	}

	/**
	 * @param newState
	 *            the state to set the EditImage to
	 */
	public void setState(AnnotationSnapshot newState) {
		if (newState == null)
			return;
		// reset MetaImage Annotations
		getMetaImage().getAnnotations().clear();
		getMetaImage().getAnnotations().addAll(Annotation.cloneList(newState.annotations));

		this.attributeMenu.setState(newState);

	}

	/** Record the current state of the EditImage */
	public void snap() {
		getUndoRedo().takeSnapshot();
		getMetaImage().fireListeners();
	}

	/** @return the UndoReo tool that tracks the state of the EditImage */
	UndoRedo getUndoRedo() {
		return this.ur;
	}

	/** @return the ImageLabel that displays the image being Edited */
	ImageLabel getImageLabel() {
		return this.il;
	}

	/**
	 * @return the MetaImage this EditImage Displays
	 */
	public MetaImage getMetaImage() {
		return this.metaImage;
	}

	////////
	//////// ABSTRACT ACTIONS THAT CAN BE BUNDLED INTO BUTTONS mnemonics
	//////// Accelerators, etc
	///////

	/** clears all annotations for the given edit Image */
	class Clear extends AbstractAction {
		/***/
		public Clear() {
			super("Clear");
			putValue(SHORT_DESCRIPTION, "Remove all Annotations from the image.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			getMetaImage().getAnnotations().clear();
		}
	}

	/**
	 * call the undo action
	 */
	class Undo extends AbstractAction {
		/** prep the action's text and tooltip */
		public Undo() {
			super("Undo");
			putValue(SHORT_DESCRIPTION, "Undo the last action taken in changing the image annotation.");
		}

		public void actionPerformed(ActionEvent arg0) {
			EditImage.this.getUndoRedo().undo();
		}
	}

	public Map<? extends String, ? extends Object> getOutput() {
		return this.attributeMenu.getOutput();

	}

	public Dimension getPreferredSize() {
		Dimension d = new Dimension();
		d.height = (int) (this.il.getHeight() * 1.1);
		d.width = (int) (this.il.getWidth() * 2.0);
		return d;
	}

}
