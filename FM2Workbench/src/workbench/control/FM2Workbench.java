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
/************************************************************************
 * Class FM Workbench
 *
 * This is the top level FaceMatch Workbench  control class, which displays the 
 * operation  menubar and handles menu commands issued by the user. 
 * It then initiates the command processing, sending requests to the FM2WebServer 
 * as required for real-time operations..
 * 
 * 
 **************************************************************************
 *
 * Change Log:
 *
 ***************************************************************************/

package workbench.control;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;

import workbench.display.DisplayUtils;
import workbench.event.ActionCompletionEvent;
import workbench.event.ActionCompletionListener;
import workbench.reqproc.BatchRequestProcessor;
import workbench.reqproc.RegionRequestProcessor;
import workbench.reqproc.StoredResultProcessor;
import workbench.util.PropertyLoader;
import workbench.util.Utils;


//----------------------------------------------------------------------------//

public class FM2Workbench extends JFrame 
        implements FMMenuCommands,  ActionCompletionListener
{
    private static Logger log = Logger.getLogger(FM2Workbench.class);
    
    static FM2Workbench wbManager;         // a single instance

    final static int INFO = 1;
    final static int WARNING = 2;
    final static int ERROR = 3;
    
    // For grouping of menu elements
    final static String SEPARATOR = "";
    
    

    final static String[] topmenuCommands =
    {
        MENU_ADMIN, MENU_EXTENT_OPS, 
        MENU_REGION_OPS, 
        MENU_STORED_RESULTS, MENU_REPORT, 
        MENU_PERFORMANCE, MENU_HELP
    };
    
    final static  String[] regionOpsSubmenu = {MENU_REGION_OPS_SINGLE,  MENU_REGION_OPS_BATCH};

    final static String[] topmenuTooltips = 
    {
        "Administrative Functions",
        "Image Extent operations",
        " Face find, Ingest and Query operations",
        "Stored Result Display and Analysiss",
        "System Status and Report generation",
         "FaceMatch Performance queries",
         "Help about FaceMatch"
    };
   /*---------------------------------------------------------------------------------------------------*/ 
     final static String[] adminCommands = {
         ADD_CLIENT, SET_FF_PARAMS, SET_GPU_USE 
    };
    final static String[] adminCommandTooltips = {
        "Add a new client for FaceMatch", "Set FaceFinding Flags for testing",
        "Turn GPU on/off"
    };
    /*---------------------------------------------------------------------------------------------------*/
    final static String[] extentCommands = {
         ADD_EXTENT, REMOVE_EXTENT, ACTIVATE_EXTENT, DEACTIVATE_EXTENT
    };
    final static String[] extentCommandTooltips = {
        "Add a new Image Extent for a client",  "Remove an existing Image Extent",
         "Activate an Image Extent for ingest/query", "Deactivate an Image Extent"
};     
  /*---------------------------------------------------------------------------------------------------*/     
    final static String[]  batchRegionOpsCommands = {
        INGEST_BATCH_RT,   REMOVE_BATCH_RT, QUERY_BATCH_RT, 
        FF_BATCH_RT, SCENARIO_TEST
    };
    final static String[] batchRegionOpsTooltips = {
   "Batch Ingest images in real-time", 
    "Multiple image queries in real-time", 
    "Find faces for a batch of images in real-time",
    "Remove Image Regions in real-time",
    "Perform tests using an operational scenario"    
};     
final static String[]  singleRegionOpsCommands = {
        INGEST_SINGLE_RT, QUERY_SINGLE_RT,
        FF_SINGLE_RT,  REMOVE_SINGLE_RT
    };
    final static String[] singleRegionOpsTooltips = {
    "Ingest an image",
    "Query for  faces matching the face(s) in the given image", 
    "Find faces in an image",
     "Remove an ingested image or a detected region in it",
    };
 /*---------------------------------------------------------------------------------------------------*/       
     final static String[]  resultDisplayCommands = {
           VIEW_STORED_FF_DATA , VIEW_STORED_INGEST_DATA, VIEW_STORED_QUERY_DATA
    };
    final static String[] resultDisplayTooltips = {
        "View and analyze stored Face finding  results", "View and analysestored  Region Ingest results", 
        "View and analyze stored Region Query results"
    };
    /*---------------------------------------------------------------------------------------------------*/         
     final static String[] reportsCommands = {
       REPORT_CLIENT, REPORT_EXTENT, 
       REPORT_IMAGE,   REPORT_DATABASE , SHOW_SERVER_LOG, SHOW_CLIENT_LOG
    };
                    
     final static String[] reportsCommandTooltips = {
         "Information about a Client", "Information about an image Collection", 
         "Information about an Image", "Database information", "",
         "Messages from Server log at FM2 server facility", "Messages from Client Log at FM2 client facility"};     // TBD
    
    /*---------------------------------------------------------------------------------------------------*/      
     final static String[]  performanceCommands = {"To be implemented"};
     final static String[]  performanceCommandTooltips = {"To be implemented later"};
      /*---------------------------------------------------------------------------------------------------*/
  
     final static String[] helpCommands = {"To be implemented"};
     final static String[] helpCommandTooltips = {"To be implemented later"};
  
     /*---------------------------------------------------------------------------------------------------*/

    /**
     * ***************************************************************
     */
    // local variables
    JFrame mainFrame = null;       // Application's main Window  
    JMenuBar menuBar = null;

    /*HelpPanel*/ JFrame helpPanel = null;

    // top level menus
    JMenu adminMenu;
    JMenu extentMenu;
    JMenu regionOpsMenu;
    JMenu resultDisplayMenu;
    JMenu reportsMenu;
    JMenu performanceMenu;
    JMenu helpMenu;
    
    // Control classes for variousmenu command execition
    //AdminOpsManager adminManage;
    //ExtentOpsManager extentOpsManager;
    BatchRequestProcessor batchRequestProcessor;
    RegionRequestProcessor  regionRequestProcessor;
    StoredResultProcessor storedResultProcessor;
    //ResultDisplayManager resultDisplayManager;

    boolean firstTime = true;   // for initial messages
    boolean freshDisplay = true;

    // main window displays and tabbed panels
    Container topPanel;
    JTabbedPane opsPanel = new JTabbedPane();
    JPanel inputPanel = null;
    JPanel faceFindResultPanel = null;
    JPanel regionIngestResultPanel = null;
    JPanel regionQueryResultPanel = null;

    
    int initStatus = 0;
    JPanel splashScreen;                    // front screen of the Workbench
    
 /************************************************************************/
// constructor - only a single instance 
//-----------------------------------------------------------------------------------------
    public FM2Workbench(String configFile)
    {
        if (wbManager != null)
            return;
        
        mainFrame = this;     // Top level JFrame of the application
        mainFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        Properties testProperties = null;
        initStatus = 0;
        try
        {
            testProperties = PropertyLoader.loadProperties(configFile);
            if (testProperties == null)
            {
                DisplayUtils.displayErrorMessage("Exiting due to error in loading configuration data.");
                 return;
            }
        }
        catch (Exception e)
        {
            DisplayUtils.displayErrorMessage(" Invalid configuration file name/data, " + e.getMessage());
             return;
        }
        WBProperties.configProperties = testProperties;             // static global object
        WBProperties.mainFrame = this;
        wbManager = this;
        initStatus = 1;       
    }
    
    public FM2Workbench  getWBManager()
    {
        return wbManager;
    }
 
    /*--------------------------------------------------------------------------------------------------*/
    protected int getInitStatus()
    {
        return initStatus;
    }
        
   /*---------------------------------------------------------------------------------------------------*/
    protected void start()
    {
        initialize();       // create all MenuItems and add the menubar to frame
        freshDisplay = true;
        wbManager  = this;
    }
    
    
    protected   void initialize( )
    {
         Properties config = WBProperties.configProperties;
         String fmServerURL =  (String)config.getProperty("fm2ServerURL");
         if (fmServerURL == null)
        {
              DisplayUtils.displayErrorMessage("No FaceMatch2 server URL provided in the configuration file as \"fm2ServerURL."+ 
               "Cannot perform realtime operations. ");
        }
       
        WBProperties.fmServerURL = fmServerURL;    
        WBProperties.regionWebServiceURL = fmServerURL;           // for face related operations
        WBProperties.imageWebServiceURL = fmServerURL;    // for whole image related operations
        
        // initialize logging of client messages
        String log4jFileName =  Utils.initLogging(config);
        System.out.println("*** Writing results to Log file: " + log4jFileName);
        if (log4jFileName != null)
            WBProperties.clientLogFile = log4jFileName;
       
        // check if the client is run by the FMAdministrator
        // Note: admin password is not tested here for correctness - used only for enabling admin menus
        boolean isAdmin = ( config.get("isAdmin") !=  null) &&  config.get("isAdmin").equals("true");
        WBProperties.isAdminClient =  isAdmin && (config.get("fm2Admin.password") != null);
        
        // initialize other GUI components
        int status = buildNinitFrame();
        if (status == 0)
        {
            initStatus = -1;
            return;
        }
        
        setFrameCenter(this);
        this.pack();
         this.setVisible(true);
    }


/**************************************************************************************/
    // Build and add the Menubar to this frame
    private int  buildNinitFrame()
    {
        // Create the menu bar
        menuBar = createMenuBar();
        menuBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        mainFrame.setJMenuBar(menuBar);
    
               
        JPanel splashScreen = createSplashScreen();    
        topPanel = mainFrame.getContentPane();
        topPanel.setBackground(WBProperties.defaultBgColor);
        topPanel.add(splashScreen);
        //Dimension d = topPanel.getSize();
        mainFrame.pack();
        mainFrame.setTitle("FaceMatch Test  and Analysis Workbench");           
        setFrameCenter(mainFrame);
    
       //initSession();

	// TBD: enable and disable menu items
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
               ; // System.exit();
            }
        });
        
   //initBatchOps();
   //     initMetadataOps();
        
       mainFrame.setVisible(true);
       return 1;
    }

