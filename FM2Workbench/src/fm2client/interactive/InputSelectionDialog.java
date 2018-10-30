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

/** 
 * InputSelectionDialog.java
 *  Allows FaceMatch2 user to provide inputs to conduct a test operation ,or analyze 
 * previously performed test results
 */

package fm2client.interactive;


import fm2client.display.DisplayUtils;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JRadioButton;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import javax.swing.JFrame;
import javax.swing.JDialog;

import java.util.HashMap;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

public class InputSelectionDialog extends JDialog 
                                  implements ItemListener, ActionListener       
{

    // GUI components
    //1.
    private javax.swing.JLabel clientLabel;
    private javax.swing.JLabel imageCollectionLabel;
    private javax.swing.JComboBox imageCollectionComboBox;
    
    //2. TestMode RadioButtons
    private static String[] OpsModeRBTexts = {"Real Time", "Stored Results"};
    private JRadioButton[] opsModeRadioButtons;     

    //3. Operation type Radio Buttns.
     private static String[] OpsTypeRBTexts = {"Face Find", "Region Ingest", "Region Query"};
     private JRadioButton[] opsTypeRadioButtons;    

    
    //4.5.  Test/result Data file names
     private static JLabel  selectedDataFileLabel;
     private static JLabel  selectedResultDirLabel;
    
    // OK, Reset and Exitl bottons
    
    private javax.swing.JButton submitButton;
    private javax.swing.JButton cancelButton; 
     private javax.swing.JButton exitButton; 
    
    private JButton testDataFileButton;
    private JButton resultDirectoryButton;

 
    private static Color panelBgColor = Color.lightGray;             // default color
    private static Color  fillColor = Color.WHITE;
    private static Color NAVY = new Color(0x0, 0x20, 0x60);
    
    protected HashMap<String, String>initialParams;
    protected HashMap<String, String>selectedParams;
     
     /*--------------------------------------------------------------------------------------------------------*/
     // constructor
    public InputSelectionDialog( JFrame frame, HashMap<String, String>defaultParams)
    {
        super(frame);
        setModal(true);
        initialParams = defaultParams;
        selectedParams = new HashMap();             // user selections
       // selectedParams.putAll(initialParams);

        JPanel  ip = buildSelectionPanel(initialParams);
        this.add(ip); 
        int width = getPreferredSize().width+100;
        int height = getPreferredSize().height+100;
        this.setSize(new Dimension(width, height));
        System.out.println("Input Dialog size: " + getSize());
    }
    
    // Display selfto receive user input
    public void start()
    {
        setVisible(true);
    }

    /************************************************************************
     * Create an input panel for receiving user selections
     **************************************************************************/
    
    protected  JPanel  buildSelectionPanel( HashMap<String, String> inputParams)
    { 
        Border lineBorder = LineBorder.createBlackLineBorder();
        Border bevelBorder = BorderFactory.createRaisedBevelBorder();
        Border emptyBorder = BorderFactory.createEmptyBorder(10, 20, 10, 10);
      
        Border bevelLineBorder = new CompoundBorder(bevelBorder, lineBorder);
        Border emptyLineBorder = new CompoundBorder(emptyBorder, lineBorder);
      
        
       String clientName  = inputParams.get("client");
       clientLabel = new javax.swing.JLabel();
       clientLabel.setFont(new java.awt.Font("MS Sans Serif", 1, 13));
       clientLabel.setText("Client: " +   clientName +"        ");
        
       imageCollectionLabel = new javax.swing.JLabel();
       imageCollectionLabel.setFont(new java.awt.Font("MS Sans Serif", 1, 13));
       imageCollectionLabel.setText("Image Collection:  ");
       
       //1. Create the imageCollection combo box
       imageCollectionComboBox = new javax.swing.JComboBox();
       imageCollectionComboBox.setMinimumSize(new java.awt.Dimension(160, 24));

       // load the combo box, and set the default selection
       String imageCollectionList = inputParams.get("imageCollectionList");
       String[]  imageCollectionNames = {""};
       if (imageCollectionList != null && ! imageCollectionList.trim().isEmpty())
           imageCollectionNames = imageCollectionList.split("\\W*,\\W*");       // remove whitespace
       
        for(int i = 0; i < imageCollectionNames.length; i++)
        {
               imageCollectionComboBox.addItem(imageCollectionNames[i]);
         }
        imageCollectionComboBox.setMinimumSize(new java.awt.Dimension(160, 24));
        imageCollectionComboBox.setPreferredSize(new java.awt.Dimension(160, 24));
        imageCollectionComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageCollectionSelectionActionPerformed(evt);
            }
        });
        JPanel imageCollectionPanel  = new JPanel();
        imageCollectionPanel.setBorder( BorderFactory.createEmptyBorder(10, 0, 20, 0));
        imageCollectionPanel.add(clientLabel);
        imageCollectionPanel.add(imageCollectionLabel);
        imageCollectionPanel.add(imageCollectionComboBox);
    
        //--------------------------------------------------------------------------------------------------
        // 2. create a RadioButton group to select operation type          
        //--------------------------------------------------------------------------------------------------
        JPanel opsModePanel = new JPanel(new GridLayout(0, 1));
       // opsModePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));
  
        opsModePanel.setBackground(fillColor);
       TitledBorder opsModeBorder = new TitledBorder("Operation Mode");
        opsModeBorder.setBorder(lineBorder);
        opsModePanel.setBorder(opsModeBorder);
        ButtonGroup opsModeButtonGroup = new ButtonGroup();
       
        int n = OpsModeRBTexts.length;
       opsModeRadioButtons = new JRadioButton[n];
        for (int i = 0; i < n; i++)
        {  
            String text = OpsModeRBTexts[i];
            JRadioButton radioButton = new JRadioButton(text,  false);
            radioButton.setName(text);
            radioButton.setBackground(fillColor);
            radioButton.addItemListener(this);
            opsModeButtonGroup.add(radioButton);
            
           opsModePanel.add(radioButton);
           opsModeRadioButtons[i] = radioButton;
           opsModeRadioButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    opsModeSelectionActionPerformed(evt); }
            });
        } 
        //--------------------------------------------------------------------------------------------------
       // 3. create a RadioButton group to select FaceMatch operation type    
        //--------------------------------------------------------------------------------------------------
       JPanel opsTypePanel = new JPanel(new GridLayout(0, 1));
       TitledBorder opsTypeBorder = new TitledBorder("Operation Type");
       opsTypeBorder.setBorder(lineBorder);
       opsTypePanel.setBorder(opsTypeBorder);
      // opsTypePanel.setBorder(emptyLineBorder);
       opsTypePanel.setBackground(fillColor);
      // JLabel opsTypeLabel = new JLabel("Operation Type");
      // opsTypePanel.add(opsTypeLabel);
        
       ButtonGroup opsTypeButtonGroup = new ButtonGroup();
        int n1 = OpsTypeRBTexts.length;
        opsTypeRadioButtons = new JRadioButton[n1];
        for (int i = 0; i < n1; i++)
        {  
            String text = OpsTypeRBTexts[i];
            JRadioButton radioButton = new JRadioButton(text,  false);
            radioButton.setName(text);
            radioButton.setBackground(fillColor);
            radioButton.addItemListener(this);
            opsTypeButtonGroup.add(radioButton);
            
           opsTypePanel.add(radioButton);
          opsTypeRadioButtons[i] = radioButton;
           opsTypeRadioButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                   opsTypeSelectionActionPerformed(evt); }
            });
        } 
        
        JPanel  rbPanel = new JPanel();
        rbPanel.setLayout(new GridLayout(1, 0));
        rbPanel.add(opsModePanel);
        rbPanel.add(opsTypePanel);
        
         Font labelFont = new Font("Halvetica", Font.PLAIN, 12);
         
      //  #4.  Get Test data file name
        JPanel testFileNamePanel = new JPanel();
        //testFileNamePanel.setLayout(new BoxLayout(testFileNamePanel, BoxLayout.LINE_AXIS));
        testFileNamePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
        testDataFileButton = new JButton("Test  file name..."); 
        testDataFileButton.addActionListener(this);
        testDataFileButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        selectedDataFileLabel = new JLabel("  ");
        selectedDataFileLabel.setFont(labelFont);
        selectedDataFileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        testFileNamePanel.add(testDataFileButton);
        testFileNamePanel.add(selectedDataFileLabel);
        testFileNamePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
  
        
     // #5. Get the  stored file name
        JPanel resultDirPanel = new JPanel();
        resultDirPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
         resultDirectoryButton = new JButton("Result store directory ..."); 
         resultDirectoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
         resultDirectoryButton .addActionListener(this);
         
         selectedResultDirLabel = new JLabel("");
         selectedResultDirLabel.setFont(labelFont);
         resultDirPanel.add(resultDirectoryButton);
         resultDirPanel.add(selectedResultDirLabel);
         
         JPanel filePanel = new JPanel(new GridLayout(0, 1));
        TitledBorder filePanelBorder = new TitledBorder("Test Data Files");
         filePanelBorder.setBorder(lineBorder);
         filePanel.setBorder(filePanelBorder);
         filePanel.add(testFileNamePanel);
         filePanel.add(resultDirPanel);

        /*----------------------------------------------------------------------------------------------*/
        // Create a new JPanel for the Action buttons
        // finally add the Action buttons for  okay or cancel
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
        actionPanel.setBorder(bevelBorder);
        FlowLayout flow = new FlowLayout();
        flow.setHgap(50);
        actionPanel.setLayout(flow);
        actionPanel.add(submitButton);
        actionPanel.add(cancelButton);
        actionPanel.add(exitButton);
        actionPanel.setBackground(panelBgColor);
        
        
        /*---------------------------------------------------------------------------------------------------------------*/
        // Put all choices in a Box with vertical alignment
        // TBD: How to align componts to LEFT? -- Standard trchniques do not work ---
        //
        Box verticalBox =  Box.createVerticalBox(); 
        verticalBox.add(imageCollectionPanel);
        verticalBox.add(rbPanel);
        verticalBox.add(filePanel);
        
                
       /*---------------------------------------------------------------------------------------------------------------*/
        // Add a label to the panel
        /*---------------------------------------------------------------------------------------------------------------*/
        JLabel panelLabel = new JLabel("Choose options to perform FaceMatch2 operations:");
        panelLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panelLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 0));
        panelLabel.setFont(new Font("TimesNewRoman", Font.BOLD, 16) );
        panelLabel.setForeground(NAVY);
        panelLabel.setOpaque(true);
        panelLabel.setBackground(Color.lightGray);
        
        JPanel selectionPanel = new JPanel();
        selectionPanel.setBackground(panelBgColor);
        selectionPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        selectionPanel.setLayout(new BorderLayout()); 
        
       selectionPanel.add(panelLabel, BorderLayout.NORTH);
       selectionPanel.add(verticalBox,  BorderLayout.CENTER);
       selectionPanel.add(actionPanel, BorderLayout.SOUTH);  

       setInitialChoices(inputParams);
       return selectionPanel;
    }
    
    /*------------------------------------------------------------------------------------*/
    // set the initial choices as indicated in the input parameter map
    /*------------------------------------------------------------------------------------*/
    protected void setInitialChoices( HashMap<String, String> inputParams)
    {  
        String selectedClient = inputParams.get("client");      // always the same
        selectedParams.put("client",  selectedClient); 
        String selectedCollection = inputParams.get("selectedCollection");
        String imageCollectionList = inputParams.get("imageCollectionList");
        String[] imageCollectionNames = imageCollectionList.split(",\\s*");
        for(int i = 0; i < imageCollectionNames.length; i++)
        {
            if (selectedCollection != null && selectedCollection.equalsIgnoreCase(imageCollectionNames[i]))
            {
                 imageCollectionComboBox.setSelectedIndex(i);
                 selectedParams.put("imageCollection", imageCollectionNames[i]);
                 break;
            }
         }
        
        String selectedOpsMode = inputParams.get("operationMode");
        for (int i = 0; i < OpsModeRBTexts.length; i++)
        {  
               if  (selectedOpsMode != null && selectedOpsMode.equalsIgnoreCase(OpsModeRBTexts[i]))
               {
                    opsModeRadioButtons[i].setSelected(true);
                      selectedParams.put("operationMode", OpsModeRBTexts[i]);
                     break;
               }
        }
        
        String selectedOpsType = inputParams.get("operationType");
        for (int i = 0; i < OpsTypeRBTexts.length; i++)
        {  
               if  (selectedOpsType != null && selectedOpsType.equalsIgnoreCase(OpsTypeRBTexts[i]))
               {
                     opsTypeRadioButtons[i].setSelected(true);
                     selectedParams.put("operationType", OpsTypeRBTexts[i]);
                     break;
               }
        }
        String testFileName = inputParams.get("testDataFile");
        selectedDataFileLabel.setText(testFileName);
        selectedParams.put("testDataFile", testFileName); 
        
        String resultDirName = inputParams.get("resultStoreDir");
        selectedResultDirLabel.setText(resultDirName);
        selectedParams.put("resultStoreDir", resultDirName);
    }

    public HashMap<String, String> getSelectedParams()
    {
        return selectedParams;
    }
   /****************************************************************************
    * Action Listener for indicating that  user selected a imageCollection
    **************************************************************************/
    private void imageCollectionSelectionActionPerformed(ActionEvent evt) 
    {
        int index = imageCollectionComboBox.getSelectedIndex();
        String collectionName =  (String) imageCollectionComboBox.getItemAt(index);
        selectedParams.put("imageCollection",  collectionName);
    }          
        
  /****************************************************************************
    * Action Listener for indicating that  user selected an operation mode
    **************************************************************************/
    private void opsModeSelectionActionPerformed(ActionEvent evt) 
    {
         JRadioButton currentSelection = (JRadioButton)(evt.getSource());    
         selectedParams.put("operationMode", currentSelection.getActionCommand());
    }

      /****************************************************************************
    *  Action Listener for indicating that  user selected an operation type
    **************************************************************************/
    private void opsTypeSelectionActionPerformed(ActionEvent evt) 
    {
         JRadioButton currentSelection = (JRadioButton)(evt.getSource());
          selectedParams.put("operationType", currentSelection.getActionCommand());
    }
    
    /****************************************************************************
    * Action Listener for indicating that  user  provided all options
    **************************************************************************/
    private void buttonActionPerformed(ActionEvent evt) 
    {
        // Check the event source
           if (evt.getSource() == submitButton)
           {
                dispose();
                return;
           }
           // reinitialize all parameters
           else if (evt.getSource() == cancelButton)
           {
               selectedParams.putAll(initialParams);
               setInitialChoices(selectedParams);
           } 
            else if (evt.getSource() == exitButton)
           {
               String[] options = {"OK", "Cancel"};
               int option = DisplayUtils.displayConfirmationMessage("Exiting the application",  options);
               if (option == 0)
               {
                    selectedParams = null;         // user wants to exit
                    dispose();
               }
               else
                   return;              // ignore
           } 
    }

    
   /****************************************************************************
    * Action Listener for choosing files
    **************************************************************************/
    public void actionPerformed(ActionEvent evt) 
    {
      //Handle  action.
        if (evt.getSource() == testDataFileButton)
        {
            String fileName = DisplayUtils.selectFile(selectedDataFileLabel.getText());  // initial value);
            selectedDataFileLabel.setText(fileName);
            selectedParams.put("testDataFile",  fileName);
        }
        
        // We only ask for the directory here because the result file name is derived from the test file
        else if  (evt.getSource() == this.resultDirectoryButton)
        {
            String resultDirName = DisplayUtils.selectFile(selectedResultDirLabel.getText());  /// initial value
            selectedResultDirLabel.setText(resultDirName);      // user selected value
            selectedParams.put("resultStoreDir",  resultDirName);
        }
    }

    /*********************************************************/
    // A no-op method
     public void itemStateChanged(ItemEvent e)
    {
       ;
    }
/*********************************************************/
}
