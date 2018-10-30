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

/** BatchInputSelectionDialog.java
 * 
 *  Allows FaceMatch2 user to provide inputs to conduct a test/operation or analyze results of a
 * previously stored  test operation performed in "batch mode"
 */

package workbench.reqproc.input;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import fmservice.httputils.common.ServiceConstants;
import javafx.util.Pair;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import workbench.display.DisplayUtils;



public class SingleRequestInputDialog extends InputSelectionDialog {

	// Note: Must be in same order as in Menu commands
	private static String[] singleOperationCommands = { ServiceConstants.INGEST, ServiceConstants.REMOVE,
			ServiceConstants.QUERY, ServiceConstants.FACE_FIND, SCENARIO_TEST };

	private static String[] OperationTypeRBTexts = { INGEST_SINGLE_RT, REMOVE_SINGLE_RT, QUERY_SINGLE_RT, FF_SINGLE_RT,
			SCENARIO_TEST };// singleOperationCommands;
	private static String myPanelTitle = "Select options for Single operations";

	private static final String defualtImageURL = "http://dlwsd4gyk2o60.cloudfront.net/63/94/93b4c6934858a3f151ac1947fc96/governor-pence-official-headshot-high-res.jpg";// the
	// NLM
	// logo

	/*--------------------------------------------------------------------------------------------------------*/
	// constructor
	public SingleRequestInputDialog(JFrame frame, HashMap<String, Object> defaultParams) {
		super(frame, defaultParams);
		setPanelTitle(myPanelTitle);
		setOperationTypeInfo(singleOperationCommands, OperationTypeRBTexts);
		buildInputSelectionPanel();
	}

	/*------------------------------------------------------------------------------------*/
	// set the initial choices as indicated in the input parameter map
	/*------------------------------------------------------------------------------------*/
	public void setInitialChoices(String command, HashMap<String, Object> inputParams) {
		super.setInitialChoices(command, inputParams);
	}
	////////////////////////////////////////////////////////////////////////

	// Check the input parameters provided by the user
	protected boolean validateSelectedParams(HashMap<String, Object> inputParams) {
		// Check image Collection name
		String clientName = (String) inputParams.get(ServiceConstants.CLIENT_NAME_PARAM);
		String imageExtent = (String) inputParams.get(ServiceConstants.EXTENT);
		String fmOperation = (String) inputParams.get(ServiceConstants.OPERATION);
		// check URL is real
		String imageURL = (String) inputParams.get(ServiceConstants.URL);
		if (imageURL == null || !uRLExists(imageURL)) {
			return false;
		}

		// check the Stored result directory
		boolean storeResults = ((Boolean) inputParams.get("storeResults")).booleanValue();
		if (storeResults) {
			String resultStoreDir = (String) inputParams.get("resultsDirectory");
			File dir = new File(resultStoreDir);
			if (!dir.exists() || !dir.isDirectory()) {
				DisplayUtils.displayErrorMessage(
						"Please specify a valid directory name to store Facematch Service results");
				return false;
			}
		}

		if (clientName == null) {
			clientName = this.clientComboBox.getSelectedItem().toString();
			inputParams.put(ServiceConstants.CLIENT_NAME_PARAM, clientName);
		}
		if (imageExtent == null) {
			imageExtent = this.imageExtentComboBox.getSelectedItem().toString();
			inputParams.put(ServiceConstants.EXTENT, imageExtent);
		}

		if (fmOperation == null) {
			switch (operation) {
			case INGEST_SINGLE_RT:
				fmOperation = ServiceConstants.INGEST;
				break;
			case REMOVE_SINGLE_RT:
				fmOperation = ServiceConstants.REMOVE;
				break;
			case QUERY_SINGLE_RT:
				fmOperation = ServiceConstants.QUERY;
				break;
			case FF_SINGLE_RT:
				fmOperation = ServiceConstants.GET_FACES;
				break;
			case SCENARIO_TEST:
			default:
				return false;

			}
			inputParams.put(ServiceConstants.OPERATION, fmOperation);
		}

		return true;
	}

	/****************************************************************************
	 * Action Listener for selecting FileChoosers
	 **************************************************************************/
	public void actionPerformed(ActionEvent evt) {
		System.out.println("Fancy Version");
		// Handle action.
		if (evt.getSource() == dataFilenameButton) {

			String url = DisplayUtils.getUserInput("Please enter an Image URI", defualtImageURL);

			// user canceled:
			if (url == null)
				return;

			// validate image exists:
			boolean urlIsReal = uRLExists(url);
			if (!urlIsReal) {
				DisplayUtils.displayErrorMessage("URL does not exist/is not accessible.");
				return;
			}

			// Get params for the different webservices
			int confirmation = DisplayUtils.DisplayImageMetaDataQuery(url, selectedParams, operation);

			if (confirmation == JOptionPane.CANCEL_OPTION)
				return;
			if (confirmation == JOptionPane.NO_OPTION) {
				actionPerformed(evt);
				return;
			}
			if (confirmation != JOptionPane.OK_OPTION) {
				DisplayUtils.displayErrorMessage("There was an error loading the image you requested");
				return;
			}

			selectedDataFileLabel.setText(url);
			selectedParams.put(ServiceConstants.URL, url);
		}

		// We only ask for the directory here because the result file name is
		// derived from the test file
		else if (evt.getSource() == this.resultDirectoryButton) {
			String resultDirName = DisplayUtils.selectDirectory(selectedResultDirLabel.getText(), ""); /// initial
																										/// value
			selectedResultDirLabel.setText(resultDirName); // user selected
															// value
			selectedParams.put("resultsDirectory", resultDirName);
		}
	}

	/************************************************************************
	 * Validate that a given URL actually exists and is accessible.
	 ************************************************************************/
	private boolean uRLExists(String fileName) {
		try {
			HttpURLConnection.setFollowRedirects(false);
			URLConnection con = new java.net.URL(fileName).openConnection();
			if (con.getContent()==null) {
				DisplayUtils.displayErrorMessage("Please select a valid image URL");
				return false;
			}
			return true;
		} catch (IOException e) {
			DisplayUtils.displayErrorMessage("Could not validate URL");
			return false;
		}
	}

}
