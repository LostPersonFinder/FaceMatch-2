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

import fmservice.server.storage.rdbms.TableRow;
import fmservice.server.storage.rdbms.DatabaseManager;
import java.sql.SQLException;
import org.apache.log4j.Logger;

/**
 * This class holds the metadata fields and corresponding values associated with an Image. 
 * 
 *
 */
public class ImageMetadata extends DBContentObject
{
       /** log4j logger */
    private static Logger log = Logger.getLogger(FMImage.class);
    
    private DBContext dbContext;

    /** The table row corresponding to this bundle */
    private TableRow myRow;
    
   // Cache Object type of this class
   public static String  objectType = "ImageMetadata";
  public static String  tableName  = "imagemetadata";


  /**
     * Construct a ImageMetadata object with the given table row in the database
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    public ImageMetadata(DBContext context, TableRow row) throws SQLException
    {
        dbContext = context;
        myRow = row;
        
        dbContext.addToCache(this);
    } 
    
    // Note: this is really a shortcut of creating the object first and then setting the
    // field values
   public static  ImageMetadata create (DBContext context,  int imageId,
      int  metadataFieldId, String metadataValue) throws SQLException
    {
        // Create a new table row and set the column values
        TableRow row = DatabaseManager.create(context, tableName);
        row.setColumn("image_id", imageId);
        row.setColumn("metadata_field_id", metadataFieldId);
        row.setColumn("metadata_value", metadataValue);
        DatabaseManager.update(context, row);
         
        return new ImageMetadata(context, row);
    }
   
   public int getID() 
   {
       return myRow.getIntColumn("metadata_entry_id");
   }
   
   public int getFieldID() 
   {
       return myRow.getIntColumn("metadata_field_id");
   }
   
   public String getValue()
   {
        return myRow.getStringColumn("metadata_value");
   }
   
   public void delete() throws SQLException
   {
       DatabaseManager.delete(dbContext, myRow);
       dbContext.removeFromCache(objectType, getID());
   }
     /**
     * return type found in Constants
     */
   
    public String getObjectType()
    {
        return objectType;
    }

}
