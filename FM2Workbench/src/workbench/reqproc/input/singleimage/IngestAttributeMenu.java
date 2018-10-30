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
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import fmservice.httputils.common.ServiceConstants;

public class IngestAttributeMenu extends AttributeMenu {
	protected JTextField tag = new JTextField(), location = new JTextField();
	protected JRadioButton male = new JRadioButton("male"), female = new JRadioButton("female"),
			unknownGender = new JRadioButton("unknown"), adult = new JRadioButton("adult"),
			youth = new JRadioButton("youth"), unknownAge = new JRadioButton("unknown");

	IngestAttributeMenu(EditImage editImage) {
		super(editImage);
		mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));

		JPanel tagArea = new JPanel();
		tagArea.setLayout(new BoxLayout(tagArea, BoxLayout.X_AXIS));
		tagArea.add(new JLabel("Tag: "));
		tagArea.add(tag);
		tag.setMaximumSize(new Dimension(Integer.MAX_VALUE, tag.getPreferredSize().height));

		mainContent.add(tagArea);

		JPanel checkBoxArea = new JPanel();
		checkBoxArea.setLayout(new BoxLayout(checkBoxArea, BoxLayout.X_AXIS));

		JPanel genderArea = new JPanel();
		genderArea.setLayout(new BoxLayout(genderArea, BoxLayout.Y_AXIS));
		genderArea.add(new JLabel("Gender"));
		genderArea.add(male);
		genderArea.add(female);
		genderArea.add(unknownGender);
		checkBoxArea.add(genderArea);
		ActionListener genderListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				male.setSelected(false);
				female.setSelected(false);
				unknownGender.setSelected(false);
				((JRadioButton) arg0.getSource()).setSelected(true);
			}
		};
		male.addActionListener(genderListener);
		female.addActionListener(genderListener);
		unknownGender.addActionListener(genderListener);

		JPanel ageArea = new JPanel();
		ageArea.setLayout(new BoxLayout(ageArea, BoxLayout.Y_AXIS));
		ageArea.add(new JLabel("Age"));
		ageArea.add(adult);
		ageArea.add(youth);
		ageArea.add(unknownAge);
		checkBoxArea.add(ageArea);
		ActionListener ageListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				adult.setSelected(false);
				youth.setSelected(false);
				unknownAge.setSelected(false);
				((JRadioButton) arg0.getSource()).setSelected(true);
			}
		};
		adult.addActionListener(ageListener);
		youth.addActionListener(ageListener);
		unknownAge.addActionListener(ageListener);

		mainContent.add(checkBoxArea);

		JPanel locationArea = new JPanel();
		locationArea.setLayout(new BoxLayout(locationArea, BoxLayout.X_AXIS));
		locationArea.add(new JLabel("Location: "));
		locationArea.add(location);
		location.setMaximumSize(new Dimension(Integer.MAX_VALUE, location.getPreferredSize().height));

		mainContent.add(locationArea);

	}

	public Map<? extends String, ? extends Object> getOutput() {
		Map<String, Object> result = new HashMap<>();
		result.put(ServiceConstants.IMAGE_TAG, this.tag.getText());
		String gender = male.isSelected() ? male.getText()
				: female.isSelected() ? female.getText() : unknownGender.getText();
		result.put(ServiceConstants.GENDER, gender);
		String age = adult.isSelected() ? adult.getText() : youth.isSelected() ? youth.getText() : unknownAge.getText();
		result.put(ServiceConstants.AGE_GROUP, age);
		result.put(ServiceConstants.LOCATION, location.getText());
		return result;
	}

}
