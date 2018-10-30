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
 * <p>
 * FaceMatch2 System Admin  servlet. 
 * <p>
 * This service may be invoked by the WebFM2 system administrator to turn on/off
 *  certain system features (such as performance recording)  or hardware/software 
 *  options.
 *
 * It is not available to a client application for any Face match operation requests.
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
import fmservice.server.result.FMServiceResult;

//import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.util.HashMap;


/*****************************************************************************************/

//@WebServlet("/facematch")
public class FaceMatchAdminServlet extends FaceMatchBaseServlet 
        implements ServiceConstants
{    
    /** log4j category */
    private static Logger log = Logger.getLogger(FaceMatchAdminServlet.class);

  

 /*-----------------------------------------------------------------------------------------------------------------*/
  /** 
   * Process FaceMatch admin requests, with the path in URL as "admin".
    * Note: The administrator  username and password must be provided as parameters.
    *  URL format example:
    *      http://fmhost:8080/WebFM2/admin/shutdown?user=fmadmin&password=adminpassword
    * or 
    *      http://fmhost:8080/WebFM2/admin/gpuon?user=fmadmin&password=adminpassword
    * NOTE: This service is also used to add a new client to the FaceMatch2 system without 
    * shutting down/restarting the system
    * 
    * Note: Most of these requests (except ADD_FMCLIENT) are to be implemented/tested ...dm
     *-----------------------------------------------------------------------------------------------------------*/
    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        // set all incoming encoding to UTF-8
        request.setCharacterEncoding("UTF-8");              // the relative url for the named servlet
        String servletPath = request.getServletPath();
        log.trace("FM2ServletPath: " + (servletPath == null ? "null" : servletPath) );
        
        // All requests must have username and password specified
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        if (user == null || user.isEmpty())
        {
            log.trace("No  username");
            setErrorHeaders(response, MISSING_PARAM, "No  username");
            return;
        }
        else if (password == null || password.isEmpty())
        {
            log.trace("No  password");
              setErrorHeaders(response, MISSING_PARAM, "No  password");
            return;
        }
        else
        {
           if ( verifyAdmin(user, password) == false)
           {
                log.trace("Invalid  admin name or password");
                  setErrorHeaders(response,INVALID_USER_ID,  "Invalid  admin name or password");
                return;
           }
        }      
        
        // get the  path in the url request  (last segment)
       // String urlPattern = request.getContextPath();

        int service = ADMIN_SVC;
        int operation;
        String Success = "Operation successful";
        String Fail = "Operation failed";
       
        
        String opsName = "";
        FMServiceResult result;
        
        if (servletPath != null)
        {
            String[]  comps =  servletPath.split("/");     
            opsName = comps[comps.length-1];            // the last component of path 
        }

        if  (opsName.equalsIgnoreCase("shutdown"))
        {
             operation = FM_SHUTDOWN;
             result = closeServiceManager();
        }
        else if  (opsName.equalsIgnoreCase("gpuon"))
         {
             result =  setGPUForFaceMatchOps(GPU_ON); 
         }
          else if (opsName.equalsIgnoreCase("gpuoff"))
          {
              result =  setGPUForFaceMatchOps(GPU_OFF); 
         }  // end gpu
          
        else if  (opsName.equalsIgnoreCase("addclient"))
        {
            operation = ADD_FMCLIENT;
            result =addFMClient(request);
        }
        
         else if (opsName.equalsIgnoreCase("perfrec"))
        {
            // get the parameters, if none specified turn on all recording
             operation = RECORD_PERF;
             boolean record = true;
             String recording = request.getParameter("record");
             if (recording != null && recording.equals("off"))
                 record = false;
             String fmOps = request.getParameter("ops");        // null means all operation types
             result  = recordPerformance(record, fmOps);
        }
        else                // We should not be coming here from web.xml
         {
             log.trace(servletPath + ": no such request implemented");
             response.setIntHeader("Status", 0);
             setErrorHeaders(response,INVALID_OPERATION,  servletPath +": no such request implemented");
             return;
         }
       
        int status = result.getStatusCode();
        String msg = result.getStatusMessage();
        response.setIntHeader("Status", status);
        response.setStatus(HttpURLConnection.HTTP_OK);
        buildNSendResponseToClient(request, response, result);
     }

     /*--------------------------------------------------------------------------------------------------*/
    // Close the FM2ServiceManager (backend server)
    // including freeing the database connection etc.
    /*--------------------------------------------------------------------------------------------------*/
    protected FMServiceResult  closeServiceManager()
    {
       FMServiceManager fmManager = FMServiceManager.getFMServiceManager();
        if ( fmManager == null)
        {
              return   (new FMServiceResult(ADMIN_SVC, FM_SHUTDOWN,  FAILURE, 
                      "FaceMatch operations stopped  already"));
        }
        
        fmManager.close();                          // 
        return   (new FMServiceResult(ADMIN_SVC, FM_SHUTDOWN, SUCCESS, 
                      "FaceMatch systen stopped fot further operations."));
    } 

    /*--------------------------------------------------------------------------------------------------*/
    // Close down fm operation in a systematic way. 
    // including freeing the database connection etc.
    /*--------------------------------------------------------------------------------------------------*/
    private  FMServiceResult addFMClient(HttpServletRequest request)
    {
       FMServiceManager fmManager = FMServiceManager.getFMServiceManager();
        if ( fmManager == null)
        {
            log.warn("FaceMatch operations stopped  already. Should restart it first" );
            return   (new FMServiceResult(ADMIN_SVC, ADD_FMCLIENT,   -1, "FaceMatch operations stopped  already"));
        }
        HashMap <String, Object> inputParams = new HashMap();
        inputParams.put(CLIENT_NAME_PARAM,  request.getParameter(CLIENT_NAME_PARAM));
        inputParams.put(CLIENT_INFO,  request.getParameter(CLIENT_INFO));
        
        FMServiceResult  result = fmManager.performService(ADMIN_SVC, ADD_FMCLIENT, inputParams);
        return result;
    }
    /*--------------------------------------------------------------------------------------------------*/
    //  Turn the GPU on or off  for Facematch operations(for testing)
    /*--------------------------------------------------------------------------------------------------*/
     protected FMServiceResult  setGPUForFaceMatchOps( int operation)
     {
         FMServiceManager fmManager = FMServiceManager.getFMServiceManager();
        if ( fmManager == null)
        {
            log.warn("FaceMatch operations stopped already. Should restart it first" );
            return   (new FMServiceResult(ADMIN_SVC, ADD_FMCLIENT,   -1, "FaceMatch operations stopped  already"));
        }
         FMServiceResult  result = fmManager.performService(ADMIN_SVC, operation,null);
          return result;
     }
     
     /*--------------------------------------------------------------------------------------------------*/
    //  Turn the performance on or off  for Facematch operations(for testing)
    /*--------------------------------------------------------------------------------------------------*/
     private FMServiceResult  recordPerformance(boolean record, String fmOps)
     {
         FMServiceManager fmManager = FMServiceManager.getFMServiceManager();
        if ( fmManager == null)
        {
            return   (new FMServiceResult(ADMIN_SVC, RECORD_PERF,  FAILURE, 
                      "FaceMatch operations stopped  already. Should restart it first"));
        }
          fmManager.recordPerformance(record, fmOps);
          return new FMServiceResult(ADMIN_SVC, RECORD_PERF,  SUCCESS,
                      "Performance recording enabled fpr FaceMatch operations");
     }
    /*----------------------------------------------------------------------------------------------*/
    // Verify the validity of the Admin data
    /*----------------------------------------------------------------------------------------------*/
    private boolean  verifyAdmin(String name, String password)
    {
        FMServiceManager fmManager = FMServiceManager.getFMServiceManager();
        if ( fmManager == null)
        {
            log.warn("FaceMatch operations stopped already. Cannot perform operation" );
            return false;
        }
        return fmManager.verifyAdmin(name, password);
    }
}

        
        
    