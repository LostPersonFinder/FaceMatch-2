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
import org.apache.log4j.Logger;

/**
 *
 * This singleton class holds Metadata fields and their valid values 
 * as MetadataFieldEntry objects for each FM Client.
 * 
 * Note: It is held separately, not added to the FM's object cache.
 * 
 *
 */
public class MetadataFieldRegistry
{  
    // Represents attributes of a single metadata field of a FMClient
    // Amn arralist of such Entris for each FMClient
    public  class MetadataFieldEntry
    {
        int fieldId;
        int clientId;
        String  fieldName;		 
        String fieldType;	  
        String[] validValues;
        String defaultValue;                           // must  be a valid one
        boolean searchable;                         // can be used as a seach criteria in query     
    }
        
    private static Logger log =  Logger.getLogger(MetadataFieldRegistry.class);

    // singleton class  object
    protected static MetadataFieldRegistry myRegistry = null;       
    
    protected static String MDF_REGISTRY_QUERY = 
        "Select * from metadatafieldregistry"; // "Select metadatafieldregistry.* from metadatafieldregistry";
    
    // All entries from DB for all clients
     protected   static HashMap<Integer, ArrayList<MetadataFieldEntry>>   metadataClient2FieldMap = null;
     
     // FieldID to Name mapping
     protected   static HashMap<Integer, String>metadataFieldId2Name ;
     
     /*--------------------------------------------------------------------------------------------------------*/
     
    public MetadataFieldRegistry (DBContext dbContext)  throws SQLException
    {
        if (myRegistry == null)
        {
            myRegistry  = this;
            loadMetadataRegistry(dbContext);
        }
    }  
     
   
 /*--------------------------------------------------------------------------------------------------------*/
  /** Return the static MetadataFieldRegistry object
   * 
   * @return 
   *--------------------------------------------------------------------------------------------------------*/   
  public  static MetadataFieldRegistry getMetadataFieldRegistry(DBContext dbContext)  
   {
       try
       {
            if (myRegistry == null)
            {
                myRegistry  = new MetadataFieldRegistry(dbContext);
            }
            return  myRegistry;
       }
       catch (SQLException sqle)
       {
           log.error("Could not load MetadataFieldRegistry from the database", sqle);
           return null;
       }
   }
    
    /*------------------------------------------------------------------------------------------------*/
    // Create a new Entry in the Metadata Registry
    //*----------------------------------------------------------------------------------------------*/
    protected MetadataFieldEntry createMetadataFieldEntry()
    {
        return new MetadataFieldEntry();
    }

