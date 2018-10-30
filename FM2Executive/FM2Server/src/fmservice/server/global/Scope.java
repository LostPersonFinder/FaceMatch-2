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
package fmservice.server.global;

import fmservice.server.cache.dbcontent.FMClient;
import fmservice.server.cache.dbcontent.FMImage;
import fmservice.server.cache.dbcontent.ImageExtent;
import fmservice.server.cache.dbcontent.ZoneDescriptor;

import fmservice.server.cache.dbcontent.IndexTypeRegistry;
import fmservice.server.cache.dbcontent.MetadataField;

import java.sql.SQLException;

import fmservice.server.cache.dbcontent.DBUtils;
import fmservice.server.util.ChecksumCalculator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * <P>
 * Scope is a singleton class which defines the scope of FaceMatch operations.
 * 
 *  It is  a Registry with information about high level objects and their relationships
 * as well maintains the FM2 context objects including connection to the database.
 * It is created by the FMContext object when the later is created prior to any operation.
 * <p>
 * Note that the semi-static lists (such as clients extents etc.) are created at construction from the database
 * information. The corresponding object manager is responsible for
 * updating the list when new objects are added to the database or existing ones removed.
 * <p>
 * It acts as a central (global) object to retrieve such system level information from lower levels.
 * It is initialized at FM2 Server startup time from the database and system configuration. 
 * It also acts as a conduit to retrieve the properties of an FM2Client or its ImageExtents anywhere 
 * within the application. Therefore, the Scope object  should be maintained by invoking its appropriate methods
 * when such semistatic information changes. (A new FMClient or Extent is created, deleted etc.).
 * 
 * Example of static tables read are:
 *
 *  - metadataFieldRegistry
 *  - indextyperegistry
 *  - FMContext
 * 
 * Example of semi-static information
 *    - FMClient, ImageExtent, DBContext
 *
 * For convenience, it provides a number of utility functions related to FMClient and ImageExtent
 * 
 *
 * 
 */

/*-------------------------------------------------------------------------------------*/
// Note: Scope does not have to update any lower level database info
// such as Extent related data as they are dynamically added to the
// Object cache at creation time and and the elements are dynamically 
// retrieved when needed.
/*-------------------------------------------------------------------------------------*/

public class Scope
{
    private static Logger log =  Logger.getLogger(Scope.class);
    
    protected  String ITREGISTRY_QUERY = "Select * from indextyperegistry";
    protected  String MDFREGISTRY_QUERY = "Select * from metadatafieldregistry"; 
   
    /****/
    // singleton objects
    protected  FMContext fmContext = null;
    protected  DBContext dbContext = null;
    /****/

    protected  ArrayList<Integer> allClientIDs = null;                                     // list of all clients by their IDs
    protected  ArrayList<String> allClientNames = null;                                // list of all clients by their names
    protected  ArrayList<String>allClientKeys = null;
    protected ArrayList<String>deferredClientNames = null;                         // the ones to ignore in initial load
   
    protected   HashMap<Integer, FMClient> clientMap  = null;                       // key: client 
    protected   IndexTypeRegistry indexTypeRegistry;  

   protected  static Scope  scope = null;           // The SINGLETON object
    
/*-----------------------------------------------------------------------------------------------*/  
/**  
 * Return the  singleton Scope object, create it if not already instantiated.
 * This method is invoked at the Facematch system initialization time
 * @param dbContext
 * @return scope = the Singleton Scope object
 * Note: This way, we pass the FMContext parameter to Scope without losing 
 *              the advantage of having a singletin instance.
 */
    public  static Scope setDomainScope(FMContext context)
    {
        if ( scope== null)            // first time
        {
            scope = new Scope (context);
        }
       return scope;  
    }
    