/******************************************************************************************    
    /**
     * Create a top panel to be used as a splash screen
     ***/
    private JPanel createSplashScreen()
    {
        // set the default panel backgroud color
        Color bgColor  = WBProperties.defaultBgColor;

        // create a splash screen - TBD: a nice one
        JPanel  topPanel= new JPanel(new BorderLayout());
        String labelText = "<HTML><center>Test  and Analysis Workbench<br><i>for</i><br>FaceMatch Operations"
                                      + "</center></HTML>";
        JLabel idLabel = new JLabel(labelText);
        idLabel.setFont(new Font("Halvetica", Font.BOLD, 28));
        idLabel.setForeground(WBProperties.defaultFontColor);
        idLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        idLabel.setBorder(BorderFactory.createEmptyBorder(125, 100, 125, 100));         // initial min spacing
         
         JPanel middlePanel = new JPanel();
         middlePanel.setLayout(new BorderLayout());
         middlePanel.add(idLabel, BorderLayout.CENTER);
         
        Border lineBorder = LineBorder.createBlackLineBorder();
        Border bevelBorder = BorderFactory.createRaisedBevelBorder();
        Border bevelLineBorder = new CompoundBorder(bevelBorder, lineBorder);
        middlePanel.setBorder(bevelLineBorder);
        
        JPanel middlePanel1 = new JPanel();
        middlePanel1.setLayout(new BorderLayout());
        middlePanel1.setBorder(BorderFactory.createEmptyBorder(50, 100, 30, 100));
        middlePanel1.add(middlePanel, BorderLayout.CENTER);
        middlePanel1.setBackground(bgColor);
        
        JLabel messageLabel = new JLabel();
        String serverURL = WBProperties.fmServerURL;
        messageLabel.setText ("FaceMatch2 Web Service URL -  " + serverURL );
        messageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        messageLabel.setFont(new Font("Halvetica", Font.BOLD, 14));
       // messageLabel.setForeground(WBProperties.defaultFontColor);
        middlePanel1.add(messageLabel,  BorderLayout.SOUTH);
        topPanel.add(middlePanel1, BorderLayout.CENTER);

        JLabel startLabel = new JLabel();
        startLabel.setText ("Select a menu item to continue...");
        startLabel.setFont(new Font("Halvetica", Font.BOLD|Font.ITALIC, 18));
        startLabel.setForeground(Color.MAGENTA);
         
        startLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        startLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        topPanel.add(startLabel,  BorderLayout.SOUTH);
        topPanel.setBackground(bgColor);
        
      //  JLabel branchLabel = new JLabel("<html><center>Communication Engineering Branch, "+
      //      "LHNCBC, NLM, NIH</html>", JLabel.CENTER);
        
        return topPanel;
    }
   
    
    /**********************************************************************
     * Center a frame of dimension on the screen
     **********************************************************************/
     private void setFrameCenter(JFrame frame)
    {
        Dimension dim = frame.getSize();
        Dimension screenSize = this.getToolkit().getScreenSize();
        int x = (screenSize.width/2) - (dim.width/2);
        int y = (screenSize.height/2) - (dim.height/2);
        frame.setLocation(x,y);
    }

