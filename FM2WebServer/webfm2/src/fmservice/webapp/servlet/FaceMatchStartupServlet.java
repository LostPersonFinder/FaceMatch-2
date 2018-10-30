/*
 * /*
 * Informational Notice:
 * This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
 * an agency of the Department of Health and Human Services, United States Government.
 *
 * The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.
 *
 * The license does not supersede any applicable United States law.
 *
 * The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.
 *
 * Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
 * (FAR) 48 C.F.R. Part52.227-14, Rights in Dataï¿½General.
 * The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
 * non-commercial products.
 *
 * LICENSE:
 *
 * Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
 * as provided by Federal law.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * -	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.
 *
 * -	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 *
 * -	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
 * of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package fmservice.webapp.servlet;

/**
 * FaceMatch system Startup servlet. 
 * This servlet is loaded (by Tomcat) at start up time of the FaceMatch Web server and it
 * initializes the FM system, including database access and other functions
 * by invoking lower level FaceMatchManager functions.
 * <p>
 * It also receives requests from Administrators to shutdown and /or restart fmservice.
 * It is not available to a client application, as well as general user queries regarding 
 * FaceMatch system, but not for any Facematch operation requests.
 * <p>
 *  @author
 *  @version $Revision: 1.0 $
 * @date Feb 19, 2015
 *
 * Change log:
 * 
 */


import fmservice.server.entry.FMServiceManager;
import fmservice.httputils.common.ServiceConstants;
import fmservice.server.result.ServerStatusResult;
import fmservice.server.util.PropertyLoader;

//import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.File;

import java.util.Properties;
import java.util.Calendar;

import java.net.HttpURLConnection;



/*****************************************************************************************/

//@WebServlet("/facematch")
public class FaceMatchStartupServlet extends FaceMatchBaseServlet 
        implements ServiceConstants
{    
    /** log4j category */
    private static Logger log = Logger.getLogger(FaceMatchStartupServlet.class);
        
    protected class FMErrorStatus
    {
        int     errorCode;
        String errorMsg;
    }
    private static FMServiceManager  fmManager = null;
    private static String webConfigFile = null;
    
  
    /*------------------------------------------------------------------------------------------------------*/
    /**
    * Initialize  the FaceMatch system for operations, using the FM2 configuration file.
    * Invoked by the Servlet Container at Facematch startup (load-on-startup = 1)
    * The configuration file name is specified as the parameter "facematch-config" in web.xml 
    * <p>
    * @return if the FM2 system initializes normally, it simply returns, otherwise exits the
    * application with message to the Standard output (as logging may not be enabled.
    * 
    * Note: We cannot use log function until initialization using config file is complete.
    *------------------------------------------------------------------------------------------------------*/
    public void init()
    {
        try
        {
            // Get config parameter
            webConfigFile = getServletContext().getInitParameter("facematch-config");
            System.out.println("\n**********************************************************************************************");
            System.out.println("Facematch system will be intialized using Configuration: " + webConfigFile);


            // get the name of the FM2 server configuration file from the webConfig file
            Properties configProperties = PropertyLoader.loadProperties(webConfigFile);
            String serverConfigFile = configProperties.getProperty("fm2server.configfile");
            if (serverConfigFile == null ||   !(new File(serverConfigFile).exists()))
            {
                  System.out.println( "Invalid Server configuration file name provided: " +
                         (serverConfigFile == null ? "NULL" : serverConfigFile));
                  System.exit(-1);
            }
            startServiceManager(serverConfigFile);
        }
        catch (Exception e)
        {
            System.out.println(
                  ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"
                + "    ERROR INITIALIZING THE FACEMATCH SYSTEM. EXITING. \n"
                + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    /*----------------------------------------------------------------------------------------------*/
    protected void  startServiceManager(String serverConfigFile)
    {
        // Instantiate the facematch operations manager
        if (fmManager != null)          //  already running
        {  
             int status = fmManager.getStatus();
             if (status == 1)
             {
                log.info("FaceMatch system initialized already" );
             }
            else
            {
                String[] errorCodes = fmManager.getError();
                log.error("FaceMatch System could not be started, error code: " + errorCodes[0] +
                    ", error message:" + errorCodes[1]);
            }
             return;
        }
        
        /*----------------------------------------------------------------------------------------------*/
        // FaceMatch manager not running. Start it to initialize various high level control  objects
        /*------------------------------------------------------------------------------------------------*/
        fmManager=   FMServiceManager.createServiceManager(serverConfigFile);    
        int status = fmManager.getStatus();
        if (status == 1)
        {
            log.info(
                   "\n*******************************************************************************************"
                + "\n             FACEMATCH2  WEB SERVER  STARTED UP SUCCESSFULLY  AT " + Calendar.getInstance().getTime()
                + "\n             Using configuration file: " + webConfigFile 
                + "\n******************************************************************************************" );
           
        }
        else
        {
            String[] errorInfo = fmManager.getError();
            log.error(">>> FaceMatch System could not be started, error code: " + errorInfo[0] +
                ", error message:" + errorInfo[1] +
                "\n Using dsta in Server configuration files" + serverConfigFile );
            return; 
        }
    }       

    /*****************************************************************************/
    /* Process the general user requests related to the FM2 System status and options
    * Method inherited from the super class
    * @param request - User request handed down by the Servlet container (Tomcat)
    * @param response - Properly built resopnse to the caller after processing the request
    ******************************************************************************/
    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        
       // set all incoming encoding to UTF-8
        request.setCharacterEncoding("UTF-8");
        
        /**
         * First process the general user queries for version and GPU status
         */
        
        String servletPath = request.getServletPath();
        log.trace("FM2ServletPath: " + (servletPath == null ? "null" : servletPath) );
        
        ServerStatusResult serviceResult;
        String infoReq = servletPath.substring(1);      // discard the "/"
        
          // Check and return the general FM2 server operational status if requested
        if (infoReq == null || infoReq.equals("") )
            infoReq = "status";         // default
        if (infoReq.equalsIgnoreCase("status"))            // FM2 system status
        {
           serviceResult = fmManager.getServerStatusInfo();
        }  
        
        // Return the GPU status if required
        else  if (infoReq.equalsIgnoreCase("gpustatus"))
        {
           serviceResult = new ServerStatusResult(INFO_SVC, GET_GPU_STATUS);
           serviceResult.gpuStatus = fmManager.isGpuAvailable();
        }
         // Return if the Server monitoring  operation performance and recording it in the database
        else  if (infoReq.equalsIgnoreCase("perfmon"))
        {
           serviceResult = fmManager.getServerStatusInfo();
           serviceResult.operation = ServiceConstants.GET_PERF_MON;
        }
        else
        {
              serviceResult = new ServerStatusResult(INFO_SVC, INVALID_OPERATION, 0,  "Unknown Request");
        }  
        response.setIntHeader("Status", 1);
        response.setStatus(HttpURLConnection.HTTP_OK);
        
        // build a formatted response for the user ans send it
        buildNSendResponseToClient( request, response, serviceResult);
        return;
    }
}
   

        
        
    