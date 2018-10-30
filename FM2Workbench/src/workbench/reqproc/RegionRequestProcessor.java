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

package workbench.reqproc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import fm2client.core.FM2ServiceRequestor;
import fm2client.core.FM2ServiceResult;
import fmservice.httputils.common.ServiceConstants;
import workbench.control.FMMenuCommands;
import workbench.control.WBProperties;
import workbench.display.DisplayUtils;
import workbench.event.ActionCompletionListener;
import workbench.reqproc.input.SingleRequestInputDialog;
import workbench.util.Utils;
import workbench.util.WBUtils;


public class RegionRequestProcessor implements RequestProcessor, FMMenuCommands {
	public static long TAG = 0L;

	protected static HashMap<String, String> External2InternalString = new HashMap<String, String>() {
		{
			put(FF_SINGLE_RT, FM_FACE_FIND_OP);
			put(INGEST_SINGLE_RT, FM_REGION_INGEST_OP);
			put(QUERY_SINGLE_RT, FM_REGION_QUERY_OP);
		}
	};

	protected static JFrame requestInputFrame = null;

	protected ActionCompletionListener parent;
	HashMap<String, Object> initialParams;
	HashMap<String, Object> selectedParams;

	public RegionRequestProcessor() throws Exception {
		initialParams = getInitialBatchInputParams();
	}

	// *-------------------------------------------------------------------------------------------------------------
	// Create the input Selection panel.
	// Check for parameters specified in the config file
	// and set as initial parameters. Provide defaults if not specified
	// *---------------------------------------------------------------------------------------------------------------
	protected HashMap<String, Object> getInitialBatchInputParams() {
		String fileSystemRoot = Utils.getFileSystemRoot();
		HashMap<String, Object> batchParams = new HashMap();

		Properties configProperties = WBProperties.configProperties;
		String testDataDir = configProperties.getProperty("fm2test.datadir", fileSystemRoot);
		if (testDataDir != null)
			batchParams.put("dataFilename", testDataDir);
		// Select file with BatchRequest data

		// show the checkbox whether or not to store FaceMatch return results

		String resultStoreDir = configProperties.getProperty("fm2test.resultstore.dir", fileSystemRoot);
		if (resultStoreDir != null)
			batchParams.put("resultsDirectory", resultStoreDir);

		HashMap<String, String[]> clientToExtentMap = WBUtils.getClient2ExtentMap(configProperties);
		batchParams.put("clientInfoMap", clientToExtentMap);

		String testType = configProperties.getProperty("operationType", "Face Find"); // default
		batchParams.put("operationType", testType);

		batchParams.put("displayResults", new Boolean(true));
		batchParams.put("storeResults", new Boolean(true));

		return batchParams;
	}

	// ---------------------------------------------------------------------------------------------------------------------------
	// Execute the Menu command selectedby the user
	// invoked by the parent object
	// ---------------------------------------------------------------------------------------------------------------------------
	public void executeCommand(ActionCompletionListener listener, String command, Object commandParam) {
		try {
			parent = listener;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					SingleRequestInputDialog inputSelectionDialog = createSingleRequestDialog();
					inputSelectionDialog.setInitialChoices(command, initialParams);

					WBProperties.mainFrame.setVisible(false);
					requestInputFrame.toFront();
					inputSelectionDialog.start();

					// ---------------------------------------------------------------------------------------------------------------------//
					// We will come here and get the selected params only after
					// the selection window closes
					// ------------------------------------------------------------------------------------------------------------------------//
					HashMap<String, Object> userParams = inputSelectionDialog.getSelectedParams();
					WBProperties.mainFrame.setVisible(true);

					System.out.println("Config properties: " + WBProperties.configProperties);
					processUserInput(command, userParams);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			notifyCompletion(command, -1, null);
		}
	}

	/*--------------------------------------------------------------------------------------------------------------*/
	// build the dialog to receive user input for performing Batch operation
	// requests
	protected SingleRequestInputDialog createSingleRequestDialog() {
		String dialogName = "Single FaceMatch Operation Request";
		// String displayName = " Select parameters for FaceMatch Region
		// Operation Requests in Batch mode";

		requestInputFrame = new JFrame(dialogName);
		requestInputFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // hide
																				// it
																				// for
																				// next
																				// time

		SingleRequestInputDialog InputDialog = new SingleRequestInputDialog(requestInputFrame, initialParams);

		// setFrameCenter(inputSelectionFrame,
		// inputSelectionFrame.getPreferredSize());
		requestInputFrame.pack();
		return InputDialog;
	}

