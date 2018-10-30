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
package fmservice.server.ops;

import fmservice.server.global.FMContext;
import fmservice.server.global.Scope;
import fmservice.httputils.common.ServiceConstants;
import fmservice.server.cache.dbcontent.ClientStore;
import fmservice.server.cache.dbcontent.FMClient;
import fmservice.server.cache.dbcontent.MetadataField;

import fmservice.server.result.FMServiceResult;
import fmservice.server.result.Status;

import fmservice.server.util.Utils;
import fmservice.server.util.AgeGroupAllocator;

import java.util.HashMap;
import java.util.ArrayList;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 *
 */
public class FMClientManager implements ServiceConstants
{
    //------------------------------------------------------------------
    // Information required to add a Client to FM2
    //------------------------------------------------------------------
    protected class ClientInfo
    {
        //------------------------------------------------------------------
        // Descripton of MetadataFields of a client
        //------------------------------------------------------------------
        protected class ClientMetadata
        {
            String fieldName;
            String fieldType;
            String validSet;
            String defaultValue;
            boolean searchable;
            String note;
        }
        
        String name;
        String description;
        String clientKey;
        boolean storeThumbnail;
        String indexRoot;
        String thumbnailRoot;
        String storeDescription;
        ArrayList<ClientMetadata> metadata;
        Status infoStatus;           // status in retrieving client info from the input file
        
        protected ClientInfo(String clientName) 
        {
            name = clientName;
        }
        protected boolean isOk()
        {
            return (infoStatus.statusCode == 1);
        }
    }
    
    
    private Logger log = Logger.getLogger(FMClientManager.class);
    FMContext   fmContext;
    
