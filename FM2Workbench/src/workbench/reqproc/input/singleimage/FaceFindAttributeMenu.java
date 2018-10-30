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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import fmservice.httputils.common.ServiceConstants;
import workbench.reqproc.input.singleimage.UndoRedo.AnnotationSnapshot;

public class FaceFindAttributeMenu extends AttributeMenu {
	/** selects which feature type is being annotated */
	protected JComboBox<Feature> featureType = new JComboBox<>(Feature.values());
	protected JCheckBox landmarks = new JCheckBox("Landmarks");

	protected DefaultComboBoxModel<Feature> topLevelOnly = new DefaultComboBoxModel<>(Feature.topLevelValues());
	protected ComboBoxModel<Feature> allFeatures = new DefaultComboBoxModel<>(Feature.values());
	protected JFormattedTextField inflation = new JFormattedTextField(NumberFormat.getNumberInstance());
	protected JTextField performance = new JTextField("progressive");
	protected DrawingTool dt;
	/** the toString of the Annotation */
	JLabel title = new JLabel();
	/**
	 * JScrollpane that wraps the title label, so as the title grows, it doesn't
	 * push down the rest of the buttons
	 */
	FixedHeightJScrollPane titleScroll = new FixedHeightJScrollPane(title, FixedHeightJScrollPane.DEFAULT);

	public FaceFindAttributeMenu(EditImage editImage) {
		super(editImage);
		topRow.add(featureType);
		topRow.add(landmarks);
		featureType.setPrototypeDisplayValue(Feature.Face);
		landmarks.addActionListener(new LandmarkListener());
		landmarks.setSelected(true);
		featureType.addActionListener(new FeatureTypeListener());

		inflation.setText("0.0");

		mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
		mainContent.add(titleScroll);

		JPanel inflate = new JPanel();
		inflate.setLayout(new BoxLayout(inflate, BoxLayout.X_AXIS));
		inflate.add(new JLabel("Inflation: "));
		inflate.add(inflation);
		mainContent.add(inflate);

		JPanel perf = new JPanel();
		perf.setLayout(new BoxLayout(perf, BoxLayout.X_AXIS));
		perf.add(new JLabel("Performance: "));
		perf.add(performance);
		mainContent.add(perf);
		mainContent.add(Box.createVerticalGlue());

		dt = new DrawingTool(this.editImage);
		this.editImage.getImageLabel().addMouseListener(dt);
		this.editImage.getImageLabel().addMouseMotionListener(dt);

		this.title.setText(titleFormat(this.editImage.getMetaImage().marksString().toString()));
		title.setOpaque(true);
		title.setBackground(Color.white);
		titleScroll.setBackground(Color.white);
		titleScroll.setBorder(BorderFactory.createMatteBorder(2, 5, 2, 2, Color.WHITE));

		inflation.setMaximumSize(new Dimension(Integer.MAX_VALUE, inflation.getPreferredSize().height));
		performance.setMaximumSize(new Dimension(Integer.MAX_VALUE, performance.getPreferredSize().height));
	}

	private class LandmarkListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (landmarks.isSelected())
				featureType.setModel(allFeatures);// featureType
			else {
				featureType.setModel(topLevelOnly);
				for (Annotation a : editImage.getMetaImage().getAnnotations())
					a.getSubannotes().clear();
				editImage.getMetaImage().fireListeners();
			}
			featureType.setPrototypeDisplayValue(Feature.Face);

		}
	}

	/**
	 * A Listener for this class' featureType JComboBox.
	 */
	private class FeatureTypeListener implements ActionListener {

		public void actionPerformed(ActionEvent arg0) {
			if (dt == null)
				return;
			dt.setDrawingFeature(featureType.getItemAt(featureType.getSelectedIndex()));

		}
	}

	public void setState(AnnotationSnapshot newState) {
		super.setState(newState);
		dt.setAnchor(newState.snapStart);
		dt.setTempAnnotation(newState.snapTempAnnotation == null ? null : newState.snapTempAnnotation.clone());
		dt.setDrawingFeature(newState.snapDrawingFeature);
		dt.setParent(newState.snapParent);

	}

	public Annotation getTempAnnotation() {
		return this.dt.getTempAnnotation();

	}

	public void onChange() {
		super.onChange();
		System.out.println("TIMES THEY ARE A CHANGING");
		this.title.setText(titleFormat(editImage.getMetaImage().marksString()));
	}

	/**
	 * 
	 * @param title
	 *            the title to format
	 * @return formatted version of the title, with html.
	 */
	String titleFormat(String title) {
		System.out.println(this.mainContent.getWidth());
		title = "<html><p style=\"width:" + 150 + "\">" + title + "</p></html>";
		return title;
	}
	
	
	
	public Map<?extends String, ?extends Object> getOutput(){
		Map<String,Object> result = new HashMap<>();
		result.put(ServiceConstants.REGION, this.editImage.getMetaImage().marksString());
		result.put(ServiceConstants.LANDMARKS, this.landmarks.isSelected());
		result.put(ServiceConstants.INFLATE_BY, this.inflation.getText());
		result.put(ServiceConstants.PERFORMANCE_PREF, this.performance.getText());
		//result.put(ServiceConstants.OPERATION, ServiceConstants.FACE_FIND);
		return result;
	}
}
