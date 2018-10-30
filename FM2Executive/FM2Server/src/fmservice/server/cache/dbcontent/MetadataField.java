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

import java.util.HashMap;

import java.sql.SQLException;
import org.apache.log4j.Logger;

/**
 *
 * This  class holds Metadata fields and their valid values for an fmclient
 *
 *
 */
public  class MetadataField extends DBContentObject
{
    public static  String objectType = "MetadataField";     // class name in Cache
    public static String tableName = "metadatafield";       // table name in daabase

    private static Logger log =  Logger.getLogger(MetadataField.class);

    // fields in the table
    /*  int fieldId;
    int clientId;
    String  fieldName;		 
    String fieldType;	  
    String[] validValues;
    String defaultValue;                           // must  be a valid one
    boolean searchable;                         // can be used as a seach criteria in query     */
    
    // map of field ID to name for faster performance
   protected static HashMap <Integer, String> mdFieldID2Name = new HashMap();

    private DBContext  dbContext;
    private TableRow myRow;
    /*--------------------------*/
      /**
     * Construct an  MetadataField object from an existing (or newly created)  database row
     * in the metadatafield table.
     * 
     * Fill the static information
     * 
     * @param dbcontext
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     ***/
    public  MetadataField(DBContext context, TableRow row) throws SQLException
    {
        dbContext = context;
        myRow = row;

        dbContext.addToCache(this);
    }
    
     /*-----------------------------------------------------------------------------------------------*/
     /**
     * Create a new MetadataField object in the database (inserting a new row in the
     * database table and return the corresponding in-memory object. 
     * 
     * @param context
     *           Database context object
     * 
     * @return the newly created MetadataField  object
     */
    public static MetadataField create(DBContext context, int client_id) throws SQLException
    {
        // Create a table row
        TableRow row = DatabaseManager.create(context, tableName);
        if (row == null)
            return null;
        row.setColumn("client_id",  client_id);
        
        DatabaseManager.update(context, row);    
        return new MetadataField(context, row);
    }

      /*--------------------------------------------------------------------------------------------------*/
      /**
     * Find  a metadatafield object. First check the cache, if not there, load from Database
     * (which automatically adds to cache) 
     * 
     * @param dbContext
     *           FaceMatch  context object
     * @param id
     *            ID of the client
     * 
     * @return the metadatafield   object, or null if the ID is invalid.
     */
    public static  MetadataField  find(DBContext context, int id) throws SQLException
    {
          // First check the cache
        MetadataField cachedEntry = (MetadataField)context.getFromCache(objectType, id);
        if (cachedEntry != null)
        {
            return cachedEntry;
        }
        // get from the database, if exists
        TableRow row = DatabaseManager.find(context, tableName, id);
        if (row == null)
        {
            log.error("No  entry found in database table "+ tableName +" with ID = " + id);
            return null;
        }
        // store entry the static map
        MetadataField mdField = new MetadataField(context, row);
        return mdField;
    }
 /*---------------------------------------------------------------------------------------------------------------*/
    // getter methods
    /*---------------------------------------------------------------------------------------------------------------*/
    /* Get the internal identifier of this zone
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("field_id");
    }
    
    // get the client ID
    public int getClientID()
    {
        return myRow.getIntColumn("client_id");
    }
    // get the field name
    public String getFieldName()
    {
        return myRow.getStringColumn("field_name");
    }
    // get the field type
    public String getFieldType()
    {
        return myRow.getStringColumn("field_type");
    }
        
    // get the valid value set  for this field
     public String getValidSet()
    {
        return myRow.getStringColumn("valid_set");
    }
     
    // get the set of individual valid values for this field
     public String[]  getValidValues()
    {
       String validStr = getValidSet();
       if (validStr != null)
           return (validStr.split("\\|"));
       return null;
    }
     
    // get the default value  for this field
     public String getDefaultValue()
    {
        return myRow.getStringColumn("default_value");
    }
     
     // is this a searchable field for queries
     public boolean isSearchable()
     {
         return myRow.getBooleanColumn("is_searchable");
     }
     
     // get the "note" , if any, associated for this field
     public String getScopeNote()
     {
         return myRow.getStringColumn("scope_note");
     }
     
  /*---------------------------------------------------------------------------------------------------------------*/
  // Setters  methods
  /*---------------------------------------------------------------------------------------------------------------*/
  
    /**
     * Set the client_id
     * */
    /*------------------------------------------------------------------------------*/
    public void setFieldName(String field_name)
    {
        myRow.setColumn("field_name", field_name);       
    }
    /*------------------------------------------------------------------------------*/
    public void setFieldType(String field_type)
    {
        myRow.setColumn("field_type", field_type); 
    }
     /*------------------------------------------------------------------------------*/
    public void setValidSet(String valid_set)
    {
        myRow.setColumn("valid_set", valid_set.trim()); 
    }
    /*------------------------------------------------------------------------------*/
    public void setdefaultValue(String default_value)
    {
        myRow.setColumn("default_value", default_value); 
    }
   /*------------------------------------------------------------------------------*/
    public void setSearchable(boolean searchable)
    {
        myRow.setColumn("is_searchable",  searchable); 
    }
    /*------------------------------------------------------------------------------*/
    public void setScopeNote(String scope_note)
    {
        myRow.setColumn("scope_note", scope_note); 
    }
    /*-------------------------------------------------------------------------------*/      
     /**
     * Update the row in the database
     */
    public void update() throws SQLException
    {
        DatabaseManager.update(dbContext, myRow);
    }
    /*-------------------------------------------------------------------------------*/
    public void delete() throws SQLException
    {
        // delete  self from database and cache
        DatabaseManager.delete(dbContext, myRow);
        
        // and remove from cache
        dbContext.removeFromCache(objectType, getID());
    }
    
    /**
     * Get the type of this object - same as the class name
     */
    public String  getObjectType()
    {
        return objectType;
    }
    
     /*--------------------------------------------------------------------------------------------------*/
    // static method
     public static  void  storeFieldID2Name( int mdFieldId, String fieldName)
     {
        mdFieldID2Name.put(new Integer(mdFieldId), fieldName);
     }
    /*--------------------------------------------------------------------------------------------------*/
    // static method
     public static  String  getFieldID2Name(DBContext dbContext, int mdFieldId)
     {
        return mdFieldID2Name.get(new Integer(mdFieldId));
     }
}
