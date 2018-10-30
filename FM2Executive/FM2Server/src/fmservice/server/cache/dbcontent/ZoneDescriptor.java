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
import  fmservice.server.storage.rdbms.TableRow;

import fmservice.server.storage.index.IndexFileInfo;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Class representing  descriptors of an image zone stored in the FaceMatch system.
 * <P>
 * When modifying the imagedescriptor metadata, changes are not reflected in the
 * database until <code>update</code> is called. Note that you cannot alter
 * the contents of a imagedescriptor; you need to create a new imagedescriptor.

 */
public class ZoneDescriptor extends DBContentObject
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ZoneDescriptor.class);

    /** Our dbContext */
    private final DBContext dbContext;

    /** The row in the table representing this zone descriptor */
    private final TableRow myRow;

   // Object type of this class
   public static String  objectType = "ZoneDescriptor"; 
   public static String  tableName = "zonedescriptor";
   
       
    /**
     * Create a new zone  descriptor, with a new  database row ID. 
     * called only when a new ZoneDescriptor row has to be entered into the database
     * 
     * @param dbContext
     *            FaceMatch  dbDBContextobject
     * @param fileInfo
     *            Information about the Stored index data corresponding to this imagedescriptor
     * 
     * @return the newly created imagedescriptor
     * @throws SQLException
     */
    public static ZoneDescriptor create(DBContext context,  int zone_id)
            throws SQLException
    {
         // Create a table row
        TableRow row = DatabaseManager.create(context, tableName);
        if (row == null)
            return null;
       
       // row.setColumn("descriptor_version", 1);
        row.setColumn("zone_id", zone_id);
        row.setColumn("creation_date",   Calendar.getInstance().getTime());     
        DatabaseManager.update(context, row);
        return  new ZoneDescriptor(context, row);
    }
    
       /*--------------------------------------------------------------------------------------------------*/
      /**
     * Find  a ZoneDescriptor  object in the system. 
     * First check the object cache, if not found, load from DB
     * 
     * @param fmcontext
     *           context object
     * @param id
     *            ID of the zonedescriptor object
     * 
     * @return the ZoneDescriptor  object (added to cache), or null if the ID is invalid.
     */
    public static  ZoneDescriptor  find(DBContext context, int id) throws SQLException
    {
       // First check the cache
        ZoneDescriptor cachedDescriptor  = (ZoneDescriptor)context.getFromCache(objectType, id);
        if (cachedDescriptor  != null)
        {
            return cachedDescriptor;
        }
        TableRow row = DatabaseManager.find(context, tableName, id);
        if (row == null)
        {
            log.error("No  entry found in database table \"zonedescriptor\" with ID = " + id);
            return null;
        }
         return new ZoneDescriptor(context, row);
    }
 /*--------------------------------------------------------------------------------------------------*/   

   
    /**
     * constructor for creating a ImageDescriptor object based on the contents
     * of a DB table row. Accessible only to other classes in the package
     * 
     * @param dbContext
     *            the dbContext this object exists in
     * @param row
     *            the corresponding row in the table
     * @throws SQLException
     */
     ZoneDescriptor(DBContext context, TableRow row) throws SQLException
    {
        dbContext = context;
        myRow = row;
        
          // Add self to cache
        //String handle = objectType+"_"+getID();
        //dbContext.getObjectCache().put(handle, this);
         dbContext.addToCache(this);
    }    

  /*-------------------------------------------------------------------------------------*/

    /**
     * Get the internal identifier of this zone descriptor
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("descriptor_id");
    }

    public int getZoneID()
    {
        return myRow.getIntColumn("zone_id");
    }
    /*----------------------------------------------------------*/
    public void setImageID(int imageId) throws SQLException
    {
        myRow.setColumn("image_id", imageId);
    }
    /*
    /** This method is invoked after the index file is written to the disk
     * 
     * @param fileInfo
     * @throws SQLException 
     */
    public void setIndexInfo(IndexFileInfo fileInfo) throws SQLException
    {
        myRow.setColumn("file_path", fileInfo.indexFilePath);
        myRow.setColumn("size_bytes", fileInfo.fileSize);
        myRow.setColumn("md5_checksum",  fileInfo.fileChecksum);
        // save info in the database
        update(); 
    };
    
/*---------------------------------------------------------------------------*/
        
    public Date getCreationDate()
    {
        return myRow.getDateColumn("creation_date");
    }
    
    public int getImageID()
    {
        return myRow.getIntColumn("image_id");
    }
    
     
    /*----------------------------------------------------------*/
    public void setIndexType(String type) throws SQLException
    {
       myRow.setColumn("index_type", type);
    }
     /**
     * Get the Index type of this stored data
     * 
     * @return index type name
     */
    public String getIndexType()
    {
      return myRow.getStringColumn("index_type");
    }
    
    public void  setIndexVersion(int version)
    {
       myRow.setColumn("index_version", version);
    }
    
     /**
     * Get the version  of the image descriptor file
     * 
     * @return the version number
     */
    public String getIndexVersion()
    {
      return myRow.getStringColumn("index_version");
    }
 /*-----------------------------------------------------------------------------------------*/    
    
    /**
    * Get the relative file path (w.r.t. the client's index root) where this index file is stored.)
     * 
     * @return the the image descriptor relative file path
     */
    public String getFilePath()
    {
        return myRow.getStringColumn("file_path");
    }
       
    /**
     * Get the checksum of the content of the image descriptor, for integrity checking
     * 
     * @return the checksum
     */
    public String getChecksum()
    {
        return myRow.getStringColumn("md5_checksum");
    }

  
    /**
     * Get the size of the image descriptor file
     * 
     * @return the size in bytes
     */
    public long getSize()
    {
        return myRow.getIntColumn("size_bytes");
    }

    /**
     * Update the Zone Descriptor. 
     * 
     * @throws SQLException
     */
    public void update() throws SQLException
    {
        DatabaseManager.update(dbContext, myRow);
    }

    /**
     * Delete the zonedescriptor row in the database.
     * 
     * The caller is responsible for deleting entry in the mapping table.
     * @throws SQLException
     */
    public void delete() throws SQLException
    {
        DatabaseManager.delete(dbContext, myRow);
         
        // remove from owning image's list
        int zone_id = getZoneID();
        ImageZone zone =  ImageZone.find(dbContext, zone_id);
        zone.removeDescriptor(this);
        
        // delete  self from database and cache
        DatabaseManager.delete(dbContext, myRow);
        
        // remove self from cache
        dbContext.removeFromCache(objectType, getID());  
        
        // Note: It does not delete the associated Image Desctiptor files. That is the responsibility
        // of the higher level object which invokes this.
    }
    
/*-------------------------------------------------------------------------------------------------------*/
    public void  setVersion(String version)
    {
       myRow.setColumn("index_version",  version);
    }
    
    // set the indexFilePath relative to the index root
    public void  setFilePath(String filePath)
    {
       myRow.setColumn("file_path",  filePath);
    }
    
    public void  setFileSize(long size)
    {
       myRow.setColumn("size_bytes", size);
    }
    
     public void  setChecksum(String checksum)
     {
        myRow.setColumn("md5_checksum", checksum);
     }
   
     /*------------------------------------------------------------------------------------*/
   public String getVersion()
    {
        return myRow.getStringColumn("version_number");
    }

    /**
     * Get the type of this object - same as the class name.
    */
    public String getObjectType()
    {
        return objectType;
    }
}

