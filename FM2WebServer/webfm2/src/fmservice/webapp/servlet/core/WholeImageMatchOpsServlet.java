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

import fmservice.webapp.servlet.FaceMatchBaseServlet;

import fmservice.server.ops.FMServiceBroker;
import fmservice.httputils.common.ServiceUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fmservice.httputils.common.ServiceConstants;
import fmservice.server.result.FMServiceResult;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;


/**
 * <P> WholeImageMatcherServlet processes an  request perform Image Matching 
 * services such as Ingest and Query.
*
* >>> Note: Actual lower level functions for this service are not yet implemented. <<<
 * 
 * @version $Revision: 1.1
 * 
 * Author:
 * 
 */
public class WholeImageMatchOpsServlet extends FaceMatchBaseServlet
{    
    /** log4j category */
    private static Logger log = Logger.getLogger(FaceQueryOpsServlet.class);

    // implemts the following service type
     public static int serviceType = ServiceConstants.FACE_MATCH_REGION_SVC;
      
    /*****************************************************************************/
    /** Process an incoming WholeImageMatcher request. The request is the last
     * component of the url-pattern, and one of: 
     *       ingest, query, remove and deleteEvent.
     * 
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException 
     */
    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {

        // set all incoming encoding to UTF-8
        request.setCharacterEncoding("UTF-8");
        
        String servletPath = request.getServletPath();
        int operationType = 0;
         
        FMServiceBroker serviceBroker = new FMServiceBroker();
        if (servletPath != null)
        {
            String[]  comps =  servletPath.split("/");     
            String operation = comps[comps.length-1];            // the last component of path 
            operationType = ServiceUtils.getOperationType(serviceType, operation);
        }
         log.trace("FM2ServletPath: " + (servletPath == null ? "null" : servletPath) +
             ", operation=  " + operationType);
        HashMap<String, String[]> requestParams = getRequestParameters(request);
       
        // Perform the operation by going through the Adaptor
         FMServiceResult result  = serviceBroker.processServiceRequest(
             serviceType, operationType, requestParams);

        FMServiceResult  serviceResult  = serviceBroker.processServiceRequest(
             serviceType, operationType, requestParams);
        
        buildNSendResponseToClient(request, response, serviceResult);
    }
}        

