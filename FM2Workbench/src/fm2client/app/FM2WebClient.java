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

package fm2client.app;

import fm2client.core.FM2RequestAgent;
import fmservice.httputils.client.ClientRequest;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;


import fm2client.util.PropertyLoader;
import fm2client.util.Utils;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.HashMap;
import java.util.Iterator;


public class FM2WebClient
{
    private static Logger log = Logger.getLogger(FM2WebClient.class);
    private static String OS_NAME = System.getProperty("os.name").toLowerCase();
    
    public static int HTTP_OK = ClientRequest.HTTP_OK;

    protected  String fm2ServerURL = "";           // FaceMatch WebService provider URL
    protected String faceWebServiceURL = "";       // for Web applications (vs. browsers)
    protected  String imageWebServiceURL = "";
    
    protected   FM2RequestAgent  webServiceRequestor = null;
    protected  static boolean  validConnection = false;
    
   protected  Properties  clientProperties;
   protected  Properties  testProperties;
   protected HashMap <String, String[]>client2extentMap = null;
   
    protected static JSONParser jparser = new JSONParser();

    protected boolean debug = true;

    int numIngest = 0;
   
    /*---------------------------------------------------------------------------------------------------------------------------*/
    // Get the name of the default configuration file.
    // Must be located in a directory defined by the system proprty -Dfm2client.dir
    // with the path name config/fm2client.cfg
    /*---------------------------------------------------------------------------------------------------------------------------*/
    public static String getDefaultConfigFile()
    {
        String configFile = System.getProperty("fm2client.config");
        if (configFile == null)
            log.fatal("Please specify Client configuration  using the property -Dfm2client.config ");
        return configFile;
    }
    /*----------------------------------------------------------------------------------------------*/
    
  protected  FM2WebClient (String clientConfigFile, boolean realtime) throws Exception
    {
        this( PropertyLoader.loadProperties(clientConfigFile), realtime);
    }
  
  /*------------------------------------------------------------------------------------------------------*/        
  protected  FM2WebClient (Properties opsProperties, boolean realtime) throws Exception
    {
        clientProperties = opsProperties;
        testProperties = clientProperties;
        if (opsProperties == null)
        {
             log.fatal (">>> Exiting due to error in loading configuration data.");
             return;
        }
        String log4jFileName = Utils. initLogging(clientProperties);
        System.out.println("*** Writing results to Log file: " + log4jFileName);
        
        if (realtime)                       // regular realtime operational mode   
        {
                int status = initClient();
                if (status <= 0)
                    return;
        }
    }
    //-------------------------------------------------------------------------
    // Initialize the client application for communication with the server
    //--------------------------------------------------------------------------
    protected int  initClient()
    {
        fm2ServerURL = clientProperties.getProperty("fm2ServerURL");
        if (fm2ServerURL == null)
        {
            log.fatal(">>> Could not find  property \"fm2ServerURL\" defined in the configuration file.");
            return 0;
        }   
            
        log.info(">>> Connecting to FaceMatch Server: " + fm2ServerURL +"<<<");
        
        webServiceRequestor = new FM2RequestAgent(fm2ServerURL);
        validConnection = webServiceRequestor.isValidConnection();
        return 1;
    }
    
 
    
    
    /**----------------------------------------------------------------------------------------------------------
    // Make sure that the server is running on the at the given location in the URL
   //------------------------------------------------------------------------------------------------------------*/
    public boolean isValidConnection()
    {
        return validConnection;
    }
   /*--------------------------------------------------------------------------------------------------------*/
    public static boolean isValidResult(String serviceResult)
    {
        try
        {
            JSONObject resultObject = (JSONObject) jparser.parse(serviceResult);
            int statusCode = ( (Long)resultObject.get("statusCode")).intValue();
            return (statusCode == 1) ? true : false;
          /*  else
            {
                log.error("Invalid request: status code " +  statusCode  +", status message: " + resultObject.get("statusMessage") );
                 return false; 
            }*/
        }  
        catch (Exception e)
        {
            log.error ("Invalid JSON record received from server", e);
            return false;
        }
    }
   /*---------------------------------------------------------------------------------------------------------------*/   
   // Note: This is valid only as long as the extent  names are unique
    public String getClientName(String extent)
    {
        if (client2extentMap == null)
           client2extentMap =  buildClient2ExtentMap();
       
        Iterator<String> it = client2extentMap.keySet().iterator();
        while(it.hasNext())
        {
            String client = it.next();
            String[]  extents = client2extentMap.get(client);
            if ( extents[0].equals("*"))
                return client;
            for (int i = 0; i < extents.length; i++)
            {
                if (extents[i].equalsIgnoreCase(extent))
                    return client;
            }
        }
        return null;
    }
 /*--------------------------------------------------------------------------------------------------------*/                   
            
    protected  HashMap <String, String[]> buildClient2ExtentMap()
    {
        HashMap <String, String[]>client2collection = new HashMap();
        String clientNameList = testProperties.getProperty("fm2test.clients");
        if (clientNameList == null)
                return null;
        String[] clients  = clientNameList.split(",");
        for (int i = 0; i < clients.length; i++)
        {
            String client = clients[i].replace("^\\s+", "").trim();
            String extentList = testProperties.getProperty(client+".extents");
            if (extentList != null)
            {
                String[] extents  = extentList.split(",");
                for (int j = 0; j < extents.length; j++)
                    extents[j] =  extents[j].replace("^\\s+", "").trim();
                client2collection.put(client, extents);
            }
            else
                client2collection.put(client, new String[]{"*"});
        }
        return client2collection;             
    }
  
/*-------------------------------------------------------------------------------------------
// Check if the operation is requested by a FM2 System Administrator
/*-------------------------------------------------------------------------------------------
*/
  protected  boolean isAdmin()
  {
      return (getAdminPW() != null);
  }
  /*-----------------------------------------------------------------------------------------------------*/
   protected  String  getAdminPW()
  {
        String pw = null;
        boolean admin =clientProperties.get("isAdmin").equals("true");
        if (admin)
        {
            pw =  (String) clientProperties.get("fm2Admin.password");
        }
        return pw;
   }   
}

        