    /*--------------------------------------------------------------------*/
    // Return the  singleton Scope object after it is initialized
    public  static Scope getInstance()
    {
       return scope;  
    }
    /*--------------------------------------------------------------------*/
     // Return the  database context used by the Scope object 
    public DBContext getDBContext()
    {
        return dbContext;
    }
  
    /*--------------------------------------------------------------------*/
    // Return the  FaceMatch configuration  context used by the Scope object 
      public FMContext  getFMContext()
      {
          return fmContext;
      }
      
     /*--------------------------------------------------------------------*/
    // Return the  visual Index match  type used for query (as specified in the config file" 
       public  String getIndexMatchType()
      {
          String matchType = Scope.getInstance().getFMConfig().getProperty("indexMatch.type");
          String indexMatchType = "";              // standard linear query match
          if(matchType != null)
                 indexMatchType = matchType.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
          return indexMatchType;
       }

        
    /*--------------------------------------------------------------------*/
    // Return the  FaceMatch configuration  context used by the Scope object 
      public Properties  getFMConfig()
      {
          return fmContext.fmConfig;
      }
      


    /*-----------------------------------------------------------------------------------------------*/
    /** Constructor. Instantiate the singleton object and initialize the contents
     * from the static data stored in the database
     * 
     * @param context 
     */
    private Scope(FMContext myContext)
    {
        fmContext = myContext;
        dbContext = fmContext.getDBContext();
        loadDomainScope();
    }
    
    /*-----------------------------------------------------------------------------------------------*/
    /**
     * Load all static data from the FaceMatch database at application initialization time.
     * 
     * This method's body is executed only once successfully.
     */
    private  int loadDomainScope()
    {
        // load the following Top level table contents from the database through the database manager
       int numClients =  loadClientInfo(dbContext);
        int status =  loadIndexTypeRegistry(dbContext);
        if (status != 1) 
           return 0;

         getAllClientKeys();
         getAllClientNames();
        return 1;
    }
    /*--------------------------------------------------------------------------------------------------------------------*/
    /**
     * <P>
     * Load information about all clients which want to perform facematch operations.
     * .
     *  This method is invoked atSystem startup
     * @param dbContext 
     *      Database context for connection to Facematch DB
     */
    protected  int  loadClientInfo(DBContext context)
    {  
        allClientIDs = new ArrayList<Integer>();
        allClientNames = new ArrayList<String>();
        clientMap= new  HashMap<Integer, FMClient> ();
        allClientKeys = new ArrayList<String>();
        deferredClientNames = new ArrayList<String>();
        
        ArrayList<FMClient> fmClients = DBUtils.getAllClientInfo(context);
        if ( fmClients == null || fmClients.isEmpty())
            return 0;
        for (FMClient client : fmClients)
        {
            clientMap.put(new Integer(client.getID()), client);
             allClientNames.add(client.getName());
             allClientKeys.add(client.getKey());
             allClientIDs.add(new Integer(client.getID()));
        }
        return fmClients.size() ;  
    }   
   /*------------------------------------------------------------------------------------------*/
    /**
     * Load information about different  image index types stored in the database.
     * This is a static table, which is loaded at system startup time and does not change.
     *  @param dbContext 
     *      Database context for connection to Facematch DB
     */

    protected int loadIndexTypeRegistry(DBContext context)
    {  
        try
        {
            indexTypeRegistry  =  new IndexTypeRegistry(context);
            return 1;
        }
        catch (SQLException sqle)
        {
            log.error("Error loading IndexType information from database", sqle);
        }
        return 0;
    }
    