/*********************************************************************************
 * Info, warning and ErrorMessage
 */
    public static void displayMessage(int messageType, String msg)
    {
        if (messageType == INFO)
            DisplayUtils.displayInfoMessage(msg);
        if(messageType == WARNING)
            DisplayUtils.displayWarningMessage(msg);
        else if (messageType == ERROR)
            DisplayUtils.displayErrorMessage(msg);
    }


  /************************************************************************
   * Processing to create menu items and subitems
   ************************************************************************/
    private JMenuBar createMenuBar()
    {
        /* Note: the value of "index" and hence the ordering of JMenuItems must
        * match the order in topMenuCommands[] array
        */
        int index = 0;          // start menu index
        JMenuBar menuBar = new JMenuBar();
        adminMenu = createAdminMenu(index++);
        menuBar.add(adminMenu);

        extentMenu = createExtentMenu(index++);
        menuBar.add(extentMenu);

       // regionOpsMenu = createRegionOpsMenu(index++);
        //menuBar.add(regionOpsMenu);
        
        regionOpsMenu = createRegionOpsMenu(index++);
        menuBar.add(regionOpsMenu);
        
        resultDisplayMenu =  createStoredResultDisplayMenu(index++);
        menuBar.add(resultDisplayMenu);

        reportsMenu = createReportsMenu(index++);
        menuBar.add(reportsMenu);
        
        performanceMenu = createPerformanceMenu(index++);
        menuBar.add(performanceMenu);

       helpMenu = createHelpMenu(index++);
        menuBar.add(helpMenu);
        return menuBar;
   }
   
  /*******************************************************************
   * create the Administrative menu
   *******************************************************************/
    private JMenu createAdminMenu(int menuIndex)
    {
        int n = menuIndex;
       adminMenu = new JMenu(topmenuCommands[n]);
        adminMenu.setToolTipText(topmenuTooltips[n]);
        MenuAction adminAction = new MenuAction(topmenuCommands[n]);

        JMenuItem[] items = new JMenuItem [adminCommands.length]; // add separators
        for (int i = 0; i < adminCommands.length; i++)
        {
            JMenuItem item = createSubmenuItem(adminCommands[i],
                    adminCommandTooltips[i], adminAction);
            adminMenu.add(item);
            items[i] = item;
        }
        return  adminMenu;
    }

   
  /*******************************************************************
  * create the ExtentOps  menu
  *******************************************************************/
       private JMenu createExtentMenu(int menuIndex)
    {
        int n = menuIndex;
        extentMenu = new JMenu(topmenuCommands[n]);
        extentMenu.setToolTipText(topmenuTooltips[n]);
        MenuAction 
            extentOpsAction = new MenuAction(topmenuCommands[n]);

        JMenuItem[] items = new JMenuItem [extentCommands.length];
        for (int i = 0; i < extentCommands.length; i++)
        {
            JMenuItem item;
            item = createSubmenuItem(extentCommands[i],
                    extentCommandTooltips[i],extentOpsAction);
            extentMenu.add(item);
            items[i] = item;
        }      
        return extentMenu ;
    }

  /*******************************************************************
  * create the RegionOps  menu - through its two submenus
  *******************************************************************/
    private JMenu createRegionOpsMenu(int menuIndex)
    {
        int n = menuIndex;
        regionOpsMenu = new JMenu(topmenuCommands[n]);
        regionOpsMenu.setToolTipText(topmenuTooltips[n]);
       
        JMenu[] items = new JMenu [regionOpsSubmenu.length];
       
        items[0] = createSingleRegionOpsMenu(regionOpsSubmenu[0]);
        items[1] = createBatchRegionOpsMenu(regionOpsSubmenu[1]);

        regionOpsMenu.add(items[0]);
        regionOpsMenu.add(items[1]);
      
        return regionOpsMenu;
    }
   /*******************************************************************
  * create the RegionOps  menu for a single FaceMatch  operations
  *******************************************************************/      
    private JMenu createSingleRegionOpsMenu(String command)
    {
        JMenu  singleRegionOpsMenu = new JMenu(command);
        MenuAction  singleRegionOpsAction = new MenuAction(command);
        int nm = singleRegionOpsCommands.length;
        JMenuItem[] items = new JMenuItem [nm];
         for (int i = 0; i < nm; i++)
        {
            JMenuItem item;
            item = createSubmenuItem(singleRegionOpsCommands[i],
                    singleRegionOpsTooltips[i], singleRegionOpsAction);
           singleRegionOpsMenu.add(item); 
            items[i] = item;
            if (i == 2)   
		singleRegionOpsMenu.addSeparator();  
        }      
        return singleRegionOpsMenu ;
    }

    
 /*******************************************************************
  * create the RegionOps  menu for batch mode operations
  *******************************************************************/
    private JMenu createBatchRegionOpsMenu(String command)
    {
        JMenu batchRegionOpsMenu = new JMenu(command);
        MenuAction  batchRegionOpsAction = new MenuAction(command);
        int nm = batchRegionOpsCommands.length;
        JMenuItem[] items = new JMenuItem [nm];

        for (int i = 0; i < nm; i++)
        {
            JMenuItem item;
            item = createSubmenuItem(batchRegionOpsCommands[i],
                    batchRegionOpsTooltips[i], batchRegionOpsAction);
            batchRegionOpsMenu.add(item);
            // add separators after the third item
            if (i == 3)   
		batchRegionOpsMenu.addSeparator();        
            items[i] = item;
            
       /* allow it 
            // Scenario test currently implemented
            if (batchRegionOpsCommands[i].equals(SCENARIO_TEST))
                 item.setEnabled(false)
            */;
        }      
        
        return batchRegionOpsMenu ;
    }
 
  /*******************************************************************
   * create the menu
   *******************************************************************/
    private JMenu createStoredResultDisplayMenu(int menuIndex)
    {
        int n = menuIndex;
        
        resultDisplayMenu = new JMenu(topmenuCommands[n]);
        resultDisplayMenu.setToolTipText(topmenuTooltips[n]);
        MenuAction resultDisplayAction = new MenuAction(topmenuCommands[n]);

        JMenuItem[] items = new JMenuItem [resultDisplayCommands.length]; 
        for (int i = 0; i < resultDisplayCommands.length; i++)
        {
            JMenuItem item = createSubmenuItem(resultDisplayCommands[i],
                   resultDisplayTooltips[i],  resultDisplayAction);  // 
            resultDisplayMenu.add(item);      
        }
        return  resultDisplayMenu;
    }

  /*******************************************************************
  * create the Reports and Status menu
  *******************************************************************/
     private JMenu createReportsMenu(int menuIndex)
     {
        int n = menuIndex;
        reportsMenu = new JMenu(topmenuCommands[n]);
        reportsMenu.setToolTipText(topmenuTooltips[n]);
        MenuAction reportsAction = new MenuAction(topmenuCommands[n]);

        JMenuItem[] items= new JMenuItem [reportsCommands.length];
        for (int i = 0; i < reportsCommands.length; i++)
        {
            if (i == 4)
                  reportsMenu.addSeparator();
            JMenuItem item = createSubmenuItem(reportsCommands[i],
                  reportsCommandTooltips[i], reportsAction);
            reportsMenu.add(item);
             items[i] = item;
             item.setEnabled(true);                    
        }
    /*       reportsImageItem = items[0];
        displayReportItem = items[1];
	*/
        // label the menu items for later reference
	return  reportsMenu;
    }
     
   /**********************************************************
   * create the Performance menu
   *********************************************************/
    private JMenu createPerformanceMenu(int menuIndex)
    {
        int n = menuIndex;
        performanceMenu = new JMenu(topmenuCommands[n]);
        performanceMenu.setToolTipText(topmenuTooltips[n]);
        
        MenuAction performanceAction = new MenuAction(topmenuCommands[n]);

       JMenuItem[] items= new JMenuItem [performanceCommands.length];
        for (int i = 0; i < performanceCommands.length; i++)
        {
            JMenuItem item = createSubmenuItem(performanceCommands[i],
                  performanceCommandTooltips[i], performanceAction);
            performanceMenu.add(item);
            items[i] = item;
            item.setEnabled(false);                     // currently not available
        }
        return performanceMenu;
    }

  /**********************************************************
   * create the Help menu
   *********************************************************/

    private JMenu createHelpMenu(int menuIndex)
    {
        int n = menuIndex;
        helpMenu = new JMenu(topmenuCommands[n]);
        helpMenu.setToolTipText(topmenuTooltips[n]);
        MenuAction helpAction = new MenuAction(topmenuCommands[n]);

        JMenuItem[] items= new JMenuItem [helpCommands.length];
        for (int i = 0; i < helpCommands.length; i++)
        {
            JMenuItem item = createSubmenuItem(helpCommands[i],
                  helpCommandTooltips[i], helpAction);
            helpMenu.add(item);
            items[i] = item;
            item.setEnabled(false);                     // currently not available
        }
        return helpMenu;
    }

  /*********************************************************************
   * Create a submenu item with given attributes
   *********************************************************************/

    private JMenuItem createSubmenuItem(String name, String tooltip,
        MenuAction menuAction)
    {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(menuAction);
        if (tooltip != null)
            item.setToolTipText(tooltip);
        return item;
    }
    
       
  /*********************************************************************
   * Create a RadioButton submenu item with given attributes
   *********************************************************************/

