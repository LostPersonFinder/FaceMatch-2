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

/** DBInfoRequestSelectionDialog
 * 
 *  Allows FaceMatch2 user to provide inputs to request for database information related
 * to their client and ingested images.
 */

package workbench.reqproc.input;

    
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import fm2client.util.ImageTagIO;
import fmservice.httputils.common.FormatUtils;
import workbench.control.WBProperties;
import workbench.display.DisplayUtils;


public class ReportRequestDialog extends InputSelectionDialog
{

    // Note: Must be in same order as in Menu commands 
     private static String[] reportReqCommands = {
            REPORT_CLIENT, REPORT_EXTENT,  REPORT_IMAGE 
     };
    
    private static String[] OperationTypeRBTexts = reportReqCommands;
    private static String myPanelTitle =  "Select options to retrieve Database Reports";
    
  // GUI components
    //1.  
  protected  String[] clientNames;
  protected javax.swing.JLabel clientLabel;
  protected javax.swing.JComboBox clientComboBox;
  
 //2.
  protected  String[] imageExtentNames;
  protected javax.swing.JLabel imageExtentLabel;
  protected javax.swing.JComboBox imageExtentComboBox;
   
  // OK, Reset and Exitl bottons  
  protected javax.swing.JButton submitButton;
  protected javax.swing.JButton cancelButton; 
  protected javax.swing.JButton exitButton; 
    
    boolean showTags = false;
    boolean showUri = false;   
    String[]  imageTags;
    String[] imageURIs;
    
    JTextField imageNameField;          // Field to type in image ID
    JList imageTagList;                          // JList with all image tags or URIs
    JList imageURIList;                          // JList with all image tags or URIs

    boolean newExtentSelected;
     
     protected static String[]  imageButtonTexts = 
            {"Enter Image Tag/URI", "Select Image Tag(s)",  "Select Image URI(s)"};

     /*--------------------------------------------------------------------------------------------------------*/
     // constructor
    public ReportRequestDialog( JFrame frame, HashMap<String, Object>defaultParams)
    {
        super(frame, defaultParams);
        setPanelTitle(myPanelTitle);
       // setOperationTypeInfo();
        opsTypeRadioButtons = 
           new JRadioButton[OperationTypeRBTexts.length];
         buildInputSelectionPanel();
    }

    /*------------------------------------------------------------------------------------*/
    // set the initial choices as indicated in the input parameter map
    /*------------------------------------------------------------------------------------*/
    public  void setInitialChoices( String command, HashMap<String, Object> inputParams)
    {  
        super.setInitialChoices(command,  inputParams);
    }
    ////////////////////////////////////////////////////////////////////////
  
    protected void buildInputSelectionPanel()
    {
        JPanel inputPanel =  buildSelectionPanel(initialParams);
        this.add(inputPanel);
        Dimension d = new Dimension(inputPanel.getPreferredSize().width+100, 
            inputPanel.getPreferredSize().height+100);
       this.setSize(d);
       this.setLocation(250, 200);
    }
    
        
    // Display self to receive user input
    public void start()
    {
        setVisible(true);
    }

    
    public HashMap<String, Object> getSelectedParams()
    {
         System.out.println(selectedParams);
        return selectedParams;
    }
    /************************************************************************
     * Create an input panel for receiving user selections
     **************************************************************************/
    
