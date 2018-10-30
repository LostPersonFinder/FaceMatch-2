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
package fmservice.webapp.servlet.core;

/**
 * <p>
 * FaceMatch system Image Extent request servlet. 
 * 
 * This Servlet is responsible for adding/ removing ImageExtents from the system and
 * designating their status as active or inactive at client's request
 * <p>
 *  @author
 *  @version $Revision: 1.0 $
 * @date Feb 19, 2015
 *
 * Change log:
 * 
 */
import fmservice.webapp.servlet.*;

import fmservice.httputils.common.ServiceConstants;
import fmservice.httputils.common.ServiceUtils;
import fmservice.server.ops.FMServiceBroker;
import fmservice.server.result.FMServiceResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import java.io.IOException;

import java.util.HashMap;


/*****************************************************************************************/

//@WebServlet("/facematch")
public class ExtentOpsServlet extends FaceMatchBaseServlet 
        implements ServiceConstants
{    
    /** log4j category */
    private static Logger log = Logger.getLogger(ExtentOpsServlet.class);

    public static int serviceType = ServiceConstants.IMAGE_EXTENT_SVC;

    /*---------------------------------------------------------------------------------------------------------------*/
    /** 
     * Process FaceMatch ImageExtent administration requests, such as adding,
     * removing , and activating/ deactivating an entry upon client request.
     * <p>
     *  URL format example:
     *      http://fmhost:8080/webfm2/ext/add?key=xxx&name=Nepal&description="Nepal EarthQuake of 2015"
     * or 
     *     http://fmhost:8080/webfm2/ext/deactivate?key=xxx&name=Nepal
     * 
     * @param request - HTTP service request from the client
     * @param response - formatted HTTP response from the operation to be sent to the client
     *---------------------------------------------------------------------------------------------------------------*/
    
    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        // set all incoming encoding to UTF-8
        request.setCharacterEncoding("UTF-8");              // the relative url for the named servlet
        String servletPath = request.getServletPath().toLowerCase();
        log.trace("FM2ServletPath: " + (servletPath == null ? "null" : servletPath) );
        
        String operation = "";
        int operationType = -1;
         if (servletPath != null)
        {
            String[]  comps =  servletPath.split("/");     
            operation = comps[comps.length-1];            // the last component of path 
            operationType = ServiceUtils.getOperationType(serviceType, operation);
        }
        log.trace("FM2ServletPath: " + (servletPath == null ? "null" : servletPath) +
             ", operation=  " + operationType);
        
        operation = operation.replaceAll("\\W+", "");       // ignore all white space
        boolean validOp = operation.equals(ADD_EXTENT)||operation.equals(REMOVE_EXTENT)
                ||operation.equals(ACTIVATE_EXTENT)||operation.equals(DEACTIVATE_EXTENT)
                ||operation.equals(SET_PERFORMANCE_PREF);
            
        if (!validOp)
        {
            log.trace("Invalid operation: " + servletPath);
            setErrorHeaders(response, INVALID_OPERATION, "Invalid operation" + servletPath);
            return;
        }
        
        // Perform the operation
        FMServiceBroker serviceBroker = new FMServiceBroker();
        log.trace("FM2ServletPath: " + (servletPath == null ? "null" : servletPath)
            + ", operation=  " + operationType);
        
        HashMap<String, String[]> requestParams = getRequestParameters(request);

        // Perform the operation by going through the Adaptor
        FMServiceResult serviceResult = serviceBroker.processServiceRequest(
            serviceType, operationType, requestParams);
        
        buildNSendResponseToClient(request, response, serviceResult);
        return;
    }
}

        
        
    