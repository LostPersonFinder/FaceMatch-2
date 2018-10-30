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
package fmservice.server.cache.dbcontent;

import fmservice.server.global.DBContext;

import fmservice.server.storage.rdbms.DatabaseManager;
import fmservice.server.storage.rdbms.TableRow;
import fmservice.server.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 *
 * An FMClient represents a FaceMatch Client (an organization or person), who uses 
 * the FaceMatch services.
 * A FMClient may have one or more image collections partitioned as ImageExtents. 
 * All FaceMatch operations such as ingest or query are confined to a 
 * specified ImageExtent in the client's request
 * 
 *
 */
public class FMClient extends DBContentObject
{
       /** log4j logger */
    private static Logger log = Logger.getLogger(FMClient.class);
    
    /** Our (database) context */
    private DBContext  dbContext;

    /** The table row corresponding to this bundle */
    private TableRow myRow;
    private int client_id;
    
    /** data Storage  information for this client **/
     protected ClientStore dataStore;
     
     public static String  objectType = "FMClient";

     // Map of Metadata field name vs. its database row for this client
     protected  HashMap <String,  MetadataField>  mdFieldMap = null;
     protected  HashMap <String, String[]>  mdField2valuesMap = null;
     
     protected ArrayList<ImageExtent> imageExtentList = null;
  
    /*------------------------------------------------------------------------------------------------*/
      /**
     * Construct an  FMClient object from an existing (or newly created)  database row
     * in the fmclient table.
     * 
     * Fill the static information
     * 
     * @param fmcontext
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     ***/
    public  FMClient(DBContext context, TableRow row) throws SQLException
    {
        dbContext = context;
        myRow = row;
        client_id = myRow.getIntColumn("client_id"); 
               
        // Create two mapping objects for metadata
        mdFieldMap = new HashMap();         // field name to database object
        mdField2valuesMap = new HashMap();      // field name to valid values

          // Add dependent information from the database, if presently exists
          addClientStore();
          
          // Add Metadata Map
          addMetadataFields();
          
          imageExtentList = new ArrayList();
          addImageExtents();
   
          dbContext.addToCache(this);
          return;
    }
    
  /*----------------------------------------------------------------------------------------------------*/
    public void addClientStore() throws SQLException
    {
        // Add the static ClientStore information
         TableRowIterator tri1 = DatabaseManager.queryTable(
                dbContext, "clientstore",
                "SELECT clientstore.* FROM clientstore  WHERE "
                        + "clientstore.client_id="+client_id);

        // should be one row
        TableRow tr  = null;
        if (tri1.hasNext())
        {
           tr = (TableRow) tri1.next();
           dataStore = new ClientStore(dbContext, tr);
        }
        else
        {
            log.warn("No ClientStore information exists for Client " + myRow.getStringColumn("client_name"));
        }
          tri1.close();
    }
 
    //-----------------------------------------------------------------------------------------------------------
    // Set the various mappings for getting metadata feild names, valid values etc for this client
    //-----------------------------------------------------------------------------------------------------------
    public  void addMetadataFields() throws SQLException
    {
         // create the metadata  Field registry mapping, unique to this client
        if (mdFieldMap == null) 
        {
            mdFieldMap = new HashMap();         // field name to database object
            // also create the field name to valid values for each field
            mdField2valuesMap = new HashMap();      // field name to valid values
        }
        TableRowIterator tri2 = DatabaseManager.queryTable(
                dbContext, "metadatafield",
                "SELECT metadatafield.* FROM metadatafield  WHERE "
                        + "metadatafield.client_id="+client_id);

        while (tri2.hasNext())
        {
            TableRow  tr = (TableRow) tri2.next();
             MetadataField mdField  = new MetadataField(dbContext, tr);
             addMetadataField(mdField);
        }
         tri2.close();
        
        if (mdFieldMap.size() == 0)
        {
            log.warn("No Metadata  information exists for Client " + myRow.getStringColumn("client_name"));
        }
    }
    public  void addMetadataField( MetadataField mdField) throws SQLException
    {
        mdFieldMap.put(mdField.getFieldName(), mdField);

        String[] validValues = mdField.getValidValues();
        mdField2valuesMap.put(mdField.getFieldName(), validValues);
        
        MetadataField.storeFieldID2Name(mdField.getID(), mdField.getFieldName());
    }
    
        
     /**-----------------------------------------------------------------------------------------------------
     * Add the set of  ImageExtents known  for this client.
     * 
     *    @return  ImageExtent ArrayList
     /**-----------------------------------------------------------------------------------------------------*/
   protected void  addImageExtents() throws SQLException
    {
         // Add ImageExtent list for this client
        TableRowIterator tri = DatabaseManager.queryTable(
                dbContext, "imageextent",
                "SELECT imageextent.* FROM imageextent  WHERE "
                        + "imageextent.client_id="+ getID());
        
        while (tri.hasNext())
        {
            TableRow tr = tri.next();
            imageExtentList.add(new ImageExtent(dbContext, tr));
        }  
        // close the TableRowIterator to free up resources
        tri.close();
    }
    /*---------------------------------------------------------------------------------------------*/
   // get Client's metadata field name vs. MetadataRegistry row in the database
    /*---------------------------------------------------------------------------------------------*/
    public  HashMap<String, MetadataField> getMetadataMap()
    {
        return mdFieldMap;
    }
    