/*    private JMenuItem createSubmenuRadioButton(String name, String tooltip,
        MenuAction menuAction)
    {
        JMenuItem item = (JMenuItem) new JRadioButtonMenuItem(name);
        item.addActionListener(menuAction);
        if (tooltip != null)
            item.setToolTipText(tooltip);
        return item;
    }
*/
/////////////////////////////////////////////////////////////////
/********************************************************************
 * Menu related Actions
 * MenuAction is an inner class holding information about the Menu 
 * Item that is being chosen
********************************************************************/

 /** An inner class to define the MenuAction. */
    class MenuAction extends AbstractAction
    {
        String menuName;
        String subMenuName = null;     // default: no submenu

        // Constructor
        MenuAction(String name)
        {
            super(name);
            menuName = name;
            subMenuName = null;
        }

        // Constructor
        MenuAction(String name, KeyStroke keystroke)
        {
            super(name);
            if (keystroke != null)
            {
                putValue(ACCELERATOR_KEY, keystroke);
            }
            menuName = name;
        }

        // Constructor
        MenuAction(String name, String subName)
        {
            this(name);                 // Call the other constructor
            subMenuName = subName;
        }

        // Constructor
        MenuAction(String name, String subName, KeyStroke keystroke)
        {
            this(name, subName);
            if (keystroke != null)
            {
                putValue(ACCELERATOR_KEY, keystroke);
            }
        }
      /*********************************************************************
      * Implementation of the Menu Event Handler
      **********************************************************************/
      public void actionPerformed(ActionEvent e)
      {     
         // process the action events by invoking methods
          // in the enclosing class
          String command = e.getActionCommand();    // Menu Item
          String parentMenu = subMenuName;
          
          String info = "In MenuAction: commandText=" + command
                  +  "  menuName:  " + menuName; 
           System.out.println(info);
          if (subMenuName != null)
              info += ", subMenu: " + subMenuName;

        /*  if (menuName.equals(MENU_ADMIN))
              processAdminCommand(command, null);        
          */
        else if (menuName.equals(MENU_EXTENT_OPS))        
            processExtentCommand(command);

        else  if (menuName.equals( MENU_REGION_OPS_SINGLE))  
            processSingleRegionOpsCommand(command);
        
        else if (menuName.equals(MENU_REGION_OPS_BATCH))      
            processBatchRegionOpsCommand(command);

        else if (menuName.equals(MENU_STORED_RESULTS))        
            processResultDisplayCommand(command);

       else if (menuName.equals(MENU_REPORT))         
            processReportCommand(command);

        else if (menuName.equals(MENU_HELP))           // "Help"
            processHelpCommand(command);

        else
            System.out.println("Unknown Menu: " + menuName);
        }
    }		
    // end of inner class MenuAction
