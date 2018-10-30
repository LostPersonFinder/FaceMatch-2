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

// import fmservice.webapp.serverutils.ServletIO;  -- not used
import fmservice.httputils.common.ServiceConstants;
import fmservice.server.result.FMServiceResult;
import fmservice.server.util.Timer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import java.net.HttpURLConnection;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * Base class for  all FaceMatch servlets, provides common functions.  
 *
 * @version $Revision: 1.10
 * 
 * Author:
 * 
 */
public class FaceMatchBaseServlet extends HttpServlet 
{    
    /** log4j category */
    private static Logger log = Logger.getLogger(FaceMatchBaseServlet.class);
    
    Timer serviceTimer;

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
             log.info("---------------------------------------------------------------------");
           // printRequestParameters(request);        // for debugging only
            serviceTimer = new Timer();
            processRequest(request, response);
        }
        catch(Exception e)
        {
           e.printStackTrace();
        }
    }
    
  //----------------------------------------------------------------------------------------------------------------------------- 
  /** 
   * Print the contents of the service request to the standard output.
  * This method should be used for debugging purpose.
  /*----------------------------------------------------------------------------------------------------------------------------*/  
    protected void printRequestParameters(HttpServletRequest request) throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");              // the relative url for the named servlet
        String servletPath = request.getServletPath();

      log.trace("FM2ServletPath(): " + (servletPath == null ? "null" : servletPath) );
        log.trace("Method(): " + request.getMethod());
        log.trace("RequestString: " + request.getQueryString());
        log.trace("RequestURL() : " + request.getRequestURL());
        log.trace("RequestURI() : " + request.getRequestURI());
        
        String userAgent = request.getHeader("User-Agent");
        log.trace("UserAgent():  " + ( userAgent == null ?  "" : userAgent));
        HashMap paramMap = (HashMap)request.getParameterMap();
  /*      if (paramMap != null)
        {
            Iterator <String>it = paramMap.keySet().iterator();
            while (it.hasNext())
            {
                String param = it.next();
                log.trace("Parameter: " + param +"= " + (String)paramMap.get(param));
            }   
        }*/
        log.trace("------------");
    }

    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
         doGet(request, response);
    }
    

    /*******************************************************************************/
     /**
      * Process a client's service request, performed in the derived classes
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
     protected void processRequest( HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
     {
         //  implemented  by derived classes
     }
  
    /******************************************************************************/
    /** 
     * Build a HTTPResponse object with the result of the service  and send it
     *  to the client.
    * @param request - HTTP service request from the client
    * @param response - formatted HTTP response to be sent to the client
    * @param result - The service result object corresponding to a specific service request
    *---------------------------------------------------------------------------------------------------------------------*/
     
    protected void buildNSendResponseToClient(HttpServletRequest request, HttpServletResponse response,
       FMServiceResult result)
    {
        try
         {
            int statusCode;
            String reasonPhrase;
            if (result == null)           // in case a null object sent by caller
            {
                log.error("Received null ResponseMessage from Servlet.");
                statusCode = ServiceConstants.INTERNAL_SERVER_ERROR;
                reasonPhrase = "Internal server error in processing user request";
                setErrorHeaders(response, statusCode, reasonPhrase);
                return;
             }

            // request successful, format and send results
            if (isInteractive(request))
               buildNSendInteractiveResponse(request,  response, result);
            else
                buildNSendWSAppResponse(response, result);
            log.info(">>> Successfully sent response to Client: " +  result.convertToJSONString());
        }
        catch(IOException ioe)
        {
            log.error("Error sending  response to the client", ioe);
            log.error("ResponseMessage was: " + result.convertToJSONString() );
        }
        return;
    }    
    
    /*---------------------------------------------------------------------------------------------------------*/
    /** 
     * Check if the request comes from a Web browser  rather than a client application.
    * Note: The two types of requests come in two different servlet paths in web.xml 
    *----------------------------------------------------------------------------------------------------------*/
    protected boolean isInteractive(HttpServletRequest request)
    { 
        String servletPath = request.getServletPath();
       if (servletPath.startsWith("/ir/"))
           return true;
       
      String userAgent = request.getHeader("User-Agent");
      if (userAgent != null && 
                 (userAgent.contains("Mozilla") || userAgent.contains("MSIE")) )        // covers firefox, ie, chrome, safari
          return true;     
      return false;
    }
    
    //-----------------------------------------------------------------------------------------
    /** Send response to a non-interactive  Webservice client which sent the  
    * request using the  xxx servlet path. This is assumed to be a non-interactive
    * client, for which the result is streamed as a JSON string.
    *----------------------------------------------------------------------------------------*/
    protected void buildNSendWSAppResponse(HttpServletResponse response,
        FMServiceResult result)
    {
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpURLConnection.HTTP_OK);   
       
        response.setIntHeader(ServiceConstants.STATUS_CODE, result.getStatusCode());
        response.setHeader(ServiceConstants.STATUS_MESSAGE, result.getStatusMessage());

        try
        {
             // write resultto the Servlet Output Stream as a text stream, since it is written as a JSON string
            PrintWriter printWriter = response.getWriter();
            printWriter.write(result.convertToJSONString());
            printWriter.flush();
            printWriter.close();
            log.info("Successfully wrote data to HttpResponse for client" );
          }
          catch (Exception e)
          {
            setErrorHeaders(response, ServiceConstants.INTERNAL_SERVER_ERROR,e.getMessage());
            log.error("Could not write output data  to HttpResponse for client",  e);
          }
        return;
    }
     //-----------------------------------------------------------------------------------------
    /** 
     * Send response to an interactive user such as a Web Browser
     * which sent the request using the  /ws/xxx servlet path. 
     * The output result is formatted as an HTML document for interactive display.
     * <p>
     * Note: Only a  few service requests are currently supported interactively
     * Mainly status checking request is currently implemented
    *----------------------------------------------------------------------------------------*/
    
    protected void buildNSendInteractiveResponse(HttpServletRequest request, 
        HttpServletResponse response,  FMServiceResult result)
        throws IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpURLConnection.HTTP_OK);   
       
        response.setIntHeader(ServiceConstants.STATUS_CODE, result.getStatusCode());
        response.setHeader(ServiceConstants.STATUS_MESSAGE, result.getStatusMessage());
      
        
         PrintWriter out = null;
        /* TODO output your page here. You may use following sample code. */
        try
        {
            out = response.getWriter();
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FaceMatch2 Service Result</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h2>" +  request.getContextPath() + " -- Service Result </h2>");
            
            // add the parameters
            String query  = request.getQueryString();
            String str = "";
            if (query != null && query.length() > 0) str += "Query: "+  query;      
           
            // Add the operation name
            String operation  = "status";          // default
            String path  = request.getServletPath();  // uri without query String
            if (path != null && path.length()==0 )
            {
                String[]  parts = path.split("/");
                operation = parts[parts.length-1];
            }
            String msg = "" ;
            
            if (str.length() > 0)
                msg = str +"<br>    ";
           
            String  parmStr =  result.convertToJSONString();
            String formattedValueStr =convertToFormattedStr(parmStr);
            msg +=  formattedValueStr;
            log.trace(msg);
            out.println("<h4>" + "Request:  " + operation + "- " +  result.operation);
            if (msg != null && msg.length() > 0)
                 out.println("<br>" + msg);
            out.println("</h4>");
            out.println("</body>");
            out.println("</html>");
        } 
        catch (Exception e)
        {
             log.error( "Error sending formatted response to client", e);
        }
            
        finally
        {
            if (out != null)
                out.close();
        }
    }
    /*-------------------------------------------------------------------------------------------------*/
    /** 
     * Format the return <key, value> pair JSON string into a user readable 
     *  key : value form for interactive user
     * @param jsonStr - input  JSON string  
     * @return  string in (key : value) form
     */
     protected String convertToFormattedStr(String jsonStr)
    {
      JSONParser parser = new JSONParser();
      String formattedMsg = "";
       try
       {
            JSONObject retObj = (JSONObject)parser.parse(jsonStr);
            Iterator <String> it = retObj.keySet().iterator();
            while (it.hasNext())
            {
                String key = it.next();
                String value =  retObj.get(key).toString();
                formattedMsg+=  ( key +":  " + value +"<br>");
            }
       }
       catch (ParseException pe)
       {
           log.error("Invalid JSON format for server return result : \n" + jsonStr);
           formattedMsg = jsonStr;
       }
       return formattedMsg;
    }