   /*---------------------------------------------------------------------------------------------*/
   // get theMap of Client;s metadata field name vs. its valid values in the database
    /*---------------------------------------------------------------------------------------------*/
    
    public HashMap< String, String[]> getMetadataValuesMap()
    {
        return mdField2valuesMap;
    }
  /*---------------------------------------------------------------------------------------------*/  
    // Is this metadata field searchable?
    //---------------------------------------------------------------------------------------------
     public boolean isSearchableMDField(String fieldName)
    {
        MetadataField mdField = mdFieldMap.get(fieldName);
        if (mdField == null)
            return false;
        return mdField.isSearchable();
    }

    /*------------------------------------------------------------------------------------------------*/
    /**
     * Get the set of all ImageExtents (both active and inactive) for this client.
     */
     public ArrayList <ImageExtent> getImageExtents() throws SQLException
    {
        return imageExtentList;
    }
     /*------------------------------------------------------------------------------------------------*/
      /**
     * Get the set of active  ImageExtents for this client.
     */
     public ArrayList <ImageExtent> getActiveImageExtents() throws SQLException
    {
           if (imageExtentList == null || imageExtentList.isEmpty())
            return null;           // the empty list
        
        ArrayList<ImageExtent> activeExtents  = new ArrayList();
        for (ImageExtent extent :   imageExtentList)
        {
            if (extent.isActive())
                activeExtents.add(extent);
        }
        return activeExtents;
    }
 /*------------------------------------------------------------------------------------------------*/    
    public ArrayList <Integer> getImageExtentIds() throws SQLException
    {
        ArrayList<Integer> extentIds  = new ArrayList();
        if (imageExtentList == null || imageExtentList.isEmpty())
            return extentIds;           // the empty list
        
        for (ImageExtent extent :   imageExtentList)
            extentIds.add(new Integer(extent.getID()));
        return  extentIds;
    }
    
    /*------------------------------------------------------------------------------------------------*/
    /**
     * Get the type of this object - same as the class name
     * 
     * @return type of the object
     */
    public  String getObjectType()
    {
        return objectType;
    }
    /**
     * Get the internal ID (database primary key) of this object
     * 
     * @return internal ID of object
     */
    public int getID()
    {
        return  myRow.getIntColumn("client_id");
    }
    
    public String getName()
    {
        return myRow.getStringColumn("client_name");
    }
    
    public String getKey()
    {
        return myRow.getStringColumn("client_key");
    }
    
    /**
     * Get the description of this fmclient object
     * 
     * @return description  of object
     */
    public String getDescription()
    {
        return  myRow.getStringColumn("description");
    }
    
     /**
      * Get the date when the client was added to the database
      * @return 
      */     
      public Date getCreationDate()
      {
            return myRow.getDateColumn("creation_date");
      }

    
 
      /**
     * Get the DataStore for this client.
     * 
     * @return Client's data store object
     */
    public ClientStore getDataStore()
    {
        return  dataStore;
    }
  
    /** ---------------------------------------------------------------------------------------------
     * Set methods 
     */
    public void  setClientKey(String key)
    {
        myRow.setColumn("client_key", key);
    }
        
    public void setDescription(String descr) throws SQLException
    {
          myRow.setColumn("description", descr);
           update();
    }    
    
    
     public void  setStoreThumbnails(boolean store)
    {
        myRow.setColumn("store_thumbnails", store);
    }
     
     public boolean isStoreThumbnails()
     {
         return  myRow.getBooleanColumn("store_thumbnails");
     }
    /*------------------------------------------------------------------------------------------------*/
    // Create a new ImageExtent under this collection in the database
    // Returns the newly created objct
    //------------------------------------------------------------------------------------------------*/
    public ImageExtent createImageExtent()  throws SQLException
    {
        ImageExtent  extent = ImageExtent.create(dbContext, getID());
        imageExtentList.add(extent);
        
        return extent;
    }
     /*-----------------------------------------------------------------------------------------------*/
     /**
     * Create a new FMClient object in the database (inserting a new row in the
     * database table and return the corresponding in-memory object. 
     * 
     * @param context
     *           Database context object
     * 
     * @return the newly created FMImage object
     */
    public static FMClient create(DBContext context, String clientName) throws SQLException
    {
        // Create a table row
        TableRow row = DatabaseManager.create(context, "fmclient");
        row.setColumn("client_name", clientName);
        return new FMClient(context, row);
    }
    