//////////////////////////////////////////////////////////////////////////
    
/***************************************************************
 * Process commands from the Admin  menu
 ***************************************************************/

  
/************************************************************************
 * Process commands related to Region operations from a Menu bar
 * @param command - The menu command to perform
 * Note: We don't process Batch requests and single requests simultaneously
 **************************************************************************/

    public void processSingleRegionOpsCommand(String command)
    {       

         System.out.println("Processing Region Ops request: " + command);  
         regionOpsMenu.setEnabled(false);    
        
          if (regionRequestProcessor == null)
        {
            try
            {
            	
            	//TODO correct RegionRequestProcessor to work as a one
            	// at a time insert image and run FM on it tool
            	// for now use the BatchRequestProcessor, just send 
            	// batches of size 1.
                regionRequestProcessor = new RegionRequestProcessor();
            }
            catch (Exception e)
            {
            }
        }          

         regionRequestProcessor.executeCommand(this, command, null);
         regionOpsMenu.setEnabled(true);  
        return;
    }
    
    /************************************************************************
 * Process commands related to Region operations from a Menu bar
 * @param command - The menu command to perform
 **************************************************************************/

    public void processBatchRegionOpsCommand(String command)
    {       
        System.out.println("Processing Region Ops Batch request: " + command);  
        regionOpsMenu.setEnabled(false);   

        if (batchRequestProcessor == null)
        {
            try
            {
                batchRequestProcessor = new BatchRequestProcessor();
            }
            catch (Exception e)
            {
            }
        }               
               // iconify self
           // inputSelectionFrame.setState(JFrame.ICONIFIED);
           
          // final   boolean realtime = true;                // batch requests are not from stored data      
          //final boolean displayResults = true;              //default  set to true; currently   crashes on false;
        try
        {
           SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                      batchRequestProcessor.executeCommand(wbManager, command, null);   

                }
           } );
        }
        catch(Exception e) {}
    }

 /************************************************************************
 * Process commands related to Stored Result Display functions
 * @param command - The menu command to perform
 **************************************************************************/

    public void processResultDisplayCommand(String command)
    {       
         System.out.println("Processing Stored Result Display request: " + command);  
         resultDisplayMenu.setEnabled(false);    
         if (storedResultProcessor == null)
        {
            try
            {
                storedResultProcessor = new StoredResultProcessor();
            }
            catch (Exception e)
            {
            }
        }               
      storedResultProcessor.executeCommand(wbManager, command, null);   
         
         /* try
        {
           SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                      storedResultProcessor.executeCommand(wbManager, command, null);   

                }
           } );
        }
        catch(Exception e) {}
         */
        resultDisplayMenu.setEnabled(true);  
    }

 /*************************************************************************************
  * Commands to display Batch related reports on the currently selected batch
  * Note: The user is responsible for closing the display windows
 ************************************************************************************/

    protected void processReportCommand(String command)
    {
         System.out.println("Processing Report Command");  
    }   
  
 /***************************************************************
 * Process the Extent Ops Command for the Application operations
 ***************************************************************/

    protected void processExtentCommand(String commandText)
    {
	  System.out.println("Processing Extent Command");  
    }
    