     /*----------------------------------------------------------------------------------------------------------------*/
    /**
     * Add a new client to the system scope. 
     * <p>
     * This is an administrative  function, which allows to perform operations for a new client for the system
     *  after it is entered to the database, along with its indexStore and Extent definitions through an administrative 
     * Web service call during operation.
     * <p>
     * Note that it is not called for existing clients already stored in the database.
     * 
     * @return 1=success in adding the client, 0 = failure in retrieving the client's information
     * --------------------------------------------------------------------------------------------------------------------
     */
    public  int  addNewClientInfo(int clientId)
    {   
        Integer cId= new Integer (clientId);
        FMClient client =null;
        try
        {
            client = FMClient.find(dbContext, cId);            // should not happen
        }
        catch (Exception e)
        {
             log.error(" SQL Exception in trying to retrieve FaceMatch Client  with id = " + clientId);
            return 0;
        }
        if (client == null)
        {
            log.error(" FaceMatch Client  with id = " + clientId + " does not exist.");
            return 0;
        }
        // add info to stored data 
        clientMap.put(new Integer(client.getID()), client);
        allClientIDs.add(new Integer(client.getID()));
        allClientNames.add(client.getName());
        allClientKeys.add(client.getKey()); 
        {
            log.info ("New client " + client.getName()  + " added to system dynamically");
        }
        return 1 ;  
    }   
 
   /*----------------------------------------------------------------------------------------------------------*/
    /**
     * Retrieve names of all Facematch  clients
     * @return  List of all FM2 client names
     */
    public  ArrayList<String> getAllClientNames()
    { 
        return allClientNames;
    }
    
    
    /*----------------------------------------------------------------------------------------------------------*/
    /**
     * Retrieve names of all Facematch  clients whose activities are deferred at startup
     * They are activated if an explicit request is made about them
     * @return  List of all FM2 client names
     */
    public  ArrayList<String> getDeferredClientNames()
    { 
        return deferredClientNames;
    }
    
    /*----------------------------------------------------------------------------------------------------------*/
    /**
     * Retrieve  keys of all Facematch  clients
     * @param clientNames
     * @return  List of all FM2 client keys
     */
    public  ArrayList<String> getAllClientKeys()
    {
        return allClientKeys;
    }
    
    /*-----------------------------------------------------------------------------------------------------------*/
    /**
     * Retrieve the client's unique ID  given the client's name
     * @param clientName
     * @return  Client's database ID
     */
    public  int clientName2Id (String clientName)
    {
        Iterator <Integer> it = clientMap.keySet().iterator();
        while (it.hasNext())
        {
            FMClient client  = clientMap.get(it.next());
            if (client.getName().equals(clientName))
                return client.getID();
        }
        log.warn("No client found with name: " + clientName);
        return 0;
    }
    /*-----------------------------------------------------------------------------------------------------------*/
    /**
     * Retrieve the Client key  given its name.
     * @param  clientName client's name
     * @return    client's key
     * */
    public String clientName2Key(String clientName)
    {
       Iterator <Integer> it = clientMap.keySet().iterator();
        while (it.hasNext())
        {
            FMClient client  = clientMap.get(it.next());
            if (client.getName().equalsIgnoreCase(clientName))
                return client.getKey();
        }
        log.warn("No client found with name: " + clientName);
        return null;
    }
   /*-----------------------------------------------------------------------------------------------------------*/ 
   /**
     * Retrieve the Client object with the specific key
     * @param  clientKey  client's key
     * @return   FMClient object
     * */
     public  FMClient  getClientWithKey (String clientKey)
    {
        Iterator <Integer> it = clientMap.keySet().iterator();
        while (it.hasNext())
        {
            FMClient client  = clientMap.get(it.next());
            if (client.getKey().equals(clientKey))
                return client;;
        }
        log.warn("No client found with key: " + clientKey);
        return null;
    }
   /*-----------------------------------------------------------------------------------------------------------*/
    /**
     * Retrieve the client's unique_name  given the client's key
     * @param key client's key
     * @return client's name
     */
    public  String getClientName (String clientKey)
    {
        FMClient client = getClientWithKey(clientKey);
        return (client == null ? null : client.getName());
    }
    
