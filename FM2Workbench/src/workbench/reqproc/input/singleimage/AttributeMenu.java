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

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import workbench.reqproc.input.singleimage.MetaImage.MetaImageChangeListener;
import workbench.reqproc.input.singleimage.UndoRedo.AnnotationSnapshot;

/**
 * Used for selecting attributes for an image
 * 
 * @author bonifantmc
 */
@SuppressWarnings("serial")
public class AttributeMenu extends JPanel implements MetaImageChangeListener {
	/** the MetaImage being annotated */
	protected MetaImage image;

	/** the EditImage this belongs to */
	protected EditImage editImage;

	protected JPanel topRow = new JPanel();
	protected JPanel bottomRow = new JPanel();
	protected JPanel mainContent = new JPanel();

	/**
	 * @param editImage
	 *            the frame/image this menu is editing
	 */
	AttributeMenu(EditImage editImage) {
		// set important fields
		this.editImage = editImage;
		topRow.setLayout(new GridBagLayout());
		bottomRow.setLayout(new GridLayout(1, 5));
		this.setLayout(new BorderLayout());
		this.add(mainContent, BorderLayout.CENTER);
		this.add(topRow, BorderLayout.NORTH);
		this.add(bottomRow, BorderLayout.SOUTH);
		this.editImage.getMetaImage().addListener(this);

	}

	

	/**
	 * @author bonifantmc
	 *
	 */
	private class AttributeCheckBoxGroup extends JPanel {
		/**
		 * valid sets for the states that the AttributeCheckBox can be in.
		 */
		Attribute a;
		/** the annotation the attribute belongs to */
		Annotation annot;
		/***/
		private final ArrayList<JToggleButton> boxes = new ArrayList<>();

		/**
		 * @param a
		 *            the Attribute the box interfaces with
		 * @param annote
		 *            the annotation the Attribute belongs to
		 */
		public AttributeCheckBoxGroup(Attribute a, Annotation annote) {
			this.a = a;
			this.annot = annote;
			buildDisplay();
			for (String s : a.values())
				for (JToggleButton b : this.boxes)
					if (b.getText().equals(s))
						b.setSelected(true);
		}

		/***/
		private void buildDisplay() {
			this.removeAll();
			this.boxes.clear();

			this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			this.add(new JLabel(this.a.getRules().toString()));
			if (a.getRules().getValidLength() == 1)
				for (String s : this.a.getRules().values()) {
					JRadioButton b = new JRadioButton(s);
					this.boxes.add(b);
					this.add(b);
				}
			else
				for (String s : this.a.getRules().values()) {
					JCheckBox b = new JCheckBox(s);
					this.boxes.add(b);
					this.add(b);
				}
			for (JToggleButton b : this.boxes)
				b.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						String[] states = getSelectedStates();
						if (AttributeCheckBoxGroup.this.a.getRules().isValidSelection(states)) {
							AttributeCheckBoxGroup.this.a.setState(states);

						} else if (b.isSelected()) {
							for (JToggleButton box : AttributeCheckBoxGroup.this.boxes)
								box.setSelected(false);
							b.setSelected(true);
							AttributeCheckBoxGroup.this.a.setState(b.getText());
						} else if (states.length == 0) {
							for (JToggleButton box : AttributeCheckBoxGroup.this.boxes)
								if (box.getText().equals(Attribute.UNMARKED))
									box.setSelected(true);
							AttributeCheckBoxGroup.this.a.setState(Attribute.UNMARKED);
						}
						System.out.println("Checking if Selected.");
						boolean noneChecked = true;
						for (JToggleButton box : AttributeCheckBoxGroup.this.boxes) {
							noneChecked &= !box.isSelected();
							System.out.println(box.getText());
							System.out.println(box.isSelected());
							System.out.println(noneChecked);
							System.out.println();
						}
						System.out.println("noneChecked:" + noneChecked);
						if (noneChecked) {
							AttributeCheckBoxGroup.this.a.setState(Attribute.UNMARKED);
							for (JToggleButton box : boxes)
								if (box.getText().equals(Attribute.UNMARKED))
									box.setSelected(true);

						}
						AttributeCheckBoxGroup.this.annot.fireListener();
					}
				});
		}

		/**
		 * @return find the selected boxes, and return a list of the selected
		 *         states from them
		 */
		String[] getSelectedStates() {
			List<String> ret = new ArrayList<>();
			for (JToggleButton box : this.boxes)
				if (box.isSelected())
					ret.add(box.getText());
			return ret.toArray(new String[] {});
		}
	}

	@Override
	public void onChange() {
		// TODO Auto-generated method stub

	}

	public void setState(AnnotationSnapshot newState) {
		// TODO

	}

	public Map<? extends String, ? extends Object> getOutput() {
		return new HashMap<String, Object>();
	}

	public DrawingTool getDrawingTool() {
		return null;
	}
}
