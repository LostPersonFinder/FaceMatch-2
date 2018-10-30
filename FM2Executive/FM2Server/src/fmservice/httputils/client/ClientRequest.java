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
package fmservice.httputils.client;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.entity.UrlEncodedFormEntity;

import fmservice.httputils.common.ServiceConstants;
import fmservice.httputils.common.FormatUtils;

import org.apache.http.HttpResponse;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.net.UnknownHostException;
import java.io.IOException;
import java.io.FileInputStream;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
/**
 *
 * Create an HTTP Client and send a user supplied HTTP request to the server
 *
 */
public class ClientRequest
{
   public static int HTTP_OK = 200;
   
   private static Logger log = Logger.getLogger(ClientRequest.class);
    
    public static  HttpResponse  sendGetRequest(String serverURL, String service, HashMap <String, Object> params)
    {
        if (!FormatUtils.isValidURL (serverURL))
        {
            log.error("Invalid server URL: "+ serverURL );
            return null;
        }
        
        List< NameValuePair> reqParamList  = null;
        if (params != null && !params.isEmpty())
            reqParamList = convertMap2NVPair(params);

       CloseableHttpClient httpClient = HttpClientBuilder.create().build();
       String fullRequestURL = FormatUtils.encodeURLRequest(serverURL, service, reqParamList);
      log.info("Full request URL: \n" + fullRequestURL);

       HttpGet getRequest = new HttpGet(fullRequestURL);
        try
        {
             // send the request to the server and ask for the response (so the Server would execute the request)
             HttpResponse response = httpClient.execute(getRequest);
             //return  response; //decodeGetResponse(response);
           
            log.trace("Response Code : "  + response.getStatusLine().getStatusCode());
            return response;
        }
        catch (UnknownHostException e)          // note: should be caught before IOException
        {
         log.error("caught UnknownHostException performing GET " +getRequest.getURI(), e);
         return null;
        }
        catch (IOException e)
        {
             log.error("caught IO Exception performing GET " +getRequest.getURI());
            return null;
        }     
    }
    
    /*---------------------------------------------------------------------------------------------------------------*/
    // In PUTRequest, data is sent in the message body
    public static  HttpResponse  sendPutRequest(String serverURL, String service, HashMap params)
    {
        if (!FormatUtils.isValidURL (serverURL))
        {
            log.error("Invalid server URL: "+ serverURL );
            return null;
        }
        List< NameValuePair> reqParamList  = null;
        if (params != null && !params.isEmpty())
            reqParamList = convertMap2NVPair(params);
        
       CloseableHttpClient httpClient = HttpClientBuilder.create().build();
      // String fullRequestURL = FormatUtils.encodeURLRequest(serverURL, service, reqParamList);
       HttpPost post = new HttpPost(serverURL);

	// add header
	post.setHeader("User-Agent", "FM2ClientApp");

    try
    {
	post.setEntity(new UrlEncodedFormEntity(reqParamList));
	HttpResponse response = httpClient.execute(post);
           
            log.trace("Response Code : " 
                + response.getStatusLine().getStatusCode());
            return response;
        }
        catch (UnknownHostException e)          // note: should be caught before IOException
        {
         log.error("caught UnknownHostException performing PUT " +serverURL, e);
         return null;
        }
        catch (IOException e)
        {
             log.error("caught IO performing PUT " +serverURL, e);
            return null;
        }     
    }
    
  /*-------------------------------------------------------------------------------------------------------------*/ 
    public static String decodeServerResponse( HttpResponse response )
    {
        try
        {
            BufferedReader rd = new BufferedReader(
	        new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) 
            {
                result.append(line);
            }
                return result.toString();
         }
        catch (UnknownHostException e) 
        {
            log.error("caught UnknownHostException decogong Server response " , e);
            return null;
        }
        catch (IOException e)
        {
            log.error("caught IOException performing decogong Server response  " ,e);
         return null;
        }
    }
    
     /********************************************************************************************
     * Get the operation  status code and  message sent from a Server to  an HTTP client.
     * * Use Channel IO for faster performance
     ********************************************************************************************/
   public static String[]  getOperationStatus(HttpResponse response)
   {
       String[] opStatus = new String[2];
       Header header1 = response.getFirstHeader(ServiceConstants.STATUS_CODE);
       Header header2 =  response.getFirstHeader(ServiceConstants.STATUS_MESSAGE);
       opStatus[0] = (header1 == null) ? " " : header1.getValue();
       opStatus[1] = (header2 == null) ? " " : header2.getValue();
       return opStatus;
   }
   