    /*--------------------------------------------------------------------------------------------------*/
      /**
     * Find  an fmclient object. First check the cache, if not there, load from Database
     * (which automatically adds to cache) 
     * 
     * @param fmContext
     *           FaceMatch  context object
     * @param id
     *            ID of the client
     * 
     * @return the fmclient  object, or null if the ID is invalid.
     */
    public static  FMClient  find(DBContext context, int id) throws SQLException
    {
          // First check the cache
        FMClient cachedClient  = (FMClient)context.getFromCache(objectType, id);
        if (cachedClient != null)
        {
            return cachedClient;
        }
        TableRow row = DatabaseManager.find(context, "fmclient", id);
        if (row == null)
        {
            log.error("No  entry found in database table \"fmclient\" with ID = " + id);
            return null;
        }
         return new FMClient(context, row);
    }
      /*--------------------------------------------------------------------------------------------------*/
      /**
     * Find  an fmclient object. First check the cache, if not there, load from Database
     * (which automatically adds to cache) 
     * 
     * @param fmContext
     *           FaceMatch  context object
     * @param name
     *            name of the client
     * 
     * @return the fmclient  object, or null if the ID is invalid.
     */ 
      public static  FMClient  findByUniqueName(DBContext context, String name) throws SQLException
    {
         TableRow row = DatabaseManager.findByUnique(context, "fmclient", "name", name);
         if (row == null)
        {
            log.error("No  entry found in database table \"fmclient\" with name = " + name);
            return null;
        }
         return new FMClient(context, row);
    }
  
    /*------------------------------------------------------------------------------------------------------------*/
    /** Delete self from the database, along with the index store and ImageExtents
    * and also remove from Object cache.
     * 
     * @throws SQLException 
     */
   public void delete () throws SQLException
   {
       // delete the underneath Imageextent and other objects from the database 
       if (!imageExtentList.isEmpty())
       {
           for (int i = 0; i < imageExtentList.size(); i++)
           {
               ImageExtent extent = imageExtentList.get(i);
                    extent.delete();
           }
       }
       // delete the indexStore
       dataStore.delete();
       
       // delete the entries from metadataregistry
        Iterator <String> it = mdFieldMap.keySet().iterator();
        // convert to lower case, as stored in the DB
        while (it.hasNext())
        {
            MetadataField mdField =  mdFieldMap.get(it.next());
            if (mdField != null)
                mdField.delete();
        }
       
       // remove entry from database
        DatabaseManager.delete(dbContext, myRow);

       // remove self from cache
        dbContext.removeFromCache(objectType, getID()); 
   } 
   
   /*------------------------------------------------------------------------------------------------------------*/
    /**
     * Delete  an image from the database and its image list.  
     * This is the reverse method of createImage() 
     * @param image
     *            the image to delete
     * @throws SQLException
     */
    public void deleteImageExtent(ImageExtent extent) throws SQLException
    {
        Iterator<ImageExtent> it =imageExtentList.iterator();
        while(it.hasNext())
        {
            ImageExtent existing = it.next();
            if (existing.getID() == extent.getID())      
            {
                extent.delete();
                it.remove();            // remove from imageList;
                break;
            }
        }

    }
    /*---------------------------------------------------------------------------------*/
   
     /**
     * Update the row in the database
     */
    public void update() throws SQLException
    {
        DatabaseManager.update(dbContext, myRow);
    }
    
    /*----------------------------------------------------------------------------------*/
    /** Get the root path where to store index files of this client
     * 
     * @return 
     */
    public String getIndexRoot()
    {
        return dataStore.getIndexRoot();
    }
    
  /*----------------------------------------------------------------------------------*/
    /** Get the root path where to store index files of this client
     * 
     * @return 
     */
    public String getThumbnailRoot()
    {
        if (isStoreThumbnails())
             return dataStore.getThumbnailRoot();
        return null;
    }
    
    /*-----------------------------------------------------------------------------------------------------*/
    /** Return the field values that are applicable for searching corresponding to a given
     * query value. This is because of the "default" value that is used when the image
     * was not provided with a value. (Example: query: male => search: male, unknown 
     */ 
    public  String[] getMetadataValuesForQuery(String fieldName, String queryValue)
    {
        MetadataField mdField = mdFieldMap.get(fieldName);
        String[] searchValues = null;
         if (!mdField.isSearchable())            // not a searchable field, so no valid values
                return null;

        //if there is no default value, only search for the queryValue
        String defaultValue = mdField.getDefaultValue();
        if (defaultValue == null || defaultValue.isEmpty())
            return  (new String[]{queryValue});

        if (queryValue.equalsIgnoreCase(defaultValue))
        { 
            searchValues = mdField.getValidValues() ;            // all values to search
        }
        else
        {
            searchValues = new String[2];
            searchValues[0] = queryValue;
            searchValues[1] = defaultValue;
        }
       return searchValues;
    }
    
  
   /*-----------------------------------------------------------------------------------------------------*/
}
       

