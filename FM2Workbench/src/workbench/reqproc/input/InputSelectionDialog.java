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

/** InputSelectionPanel 
 *  Allows FaceMatch2 user to provide inputs to conduct a test/operation or analyze results of a
 * previously performed test results
 */

package workbench.reqproc.input;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;


//import fm2client.app.core.FM2OperationConstants;
import workbench.control.FMMenuCommands;
import workbench.control.WBProperties;
import workbench.display.DisplayUtils;

public class InputSelectionDialog extends JDialog implements FMMenuCommands, ItemListener, ActionListener {
	protected String panelTitle = "";

	// Operation type Radio Buttons and labels - provided by the derived class
	// Note: Must be in same order as in Menu commands
	protected static String[] batchOperationCommands = null;

	// Note: Must be in same order as in Menu commands
	protected static String[] OperationTypeRBTexts = null;

	// GUI components
	// 1.
	protected String[] clientNames;
	protected javax.swing.JLabel clientLabel;
	protected javax.swing.JComboBox clientComboBox;

	// 2.
	protected String[] imageExtentNames;
	protected javax.swing.JLabel imageExtentLabel;
	protected javax.swing.JComboBox imageExtentComboBox;

	// 4.5. Test/result Data file names
	protected static JLabel selectedDataFileLabel;
	protected static JLabel selectedResultDirLabel;

	// OK, Reset and Exitl bottons

	protected javax.swing.JButton submitButton;
	protected javax.swing.JButton cancelButton;
	protected javax.swing.JButton exitButton;

	protected JCheckBox displayResultCheckButton;
	protected JCheckBox storeResultCheckButton;

	protected JButton dataFilenameButton;
	protected JButton resultDirectoryButton;

	protected Color panelBgColor = WBProperties.BlueGrey2;
	protected JButton[] actionButtons = new JButton[3];

	protected JRadioButton[] opsTypeRadioButtons;

	// parameters: testDataFile, resultFileDir

	boolean displayResult = true;
	boolean storeResults = true;

	protected String operation;

	protected HashMap<String, Object> initialParams;
	protected HashMap<String, Object> selectedParams;
	protected HashMap<String, String[]> clientInfoMap; // Clients and Extents

	boolean initialized = false;

	/*--------------------------------------------------------------------------------------------------------*/
	// constructor
	public InputSelectionDialog(JFrame frame, HashMap<String, Object> defaultParams) {
		super(frame);
		setModal(true);
		initialParams = defaultParams;
		selectedParams = new HashMap(); // user selections
	}

	protected void setPanelTitle(String title) {
		panelTitle = title;
	}

	protected void setOperationTypeInfo(String[] commands, String[] buttonText) {
		batchOperationCommands = commands;
		OperationTypeRBTexts = buttonText;
		opsTypeRadioButtons = new JRadioButton[OperationTypeRBTexts.length];
	}

	protected void buildInputSelectionPanel() {
		JPanel inputPanel = buildSelectionPanel(initialParams);
		this.add(inputPanel);
		Dimension d = new Dimension(inputPanel.getPreferredSize().width + 100,
				inputPanel.getPreferredSize().height + 100);
		this.setSize(d);
		this.setLocation(250, 200);
	}

	// Display self to receive user input
	public void start() {
		setVisible(true);
	}

	public HashMap<String, Object> getSelectedParams() {
		System.out.println(selectedParams);
		return selectedParams;

	}

	/************************************************************************
	 * Create an input panel for receiving user selections
	 **************************************************************************/

