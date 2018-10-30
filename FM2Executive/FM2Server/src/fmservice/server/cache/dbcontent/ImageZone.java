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
import  fmservice.server.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import java.awt.Rectangle;
import java.util.Calendar;

import org.apache.log4j.Logger;

/**
 * <P>
 Class representing a ImageZone of an image  stored in the FaceMatch system.
 This is a generalization of faces in an image; that is:
 for images with faces, each face is represented as a zone. 
 ZoneDescriptors are associated with each zone rather than the full image.
 </P>
 */
public class ImageZone extends DBContentObject
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ImageZone.class);

    /** Our context */
    private DBContext  dbContext;

    /** The table row corresponding to this bundle */
    private TableRow myRow;

    /** The ZoneDescriptors in this  zone */
    private ArrayList<ZoneDescriptor> descriptorList;
    
        
    // Object type of this class
   public static String  objectType = "ImageZone";
   public static String  tableName = "imagezone";
   /*------------------------------------------------------------------------------------------------------*/
    /**
    * Create a newImageZone, with a new ID. 
    * 
    * @param context
    *           FaceMatch context object
    * 
    * @return the newly created Zone
    */
   protected static ImageZone create(DBContext context, int image_id) throws SQLException
    {
       // Create a table row
       TableRow row = DatabaseManager.create(context, tableName);
       if (row == null)
            return null;
        
        row.setColumn("image_id",   image_id);
        row.setColumn("creation_date",   Calendar.getInstance().getTime());     
        DatabaseManager.update(context, row);
        return  new ImageZone(context, row);
    }

    /*------------------------------------------------------------------------------------------------------*/
     /**
     * Get a ImageZone object  in the system
      * @param fmcontext
     *           context object
     * @param id
     *           Database row  ID of the image zone
     * 
     * @return the ImageZone  object (added to cache), or null if the ID is invalid.
     */
   
    public static  ImageZone find(DBContext context, int id) throws SQLException
    {
       // First check the cache
        ImageZone cachedZone  = (ImageZone)context.getFromCache(objectType, id);
        if (cachedZone != null)
        {
            return cachedZone;
        }
        TableRow row = DatabaseManager.find(context, tableName, id);
        if (row == null)
        {
            log.error("No  entry found in database table \"imagezone\" with ID = " + id);
            return null;
        }
         return new ImageZone(context, row);
    }
   
    /*-------------------------------------------------------------------------------------------------------------------*/
    /**
     * Construct an ImageZone object with the given  database table row
     *  Accessible only within the  class 
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    ImageZone(DBContext context, TableRow row) throws SQLException
    {
        myRow = row;
        dbContext = context;
        
        descriptorList = new ArrayList();

        int zone_id = myRow.getIntColumn("zone_id");
       // Add the  Descriptor list from the database for this imagezone
        TableRowIterator  tri = DatabaseManager.queryTable(
                dbContext, "zonedescriptor",
                "SELECT zonedescriptor .* FROM zonedescriptor  WHERE "
                        + "zone_id = "+zone_id);
        
        while (tri.hasNext())
        {
            TableRow tr =  tri.next();
             addDescriptor(new ZoneDescriptor(dbContext, tr));
        }
        // close the TableRowIterator to free up resources
        tri.close();

        // add to cache
        dbContext.addToCache(this);
    }

  /*----------------------------------------------------------------------------------------------*/ 
     /**
     * Create a ZoneDescriptor  for  this Zone
     * 
     * @return the newly created imagedescriptor
     * @throws SQLException
     */
    public  ZoneDescriptor  createDescriptor() throws SQLException
    {
        ZoneDescriptor descriptor = ZoneDescriptor.create(dbContext, getID());
       addDescriptor(descriptor);
        return descriptor;
    }
 
    /**
     * Get the internal identifier of this zone
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("zone_id");
    }

    /**
     * Get the number of the zone
     * 
     * @return number of the zone within the set
     */
    public int getIndex()
    {
        return myRow.getIntColumn("zone_index");
    }

    /**
     * Set the number of the zone
     * */
    public void setIndex(int index)
    {
        myRow.setColumn("zone_index", index);
    }

 
    /*------------------------------------------------------------------------------*/
    public void setImageID(int imageId)
    {
        myRow.setColumn("image_id", imageId); 
    }
    

    public int  getImageID()
    {
        return myRow.getIntColumn("image_id"); 
    }
    
    public void setDimensions (Rectangle rect)
    {
        myRow.setColumn("zone_coord_x", rect.x);
        myRow.setColumn("zone_coord_y", rect.y);
        myRow.setColumn("zone_width", rect.width);
        myRow.setColumn("zone_height", rect.height);
    }
    
    
     public void setFace (boolean face)
     {
         myRow.setColumn("is_face", face);
     } 
     public void setProfile (boolean profile)
     {
         myRow.setColumn("is_profile", profile);
     }     
    
     public Rectangle getDimensions ()
    {
        Rectangle rect = new Rectangle();
        rect.x = myRow.getIntColumn("zone_coord_x");
        rect.y = myRow.getIntColumn("zone_coord_y");
        rect.width = myRow.getIntColumn("zone_width");
        rect.height = myRow.getIntColumn("zone_height");
        return rect;
    }
       
     public boolean isFace ()
     {
         return myRow.getBooleanColumn("is_face");
     } 
     
     public boolean isProfile (boolean profile)
     {
         return myRow.getBooleanColumn("is_profile");
     }
     
   /*-----------------------------------------------------------------------------------------------*/    
    /** Add a Descriptor  to this zone  object. 
     * Invoked to add existing descriptor  when a new descriptor is created
     * for this image (at ingest time)
     **/
     public void addDescriptor(ZoneDescriptor descriptor) throws SQLException
    {
        // First check that the Descriptor isn't already in the list. If exists,skip
        for ( ZoneDescriptor existing : descriptorList)
        {
            if (descriptor.getID() == existing.getID())
                return;
         }
         descriptorList.add(descriptor); 
    }
   /*-----------------------------------------------------------------------------------------------*/
    /**
     * Remove an  imageDescriptor from this zone's list 
     * @param descr
     *            the  imageDescriptor to remove. Does not delete the descriptor 
     *             from database here, but deletes the entry from the mapping table
     */
    public void removeDescriptor(ZoneDescriptor descr) throws SQLException
    {
          // First check that the  imageDescriptor is in the list
            Iterator <ZoneDescriptor> it = descriptorList.iterator();
        
            while(it.hasNext())
            {
                ZoneDescriptor existing = it.next();
                if (existing.getID() == descr.getID())
                {
                    it.remove();   
                    break;
                }
        }
    }
  
     /**
      * Get  the list of descriptors for this zone
      * @return 
      *         Descriptor List
      */
    public ArrayList<ZoneDescriptor> getDescriptors()
    {
        return descriptorList;
    }

    /**
     * Update the zone row in the database
     */
    public void update() throws SQLException
    {
        DatabaseManager.update(dbContext, myRow);
    }

    /**
     * Delete the zone row. ZoneDescriptors corresponding to the zone  are removed first;
    *
     */
   protected  void delete() throws SQLException
    {
        for (int i = 0; i<  descriptorList.size(); i++)
        {
            ZoneDescriptor descr = descriptorList.get(i);
            if (descr != null)
                descr.delete();
        }
        descriptorList = null;

        // delete  self from database and cache
        DatabaseManager.delete(dbContext, myRow);
        
        // and remove from cache
        dbContext.removeFromCache(objectType, getID());
    }
    
     /**
     * Get the type of this object - same as the class name.
     */
    public String  getObjectType()
    {
        return objectType;
    }
}   
/*----------------------------------------------------------------------------------------------------*/

    
  