    /*-------------------------------------------------------------------------------------------------------*
    // Do the initial loading of the MetadataFieldRegistry from database tables
    /*-------------------------------------------------------------------------------------------------------*/
   protected   int  loadMetadataRegistry(DBContext  dbContext)
    {
        metadataFieldId2Name = new HashMap<Integer, String>();
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(dbContext,
                "metadatafieldregistry", MDF_REGISTRY_QUERY);
            metadataClient2FieldMap = new HashMap<Integer, ArrayList<MetadataFieldEntry>>();
            
             while (tri.hasNext())
            {
                TableRow row = tri.next();
                MetadataFieldEntry mdEntry  =  createMetadataFieldEntry(row); 
                
                // check if already there is a metadata field for this client
                 Integer clientIdInt = new Integer(mdEntry.clientId);
                 ArrayList<MetadataFieldEntry>  entryList = metadataClient2FieldMap.get(clientIdInt);
                if (entryList == null)
                    entryList = new ArrayList<MetadataFieldEntry>();
                metadataClient2FieldMap.put(clientIdInt, entryList);
                entryList.add(mdEntry);
            }
            tri.close();
            return 1;
        }
        catch(SQLException se)
        {
            log.error("Error loading IndexType information from database", se);
        }
        return 0;
    }
   /*---------------------------------------------------------------------------------------------------------------*/
    protected MetadataFieldEntry  createMetadataFieldEntry(TableRow row)
    {
        MetadataFieldEntry mdEntry  = myRegistry.createMetadataFieldEntry();
        mdEntry.fieldId = row.getIntColumn("metadata_field_id");
        mdEntry.clientId =  row.getIntColumn("client_id"); 
        mdEntry.fieldName =  row.getStringColumn("field_name").toLowerCase(); 
        mdEntry.searchable =  row.getBooleanColumn("is_searchable"); 
                
        // store for later reference
        metadataFieldId2Name.put(new Integer(mdEntry.fieldId), mdEntry.fieldName);
                
        // Get the set of valid values for this metadata field (e,g, gender -> male| female|unknown)
        String[] validValues = null;
        String validSetStr=  row.getStringColumn("valid_set").trim(); 
        if (!validSetStr.isEmpty())
            validValues = validSetStr.split("\\|");
        mdEntry.validValues = validValues;

            // get default value, if specified
            mdEntry.defaultValue = row.getStringColumn("default_value");        // if not explicitly specified

             // for printing only
             if (mdEntry.validValues == null)
                     log.warn("No Valid values provided for metadata field \'" +  mdEntry.fieldName +"\', client_id " + mdEntry.clientId); 
             else
             {
                String validStr = validValues[0];
                for (int i =1; i < validValues.length; i++)
                    validStr += ", "+ validValues[i];
                log.info("Valid values for metadata field " +  mdEntry.fieldName +", client_id " + mdEntry.clientId + " are: " 
                    + validStr);
             } 
             return mdEntry;
    }

   /*---------------------------------------------------------------------------------------------------------------------*
   // update the Registry with MetadataField info for a new Client
    /*---------------------------------------------------------------------------------------------------------------------*/
   public  int  updateMetadataRegistry(DBContext  dbContext, int clientId)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(dbContext,
                "metadatafieldregistry", MDF_REGISTRY_QUERY);
             while (tri.hasNext())
            {
                TableRow row = tri.next();
                MetadataFieldEntry mdEntry  = createMetadataFieldEntry(row);
                
                // check if already there is a metadata field for this client
                 Integer clientIdInt = new Integer(mdEntry.clientId);
                 ArrayList<MetadataFieldEntry>  entryList = metadataClient2FieldMap.get(clientIdInt);
                if (entryList == null)
                    entryList = new ArrayList<MetadataFieldEntry>();
                metadataClient2FieldMap.put(clientIdInt, entryList);
                entryList.add(mdEntry);
            }
            tri.close();
            return 1;
        }
        catch(SQLException se)
        {
            log.error("Error loading IndexType information from database", se);
        }
        return 0;
    }

    /*-----------------------------------------------------------------------------------------------------*/
    public static ArrayList<MetadataFieldEntry> getClientMetadataFields(int clientId)
    {
        ArrayList<MetadataFieldEntry> clientEntries =   
            metadataClient2FieldMap.get(new Integer(clientId));
        return clientEntries;
    }
  /*-----------------------------------------------------------------------------------------------------*/  
    public static HashMap<String, String[]> getClientValidMetadataValues(int clientId)
    {
        ArrayList<MetadataFieldEntry> entryList =   getClientMetadataFields(clientId);
        if (entryList == null || entryList.isEmpty())
           return null;
       HashMap<String, String[]> validSet = new HashMap<String, String[]>();
       for (MetadataFieldEntry entry : entryList)
       {
           validSet.put(entry.fieldName, entry.validValues);
       }
       return validSet;
    }
    
      /*-----------------------------------------------------------------------------------------------------*/  
    public static HashMap<String, String[]> getClientSearchableMetadataValues(int clientId)
    {
        ArrayList<MetadataFieldEntry> entryList =   getClientMetadataFields(clientId);
        if (entryList == null || entryList.isEmpty())
           return null;
       HashMap<String, String[]> validSet = new HashMap<String, String[]>();
       for (MetadataFieldEntry entry : entryList)
       {
           if (entry.searchable)
                 validSet.put(entry.fieldName, entry.validValues);
       }
       return validSet;
    }
    
    public String getDefaultMetadataValue(String metadataFieldName, int clientId)
    {
        ArrayList<MetadataFieldEntry> entryList =   getClientMetadataFields(clientId);
        if (entryList == null || entryList.isEmpty())
          return null;
        for (MetadataFieldEntry entry : entryList)
        {
            if ( entry.searchable && entry.fieldName.equalsIgnoreCase(metadataFieldName) )
                  return entry.defaultValue;
        } 
        return null;
    }
    
    /*-----------------------------------------------------------------------------------------------------*/
    public static String getFieldId2Name(int fieldId)
    {
        return metadataFieldId2Name.get(new Integer(fieldId)) ;
   }
    
   /*-----------------------------------------------------------------------------------------------------*/
    // Note: Metadata field names are unique only for each client
    
    public static int getFieldName2Id(String fieldName, int clientId)
    {
       ArrayList<MetadataFieldEntry> entryList =   getClientMetadataFields(clientId);
        if (entryList == null || entryList.isEmpty())
           return 0;
        for (MetadataFieldEntry entry : entryList)
       {
           if (entry.fieldName.equalsIgnoreCase(fieldName))
               return entry.fieldId;
       }
       return 0;
   }
    /*-----------------------------------------------------------------------------------------------------*/
    /** Return the field values that are applicable for searching corresponding to a given
     * query value. This is because of the "default" value that is used when the image
     * was not provided with a value. (Example: query: male => search: male, unknown 
     */ 
    public static String[] getMetadataValuesForQuery(String fieldName, String queryValue, int clientId)
    {
        String[] searchValues = null;
        ArrayList<MetadataFieldEntry> entryList =   getClientMetadataFields(clientId);

        for (MetadataFieldEntry entry : entryList)
        {
            if (!entry.fieldName.equalsIgnoreCase(fieldName))
               continue;
            if (!entry.searchable)
                return null;
            
            //if there is no default value, only search for the queryValue
            if (entry.defaultValue == null || entry.defaultValue.isEmpty())
                return  (new String[]{queryValue});
            
            if (queryValue.equalsIgnoreCase(entry.defaultValue))
                searchValues = entry.validValues;               // all values to search
            else
            {
                searchValues = new String[2];
                searchValues[0] = queryValue;
                searchValues[1] = entry.defaultValue;
            }
            break;
        }       // end for
       return searchValues;
    }
   /*-----------------------------------------------------------------------------------------------------*/
}