   /*-----------------------------------------------------------------------------------------------------------*/
    /**
     * Retrieve the client's database ID given the client's key
     * @param clentKey client's key
     * @return id from the database table
     */
    public  int getClientID (String clientKey)
    {
        FMClient client = getClientWithKey(clientKey);
        return (client == null ? 0 : client.getID());
    }
 
  /*-----------------------------------------------------------------------------------------------------------*/
  /* -   ImageExtent related methods 
  /*-----------------------------------------------------------------------------------------------------------*/
    
    
    public ArrayList<String> getExtentNamesForClient(String clientName)
    {
        int clientId = clientName2Id (clientName);
        if (clientId == 0)
            return null;
        ArrayList<ImageExtent> imageExtents = 
            DBUtils.getImageExtentsForClient(dbContext, clientId);
        if (imageExtents == null)
            return null;
        
         ArrayList<String> extentNames = new ArrayList();
         for (ImageExtent extent : imageExtents)
         {
             extentNames.add(extent.getName());
         }
        return extentNames;
    }
    
    public boolean isActiveExtent(int extentId)
    {
        ImageExtent extent = DBUtils.getExtentWithID(dbContext, extentId);
        if (extent != null && extent.isActive())
            return true;
        return false;
    }

    public FMClient getClientWithExtent(int extentId)
    {
        int clientId = DBUtils.getClientIDForExtent(dbContext, extentId);
        FMClient client  = clientMap.get (new Integer(clientId));
        if ( client == null)
            log.error("No Client  found in database for ImageExtent ID: " + extentId);
        return client;
    }
   /*----------------------------------------------------------------------------------------------------------*
    * Methods related to Root directory for Storage and retrieval of Index files 
    /*----------------------------------------------------------------------------------------------------------*/
    /**
    * retrieve Index store root path for a Client.
    * @param clientId  database ID of the client 
    * @return Root of the repository where the client's index files are to be stored
   /*-----------------------------------------------------------------------------------------------------------*/    
    public String getIndexStoreRootPath(int clientId)
    {
        FMClient client  = clientMap.get(new Integer(clientId));
        String  indexStoreRoot = client.getIndexRoot();
        if (indexStoreRoot == null)
        {
            log.warn("No IndexStore information available for client ID: " + clientId);
            return null;
        }
        return indexStoreRoot;
    }
 
    /*-----------------------------------------------------------------------------------------------------------*/
      public String getIndexStoreRootPathForExtent(int extentId)
    {
        int clientId = getClientWithExtent(extentId).getID();
       String rootPath =  getIndexStoreRootPath(clientId);
       String[] info =  getExtentNameInfo(extentId);
       String indexDir = rootPath+"/"+info[1];
       return indexDir;
    }
   /*-----------------------------------------------------------------------------------------------------------*/
/** 
 * Get the path to the index root (where index descriptors would be stored 
 * (based upon the Client).
 * 
 * @param descriptor
 * @return  indexRoot path
 */
    public String getIndexStoreRootPathForDescriptor(ZoneDescriptor descriptor)
    {
        try
        {
            DBContext dbContext = fmContext.getDBContext();
            FMImage image = FMImage.find(dbContext, descriptor.getImageID());
            return getIndexStoreRootPathForExtent(image.getExtentID());
        }
        catch (SQLException sqle)
        {
            log.error("Exception in retrieving index root path for ZoneDescriptor ID: " + descriptor, sqle );
            return null;
        }
    } 
      