/*-------------------------------------------------------------------------------------------------------*/
 /**
 * Set the error related headers in the Response to be sent to the client, since 
 * SendError() is pre-empted by Tomcat.
 * @param  response -  server response to be formatted with error status
 * @param errorCode - error code returned by the FM2 server
 * @param error message - corresponding error message
 *---------------------------------------------------------------------------------------------------------*/
    protected void setErrorHeaders(HttpServletResponse response,
        int errorCode, String errorMessage)
    {
           response.setIntHeader(ServiceConstants.STATUS_CODE, errorCode);
            response.setHeader(ServiceConstants.STATUS_MESSAGE,  errorMessage);
    }
    

   /*---------------------------------------------------------------------------------------------------------*/
   /** 
    * Retrieve parameters provided in the Web service request and build a Map.
    * @param request - HTTP Service request from the client
    * @return a HashMap of parameter name and values(s)
    *--------------------------------------------------------------------------------------------------------*/
     protected HashMap <String, String[]> getRequestParameters( HttpServletRequest request)
     {
           HashMap params = new HashMap();
            for ( Enumeration e = request.getParameterNames(); e.hasMoreElements() ; )
            {
                String name = (String)e.nextElement();  
                String[] val = request.getParameterValues(name);
                if (val == null || val.length == 0)
                {
                    log.error("Value not provided for parameter : " + name);
                    continue;
                }
                if (val.length == 1)
                {
                    params.put(name, val[0]);
                }
                else
                {
                    params.put(name, val);
                }
            }  
           return params;
     }
     
    /*--------------------------------------------------------------------------------------------------------*/
     /**
     * Retrieve headers provided in the Web service  request. 
     * Assume each header is single valued
     *--------------------------------------------------------------------------------------------------------*/
     protected HashMap getRequestHeaders( HttpServletRequest request)
     {
           HashMap headers = new HashMap();
            for (Enumeration e =  request.getHeaderNames(); e.hasMoreElements() ; )
            {
                String name = (String)e.nextElement();  
                String val = request.getHeader(name);
                if (val == null || val.length() == 0)
                {
                    log.error("Value not provided forheader : " + name);
                    continue;
                }
                log.debug( name+ " =  " + val);
                headers.put(name, val);
            } 
           return headers;
     }
}        
        
        
    
