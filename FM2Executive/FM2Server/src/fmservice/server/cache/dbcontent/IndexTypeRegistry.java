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

import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 *This static singleton class holds the Index registry information  stored in the FaceMatch database
* Note: The registry is maintained separately, not added to ObjectCache.
* 
 *
 */
public class IndexTypeRegistry
{
    private static Logger log =  Logger.getLogger(IndexTypeRegistry.class);
    
    // Query String to load thesingleton  IndexTypeRegistry from the database
    
    protected static String ITREGISTRY_QUERY = "Select * from indextyperegistry";
    public  static class IndexTypeInfo
    {
        int indexId;
        String  indexType;	  
        String fileExtension;
        String description; 
        boolean inUse;
    }
     
    protected static  HashMap<String, IndexTypeInfo> indexTypeMap = null; 
    protected static IndexTypeRegistry myRegistry = null;       // singleton object
    
    /*------------------------------------------------------------------------------------------*/
    public IndexTypeRegistry (DBContext dbContext) throws SQLException
    {
        if (myRegistry == null)
        {
            loadIndexRegistry(dbContext);
            myRegistry = this;
        }
    }     
    
    public static IndexTypeRegistry getInstance(DBContext dbContext)
    {
        try
        {
            if (myRegistry == null)
                myRegistry = new IndexTypeRegistry (dbContext);
            return myRegistry;
        }
        catch (SQLException e)
        {
            return null;
        }
    }
        
    /*------------------------------------------------------------------------------------------*/
    /**
     * <P>
     * Load information about different  image index types stored in the database
     * @param context 
     *      Database context for connection to Facematch DB
     */

    protected  void loadIndexRegistry(DBContext dbContext) throws SQLException
    {
        if (indexTypeMap != null)
            return;    
        indexTypeMap = new <String, IndexTypeInfo> HashMap();
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(dbContext, 
                "indextyperegistry", ITREGISTRY_QUERY);
             while (tri.hasNext())
            {
                TableRow row = tri.next();
                IndexTypeInfo  itInfo = new IndexTypeInfo();
                itInfo.indexId = row.getIntColumn("index_id");
                itInfo.indexType = row.getStringColumn("index_type");
                itInfo.fileExtension=row.getStringColumn("file_extension");
                itInfo.description = row.getStringColumn("description");   
                indexTypeMap.put(itInfo.indexType, itInfo);
            }
            tri.close();
        }
        catch(SQLException se)
        {
            log.error("Error loading IndexType information from database", se);
            throw(se);
        }
    }
       /**
     * Get the index Id  corresponding to a given index type.
     * 
     * @param indexType
     * @return 
     */
    public  int getIndexId(String indexType)
    {
        IndexTypeInfo  itInfo = indexTypeMap.get(indexType);
        if (itInfo == null)
        {
            log.warn("Index type " + indexType + " is currently not registered by the faceMatch system" );
            return 0;
        }
        return itInfo.indexId;
    }
   
     /**
     * Get the index Type corresponding to a given index ID
     * 
     * @param index Id
     * @return 
     */
    public   String getIndexType(int indexId)
    {
        
        Iterator <String> it = indexTypeMap.keySet().iterator();
        while (it.hasNext())
        {
            IndexTypeInfo  itInfo = indexTypeMap.get(it.next());
            if (itInfo.indexId == indexId)
                return itInfo.indexType;
        }
        log.warn("Index ID  " + indexId + " is  not currently recognized by the faceMatch system" );
         return null;
    }
    
    /**
     * Get the file extension corresponding to a given index type.
     * 
     * @param indexType
     * @return 
     */
    public   String getFileExtension(String indexType)
    {
        IndexTypeInfo  itInfo = indexTypeMap.get(indexType);
        if (itInfo == null)
        {
            log.warn("Index type " + indexType + " is currently not registered in by the faceMatch system" );
            return null;
        }
        return itInfo.fileExtension;
    }
  
   
    /**
     * Check if the index type (with its  feature set) is currently being used for similarity matching.
     * @param indexType
     * @return 
     */
     public   boolean isInUse(String indexType)
    {
        IndexTypeInfo  itInfo = indexTypeMap.get(indexType);
        if (itInfo == null)
        {
            log.info("Index type " + indexType + " is currently not registered by the faceMatch system" );
            return false;               // not known, so not being used
        }
        return itInfo.inUse;
    }
}
        
        
    