/***************************************************************
 * Process the Help Command for the Application operations
 ***************************************************************/

    protected void processHelpCommand(String commandText)
    {
	  System.out.println("Processing Help Command");
       
    }

    public  void showWaitCursor(boolean wait)
    {
        if (wait)
            mainFrame.setCursor(DisplayUtils.waitCursor());
        else
            mainFrame.setCursor(DisplayUtils.defaultCursor());
    }

    
    /**
     * make an ops panel active by bringing the corresponding tabbed pane to front
     ***/
  /*  public void makePanelActive(JPanel activePanel)
    {
        JPanel curOpsPanel;
        int index;
  
    
       if (activePanel == null)
        {
            // Simply display the Batch information to the user
            activePanel = batchSelectionPanel;
            String command = MenuCommands.BATCH_DETAILS;
            batchSelectionPanel.initBatchSelection(command, command, this);  
            System.out.println("BatchSelectionPanel set as Active panel");
        }
        
        String name = activePanel.getClass().getName();       
        if (name.endsWith("BatchCreationPanel") ||
             name.endsWith("BatchSelectionPanel"))
        {
            curOpsPanel = batchOpsPanel;
            index = bpIndex;
        }
        else
        {
            curOpsPanel = resourceOpsPanel;
            index = rpIndex;
        }
        
        // remove the splash screen if there and add the operationsPanel   
        if (freshDisplay)
        {      
            splashScreen.setVisible(false);
            topPanel.add(opsPanel); 
            opsPanel.setVisible(true);
            freshDisplay = false;
        }
        curOpsPanel.removeAll();
        curOpsPanel.add(activePanel, BorderLayout.CENTER);
        activePanel.invalidate();
        curOpsPanel.invalidate();
        
        // We do the following to force a repaint by hiding and showing
        curOpsPanel.setVisible(false);
        activePanel.setVisible(true);
        curOpsPanel.setVisible(true);
        
        opsPanel.setSelectedIndex(index);
        System.out.println("Bringing opsPanel " + index + " to front");
        
        currentPanel = activePanel;
    }
    
    public void clearPanel(int index)
    {
        JPanel curOpsPanel = null;
        if (index == 0)
            curOpsPanel = batchOpsPanel;
        else if(index == 1 )
            curOpsPanel =resourceOpsPanel;
        if (curOpsPanel != null)
        {
            curOpsPanel.removeAll();
            curOpsPanel.invalidate();
            curOpsPanel.repaint();
        }
    }
    */
  /**************************************************************
   * Begin processing the user requests
   * This involves enabling the appropriate menu items and
   * waiting for menu actions and other actions therefrom
   *************************************************************/
   public void processRequests()
   {
      initMenuSelection();
    }

   