    /*-----------------------------------------------------------------------------------------------------------*/
   /** 
    * Return the IndexRootPath of all ImageExtents in the system.
    * <p>
    * Note This is more efficient in loading index files for  search as they are all under the same root path 
    * for a given extent rather than getting the path individually for each Descriptor
    * 
    * @return a HashMap of (extentId vs. indexRoot)     
    /*-----------------------------------------------------------------------------------------------------------
     public  HashMap<Integer, String> getIndexStoreRootPathMap()
    {
        HashMap<Integer, String> indexRootMap = new HashMap();
        for (int i = 0; i < allClientIDs.size();  i++)
        {
            int clientId = allClientIDs.get(i).intValue();
            String indexStorePath = getIndexStoreRootPath(clientId);

            ArrayList<ImageExtent> imageExtents = 
                  DBUtils.getImageExtentsForClient(dbContext, clientId);
           if (imageExtents == null)
                 continue;
           for (int j = 0; j <  imageExtents.size(); j++)
            indexRootMap.put(new Integer(imageExtents.get(j).getID()), indexStorePath);     
        }
        return indexRootMap;
    }
  */  
    
   /*-----------------------------------------------------------------------------------------------------------*/
    public String getThumbnailRootPath(int clientId)
    {
        FMClient client  = clientMap.get(new Integer(clientId));
        String  thumbnailRoot = client.getThumbnailRoot();
        if (thumbnailRoot == null)
        {
            log.warn("No  Thumbnail storage information available for client ID: " + clientId);
            return null;
        }
        return thumbnailRoot;
    }
   /*-----------------------------------------------------------------------------------------------------------*/
      public String getThumbnailRootPathForExtent(int extentId)
    {
        int clientId = getClientWithExtent(extentId).getID();
       String rootPath =  getThumbnailRootPath(clientId);
       String[] info =  getExtentNameInfo(extentId);
       String thumbnailDir= rootPath+"/"+info[1];
       return thumbnailDir;
    }
    /*--------------------------------------------------------------------------------------------------*/
     /**
     * Retrieve the Extent Id given its name and client key.
     *  (Note: extent names are unique  for a given client only).
     * 
     * @param extentName  name of the extent
     * @param clientKey       key representing the FM2 Client
     * @return extentId         Database ID if the ImageExtent
     */
    public int getExtentId(String clientKey, String  extentName)
    {
        FMClient client = getClientWithKey (clientKey);
        if (client == null)
        {
            log.warn("No client found with key " + clientKey);
            return -1;
        }
        
        ArrayList<ImageExtent> extents =  DBUtils.getImageExtentsForClient(dbContext, client.getID());
        for (ImageExtent extent : extents)
        {
            if (extent.getName().equalsIgnoreCase(extentName))
                return (extent.getID());
        }
        log.warn("No ImageExtent found with name " + extentName + " and Client key " + clientKey);
        return 0;
    }

    /*-----------------------------------------------------------------------------------------------------*/
       /**
     * Retrieve the ImageExtent object based upon its name and parent name.
     *  (Note: extent names are unique  for a given client only).
     * 
     * @param extentName  name of the extent
     * @param clientName    name of the FM2 Client
     * @return  ImageExtent object (from database cache) 
    // Note: extent manes are not case sensitive here
    /*-----------------------------------------------------------------------------------------------------*/
   public ImageExtent getImageExtent(String clientName, String extentName)
    {
        Iterator <Integer> it = clientMap.keySet().iterator();
        while (it.hasNext())
        {
            FMClient client  = clientMap.get(it.next());
            if (client.getName().equals(clientName))
            {
                ArrayList<ImageExtent> imageExtents =  DBUtils.getImageExtentsForClient(dbContext, client.getID());
                for (ImageExtent extent : imageExtents)
                {
                    if (extent.getName().equalsIgnoreCase(extentName))
                        return extent;
                }
            }
        }
        log.warn("No  extent found with name: " + extentName + " for client name: " + clientName);
        return null;
    }
 /*-----------------------------------------------------------------------------------------*/
  /** Return names associated with this extent.
   * 
   * @param extentId
   * @return  String [2], Client name [0],  extent name [1]
   */
    public String[] getExtentNameInfo(int extentId)
    {
        String[] names = new String[2];
        DBContext dbContext = fmContext.getDBContext();
        try
        {
            ImageExtent extent = ImageExtent.find(dbContext, extentId);
            if (extent == null)
            {
                log.error("No  ImageExtent  found for extent ID: " + extentId);
                return null;                // does not exist
            }
             int  clientId = extent.getClientID();
             FMClient client = FMClient.find(dbContext, clientId);
            names[0] = client.getName();
            names[1] = extent.getName();
            return names;
        }
       catch (SQLException sqle)
       {
           log.error("Database error in retrieving information for ImageExtent and/or FMClient", sqle); 
            return null;
       }
    }
  /*-----------------------------------------------------------------------------------------------------------*/
  /*                                           Metadata Field related Methods                                        */
  /*-----------------------------------------------------------------------------------------------------------*/

