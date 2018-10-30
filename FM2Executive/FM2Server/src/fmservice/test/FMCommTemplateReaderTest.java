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
package fmservice.test;

import java.io.FileReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Set;
import java.util.Iterator;


/**
 *
 *
 */
public class FMCommTemplateReaderTest
{

    public static  JSONObject parseTemplate(String fmInterfaceTemplate)
    {
        try
        {
            FileReader fileReader = new FileReader(fmInterfaceTemplate);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(fileReader);
            JSONObject commTemplate  = (JSONObject) obj;
            parseCommTemplate(commTemplate);
            return commTemplate;
            //parseCommTemplate(commTemplate);
            
           //String str = commTemplate.toJSONString();
            //System.out.println(str);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
    /*-----------------------------------------------------------------------------------------------------------------*/
    // parse the JSON object and get the individual fields from the encoded data
    /*-----------------------------------------------------------------------------------------------------------------*/
    public static void parseCommTemplate(JSONObject commTemplate)
    {
        String[] adminOps = {"addClient", "removeClient", "useGPU", "shutdown", "reingest"};
        String[] eventOps = {"addEvent", "removeEvent", "setActive"};
        String[] regionOps = {"faceregionOps.getFaces", "faceregionOps.ingest", "faceregionOps.remove", "faceregionOps.match"};
        
       String[][] allOps = {regionOps, adminOps, eventOps};

       for (int i = 0; i < allOps.length; i++)
       {
            String[] opsName = allOps[i];
            for (int j = 0; j < opsName.length; j++)
            {
                /* String[] parts = opsName[j].split("\\.");
                String serviceType = "";
                String operation = "";
                if (parts.length > 1)
                {
                    serviceType = parts[0];
                    operation = parts[1];
                }
            else
            {*/
                   JSONObject  clientRequestParams = getRequestMessageParams(commTemplate, opsName[j]);

                   if (clientRequestParams != null)
                        System.out.println("\n>>>>> Client Request format for operation :" + opsName[j] +"\n" +
                            clientRequestParams.toJSONString());

                   JSONObject serverResponseParams = getResponseMessageParams(commTemplate, opsName[j]);
                    if (clientRequestParams != null)
                         System.out.println("\n >>>>>Server Response format for operation :" + opsName[j] +"\n" +
                            serverResponseParams.toJSONString());

                    System.out.println("----------------------------------------------------------------------------------------");
            }    
        }
    }
/*------------------------------------------------------------------------------------------------------------------------*/
   public  static JSONObject   getRequestMessageParams(JSONObject commTemplate, String operation)
    {
         String[] nameSeg = getServiceComponents(operation);      // split to components
         String opsName = nameSeg[1];
        
         JSONObject  serviceObject = getServiceObject (commTemplate, operation);
         JSONObject opsObject = null;
     
        if (serviceObject != null)
        {
            Object obj = serviceObject.get(opsName);
            if (obj.getClass().getName().endsWith("JSONObject"))   
                opsObject = (JSONObject) (obj);
        }
        if (serviceObject == null || opsObject == null)
        {
            System.out.println("Invalid Service:  or operation : " + opsName);
            return null;
        }
        return getClientRequestParams(serviceObject, opsObject);
       }
    
    /*------------------------------------------------------------------------------------------------------------------------*/
    public  static JSONObject   getResponseMessageParams(JSONObject commTemplate, String operation)
    {
          String[] nameSeg = getServiceComponents(operation);      // split to components
           String opsName = nameSeg[1];
          
         JSONObject  serviceObject = getServiceObject (commTemplate, operation);
         JSONObject opsObject = null;
         
        if (serviceObject != null)
        {
            Object obj = serviceObject.get(opsName);
            if (obj.getClass().getName().endsWith("JSONObject"))   
                opsObject = (JSONObject) (obj);
        }
        if (serviceObject == null || opsObject == null)
        {
            System.out.println("Invalid Service:  or operation : " + opsName);
            return null;
        }
        return getServerResponseParams(serviceObject, opsObject);
       }
  /*---------------------------------------------------------------------------------------------------*/  
    
    private static String[]  getServiceComponents(String operation)
    {
        String[] parts = operation.split("\\.");
        String serviceType = "";
        String operationName = "";
        if (parts.length > 1)
        {
            serviceType = parts[0];
            operationName = parts[1];
        }
        else
            operationName = operation;          // unique serviceType
        
        return  new String[] {serviceType, operationName};
    }
/*-------------------------------------------------------------------------------------------------------------------*/    
     public static JSONObject   getServiceObject (JSONObject commTemplate, String opsName)
    {
        String[] parts = opsName.split("\\.");
        String serviceType = "";
        String operation = "";
        if (parts.length > 1)
        {
            serviceType = parts[0];
            operation = parts[1];
        }
        else
            operation = opsName;          // unique serviceType

        JSONObject  serviceObject = null;
        if (!serviceType.isEmpty())
        {
              serviceObject=  (JSONObject)commTemplate.get(serviceType);
        }
        else             // Iterate over all Service types to get the operation
        {
              Set <String> keySet = commTemplate.keySet();
              Iterator<String> it  = keySet.iterator();
              while (it.hasNext())
              {
                  serviceType = it.next();
                  JSONObject parentObject = (JSONObject)commTemplate.get(serviceType);
                  if (parentObject.get(operation) != null)
                  {
                      serviceObject = parentObject;
                  }
              }
        }
        if (serviceObject == null )
        {
            System.out.println("Invalid Service: "+  serviceType +", or operation : " + operation);
            return null;
        }
        return serviceObject;
}

/*--------------------------------------------------------------------------------------------*/
 // Get the JSONObject corresponding to the specified client request
 /*--------------------------------------------------------------------------------------------*/
   public  static  JSONObject getClientRequestParams(  JSONObject serviceObject , 
       JSONObject  opsObject)
   {
         JSONObject  clientRequest = new JSONObject();
         
         // first retrieve the common input parametres for all types
         JSONObject commonRequest =  (JSONObject) serviceObject.get("commonRequest");
         if (commonRequest != null)
         {
            Iterator <String>it = commonRequest.keySet().iterator();
            while (it.hasNext())
            {
                String param = it.next();
                clientRequest.put(param, commonRequest.get(param));
            }
         }
        
         JSONObject inputParamObj=  (JSONObject) opsObject.get("requestParams");
         if (inputParamObj != null)
         {
            // now add all operation specific parameters
            Iterator <String>it1 = inputParamObj.keySet().iterator();
            while (it1.hasNext())
            {
                String param = it1.next();
                clientRequest.put(param, inputParamObj.get(param));
            }
         }
        return clientRequest;
   }     
  
   /*--------------------------------------------------------------------------------------------*/
 // Get the JSONObject corresponding to the specified server response
 /*--------------------------------------------------------------------------------------------*/
     public  static  JSONObject getServerResponseParams( JSONObject serviceObject , 
       JSONObject  opsObject)
   {
         JSONObject  serverResponse = new JSONObject();
         
          // first retrieve the common response parametres for all types
         JSONObject commonResponse =  (JSONObject) serviceObject.get("commonResponse");
         if (commonResponse != null)
         {
            Iterator <String>it = commonResponse.keySet().iterator();
            while (it.hasNext())
            {
                String param = it.next();
                serverResponse.put(param, commonResponse.get(param));
            }
         }
        
          // now add all operation specific parameters
         JSONObject outputParamObj=  (JSONObject) opsObject.get("responseParams");
        if (outputParamObj != null)
         {
            Iterator <String>it1 = outputParamObj.keySet().iterator();
            while (it1.hasNext())
            {
                String param = it1.next();
                serverResponse.put(param, outputParamObj.get(param));
            }
         }
        return serverResponse;
   }   
   /*-------------------------------------------------------------------------------------------------------*/
    public  static void main(String[] args)
    {
        String  fmInterfaceTemplate= "C:/DevWork/FaceMatch2/FM2Server/installDir/config/template/FaceMatchWebInterface.json";
        FMCommTemplateReaderTest templateReader = new FMCommTemplateReaderTest();
        JSONObject commTemplate = templateReader.parseTemplate(fmInterfaceTemplate);
        
    }
}