/***************************************************************
 * Enable and disable menu selection capabilities, depending
 * upon the command
 ****************************************************************/

    private void initMenuSelection()
    {
            // disable the following
            // TBD
    }

/****************************************************************/
    private void enableMenuSelection(String request)
    {
        JMenuItem[] enabledItems = null;
        JMenuItem[] disabledItems = null;
        handleEnabling(enabledItems, disabledItems);
    }

    private void handleEnabling(JMenuItem[] enabledItems,
            JMenuItem[] disabledItems)
    {
        if (enabledItems != null)
        {
            for (int i = 0; i < enabledItems.length; i++)
                enabledItems[i].setEnabled(true);
        }
        if (disabledItems != null)
        {
            for (int j = 0; j < disabledItems.length; j++)
                disabledItems[j].setEnabled(false);
        }
    }

public void actionCompleted (ActionCompletionEvent event)
{
    // Enable the components for selections when an operation is completed or cancelled
    showWaitCursor(false);      // no more wait
    //batchRegionOpsMenu.setEnabled(true);    
     regionOpsMenu.setEnabled(true);  
}


public void actionPerformed(ActionEvent event)
{
    //batchRegionOpsMenu.setEnabled(false);    
}
 ///////////////////////////////////////////////////////////////////////////
 // Post processing operations to be performed after BatchRequestPerocessor
 // or MetadataRequestProcessor have received user input and return a
 // synthetic ActionCompletionEvent here. This is necessary as some functions 
 // are followed up by other requests by the user for operational convenience
 // (such as CreateBatch => Sybmit for Metadata)
 ///////////////////////////////////////////////////////////////////////////
    
