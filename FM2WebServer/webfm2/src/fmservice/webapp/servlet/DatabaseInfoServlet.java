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
 * FaceMatch Database Info Servlet.
 * 
 * Provides various database related information to the HTTP client
 * 
 * <p>
 *  @author 
 *  @version $Revision: 1.0 $
 *
 * Change log:
 * 
 */
import fmservice.server.entry.FMServiceManager;
import fmservice.httputils.common.ServiceConstants;

import fmservice.server.ops.FMDataManager;
import fmservice.server.storage.index.IndexStoreManager;
import fmservice.server.cache.ImageCacheManager;
import fmservice.server.global.Scope;

import fmservice.server.ops.FMServiceBroker;
import fmservice.server.result.FMServiceResult;
import fmservice.server.util.Timer;
import static fmservice.webapp.servlet.core.FaceIngestOpsServlet.serviceType;

//import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.ArrayList;


/*****************************************************************************************/

//@WebServlet("/facematch")
public class DatabaseInfoServlet extends FaceMatchBaseServlet 
        implements ServiceConstants
{    
    /** log4j category */
    private static Logger log = Logger.getLogger(FaceMatchAdminServlet.class);
    
    protected static int service = ServiceConstants.DATABASE_QUERY_SVC;

    /*****************************************************************************/

    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        Timer startTimer = new Timer();
        
        // set all incoming encoding to UTF-8
        request.setCharacterEncoding("UTF-8");              // the relative url for the named servlet
        String servletPath = request.getServletPath();
        log.trace("FM2ServletPath: " + (servletPath == null ? "null" : servletPath) );
        
        // check if it is a request for info about all clients, which is an admin function
        FMServiceResult serviceResult;
        if (servletPath.equalsIgnoreCase("/info/clients"))
        {
             serviceResult = processAllClientInfoRequest(request);
        }
        else
        {
            serviceResult = processDBQueryReqest(request);
        } 
        response.setIntHeader("Status", serviceResult.getStatus().statusCode);
        response.setStatus(HttpURLConnection.HTTP_OK);
        buildNSendResponseToClient(request, response, serviceResult);
    }
    
  
    //---------------------------------------------------------------------------------------------------
    // Get various  information related to clients currently stored in the database
    // Return param: FMServiceResult:
    //          (int svcType, int operType, int statusCode, String statusMsg)
    //---------------------------------------------------------------------------------------------------
    
    protected FMServiceResult processAllClientInfoRequest(HttpServletRequest request)
    {  
        // check if it is a request for info about all clients, which is an admin function
        FMServiceResult result = new FMServiceResult(DATABASE_QUERY_SVC, ALL_CLIENT_QUERY_OP);

        String user = request.getParameter("user");
        String password = request.getParameter("password");
        if (user == null || user.isEmpty())
        {
              result.setStatus( MISSING_PARAM, "No  username");
              return result;
        }
         if (password == null || password.isEmpty())
        {
              result.setStatus( MISSING_PARAM, "No  password");
              return result;
        }
        if (verifyAdmin(user, password) == false)
        {
           result.setStatus(INVALID_USER_ID,  "Invalid  admin name or password");
           return result;
        }
         
        FMServiceBroker serviceBroker = new FMServiceBroker();
        HashMap<String, String[]> requestParams = getRequestParameters(request);
        FMServiceResult  serviceResult  = serviceBroker.processServiceRequest(
                serviceType, ALL_CLIENT_QUERY_OP, requestParams);
        return serviceResult;
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
    
    /*--------------------------------------------------------------------------------------------------*/
    protected FMServiceResult  processDBQueryReqest(HttpServletRequest request)
    {
        // operation type is determined by the request parameters
        int operationType;
        String imageTag = request.getParameter("image") ;
        String extentName = request.getParameter("extent") ;
        String clientName = request.getParameter("client") ;
       
        if (imageTag != null && !imageTag.isEmpty())
            operationType = IMAGE_QUERY_OP;
        else if (extentName != null && !extentName.isEmpty())
            operationType = EXTENT_QUERY_OP;
        else if (clientName != null && !clientName.isEmpty())
             operationType = CLIENT_QUERY_OP;
        else
        {
            FMServiceResult result = new FMServiceResult(DATABASE_QUERY_SVC, 
                    ServiceConstants.INVALID_OPERATION, -1, "Missing parameters");
            return result;
        }
        
        FMServiceBroker serviceBroker = new FMServiceBroker();
        HashMap<String, String[]> requestParams = getRequestParameters(request);
        FMServiceResult  serviceResult  = serviceBroker.processServiceRequest(
                serviceType, operationType, requestParams);
        return serviceResult;
    }
}

        
        
    