     /********************************************************************************************
     * Read data from the response message sent from a Server to  an HTTP client.
     * * Use Channel IO for faster performance
     ********************************************************************************************/
    public static byte[] getResponseContents(HttpResponse response)    
    {
        try
        {
          InputStream is = response.getEntity().getContent();
             return readDataFromInputStream(is);
        }     
        catch (IOException ie)
        {
            log.error( "Exception in reading server data in  HTTPResponse" , ie);
            return null;
        }  
    }
        
   /*-----------------------------------------------------------------------------------------------------*/ 
    public static byte[]  readDataFromInputStream(InputStream is)
    {
        int bytesRead = 0;
        try
        {
             ReadableByteChannel rbc = Channels.newChannel(is);

             // Stream where to store the read data
             ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
             int offset = 0;

             // read in  4k chunks
              int maxSize = ServiceConstants.DATA_CHUNK_SIZE;               
             byte[] byteData = new byte[maxSize];
           
             ByteBuffer destBuffer = ByteBuffer.wrap(byteData);     // read data to this buffer
             while (true)
             {
                  destBuffer.clear();
                  bytesRead =  rbc.read(destBuffer);

                 // log.trace ("Read " + bytesRead + " bytes from input channel.");
                  if (bytesRead <=  0)
                    break;

                  outputBytes.write(byteData, 0, bytesRead);
                  offset += bytesRead;  
               }    
                byte[] dataBuffer = outputBytes.toByteArray();
                return dataBuffer;
        }
        catch (IOException ie)
        {
            log.error( "Exception in reading server  data  after " + bytesRead +" bytes", ie);
            return null;
        }     
    }   
 
   
    /****************************************************************************
   * Read data from the input stream to a buffer. Close at EOF.
   * @param inputStream
   * @param size
   * @return
   * @throws java.io.IOException
   ***************************************************************************/
   protected  static byte[] readFileInput(FileInputStream inStream, int size, boolean atEof)
				throws IOException
   {
        if (inStream == null || atEof) // not opened or done
        {
            return null;
        }

        //Read bytes from the remote file, up to specified size.
        int available = inStream.available();
        int bsize = (available < size) ? available : size;
        byte[] buffer = new byte[bsize];

        int nb = inStream.read(buffer);
        log.trace("- Number of bytes read " + nb);
        if (nb < 0) // EOF or some unexpected error
        {
            atEof = true;
            inStream.close();
            return null;
        }
        return buffer;
    }
   
     protected static List< NameValuePair> convertMap2NVPair(HashMap<String, Object>paramMap)
     {
           if (paramMap == null || paramMap.isEmpty())
               return null;
           
         Iterator <String> it = paramMap.keySet().iterator();
          List < NameValuePair> nvList = new ArrayList();
          while (it.hasNext())
          {
              String name = it.next();
              Object value = paramMap.get(name);
               if (value == null)                // an optional paramater ? 
                  continue;
               
               // If the value is an array, concatenate the values separated by comma for HTTP transfer
               String valueStr = "";
               if (value instanceof String[] )  
               {
                  String[] valueArray  = (String[]) value;
                  valueStr =  valueArray[0];
                   for (int i = 1; i < valueArray.length; i++)
                       valueStr += ",  "+ valueArray[i];
               }
               else if (value instanceof  Integer[])
               {
                   Integer[] valueArray  = (Integer[]) value;
                   valueStr = valueArray[0].toString();
                   for (int i = 1; i < valueArray.length; i++)
                       valueStr += ",  "+ valueArray[i].toString();
               }
               else if (value instanceof  Double[])
               {
                   Double[] valueArray  = (Double[]) value;
                   valueStr = valueArray[0].toString();
                   for (int i = 1; i < valueArray.length; i++)
                       valueStr += ",  "+ valueArray[i].toString();
               }
               else         // a single entiry; not an Array
                    valueStr =  value.toString();
               
              nvList.add(new BasicNameValuePair(name, valueStr));
          }
         log.trace(nvList.toString());
          return nvList;
     }
}