	protected JPanel buildSelectionPanel(HashMap<String, Object> inputParams) {
		// set border with some spacing
		Border bevelBorder = BorderFactory.createRaisedBevelBorder();
		Border emptyBorder = BorderFactory.createEmptyBorder(20, 20, 20, 20);
		Border spacingBorder = new CompoundBorder(bevelBorder, emptyBorder);
		Border lineBorder = LineBorder.createBlackLineBorder();

		// font for text/messages
		Font labelFont = new Font("Halevetica", Font.PLAIN, 12);

		/*---------------------------------------------------------------------------------------------------------------*/
		// 1. Create a label for the panel at the top
		/*-----------------------------------------------------------------------------------------------------------------*/

		JLabel panelLabel = new JLabel(panelTitle);
		panelLabel.setHorizontalAlignment(SwingConstants.LEFT);
		panelLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 0));
		panelLabel.setFont(new Font("TimesNewRoman", Font.BOLD, 16));
		panelLabel.setForeground(WBProperties.NAVY);
		panelLabel.setOpaque(true);
		panelLabel.setBackground(WBProperties.Grey1);

		/*-------------------------------------------------------------------------------------------------------------------
		// 2, The innerPanel is for all user inputs
		/*-----------------------------------------------------------------------------------------------------------------*/

		// a. Create the client combo box
		clientLabel = new javax.swing.JLabel();
		clientLabel.setFont(new java.awt.Font("MS Sans Serif", 1, 13));
		clientLabel.setText("Select Client:");

		clientComboBox = new javax.swing.JComboBox();
		clientComboBox.setMinimumSize(new java.awt.Dimension(160, 24));

		// load the combo box, and set the default selection
		clientInfoMap = (HashMap<String, String[]>) inputParams.get("clientInfoMap");
		Set<String> clients = clientInfoMap.keySet();
		clientNames = clients.toArray(new String[clients.size()]);

		String selectString = "--Select from List -- ";
		// clientComboBox.addItem(selectString);
		for (int i = 0; i < clients.size(); i++) {
			clientComboBox.addItem(clientNames[i]);
		}
		clientComboBox.setMinimumSize(new java.awt.Dimension(160, 24));
		clientComboBox.setPreferredSize(new java.awt.Dimension(160, 24));
		clientComboBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				clientSelectionActionPerformed(evt);
			}
		});
		JPanel clientPanel = new JPanel();
		clientPanel.setBackground(panelBgColor);
		clientPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
		clientPanel.setLayout(new GridLayout(2, 1));
		clientPanel.add(clientLabel);
		clientPanel.add(clientComboBox);

		/*-------------------------------------------------------------------------------------------------------------------
		// 2, The innerPanel is for all user inputs
		/*-----------------------------------------------------------------------------------------------------------------*/

		// a. Create the imageCollection combo box
		imageExtentLabel = new javax.swing.JLabel();
		imageExtentLabel.setFont(new java.awt.Font("MS Sans Serif", 1, 13));
		imageExtentLabel.setText("Select Image Extent:  ");

		imageExtentComboBox = new javax.swing.JComboBox();
		imageExtentComboBox.setMinimumSize(new java.awt.Dimension(160, 24));

		// load the combo box, and add a place holder, to be updated after the
		// client selection
		imageExtentComboBox.addItem(selectString);

		imageExtentComboBox.setMinimumSize(new java.awt.Dimension(160, 24));
		imageExtentComboBox.setPreferredSize(new java.awt.Dimension(160, 24));
		imageExtentComboBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				imageExtentSelectionActionPerformed(evt);
			}
		});
		JPanel imageExtentPanel = new JPanel();
		imageExtentPanel.setBackground(panelBgColor);
		imageExtentPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
		imageExtentPanel.setLayout(new GridLayout(2, 1));
		imageExtentPanel.add(imageExtentLabel);
		imageExtentPanel.add(imageExtentComboBox);

		JPanel clientNExtentPanel = new JPanel();
		clientNExtentPanel.setBackground(panelBgColor);
		clientNExtentPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
		clientNExtentPanel.setLayout(new GridLayout(1, 2));
		clientNExtentPanel.add(clientPanel);
		clientNExtentPanel.add(imageExtentPanel);

		// --------------------------------------------------------------------------------------------------
		// b. create a RadioButton group to select FM operation types
		// Only one will be selected from user menu command
		// --------------------------------------------------------------------------------------------------
		JPanel opsTypePanel = new JPanel(new GridLayout(0, 1));
		opsTypePanel.setBackground(panelBgColor);

		TitledBorder opsTypeBorder = new TitledBorder("Operation Type");
		opsTypeBorder.setBorder(lineBorder);
		opsTypePanel.setBorder(opsTypeBorder);

		ButtonGroup operationButtonGroup = new ButtonGroup();
		int n = OperationTypeRBTexts.length;
		for (int i = 0; i < n; i++) {
			String text = OperationTypeRBTexts[i];
			JRadioButton radioButton = new JRadioButton(text, false);
			radioButton.setName(text);
			// radioButton.setBackground(Color.WHITE);
			operationButtonGroup.add(radioButton);

			opsTypePanel.add(radioButton);
			opsTypeRadioButtons[i] = radioButton;

			// These radio buttons are not selectable by user on this panel.
			// They become selected/deselectedbase upon user;'s WB menu
			// selection
			/*
			 * opsTypeRadioButtons[i].addActionListener(new ActionListener() {
			 * public void actionPerformed(ActionEvent evt) {
			 * operationSelectionActionPerformed(evt); } });
			 */
		}
		System.out.println("RadioButton panel preferred size" + opsTypePanel.getPreferredSize());
		opsTypePanel.setBackground(WBProperties.BlueGrey1);
		// ---------------------------------------------------------------------------------------------------------------------
		// c. Create two check boxes for display results and/or store result,
		// ---------------------------------------------------------------------------------------------------------------------
		displayResultCheckButton = new JCheckBox("Display  results on screen");
		displayResultCheckButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
		displayResultCheckButton.addItemListener(this);

		storeResultCheckButton = new JCheckBox("Save results on disk");
		storeResultCheckButton.addItemListener(this);

		// add these to the ResultOption panel
		JPanel resultOptionPanel = new JPanel();
		resultOptionPanel.setBackground(panelBgColor);
		resultOptionPanel.setLayout(new GridLayout(0, 1));
		resultOptionPanel.add(displayResultCheckButton);
		resultOptionPanel.add(storeResultCheckButton);

		TitledBorder resultsBorder = new TitledBorder("Results");
		resultsBorder.setBorder(lineBorder);
		resultOptionPanel.setBorder(resultsBorder);
		resultOptionPanel.setBackground(WBProperties.BlueGrey1);

		/*
		 * // Keep these two together horizontally, with some gap JPanel
		 * opsTypePanel1 = new JPanel();
		 * opsTypePanel1.setBorder(BorderFactory.createEmptyBorder(0, 0, 0,
		 * 20)); opsTypePanel1.add(opsTypePanel);
		 */

		JPanel rbPanel = new JPanel();
		rbPanel.setBorder(emptyBorder);
		rbPanel.setLayout(new GridLayout(1, 0));
		rbPanel.add(opsTypePanel);
		rbPanel.add(resultOptionPanel);
		// ---------------------------------------------------------------------------------------------------------------------
		// d. Create panel to get file//diretory names
		// ---------------------------------------------------------------------------------------------------------------------
		// 1. Get Test data file name
		JPanel testFileNamePanel = new JPanel();

		testFileNamePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
		dataFilenameButton = new JButton("Test data  file name...");
		dataFilenameButton.addActionListener(this);
		dataFilenameButton.setAlignmentX(Component.LEFT_ALIGNMENT);

		selectedDataFileLabel = new JLabel("  ");
		selectedDataFileLabel.setFont(labelFont);
		selectedDataFileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		testFileNamePanel.add(dataFilenameButton);
		testFileNamePanel.add(selectedDataFileLabel);
		testFileNamePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		// #2. Get the stored file name
		// # This should be enabled only if store reslts buttn is checked
		JPanel resultDirPanel = new JPanel();
		resultDirPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
		resultDirectoryButton = new JButton("Directory for Stored results...");
		resultDirectoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		resultDirectoryButton.addActionListener(this);

		selectedResultDirLabel = new JLabel("");
		selectedResultDirLabel.setFont(labelFont);
		resultDirPanel.add(resultDirectoryButton);
		resultDirPanel.add(selectedResultDirLabel);

		JPanel filePanel = new JPanel(new GridLayout(0, 1));
		TitledBorder filePanelBorder = new TitledBorder("Data/Result Files");
		filePanelBorder.setBorder(lineBorder);
		filePanel.setBorder(filePanelBorder);
		filePanel.add(testFileNamePanel);
		filePanel.add(resultDirPanel);

		/*----------------------------------------------------------------------------------------------*/
		// Create a new JPanel for the Action buttons
		// finally add the Action buttons for okay or cancel
		/*----------------------------------------------------------------------------------------------*/
		submitButton = new JButton("OK");
		submitButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonActionPerformed(evt);
			}
		});
		cancelButton = new JButton("Reset");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonActionPerformed(evt);
			}
		});
		exitButton = new JButton("EXIT");
		exitButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonActionPerformed(evt);
			}
		});

		JPanel actionPanel = new JPanel();
		actionPanel.setBorder(new BevelBorder(5));
		FlowLayout flow = new FlowLayout();
		flow.setHgap(50);
		actionPanel.setLayout(flow);
		actionPanel.setBackground(panelBgColor);
		actionPanel.add(submitButton);
		actionPanel.add(cancelButton);
		actionPanel.add(exitButton);
		actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		System.out.println("actionPanel   preferred size" + actionPanel.getPreferredSize());

		/*------------------------------------------------------------------------------------------------*/
		// Put all choices in an inner panel as a Vertical Box
		// --------------------------------------------------------------------------------------------------*/
		JPanel vgapPanel = new JPanel(); // for vrtical gap
		vgapPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
		vgapPanel.setBackground(panelBgColor);

		Box verticalBox = Box.createVerticalBox();
		verticalBox.add(clientNExtentPanel);
		verticalBox.add(rbPanel);
		// verticalBox.add(vgapPanel);
		verticalBox.add(filePanel);
		// verticalBox.add(vgapPanel);

		/*---------------------------------------------------------------------------------------------------------------*/
		// Build the full interface panel
		/*---------------------------------------------------------------------------------------------------------------*/
		JPanel selectionPanel = new JPanel();
		selectionPanel.setBackground(panelBgColor);
		selectionPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		selectionPanel.setLayout(new BorderLayout());

		selectionPanel.add(panelLabel, BorderLayout.NORTH);
		selectionPanel.add(verticalBox, BorderLayout.CENTER);
		selectionPanel.add(actionPanel, BorderLayout.SOUTH);

		// setInitialChoices(command, inputParams);
		initialized = true;
		return selectionPanel;
	}

	/*------------------------------------------------------------------------------------*/
	// set the initial choices as indicated in the input parameter map
	/*------------------------------------------------------------------------------------*/
	public void setInitialChoices(String command, HashMap<String, Object> inputParams) {
		clientComboBox.setSelectedIndex(0);
		selectedParams.put("clientName", clientNames[0]);
		operation = command;

		imageExtentComboBox.setSelectedIndex(0);
		// set the radio buttons - Selected according to chosen menu command
		String selectedOpsType = command;

		for (int i = 0; i < OperationTypeRBTexts.length; i++) {
			if (selectedOpsType != null && selectedOpsType.equalsIgnoreCase(OperationTypeRBTexts[i])) {
				opsTypeRadioButtons[i].setSelected(true);
				selectedParams.put("testType", selectedOpsType);
			} else
				opsTypeRadioButtons[i].setEnabled(false);
		}

		// Display results by default
		displayResultCheckButton.setSelected(true);
		// may be a file name or a directory name
		String batchFile = (String) inputParams.get("dataFilename");
		if (batchFile == null)
			batchFile = (String) inputParams.get("testDataDir");
		if (batchFile != null)
			selectedDataFileLabel.setText(batchFile);
		else
			selectedDataFileLabel.setText("Browse...");
		selectedParams.put("dataFilename", batchFile);

		Boolean displayResult = (Boolean) inputParams.get("displayResults");
		boolean displaying = (displayResult != null && displayResult.booleanValue() == true);
		displayResultCheckButton.setSelected(displaying);
		selectedParams.put("displayResults", displayResult);

		Boolean storeResult = (Boolean) inputParams.get("storeResults");
		boolean toStore = (storeResult != null && storeResult.booleanValue() == true);
		storeResultCheckButton.setSelected(toStore);
		selectedParams.put("storeResults", storeResult);

		// enable/disable the Directory Chooser
		resultDirectoryButton.setEnabled(toStore);

		String resultDirName = (String) inputParams.get("resultsDirectory");
		if (resultDirName != null && !resultDirName.isEmpty())
			selectedResultDirLabel.setText(resultDirName);
		else
			selectedResultDirLabel.setText("Browse...");
		selectedParams.put("resultsDirectory", resultDirName);
	}

	/****************************************************************************
	 * Action Listener for indicating that user selected a imageCollection
	 **************************************************************************/
	protected void clientSelectionActionPerformed(ActionEvent evt) {
		String clientName = (String) clientComboBox.getSelectedItem();
		selectedParams.put("clientName", clientName);
		activateExtentSelectionList(clientName);
	}

	/****************************************************************************
	 * Action Listener for indicating that user selected a imageCollection
	 **************************************************************************/
	protected void activateExtentSelectionList(String client) {
		String[] extentNames = clientInfoMap.get(client);
		if (extentNames == null) {
			DisplayUtils.displayErrorMessage(
					"No image extents currently exist for client " + client + ". Please add an extent first.");

			disposeWindow();
			return;
		}
		imageExtentComboBox.removeAllItems();
		for (int i = 0; i < extentNames.length; i++) {
			imageExtentComboBox.addItem(extentNames[i]);
		}
		imageExtentComboBox.setMinimumSize(new java.awt.Dimension(160, 24));
		imageExtentComboBox.setPreferredSize(new java.awt.Dimension(160, 24));
		imageExtentComboBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				imageExtentSelectionActionPerformed(evt);
			}
		});

	}

	/****************************************************************************
	 * Action Listener for indicating that user selected an imageExtent
	 **************************************************************************/
	protected void imageExtentSelectionActionPerformed(ActionEvent evt) {
		int extentIndex = imageExtentComboBox.getSelectedIndex();
		String extentName = (String) imageExtentComboBox.getSelectedItem();
		selectedParams.put("imageExtent", extentName);
	}

	/****************************************************************************
	 * Action Listener for indicating that user provided all options
	 **************************************************************************/
	protected void buttonActionPerformed(ActionEvent evt) {
		// Check the event source
		if (evt.getSource() == submitButton) {
			boolean inputOk = validateSelectedParams(selectedParams);
			if (!inputOk)
				return; // error message already out

			disposeWindow();
			System.out.println(">> Disposed  input selectonwindow from DialogBox");

			/*
			 * ActionCompletionEvent completionEvent = new ActionCompletionEvent
			 * (this, ActionCompletionEvent.BATCH_OPS_SELECTION_EVENT,
			 * operation, 1, selectedParams);
			 * completionListener.actionCompleted(completionEvent);
			 */
		}
		// reinitialize all parameters
		else if (evt.getSource() == cancelButton) {
			setInitialChoices(operation, initialParams);
		} else if (evt.getSource() == exitButton) {
			String[] options = { "Exit", "Cancel" };
			int option = DisplayUtils.displayConfirmationMessage("Exiting  FaceMatch operation request", options);
			if (option == 0) // Exit
			{
				/*
				 * ActionCompletionEvent completionEvent = new
				 * ActionCompletionEvent (this,
				 * ActionCompletionEvent.BATCH_OPS_SELECTION_EVENT, operation,
				 * 0, null);
				 * completionListener.actionCompleted(completionEvent);
				 */
				selectedParams = null;
				disposeWindow();
			} else
				return; // ignore
		}
	}

	/************************************************************************************/
	protected void disposeWindow() {
		WindowEvent closingEvent = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(closingEvent);
		System.out.println(">>> Disposing Selection Window. <<<");
	}

	/************************************************************************************/
	// Check the input parameters provided by the user
	protected boolean validateSelectedParams(HashMap<String, Object> inputParams) {
		// Check image Collection name
		String clientName = (String) inputParams.get("clientName");
		String imageExtent = (String) inputParams.get("imageExtent");

		// check batch File name
		String batchFileName = (String) inputParams.get("dataFilename");
		if (batchFileName == null || batchFileName.isEmpty()) {
			DisplayUtils.displayErrorMessage("Please specify a batch file name.");
			return false;
		} else {
			// make sure that it is a valid file
			File file = new File(batchFileName);
			if (!file.exists() || !file.isFile()) {
				DisplayUtils
						.displayErrorMessage("Please specify a valid file name with Facematch Service request data.");
				return false;
			}
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
		return true;
	}

	/****************************************************************************
	 * Action Listener for selecting FileChoosers
	 **************************************************************************/
	public void actionPerformed(ActionEvent evt) {
		// Handle action.
		if (evt.getSource() == dataFilenameButton) {

			String fileName = DisplayUtils.selectFile(selectedDataFileLabel.getText()); // initial
																						// value);
			// no file selected, user canceled.
			if (fileName == null)
				return;

			File file = new File(fileName);
			if (!file.exists() || !file.isFile()) {
				DisplayUtils.displayErrorMessage("Please select a file with service request data");
				return;
			}

			selectedDataFileLabel.setText(fileName);
			selectedParams.put("dataFilename", fileName);
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

	/****************************************************************************************/
	// When a Checkbox is selected/deselected, it is delivered as
	// ItemStateChangedEvent
	/****************************************************************************************/
	public void itemStateChanged(ItemEvent evt) {
		boolean checked = (evt.getStateChange() == 1);
		if (evt.getSource() == displayResultCheckButton) {
			selectedParams.put("displayResults", checked);
		} else if (evt.getSource() == storeResultCheckButton) {
			selectedParams.put("storeResults", checked);
			if (checked) {
				resultDirectoryButton.setEnabled(true);
				String dir = selectedResultDirLabel.getText(); // user selected
																// value
				if (!dir.equalsIgnoreCase("Browse..."))
					selectedParams.put("resultsDirectory", dir);
			} else {
				resultDirectoryButton.setEnabled(false);
			}
		}
	}

	/*********************************************************/
	///////////////////////////////////////////////////////////////////////////
	// Inner Classes
	// Not used - does not work well
	/////////////////////////////////////////////////////////////////////////////

	// private class to detect mouse clicks (to handle double clicks.
	// Note: if double click is to be processed, then ListSelectionListener
	// should not be used. Otherwise, that is processed first and not double
	// clicks

	private class MyMouseAdapter extends MouseAdapter {
		JTable table;
		boolean doubleClicked = false;

		public MyMouseAdapter(JTable table) {
			this.table = table;
		}

		public void mouseClicked(java.awt.event.MouseEvent e) {
			if (e.getClickCount() == 2)
				doubleClicked = true;
			else
				doubleClicked = false;

			System.out.println(table.getSelectedRow() + "doubleClicked = " + doubleClicked);
			// valueChanged(table, doubleClicked); // TBD
		}

		public boolean isMouseDoubleClicked() {
			return doubleClicked;
		}
	} // end of MyMouseAdapter

	////////////////////////////////////////////////////////////////////////

}