	// *-----------------------------------------------------------------------------------------------------
	// Comes here after the user selects the parameters for the bsatch operation
	//
	protected void processUserInput(String command, HashMap<String, Object> userParams) {
		selectedParams = userParams;
		System.out.println("\n>>> Received  Selected params: " + selectedParams);
		if (selectedParams == null) {
			System.out.println("\n>>> User canceled operation");
			notifyCompletion(command, 0, null);
			return;
		}
		try {
			int status = performRegionOperation(command, selectedParams);
			if (status != 1)// if 1 then notification was inside
							// performRegionOperation
				notifyCompletion(command, status, null);
		} catch (Exception e) {
			DisplayUtils.displayErrorMessage("Error executing batch command " + e.getMessage());
			e.printStackTrace();
		}
	}
	/*--------------------------------------------------------------------------------------------------------------*/

	protected int performRegionOperation(String command, HashMap<String, Object> requestParams) throws Exception {
		String operation = (String) requestParams.get("operation");

		HashMap<String, Object> paramsForService = new HashMap<>();

		switch (operation) {
		case ServiceConstants.GET_FACES:
		case ServiceConstants.FACE_FIND:
			if (setFaceFindParams(requestParams, paramsForService) == 0) {
				System.out.println("Malformed input for: " + operation);
				return 0;
			}
			break;
		case ServiceConstants.INGEST:
			if (setIngestParams(requestParams, paramsForService) == 0) {
				System.out.println("Malformed input for: " + operation);
				return 0;
			}
			break;
		case ServiceConstants.QUERY:
			if (setQueryParams(requestParams, paramsForService) == 0) {
				System.out.println("Malformed input for: " + operation);
				return 0;
			}
			break;
		case ServiceConstants.REMOVE:
			if (setRemoveParams(requestParams, paramsForService) == 0) {
				System.out.println("Malformed input for: " + operation);
				return 0;
			}
			break;
		default:
			System.out.println("operation is invalid: " + operation);
			return 0;
		}

		try {
			System.out.println("Parameteres prepped: " + paramsForService);
			FM2ServiceRequestor requestApp = new FM2ServiceRequestor(WBProperties.fmServerURL);
			FM2ServiceResult result;
			if (operation.equals(ServiceConstants.GET_FACES))
				result = requestApp.requestFaceFinderService(1, paramsForService);
			else
				result = requestApp.requestFaceMatchRegionService(1, paramsForService);

			notifyCompletion(operation, result.statusCode, result.serverReponseContent);
			System.out.println(result.statusMessage);
			return 1;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	private static int setQueryParams(HashMap<String, Object> input, HashMap<String, Object> webServiceParams) {
		String imageExtent = (String) input.get(ServiceConstants.EXTENT);
		String imageURL = (String) input.get(ServiceConstants.URL);
		String imageRegions = (String) input.get(ServiceConstants.REGION);
		Integer maxMatches = (Integer) (input.get(ServiceConstants.MAX_MATCHES));
		Float tolerance = (Float) (input.get(ServiceConstants.TOLERANCE));
		if (imageExtent == null) {
			System.out.println("Missing image extent");
			return 0;
		} else if (imageURL == null) {
			System.out.println("Missing image URL");
			return 0;
		} else if (maxMatches == null) {
			System.out.println("max match count missing");
			return 0;
		} else if (tolerance == null) {
			System.out.println("tolerance missing");
			return 0;
		}
		webServiceParams.put(ServiceConstants.OPERATION, ServiceConstants.QUERY);
		webServiceParams.put(ServiceConstants.CLIENT_KEY,
				WBProperties.configProperties.get(input.get("clientName") + ".key"));
		webServiceParams.put(ServiceConstants.EXTENT, imageExtent);
		webServiceParams.put(ServiceConstants.URL, imageURL);
		webServiceParams.put(ServiceConstants.REGION, imageRegions);
		webServiceParams.put(ServiceConstants.TOLERANCE, tolerance);
		webServiceParams.put(ServiceConstants.MAX_MATCHES, maxMatches);
		return 1;
	}

	private static int setIngestParams(HashMap<String, Object> input, HashMap<String, Object> webServiceParams) {
		String imageExtent = (String) input.get(ServiceConstants.EXTENT);
		String imageURL = (String) input.get(ServiceConstants.URL);
		String imageTag = (String) input.get(ServiceConstants.IMAGE_TAG);
		if (imageExtent == null) {
			System.out.println("Missing image extent");
			return 0;
		} else if (imageURL == null) {
			System.out.println("missing image URL");
			return 0;
		} else if (imageTag == null || imageTag.trim().equals("")) {
			imageTag = Long.toString(TAG++);
			System.out.println("missing image tag, generating tag: " + imageTag);
		}
		webServiceParams.put(ServiceConstants.OPERATION, ServiceConstants.INGEST);
		webServiceParams.put(ServiceConstants.CLIENT_KEY,
				WBProperties.configProperties.get(input.get("clientName") + ".key"));
		webServiceParams.put(ServiceConstants.EXTENT, imageExtent);
		webServiceParams.put(ServiceConstants.URL, imageURL);
		webServiceParams.put(ServiceConstants.IMAGE_TAG, imageTag);
		webServiceParams.put(ServiceConstants.AGE_GROUP, input.get(ServiceConstants.AGE_GROUP));
		webServiceParams.put(ServiceConstants.GENDER, input.get(ServiceConstants.GENDER));
		webServiceParams.put(ServiceConstants.LOCATION, input.get(ServiceConstants.LOCATION));
		return 1;
	}

	private static int setRemoveParams(HashMap<String, Object> input, HashMap<String, Object> webServiceParams) {
		String imageExtent = (String) input.get(ServiceConstants.EXTENT);
		String imageTag = (String) input.get(ServiceConstants.IMAGE_TAG);
		String imageRegions = (String) input.get(ServiceConstants.REGION);
		if (imageExtent == null) {
			System.out.println("image extent missing");
			return 0;
		} else if (imageTag == null || imageTag.trim().equals("")) {
			System.out.println("image tag missing");
			return 0;
		}
		webServiceParams.put(ServiceConstants.OPERATION, ServiceConstants.REMOVE);
		webServiceParams.put(ServiceConstants.CLIENT_KEY,
				WBProperties.configProperties.get(input.get("clientName") + ".key"));
		webServiceParams.put(ServiceConstants.EXTENT, imageExtent.toLowerCase());
		webServiceParams.put(ServiceConstants.IMAGE_TAG, imageTag);
		webServiceParams.put(ServiceConstants.REGION, imageRegions);
		return 1;
	}

	private static int setFaceFindParams(HashMap<String, Object> input, HashMap<String, Object> webServiceParams) {
		String imageURL = (String) input.get(ServiceConstants.URL);
		if (imageURL == null) {
			System.out.println("image url missing");
			return 0;
		}
		System.out.println(WBProperties.configProperties);
		webServiceParams.put(ServiceConstants.OPERATION, ServiceConstants.GET_FACES);
		webServiceParams.put(ServiceConstants.CLIENT_KEY,
				WBProperties.configProperties.get(input.get("clientName") + ".key"));
		webServiceParams.put(ServiceConstants.URL, input.get(ServiceConstants.URL));
		webServiceParams.put(ServiceConstants.REGION, input.get(ServiceConstants.REGION));
		webServiceParams.put(ServiceConstants.LANDMARKS, input.get(ServiceConstants.LANDMARKS));
		webServiceParams.put(ServiceConstants.INFLATE_BY, input.get(ServiceConstants.INFLATE_BY));
		webServiceParams.put(ServiceConstants.PERFORMANCE_PREF, input.get(ServiceConstants.PERFORMANCE_PREF));

		return 1;
	}

	/*----------------------------------------------------------------------------------------- 
	protected class MyWindowAdapter extends WindowAdapter
	{
	    public void windowClosing(WindowEvent e) {
	        System.out.println("Received Window closing event");
	        BatchInputSelectionDialog dialog = (BatchInputSelectionDialog) (e.getWindow());
	       selectedParams  =dialog.getSelectedParams();
	         
	       String[] choices = {"Exit", "Cancel"};
	      int option = fm2client.analyzer.display.DisplayUtils.displayConfirmationMessage("Do you want to exit? ", choices);
	       if (option == 0)
	           dialog.dispose();
	    }
	}*/

	// ---------------------------------------------------------------------------------------------------------------------------
	// Batch input action is completed. Either used "submitted" the choices or
	// wants to quit
	//
	/*
	 * public void actionCompleted(ActionCompletionEvent event) { if
	 * (event.getEventType() == ActionCompletionEvent.BATCH_OPS_SELECTION_EVENT)
	 * { int status = event.getStatus(); if (status == 0) // user wanted to exit
	 * without doing the function { batchInputFrame.setVisible(false); } else if
	 * (status == 1) { performFM2BatchRequests(event.getActionCommand(),
	 * event.getReturnParams()); } notifyCompletion(event.getFunctionType(),
	 * status, null); } }
	 */

	public void notifyCompletion(String function, int status, Object param) {
		parent.actionCompleted(null);
		System.out.println(param);
		System.out.println("SINGLE REQUEST DONE");
		if (param != null && param instanceof String) {
			JSONParser parser = new JSONParser();
			JSONObject ret = (JSONObject) JSONValue.parse((String) param);
			this.selectedParams.get("url");
			ret.put("originalURL", this.selectedParams.get("url"));
			saveOutput(ret, this.selectedParams);
			workbench.display.SingleRequestResult.showResult(ret);
		}

	}

	private static void saveOutput(JSONObject output, Map<String, Object> selectedParams) {
		String output_name = WBProperties.configProperties.getProperty("fm2test.resultstore.dir")+File.separator
				+ selectedParams.get(ServiceConstants.OPERATION) + "" + System.currentTimeMillis()+".json";
		File f = new File(output_name);
		System.out.println("Saveing to: "+output_name);
		if (!f.getParentFile().exists())
			f.getParentFile().mkdirs();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
			bw.write(output.toJSONString());
		}
		// TODO Auto-generated method stub
		catch (IOException e) {
			e.printStackTrace();
		}

	}
}