/********************************************************************
 * Get notified here when an invoke processor has completed 
 * its action
 *********************************************************************
    
    public void actionPerformed(ActionCompletionEvent evt)
    {
        int eventType = evt.getEventType();
        String function = evt.getFunctionType();
        int status = evt.getStatus();
        
        System.out.println("** SessionManager* Requested function: " + function + 
                " completed with status: " + status);
        
        if (eventType == ActionCompletionEvent.BATCH_EVENT)
        {
            processBatchActionCompletion(function, status, evt.getReturnParam());
        }
        else if (eventType == ActionCompletionEvent.METADATA_EVENT)
        {
             processMetadataActionCompletion(function, status, evt.getReturnParam());
        }
        else if (eventType == ActionCompletionEvent.INGEST_EVENT)
        {
             processIngestActionCompletion(function, status, evt.getReturnParam());
        }
        
        // check of user has queued am action request from a lower level
        UserRequest nextRequest = 
                (UserRequest) SessionProperties.getProperty("NextUserRequest");
        if (nextRequest != null)
        {
            SessionProperties.setProperty("NextUserRequest", null);
            processNextUserRequest(nextRequest);
        }
        return;
    }
    
    /*************************************************************************
     * User has completed a Batch related operation, check and perform
     * what postprocessing needs to be done
     *************************************************************************
    protected void processBatchActionCompletion(String function, int status, 
            Object retParam)
    {    
        batchMenu.setEnabled(true);   
        if (function.equals(BATCH_SELECT) )
        {
            if (status < 1)      // user cancelled
            {
                originalRequest = null;
                lastSelectedBatch = -1;        
                return;
            }
            else   
                lastSelectedBatch = ((BatchInfo)retParam).batch_id;
                Integer param = new Integer(lastSelectedBatch);          
        }  
        else if (function.equals(BATCH_CREATE) || function.equals(BATCH_DELETE))
        {
           ;                // nothing more to do
        }
        // All other Batch requests (status display) are handled locally
        // in other panels.
        return;
    }
    
    
    /**********************************************************************
    * protected void processIngestActionCompletion(String function, int status, 
            Object retParam)
    {  
        System.out.println("SessionManager.processIngestActionCompletion: function: "
                + function + ", status = " + status);
        ingestMenu.setEnabled(true);   
        batchMenu.setEnabled(true);
        
        if (status <= 1)
        {           
           System.out.println("Bringing ingest panel to front");
           opsPanel.setSelectedIndex(rpIndex);
           
            return;             // function completed 
        }
     }
   //////////////////////////////////////////////////////////////////////////
     
    private void repaintFrame()
    {
        topPanel = createSplashScreen();
        topPanel.setVisible(true);
        mainFrame.getContentPane().add(new JScrollPane(topPanel));
        mainFrame.validate();
        mainFrame.pack();
        mainFrame.repaint();
    }
    
    
    /*************************************************************
     * Get the submenu item with the specified text/command
     *************************************************************
    private JMenuItem getMenuItem(JMenu menu, String menuText)
    {
        JMenuItem item = null;
        MenuElement[] subElements = menu.getSubElements();
        for (int i = 0; i < subElements.length; i++)
        {
            item = (JMenuItem)subElements[i];
            if (menuText.equals(item.getText()))
                break;
        }
        return item;
    }
    **/
    

    /**
     * **********************************************************************
     */
    /**
     * @param args the command line arguments
     */
    public static void main(String args[])
    {
          String configFile;
        if (args.length < 1)
                    configFile ="./FM2Workbench.cfg";           // for local testing only
        else
                   configFile = args[0];

           // Start the FaceMatch2 Workbench
        try
        {
           FM2Workbench  fm2Workbench = new FM2Workbench(configFile);
          if  (fm2Workbench.initStatus < 1)
          {
              System.out.println("Exiting due to initialization error");
              System.exit(-1);
          }
          fm2Workbench.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
}









