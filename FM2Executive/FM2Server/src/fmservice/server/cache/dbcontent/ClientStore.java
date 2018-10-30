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
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 *
 * This class contains index storage information for a given Client. 
 * Currently, ClientStores are allocated at a client level rather than at an 
 ImageExtent level 
 *
 */
public class ClientStore extends DBContentObject
{
       /** log4j logger */
    private static Logger log = Logger.getLogger(ClientStore.class);
    

    /** The table row corresponding to this bundle */
    private TableRow myRow;
    
         /** Database context */
     private DBContext dbContext;    
     //
     public static String  objectType = "ClientStore";
     public static String  tableName = "clientstore";
    
    /*------------------------------------------------------------------------------------------------*/
      /**
     * Construct an ClientStore object  from the given database table row. 
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     ***/
      public ClientStore(DBContext context, TableRow row) throws SQLException
    {
        dbContext = context;
        myRow = row;
        
          dbContext.addToCache(this);
          return;
    }  
    /*------------------------------------------------------------------------------------------------*/
    /**
     * Get the type of this object, found in Constants
     * 
     * @return type of the object
     */
    public  String  getObjectType()
    {
        return objectType;
    }
    
      /**
     * Get the Name  of this object, which is its Table name
     * 
     * @return type of the object
     */
     public  String getTableName()
     {
         return tableName;
     }

    /**
     * Get the internal ID (database primary key) of this object
     * 
     * @return internal ID of object
     */
    public int getID()
    {
        return  myRow.getIntColumn("store_id");
    }
      /**
     * Get the description for this storage entity
     * 
     * @return description
     */
    public String getDescription()
    {
        return myRow.getStringColumn("description");
    }
    
    /**
     * Get the root  path for storing index data for this client's images
     * 
     * @return root path for storing indexed data
     */
    public String getIndexRoot()
    {
        return  myRow.getStringColumn("index_root");
    }
    
    
      /**
     * Get the root  path for storing thumbnails for this client's images
     * 
     * @return root path for storing indexed data
     */
    public String getThumbnailRoot()
    {
        return  myRow.getStringColumn("thumbnail_root");
    }
    
    /*-----------------------------------------------------------------------------------------------*/
    public void delete() throws SQLException
    {
             // remove entry from database
        DatabaseManager.delete(dbContext, myRow);

       // remove self from cache
        dbContext.removeFromCache(objectType, getID()); 
    }
   
     /*-----------------------------------------------------------------------------------------------*/
     /**
     * Create a new ClientStore row in the database and instantiate a skeleton object
     * to be filled in later by the caller. 
     * 
     * @param context
     *           Database context object
     * 
     * @return the newly created FMImage object (with no lower level data)
     */
    public static ClientStore create(DBContext  context, int client_id) throws SQLException
    {
        // Create a table row
        TableRow row = DatabaseManager.create(context, tableName);
         if (row == null)
            return null;
        
        row.setColumn("client_id",   client_id);  
        row.setColumn("read_permission", true);                 // the default
        
        DatabaseManager.update(context, row);
        return new ClientStore(context, row);
    }
    /*-----------------------------------------------------------------------------------------------*/
  /**
     * Find  a clientstore object. First check the cache, if not there, load from Database
     * (which automatically adds to cache) 
     * 
     * @param fmContext
     *           FaceMatch  context object
     * @param id
     *            ID of the clientstore
     * 
     * @return the clientstore  object, or null if the ID is invalid.
     */
    public static ClientStore find(DBContext  context, int id) throws SQLException
    {
         // First check the cache
       ClientStore  clientStore  = (ClientStore)context.getFromCache(objectType, id);
       if (clientStore != null)
            return clientStore;
        
        TableRow row = DatabaseManager.find(context, tableName, id);
        if (row == null)
        {
            log.error("No  entry found in database table " + tableName + "  with ID = " + id);
            return null;
        }
         return new ClientStore(context, row);
    }

        public void setDescription(String description) throws SQLException
        {
            myRow.setColumn("description", description);
        }
        
        public void setIndexRoot(String indexRoot)  throws SQLException
        {
            myRow.setColumn("index_root", indexRoot);
         }
        
           public void setThumbnailRoot(String thumbnailRoot)  throws SQLException
        {
            myRow.setColumn("thumbnail_root", thumbnailRoot);
         }
           
         public void setReadPermission(boolean permission)  throws SQLException
        {
             myRow.setColumn("read_permission", permission);                            // not used
         }
/*******************************************************************************************/
    /**
     * Update the self   row in the database
     */
    public void update() throws SQLException
    {
        DatabaseManager.update(dbContext, myRow);
    }
}

