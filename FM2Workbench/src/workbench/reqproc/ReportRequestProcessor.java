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

/*
 * ReportRequestProcessor.java
 */

package workbench.reqproc;

import java.util.HashMap;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import fm2client.app.FM2ScenarioTestApp;
import fmservice.httputils.common.ServiceConstants;
import workbench.control.FMMenuCommands;
import workbench.control.WBProperties;
import workbench.display.DisplayUtils;
import workbench.event.ActionCompletionListener;
import workbench.reqproc.input.ReportRequestDialog;
import workbench.util.WBUtils;


public class ReportRequestProcessor implements  RequestProcessor, FMMenuCommands
{
    
    private static Logger log = Logger.getLogger(ReportRequestProcessor.class);
    
  
     protected static JFrame paramInputFrame = null;
     protected static JFrame logInputFrame = null;

     protected ActionCompletionListener parent;
     
     HashMap<String, String[]> clientToExtentMap;
     HashMap <String, Object> initialParams;
     HashMap <String, Object> selectedParams;
    
    public ReportRequestProcessor ()  throws Exception
    {
               // initialParams = getInitialRequestParams();
         Properties configProperties = WBProperties.configProperties;
         clientToExtentMap = WBUtils.getClient2ExtentMap(configProperties);
    }
        
    //*-------------------------------------------------------------------------------------------------------------
    // Create the input Selection panel.
    // Check for parameters specified in the config file
    // and set as initial parameters.  Provide defaults if not specified
    //*---------------------------------------------------------------------------------------------------------------
     protected HashMap<String, Object>  getParamList(String operation)
     {
        HashMap<String, Object> initialParams = new HashMap();
        initialParams.put("clientInfoMap", clientToExtentMap); 
       
        if (operation .equals("ALL_CLIENTS"))
                return null;

         initialParams.put("Client Name", "");          //f for all operations
         if ( operation.equals(REPORT_EXTENT))
         {
             initialParams.put("Image Extent", "");
         }
         else if (operation.equals (REPORT_IMAGE))
         {
             initialParams.put("Image Extent", "");
             initialParams.put("Image Tags", "");
         }
         return initialParams;
    }

    //---------------------------------------------------------------------------------------------------------------------------
     // Execute the Menu command selected by the user 
     // invoked by the parent object 
     //---------------------------------------------------------------------------------------------------------------------------
     public void executeCommand(ActionCompletionListener listener,  String command, 
         Object commandParam)
     {
         try
         {
                 parent = listener;
                  SwingUtilities.invokeLater(new Runnable() {
                         public void run() {
                             ReportRequestDialog inputSelectionDialog= createReportRequestDialog();
                             initialParams = getParamList(command);
                             inputSelectionDialog.setInitialChoices(command, initialParams);
                             
                            WBProperties.mainFrame.setVisible(false);
                            inputSelectionDialog.toFront();
                           inputSelectionDialog.start();
                             
                             //---------------------------------------------------------------------------------------------------------------------//
                            // We will come here and get the selected params only after the selection window  closes
                             //------------------------------------------------------------------------------------------------------------------------//
                            HashMap<String, Object> userParams  =  inputSelectionDialog.getSelectedParams();
                            inputSelectionDialog.dispose();
                            WBProperties.mainFrame.setVisible(true);
                            processUserInput(command, userParams);
                     }
              });
         }
         catch (Exception e)
         {
             e.printStackTrace();
            notifyCompletion( command, -1, null);
         }
    }    
     
     /*--------------------------------------------------------------------------------------------------------------*/
    // build the dialog  to receive user input for performing Batch operation requests
     protected ReportRequestDialog  createReportRequestDialog()
    {
        String dialogName = " Reports Request";
        String displayName = " Select parameters for Information Request";
        
        paramInputFrame = new JFrame(dialogName);
        paramInputFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);              // hide it for next time
        
       ReportRequestDialog paramputDialog =
           new ReportRequestDialog(paramInputFrame,  initialParams);
           

       // setFrameCenter(inputSelectionFrame, inputSelectionFrame.getPreferredSize());
        paramInputFrame.pack();
        return paramputDialog;
    }
         
     //*-----------------------------------------------------------------------------------------------------
     // Comes here after the user selects the parameters for the bsatch operation
     ////*-----------------------------------------------------------------------------------------------------
    protected void processUserInput(String command,  HashMap<String, Object> userParams  )
    {
        selectedParams = userParams;
        System.out.println("\n>>> Received  Selected params: " + selectedParams);
        if (selectedParams == null)
        {
           System.out.println("\n>>> User canceled operation");
           notifyCompletion(command, 0, null);
           return;
        }    
        try
        {
            int status =  performRequestedOperations(command, selectedParams);
            notifyCompletion( command, status, null);
        }
        catch (Exception e)
        {
            DisplayUtils.displayErrorMessage("Error executing batch command " + e.getMessage());
            e.printStackTrace();
        }
    }
    /*--------------------------------------------------------------------------------------------------------------*/
    // @param command - the manu command dirsplated in the GU  
    /*--------------------------------------------------------------------------------------------------------------*/
    protected int   performRequestedOperations(String command,  
        HashMap<String, Object> userParams)  throws Exception
     {
          HashMap<String, Object> requestParams = new HashMap <String, Object>();
         boolean missingParams = false;
         if (command.equals(REPORT_CLIENT))
         {
             String client =  (String) (userParams.get("Client Name"));
             if (client == null || client.isEmpty())
                 missingParams = true;
             else
                 requestParams.put(ServiceConstants.CLIENT_PARAM, client);
         }
         else if (command.equals(REPORT_EXTENT))
         {
             String client =  (String) (userParams.get("Client Name"));
             String imageExtent = (String) userParams.get("Image Extent");
             if (client == null || imageExtent == null || client.isEmpty() || imageExtent.isEmpty())
                 missingParams = true;
             else
              {
                   requestParams.put(CLIENT_PARAM,  client);
                   requestParams.put(EXTENT_NAME, imageExtent);
              }
         }   
         else if (command.equals(REPORT_IMAGE))
         {
             String client =  (String) (userParams.get("Client Name"));
             String imageExtent = (String) userParams.get("Image Extent");
             String[] imageTags = (String[]) userParams.get("Image Tags");
             if (client == null || imageExtent == null || imageTags == null || imageTags.length == 0)
                 missingParams = true;
             else
              {
                   requestParams.put(CLIENT_PARAM,  client);
                   requestParams.put(EXTENT_NAME, imageExtent);
                   requestParams.put(IMAGE_TAG, imageTags);
              }
         }

      
        if (missingParams)
         {
             DisplayUtils.displayErrorMessage("Please provide all parametrs for  requesing report from server.");
             return 0;
         }

         try
        {  
            requestParams.put("operation", "");         // Srvice has no separate operations
            FM2ScenarioTestApp reportsApp = new FM2ScenarioTestApp(WBProperties.configProperties,true);
           // FM2ServiceResult result = reportsApp.requestReportOpsService(requestParams);
           // displayResult(operation, result);
            return 1;
        }
        catch (Exception e)
        {
           DisplayUtils.displayErrorMessage("Got Exception, " + e.getMessage()); 
           log.error("Got Exception", e);
           return -1;
        }
    }
  //--------------------------------------------------------------------------------------------------  
    protected void displayResult( String result)
    {
        // TBD: Create TextEditor and display the result
    }
 
      public  void notifyCompletion(String function, int status, Object param)
     {
         parent.actionCompleted(null);
     }
}