    public FMClientManager(FMContext fmContext)
    {
        this.fmContext = fmContext;
    }
  //--------------------------------------------------------------------------------------------------------------------------
   // Add a new client to the system, with info provided in the SQL file
   //--------------------------------------------------------------------------------------------------------------------------
    public   FMServiceResult   addNewClient(int service, int operation,  HashMap inputParams)
    {
        FMServiceResult result = null;
        
        // parse the parameters provided in the JSON file and get various information to set up
        // client related static table entries
    
        String clientName = (String)  inputParams.get(ServiceConstants.CLIENT_NAME_PARAM);
        ArrayList<String> clientNames = Scope.getInstance().getAllClientNames();
        if (clientNames.contains(clientName))
        {
              result = new FMServiceResult(service, operation, DUPLICATE_CLIENT_NAME, "Duplicate "+ clientName 
                      + "; already exists in the system).");
              return result;
        }
         String clientInfoFile = (String)  inputParams.get(ServiceConstants.CLIENT_INFO);
         
        ClientInfo  clientInfo = buildClientInfo(clientName, clientInfoFile);
        if (!clientInfo.isOk())
            return new FMServiceResult(ADMIN_SVC,  ADD_FMCLIENT, clientInfo.infoStatus);
       
        // create a new FMClient row in the database with this entry and store in the cache too
        // Create the client object
        Status status = createClient(fmContext, clientInfo);
        return new FMServiceResult(ADMIN_SVC,  ADD_FMCLIENT, status);
    }  
        /*------------------------------------------------------------------------------------------------*/

    
    /*------------------------------------------------------------------------------------------------------*/
   // Get the  parameters from a given file provided in JSON format
    // add the path name for the client to the info file
   // ------------------------------------------------------------------------------------------------------*/
    protected ClientInfo  buildClientInfo(String clientName, String clientInfoFile)
    {
        ClientInfo clientInfo = new ClientInfo(clientName);
        String clientInfoFileSpec = "";
        try
        {
            String infoFilePath = fmContext.getFMConfiguration().getProperty("facematch.clientinfo.path");
            if (infoFilePath == null)
            {
                log.error ("Property \"facematch.clientinfo.path\" not found in the Server configuration file.");
                clientInfo.infoStatus = new Status(INACCESIBLE_CLIENT_FILE, 
                        "Property \"facematch.clientinfo.path\" not found in the Server configuration file.");
                return  clientInfo;
            }
            clientInfoFileSpec = infoFilePath + "/" + clientInfoFile;   
            
            // retrive top level fields, specified in "info" section
            JSONObject clientData = Utils.readFileAsJSONObject(clientInfoFileSpec);
            if (clientData == null)
            {
                 clientInfo.infoStatus = new Status(INACCESIBLE_CLIENT_FILE, "Inaccessible or invalid Client info file: "+ clientInfoFileSpec);
                return  clientInfo;
            }
            
            String name = (String) clientData.get("clientname");
             if (name == null || !name.equalsIgnoreCase(clientName))
            {
                log.error("User provided:" + clientName + ", name in Info file: " + (name == null ? "NULL" :  name));
                 clientInfo.infoStatus = new Status(-1, "Client name mismatch in request and client's info file");
                return  clientInfo;
            }
             JSONObject infoObj = null;
             JSONObject storageObj = null;
             JSONObject  mdObj = null;
            JSONArray dataArray = (JSONArray) clientData.get("data");
 
            int n = dataArray.size();
            for (int i = 0; i < n; i++)
            {
                JSONObject dataObj = (JSONObject) dataArray.get(i);
                String type = (String)dataObj.get("type");
                
                // retrieve high level information
                if (type.equalsIgnoreCase("info"))
                    infoObj = dataObj;
                else if (type.equalsIgnoreCase("storage"))
                    storageObj = dataObj;
                 else if (type.equalsIgnoreCase("metadata"))
                    mdObj = dataObj;
            }
            if (infoObj == null || storageObj == null || mdObj == null)
            {
                clientInfo.infoStatus = new Status(INVALID_CLIENT_FILE, "Invalid client information file, missing data");
                return clientInfo;
            }  
            // Retrieve info data
            String description = (String) infoObj.get("description");
            String clientKey = (String) infoObj.get("key");
            Boolean tn = (Boolean) infoObj.get("thumbnails");
            boolean thumbnails = (tn == null ? false : tn.booleanValue());
            if (clientKey == null)
            {
                clientInfo.infoStatus = new Status(MISSING_CLIENTNAME_INFILE, "No Client key provided for client ");
                return clientInfo;
            }
            clientInfo.clientKey = clientKey;
            clientInfo.description = (description == null ? "" : description);
            clientInfo.storeThumbnail = thumbnails;
                
            // retrieve and  the storage area assignment 
            String indexRoot = null;
            String thumbnailRoot = null;
            indexRoot = (String) storageObj.get("indexRoot");
            if (thumbnails)
            {
                thumbnailRoot = (String) storageObj.get("thumbnailRoot");
            }
            if (indexRoot == null || (thumbnails && thumbnailRoot == null))
            {
               clientInfo.infoStatus = new Status(INVALID_INDEXSTORE_PATH, "Missing index file or thumbnail storage root  missing for client " + clientName);
                return clientInfo;
            }
            String storeDescription  = (String) storageObj.get("description");
            clientInfo.storeDescription = (storeDescription == null ? "" : storeDescription);
            clientInfo.indexRoot = indexRoot;
            clientInfo.thumbnailRoot = thumbnailRoot;

            // get the metadata field specifications
            clientInfo.metadata = new ArrayList();
            JSONArray mdArray = (JSONArray) mdObj.get("field");
            for (int i = 0; i < mdArray.size(); i++)
            {
                JSONObject fobj = (JSONObject) mdArray.get(i);
                ClientInfo.ClientMetadata md = clientInfo.new ClientMetadata();
                md.fieldName = (String) fobj.get("name");
                md.fieldType = (String) fobj.get("type");                     // currently ignired
                md.defaultValue = (String) fobj.get("default_value");
                md.validSet = (String) fobj.get("valid_set");
                if (md.fieldName == null || md.defaultValue == null || md.validSet == null)
                {
                    clientInfo.infoStatus = new Status(BAD_METADATA_INFILE,  "Missing/invalid  metadata field specifations for client" + clientName);
                    return clientInfo;
                }
                md.searchable = true;
                Boolean search = (Boolean) fobj.get("is_searchable");
                if (search != null && search.booleanValue() == false)
                {
                    md.searchable = false;
                }
                String note = (String) fobj.get("note");
                md.note = (note == null) ? "" : note;
                clientInfo.metadata.add(md);
            }
            clientInfo.infoStatus = new Status(SUCCESS, "Information retrieved successfully for  client " + clientName);
        }
        catch (Exception e)
        {
            log.error("Exeption in retrieving client data from file " + clientInfoFile + ", " +e.getMessage());
             clientInfo.infoStatus = new Status(CLIENTFILE_READ_EXCEPTION, "Exeption in retrieving client data from file " + clientInfoFile +", " + e.getMessage());
        }
        return clientInfo;
    }
    /*-------------------------------------------------------------------------------------------------------------------*/
    // Create a nre client and store the information in the database,
    // based upon the information provided in the ClientInfo file
    // Note that is converts the metadata of type "AGE" to a better searchable field.
         /*-------------------------------------------------------------------------------------------------------------------*/
   