    protected  JPanel  buildSelectionPanel( HashMap<String, Object> inputParams)
    {
           // set border with some spacing
        Border bevelBorder = BorderFactory.createRaisedBevelBorder();
        Border emptyBorder  = BorderFactory.createEmptyBorder(20, 20, 20, 20);
        Border spacingBorder = new CompoundBorder(bevelBorder, emptyBorder);
        Border lineBorder = LineBorder.createBlackLineBorder();

        
       // font for  text/messages          
        Font labelFont = new Font("Halevetica", Font.PLAIN, 12);
        
        /*---------------------------------------------------------------------------------------------------------------*/
        // 1. Create  a label for  the panel at the top
        /*-----------------------------------------------------------------------------------------------------------------*/
    
        JLabel panelLabel = new JLabel(panelTitle);
        panelLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panelLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 0));
        panelLabel.setFont(new Font("TimesNewRoman", Font.BOLD, 16) );
        panelLabel.setForeground(WBProperties.NAVY);
        panelLabel.setOpaque(true);
        panelLabel.setBackground(WBProperties.Grey1);
        
        
        //--------------------------------------------------------------------------------------------------
        // a. create a RadioButton group to select FM operation types     
        // Only one will be selected from user menu command
        // These radio buttons are not selectable by user on this panel.
        //They become selected/deselectedbase upon user;'s WB menu selection
        //-------------------------------------------------------------------------------------------------- 
       JPanel opsTypePanel = new JPanel(new GridLayout(0, 1));
       opsTypePanel.setBackground(panelBgColor);
       
        TitledBorder opsTypeBorder = new TitledBorder("Operation Type");
        opsTypeBorder.setBorder(lineBorder);
        opsTypePanel.setBorder(opsTypeBorder);
        
       ButtonGroup operationButtonGroup = new ButtonGroup();
        int n  = OperationTypeRBTexts.length;
        for (int i = 0; i < n; i++)
        {  
            String text = OperationTypeRBTexts[i];
            JRadioButton radioButton = new JRadioButton(text,  false);
            radioButton.setName(text);
           // radioButton.setBackground(Color.WHITE);
            operationButtonGroup.add(radioButton);
            
           opsTypePanel.add(radioButton);
           opsTypeRadioButtons[i] = radioButton;
           
           // These radio buttons are not selectable by user on this panel.
            //They become selected/deselectedbase upon user;'s WB menu selection
           /*opsTypeRadioButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    operationSelectionActionPerformed(evt); }
            });*/
        } 
        System.out.println("RadioButton panel preferred size" +opsTypePanel.getPreferredSize());
        opsTypePanel.setBackground(WBProperties.BlueGrey1);

        
        /*-------------------------------------------------------------------------------------------------------------------
        // 2, The innerPanel is for all user inputs
        /*-----------------------------------------------------------------------------------------------------------------*/
        
        //a. Create the client combo box
        clientLabel = new javax.swing.JLabel();
        clientLabel.setFont(new java.awt.Font("MS Sans Serif", 1, 13));
        clientLabel.setText("Select Client:");

        clientComboBox = new javax.swing.JComboBox();
        clientComboBox.setMinimumSize(new java.awt.Dimension(160, 24));

        // load the combo box, and set the default selection
        clientInfoMap = (HashMap<String, String[]>)inputParams.get("clientInfoMap");
        Set<String> clients = clientInfoMap.keySet();
        clientNames = clients.toArray(new String[clients.size()]);

       // clientComboBox.addItem(selectString);
        for(int i = 0; i < clients.size(); i++)
        {
              clientComboBox.addItem(clientNames[i]);
         }
       clientComboBox.setMinimumSize(new java.awt.Dimension(160, 24));
       clientComboBox.setPreferredSize(new java.awt.Dimension(160, 24));
       clientComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               clientSelectionActionPerformed(evt);
            }
        });
        JPanel clientPanel  = new JPanel();
       clientPanel.setBackground(panelBgColor);
       clientPanel.setBorder( BorderFactory.createEmptyBorder(10, 20, 20, 20));
       clientPanel.setLayout(new GridLayout(2, 1));
       clientPanel.add(clientLabel);
       clientPanel.add(clientComboBox);
     
      /*-------------------------------------------------------------------------------------------------------------------
        // 2, The innerPanel is for all user inputs
        /*-----------------------------------------------------------------------------------------------------------------*/
        
       //a. Create the imageCollection combo box
      imageExtentLabel = new javax.swing.JLabel();
      imageExtentLabel.setFont(new java.awt.Font("MS Sans Serif", 1, 13));
      imageExtentLabel.setText("Select Image Extent: ");

      imageExtentComboBox = new javax.swing.JComboBox();
      imageExtentComboBox.setMinimumSize(new java.awt.Dimension(160, 24));

       // load the combo box, and add a place holder, to be updated after the client selection       
       // imageExtentComboBox.addItem(selectString);
              
       imageExtentComboBox.setMinimumSize(new java.awt.Dimension(160, 24));
       imageExtentComboBox.setPreferredSize(new java.awt.Dimension(160, 24));
       imageExtentComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageExtentSelectionActionPerformed(evt);
            }
        });
        JPanel imageExtentPanel  = new JPanel();
        imageExtentPanel.setBackground(panelBgColor);
        imageExtentPanel.setBorder( BorderFactory.createEmptyBorder(10, 20, 20, 20));
        imageExtentPanel.setLayout(new GridLayout(2, 1));
        imageExtentPanel.add(imageExtentLabel);
        imageExtentPanel.add(imageExtentComboBox);
        
        JPanel clientNExtentPanel  = new JPanel();
        clientNExtentPanel.setBackground(panelBgColor);
         clientNExtentPanel.setBorder( BorderFactory.createEmptyBorder(10, 0, 20, 0));
        clientNExtentPanel.setLayout(new GridLayout(1, 2));
        clientNExtentPanel.add(clientPanel);
        clientNExtentPanel.add(imageExtentPanel);

        //--------------------------------------------------------------------------------------------------
        // b. create a radio button option to enter one or more  image tags for the extent   
        // Only one will be selected by the user
        //-------------------------------------------------------------------------------------------------- 
       JPanel imageSelectionPanel = new JPanel(new GridLayout(0, 1));
       imageSelectionPanel.setBackground(panelBgColor);
       
        TitledBorder imageTagBorder = new TitledBorder("Specify  image(s)");
        imageTagBorder.setBorder(lineBorder);
        imageTagBorder.setBorder(imageTagBorder);
        
        ButtonGroup imageButtonGroup = new ButtonGroup();
        String[] ImageSelectionRBTexts = {"Enter Image Tag/URI", "Select Image Tag(s)",  "Select Image URI(s)"};
       
        int ns  = ImageSelectionRBTexts.length;
        for (int i = 0; i < ns; i++)
        {  
            String text = ImageSelectionRBTexts[i];
            JRadioButton radioButton = new JRadioButton(text,  false);
            radioButton.setName(text);
           // radioButton.setBackground(Color.WHITE);
           imageButtonGroup.add(radioButton);
           imageSelectionPanel.add(radioButton);

           radioButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {                       
                   imageInputTypeActionPerformed(evt); }
            });
        } 
        System.out.println("RadioButton panel preferred size" +opsTypePanel.getPreferredSize());
        opsTypePanel.setBackground(WBProperties.BlueGrey1);
 
 
        JPanel  rbPanel = new JPanel();
        rbPanel.setBorder(emptyBorder);
        rbPanel.setLayout(new GridLayout(1, 0));
        rbPanel.add(opsTypePanel);
      
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
        // Put all choices in an inner panel as  a Vertical Box
        //--------------------------------------------------------------------------------------------------*/
        JPanel vgapPanel = new JPanel();            // for vrtical gap
        vgapPanel.setBorder(BorderFactory.createEmptyBorder(20, 0,20,0));
        vgapPanel.setBackground(panelBgColor);
        
        Box verticalBox =  Box.createVerticalBox(); 
        verticalBox.add(clientNExtentPanel);
        verticalBox.add(rbPanel);
        //verticalBox.add(vgapPanel);

       // verticalBox.add(vgapPanel);
             
       /*---------------------------------------------------------------------------------------------------------------*/
        // Build the full interface panel
        /*---------------------------------------------------------------------------------------------------------------*/
        JPanel selectionPanel = new JPanel();
        selectionPanel.setBackground(panelBgColor);
        selectionPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        selectionPanel.setLayout(new BorderLayout()); 
        
       selectionPanel.add(panelLabel, BorderLayout.NORTH);
       selectionPanel.add(verticalBox,  BorderLayout.CENTER);
       selectionPanel.add(actionPanel, BorderLayout.SOUTH);  

      // setInitialChoices(command, inputParams);
            
       
        initImageListSelection();
       initialized = true;
       return selectionPanel;
    }
  /*---------------------------------------------------------------------------------------------------------------*/  
    protected void initImageListSelection()
    {
            // to be populated later
       imageTagList =  new JList();
       imageURIList = new JList();

       imageTagList.setFont(new Font("Halvetica", Font.PLAIN, 12));
               // currently  we allow only single selection
        imageTagList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        JScrollPane tagScrollPane = new JScrollPane(imageTagList);
        tagScrollPane.setPreferredSize(new Dimension(300, 200)); 
        JDialog listDialog1 = new JDialog();
        listDialog1.add(tagScrollPane);
        imageTagList.addListSelectionListener(new ImageSelectionListener(listDialog1));
        
       // Create a popup JDialog with a selectable source file list
       Point loc =  new Point(400, 400);   // selectSourceButton.getLocationOnScreen();
        listDialog1.setLocation(new Point(loc.x, loc.y - 200));  
        listDialog1.setVisible(false);
       
        imageURIList.setFont(new Font("Halvetica", Font.PLAIN, 12));
        imageURIList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        JScrollPane uriScrollPane = new JScrollPane(imageURIList);
        uriScrollPane.setPreferredSize(new Dimension(300, 200)); 
        JDialog listDialog2 = new JDialog();
        listDialog2.add(uriScrollPane);
        imageTagList.addListSelectionListener(new ImageSelectionListener(listDialog2));
        listDialog2.setLocation(new Point(loc.x, loc.y - 200));  
        listDialog2.setVisible(false);
    }
        
    /*-----------------------------------------------------------------------------------------------*/
    // set the initial choices as indicated in the input parameter map
    /*------------------------------------------------------------------------------------------------
    public  void setInitialChoices( String command, HashMap<String, Object> inputParams)
    {  
       clientComboBox.setSelectedIndex(0);
       selectedParams.put("clientName", clientNames[0]);
        operation  = command;
         
        imageExtentComboBox.setSelectedIndex(0);
       // set the radio buttons - Selected according to chosen menu command
        String selectedOpsType = command;
        
        for (int i = 0; i < OperationTypeRBTexts.length; i++)
        {  
               if  (selectedOpsType != null && selectedOpsType.equalsIgnoreCase(OperationTypeRBTexts[i]))
               {
                    opsTypeRadioButtons[i].setSelected(true);
                     selectedParams.put("testType", selectedOpsType);
               }       
               else
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
            selectedDataFileLabel.setText( "Browse...");
        selectedParams.put("dataFilename", batchFile);
        
        Boolean displayResult = (Boolean) inputParams.get("displayResults");
        boolean displaying = (displayResult  != null && displayResult.booleanValue() == true);
        displayResultCheckButton.setSelected(displaying);
        selectedParams.put("displayResults", displayResult);
        
        Boolean storeResult = (Boolean) inputParams.get("storeResults");
        boolean toStore  = (storeResult  != null && storeResult.booleanValue() == true);
        storeResultCheckButton.setSelected(toStore);
        selectedParams.put("storeResults", storeResult);
        
        // enable/disable the Directory Chooser
        resultDirectoryButton.setEnabled(toStore);
        
        String resultDirName = (String) inputParams.get("resultsDirectory");
       if (resultDirName != null && !resultDirName.isEmpty())
            selectedResultDirLabel.setText(resultDirName);
       else 
           selectedResultDirLabel.setText("Browse...");
       selectedParams.put("resultsDirectory",  resultDirName);
    }
    
    
   /****************************************************************************
    * Action Listener for indicating that  user selected a imageCollection
    **************************************************************************/
  protected void clientSelectionActionPerformed(ActionEvent evt) 
    {
        String clientName = (String)clientComboBox.getSelectedItem();
        selectedParams.put("clientName",  clientName);
        activateExtentSelectionList(clientName);
    }          
   
     /****************************************************************************
    * Action Listener for indicating that  user selected a imageCollection
    **************************************************************************/
  protected void activateExtentSelectionList(String client)
   {
       String[] extentNames =  clientInfoMap.get(client);
       if (extentNames == null)
       {
           DisplayUtils.displayErrorMessage("No image extents currently exist for client " + client+
               ". Please add an extent first.");
                           
           disposeWindow();
           return;
       }
       imageExtentComboBox.removeAllItems();
        for(int i = 0; i <extentNames.length; i++)
        {
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
    * Action Listener for indicating that  user selected an imageExtent
    **************************************************************************/
  protected void imageExtentSelectionActionPerformed(ActionEvent evt) 
    {
        String extentName = (String) imageExtentComboBox.getSelectedItem();
        selectedParams.put("imageExtent",  extentName);
        newExtentSelected = true;
       
        // now update the imageList for user selection
        updateImageList();
    }          
        

    /****************************************************************************
    * Action Listener for indicating that  user  provided all options
    **************************************************************************/
  protected void buttonActionPerformed(ActionEvent evt) 
    {
        // Check the event source
           if (evt.getSource() == submitButton)
           {
                boolean inputOk = validateSelectedParams(selectedParams);
                if (!inputOk)
                    return;         // error message already out
                
                disposeWindow();
                System.out.println(">> Disposed  input selectonwindow from DialogBox" );
                
               /*  ActionCompletionEvent completionEvent =  new ActionCompletionEvent
                        (this, ActionCompletionEvent.BATCH_OPS_SELECTION_EVENT,
                            operation, 1, selectedParams); 
                completionListener.actionCompleted(completionEvent);*/
           }
           // reinitialize all parameters
           else if (evt.getSource() == cancelButton)
           {
               setInitialChoices(operation, initialParams);
           } 
            else if (evt.getSource() == exitButton)
           {
               String[] options = {"Exit", "Cancel"};
               int option = DisplayUtils.displayConfirmationMessage("Exiting  FaceMatch operation request",  options);
               if (option == 0)             // Exit
               {
                   /* ActionCompletionEvent completionEvent =  new ActionCompletionEvent
                        (this, ActionCompletionEvent.BATCH_OPS_SELECTION_EVENT,
                            operation, 0, null); 
                    completionListener.actionCompleted(completionEvent);
                   */
                   selectedParams = null;
                   disposeWindow();
               }
               else
                   return;              // ignore
           } 
    }
    
    /************************************************************************************/
  protected void disposeWindow()
      {
          WindowEvent closingEvent = new WindowEvent(this,
                                                           WindowEvent.WINDOW_CLOSING);
          Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(closingEvent);
          System.out.println(">>> Disposing Selection Window. <<<");
      }

 
    /************************************************************************************/
    // Check the input parameters provided by the user 
    protected boolean validateSelectedParams(HashMap<String, Object> inputParams)
    {
        // Check image Collection name
       String clientName = (String) inputParams.get("clientName");
        String imageExtent = (String) inputParams.get("imageExtent");

        // check batch File name
        String batchFileName = (String) inputParams.get("dataFilename");
        if (batchFileName == null || batchFileName.isEmpty())
        {
              DisplayUtils.displayErrorMessage("Please specify a batch file name.");
              return false;
        }
        else
        {
            // make sure that it is a valid file
            File file = new File(batchFileName);
            if (!file.exists() || !file.isFile())
            {
                DisplayUtils.displayErrorMessage("Please specify a valid file name with Facematch Service request data."); 
                return false;
            }
        }
        // check the Stored result directory 
       boolean storeResults =  ((Boolean)inputParams.get("storeResults")).booleanValue();
        if (storeResults)
        {
             String resultStoreDir = (String) inputParams.get("resultsDirectory");
             File dir = new File(resultStoreDir);
             if (!dir.exists() || !dir.isDirectory())
             {
                DisplayUtils.displayErrorMessage("Please specify a valid directory name to store Facematch Service results"); 
                return false;
            }
        }
        return true;             
    }
    
    /*-------------------------------------------------------------------------------------------------------------*/
    protected void updateImageList()
    {
        if (!newExtentSelected)
            return;
        String client =  (String) selectedParams.get("clientName");
        String extent = (String)selectedParams.get("extentName");
        LinkedHashMap<String, String>   imageMap = getImageTagInfo( client,  extent);
       
           
    }      
            
/*-------------------------------------------------------------------------------------------------------------*/
    // User selected a radio button indicating the source type to select
    /*----------------------------------------------------------------------------------------------------------*/
    protected void imageInputTypeActionPerformed(ActionEvent evt)
    {
        JRadioButton button = (JRadioButton) evt.getSource();
        boolean showTags = false;
        boolean showUri = false;
        
        if (button.getText().equals (imageButtonTexts[0]) )    //("Enter image Tag/URI"))
        {
             imageTagList.setVisible(false);
             imageURIList.setVisible(false);
             imageNameField.setVisible(true);
             showTags = false;
             showUri = false;
        }
        else if (button.getText().equals(imageButtonTexts[1]) )        //"Select Image Tag(s)", 
        {
            showTags = true;
            showUri = false;
            imageTagList.setVisible(true);
            imageURIList.setVisible(false);
            imageNameField.setVisible(false);
        }
        else                                                             //"Select Image URI(s)", 
        {
            showTags = false;
             showUri = true;
            imageURIList.setVisible(true);
            imageTagList.setVisible(false);
            imageNameField.setVisible(false);
        }
    }
        // Select one or more entries from the list
  /*      String client =   (String) selectedParams.get("clientName");
        
        String extent = (String)selectedParams.get("imageExtent");
        LinkedHashMap<String, String> imageTagInfo = getImageTagInfo(client, extent);
        int ns = imageTagInfo.size();
        imageTags = new String[ns];
        imageTagInfo.keySet().toArray(imageTags);
        
        imageURIs = new String[ns];
        imageTagInfo.values().toArray(imageURIs);
      
       imageList = new JList();
       imageList.setFont(new Font("Halvetica", Font.PLAIN, 12));
        if (showTags)
            imageList.setListData(imageTags);
        else
            imageList.setListData(imageURIs);

        // currently  we allow only single selection
        imageList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(imageList);
        scrollPane.setPreferredSize(new Dimension(300, 200)); 
        JDialog listDialog = new JDialog();
        listDialog.add(scrollPane);
        imageList.addListSelectionListener(new ImageSelectionListener(listDialog));
       
        // Create a popup JDialog with a selectable source file list
        Point loc =  new Point(400, 400);   // selectSourceButton.getLocationOnScreen();
        listDialog.setLocation(new Point(loc.x, loc.y - 200));       
        listDialog.setVisible(true);
     }*/
     /*----------------------------------------------------------------------------------------------------------------------------*/
        private class ImageSelectionListener implements ListSelectionListener
        { 
            JDialog popupDialog;
            public ImageSelectionListener (JDialog listDialog)
            {
                popupDialog = listDialog;
            }
            /*----------------------------------------------------------------------------------------------------------------------------*/
            public void valueChanged(ListSelectionEvent e) 
            {
                if (e.getValueIsAdjusting() )
                    return;
                popupDialog.setVisible(false);

                int ns = e.getLastIndex() -e.getFirstIndex() +1;
                String[] selectedValues  = new String[ns];
                for (int i = 0; i < ns; i++)
                {
                    int index = e.getFirstIndex() + i;
                    if (showTags)
                       selectedValues[i] = imageTags[index];
                    else if (showUri)
                      selectedValues[i] = imageTags[index];
                    else
                        return;
                }
               selectedParams.put("listType" , (showTags ? "imageTag" : "imageURI"));
               selectedParams.put("images", selectedValues);
               return;
            }
        }
        /*--------------------------------------------------------------------------------------------------*/
        // Display a JTextField editor for the user to type in an image Tag or URL 
        protected JTextField createTextField()
        {
           JTextField  textField = new JTextField(120);
           textField.addActionListener(
               new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    imageSelectionActionPerformed(evt);
                }
            });
           return textField;
        }
        
        /*--------------------------------------------------------------------------------------------------*/
        protected void  imageSelectionActionPerformed(ActionEvent evt)
        {
             String text =  ( (JTextField)evt.getSource()).getText();
             boolean isURL = FormatUtils.isValidURL(text);
              selectedParams.put("listType" , (isURL ? "imageURI" : "imageTag"));
              selectedParams.put("images", text);
        }
        /*--------------------------------------------------------------------------------------------------*/
        protected  LinkedHashMap<String, String>  getImageTagInfo(String client, String extent)
        {
                String tagFileName =  ImageTagIO.getImageTagMapFileName(WBProperties.configProperties,
                            client, extent);
                if (tagFileName == null ||   (new File(tagFileName)).exists() == false)
                    return null;                // no file name given
                LinkedHashMap<String, String> imageTagMap = ImageTagIO.readImageTagMap(tagFileName);
                if (imageTagMap == null || imageTagMap.size() == 0)
                    return null;                // error
               return imageTagMap;
        }
 /*-------------------------------------------------------------------------------------------------------------*/               
}