  /*---------------------------------  get MetadataFields vs. their  valueSets -----------------------------*/
   public HashMap<String, String[]>  getValidMetadataValueSet(int extentId)
   {
       FMClient  client = getClientWithExtent(extentId);
       if (client == null)
           return null;
        return  client.getMetadataValuesMap();
   }

  /*---------------------------------  get searchable metadataFields vs. their allowed values -----------------------------*/
   public HashMap<String, String[]>  getSearchableMetadataValueSet(int extentId)
   {
       FMClient  client = getClientWithExtent(extentId);
       if (client == null)
       {
           log.error("Invalid Extent ID " + extentId + " provided - does not have a client");
           return null;
       }
       return  client.getMetadataValuesMap();
   }
   
   /*---------------------------------  Metadata Field related Methods -----------------------------*/
   public String getDefaultMetadataValue(int extentId, String fieldName)
   {
       FMClient  client = getClientWithExtent(extentId);
       if (client == null)
           return null;
       MetadataField mdField = client.getMetadataMap().get(fieldName);
       if (mdField != null)   
            return mdField.getDefaultValue();
       return null;
   }

     /*-------------------------------------------------------------*/
   public String getMetadataFieldName(int fieldId)
   {
       return  MetadataField.getFieldID2Name(dbContext, fieldId);
   }
   
   /*-------------------------------------------------------------*/
   public int getMetadataFieldId(String metadataFieldName, int clientId)
   {
         try
        {
            FMClient client = FMClient.find(dbContext, clientId);
            MetadataField mdField =  client.getMetadataMap().get(metadataFieldName);
            return (mdField ==null ? -1 :mdField.getID());
        }
        catch (SQLException sqle)
        {
            return -1;              // no such  client
        }
   }

   /*----------------------------------------------------------------------------------------------------------------*/
   /*---------------------- IndexType related methods --------------------------------------------------*/
   /*----------------------------------------------------------------------------------------------------------------*/
   
   public String  indexId2Type(int  indexId)
   {
       return indexTypeRegistry.getIndexType(indexId);
   }
   
  /*----------------------------------------------------------------------------------------------------------------*/ 
    public int  indexType2Id(String indexType)
   {
      return indexTypeRegistry.getIndexId(indexType);        // no such type
   }
    
    /*----------------------------------------------------------------------------------------------------------------*/
    public boolean isIndexTypeInUse(String indexType)
   {
         return indexTypeRegistry.isInUse( indexType);
   }
   /*----------------------------------------------------------------------------------------------------------------*/     
    /**
     * Get the file extension corresponding to a given index type.
     * 
     * @param indexType
     * @return 
     */
    public  String getFileExtension(String indexType)
    {
        return indexTypeRegistry.getFileExtension(indexType);
    }
    /*----------------------------------------------------------------------------------------------------------------*/
    //                      admin related methods
    //---------------------------------------------------------------------------------------------------------------- */
    /**
     * Check if the given credentials are valid for an FM Administrator
     * @param name
     * @param password
     * @return 
     */
    public boolean  isAdmin(String name, String password)
    {
        String md5pw = ChecksumCalculator.getStringChecksum(password);
        return DBUtils.isAdmin(dbContext, name, md5pw);
    }

  
}
      
      