    protected Status  createClient(FMContext fmContext, ClientInfo clientInfo)
    {
        try
        {
            FMClient client = FMClient.create(fmContext.getDBContext(), clientInfo.name);
            client.setDescription(clientInfo.description);
            client.setClientKey(clientInfo.clientKey);
            client.setStoreThumbnails(clientInfo.storeThumbnail);
            client.update();            // write to the database
            
            int clientId = client.getID();
            
            // Create the clientStore for this client
            ClientStore clientStore = ClientStore.create(fmContext.getDBContext(), clientId);
            clientStore.setDescription(clientInfo.storeDescription);
            clientStore.setIndexRoot(clientInfo.indexRoot);
            clientStore.setThumbnailRoot (clientInfo.thumbnailRoot);
            clientStore.setReadPermission(true);              // not used presently
            clientStore.setDescription(clientInfo.description);
            clientStore.update();
            client.addClientStore();
            
            // Create the Metadata field and add their IDs to the client's list
            // Note that age is treated separately as it is converted to ageGroup for better searching
            ArrayList<ClientInfo.ClientMetadata>  metadataInfo =   clientInfo.metadata;
            for (int i = 0; i < metadataInfo.size(); i++)
            {
                ClientInfo.ClientMetadata mdInfo =  metadataInfo.get(i);
                MetadataField mdField = MetadataField.create(fmContext.getDBContext(), client.getID());
                mdField.setFieldName(mdInfo.fieldName);
                mdField.setFieldType(mdInfo.fieldType);
                if (mdInfo.fieldType.equalsIgnoreCase("Age"))
                {
                    String validRange = mdInfo.validSet;
                    String[] limits = validRange.split("-");        // upper and lower limits
                    int minAge = Integer.parseInt(limits[0]);
                    int maxAge = Integer.parseInt(limits[1]);
                    if (minAge < MINIMUM_AGE  || maxAge > MAXIMUM_AGE)
                    {
                        return (new Status( BAD_METADATA_INFILE, "Age of a person must be between "+ MINIMUM_AGE
                                + " and " + MAXIMUM_AGE));
                    }
                    String[] validGroups = AgeGroupAllocator.getValidSet(minAge, maxAge);
                    String validSet = validGroups[0];
                    for (int j = 1; j < validGroups.length; j++)
                        validSet += "+"+validGroups[i];
                    validSet += mdInfo.defaultValue;        // always add the default if not there
                    mdField.setValidSet(validSet);
                }
                else    // for other metdata types
                {
                    mdField.setValidSet(mdInfo.validSet);
                }
                mdField.setdefaultValue(mdInfo.defaultValue);
                mdField.setScopeNote(mdInfo.note);
                mdField.setSearchable(mdInfo.searchable);
                mdField.update();
                client.addMetadataField(mdField);
            }
            // Commit  it to the database
            fmContext.getDBContext().commit();

           // finally add this client to the global scope
           // The lower level objects should be now available through DB query
           String clientName = clientInfo.name;
           int stat =  Scope.getInstance().addNewClientInfo(clientId);

           Status status;
           if (stat == 1)
             status = new Status(SUCCESS, "Successfully added the new Client " + clientName +" to system.");
           else
               status = new Status( INTERNAL_SERVER_ERROR, "Internal error in adding the Client "+ clientName + "to system).");
           return status;
        }
        catch (SQLException e)
        {
            log.error("SQL Exception in adding new Client to database or cache", e);
            return (new Status(DATABASE_ACCESS_EXCEPTION , "SQL Exception in adding new Client to database or cache, error: "
                + e.getMessage()));
        }
    }
    
    // main method to simply access data from the info file   
    public static void main(String[] args)
    {
       FMClientManager clientManager = new FMClientManager(null);
       ClientInfo clientInfo = clientManager.buildClientInfo("PLTest",  
               "<TopDir>/FM2Server/installDir/config/clientInfo/PLTest_ClientInfo.json");
       System.out.println("Status: " + clientInfo.infoStatus.statusMessage);
    }
}


