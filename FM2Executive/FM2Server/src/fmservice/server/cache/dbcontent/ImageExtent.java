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
;
import fmservice.server.storage.rdbms.DatabaseManager;
import fmservice.server.storage.rdbms.TableRow;
import fmservice.server.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;

import java.util.List;
import org.apache.log4j.Logger;

/**
 *  This class represents a partition of images pertaining to certain event or category etc for a FMClient.
 * 
 *
 */
public class ImageExtent extends DBContentObject
{
        /** log4j logger */
    private static Logger log = Logger.getLogger(ImageExtent.class);
    
      /** Database context */
    private DBContext dbContext;

    /** The table row corresponding to this bundle */
    private TableRow myRow;
    
        /**  Images  for this Extent **/
   protected ArrayList<FMImage> imageList;
   
   boolean imageLoded = false;
    
    // Object type of this class
   public static String  objectType = "ImageExtent";
   public static String tableName = "imageextent";
   
    /*-----------------------------------------------------------------------------------------------*/
     /**
     * Insert a new row in the database  for an ImageExtent for a given FMClient.
     * Return a new fully formed  object corresponding to this row
     * 
     * @param context
     *           Facematch context object
     * 
     * @return the newly created FMImage object (with no lower level data)
     */
   static ImageExtent create(DBContext context, int client_id) throws SQLException
    {
        // Create a table row in the database (invoked only once)
        TableRow row = DatabaseManager.create(context,  tableName); 
         if (row == null)
            return null; 
        row.setColumn("client_id",  client_id);
        row.setColumn("creation_date",   Calendar.getInstance().getTime());     
        DatabaseManager.update(context, row);
        
        // Create the new ImageExtent object for Cache
        return new ImageExtent(context, row);
    }
    
    
    /*------------------------------------------------------------------------------------------------*/
   /** Instantiate an ImageExtent from the given database Row, and also save
    * it in the cache. 
    * This method is protected as it is invoked only from the create() method.
    * 
    * Store the id of all images that belong to this extent
    * 
    * @param context
    * @param row
    * @throws SQLException 
    */
     ImageExtent(DBContext context, TableRow row) throws SQLException
    {
        dbContext = context;
        myRow = row;

        // Don't add the Images until specifically requested to do so, because the extent may
        // be inactive and and we may or may not know that presently
        imageList = new ArrayList<FMImage>();

        // Add self to cache
        dbContext.addToCache(this);
    }    
     
      /*---------------------------------------------------------------------------------------------------------*/
     /** Add the set  of images  from  the database to its image list.
    * 
    * @return number of images added
    * @throws SQLException 
    * */
   protected   int addImages() throws SQLException
   {
        TableRowIterator tri = DatabaseManager.queryTable(
                dbContext, "fmimage",
                "SELECT fmimage.* FROM fmimage  WHERE "
                        + "extent_id ="+getID());

         while(tri.hasNext())
         {
             FMImage fmImage;
             TableRow tr = (TableRow) tri.next();
             if (!tr.getBooleanColumn("is_deleted"))                   // image not deleted
             {
                   fmImage = new FMImage(dbContext, tr);            //  automatically added to imageList
                   addImage(fmImage);
             }
         }
         tri.close();         // close the TableRowIterator to free up resources
        return imageList.size();
        }   
   
     /*----------------------------------------------------------------------------------------------------------*/
   // Return the list of all images in this Extent
   /*----------------------------------------------------------------------------------------------------------*/
   public ArrayList<FMImage> getImages() throws SQLException
   {
        // Note: An ImageExtent does not add its image names to list, until
       // getImages() is invoked the first time to avoid  extra time at system startup
        if (imageList == null || imageList.isEmpty())
                addImages();
          return imageList;
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
        return  myRow.getIntColumn("extent_id");
    }
    
     /**
     * Get the name  of this imageextent object
     * 
     * @returnname of the object
     */
    public String getName()
    {
        return  myRow.getStringColumn("extent_name");
    }
    
     /**
     * Get the description of this imageextent object
     * 
     * @return description  of object
     */
    public String getDescription()
    {
        return  myRow.getStringColumn("description");
    }
    
        
     /**
      * Get the date when the extent was added to the database
      * @return 
      */     
      public Date getCreationDate()
      {
            return myRow.getDateColumn("creation_date");
      }

    
    public boolean isActive()
    {
          return  myRow.getBooleanColumn("is_active");
    }
    
       /**
     * Get the internal ID (database primary key) of this object
     * 
     * @return internal ID of object
     */
    public int getClientID()
    {
        return  myRow.getIntColumn("client_id");
    }
    
     /**
     * Get type of performance the user wants for face detection for this Extent
     * 
     * @return  facefind_perf of object
     */
    public String getFacefindPerformanceOption()
    {
        return  myRow.getStringColumn("facefind_pref");
    }

    /*-----------------------------------------------------------------------------------------------*/
    //      Set methods
    //------------------------------------------------------------------------------------------------*/
    
    public void setName(String name) throws SQLException
    {
           myRow.setColumn("extent_name", name);
           update();
    }    
    
    public void setDescription(String descr) throws SQLException
    {
           myRow.setColumn("description", descr);
           update();
    }    
    
    
    public void   setActive( boolean active) throws SQLException
    {
         myRow.setColumn("is_active", active);
         update();
    }
    
    public void   setClientID(int clientId) throws SQLException
    {
         myRow.setColumn("client_id", clientId);
         update();
    }
    
     public void   setFacefindPerformanceOption(String option) throws SQLException
    {
         myRow.setColumn("facefind_pref", option);
         update();
    }

    /*----------------------------------------------------------------------------------------------------*/
    /**
     * Update the image extent row in the database
     */
    public void update() throws SQLException
    {
        DatabaseManager.update(dbContext, myRow);
    }


     /*---------------------------------------------------------------------------------------------------*/
    /** Delete self from the database and the cache, along with lower level objects
     * 
     * Note: We don't remove it from the imageList heer as the FMImage object does
     * that when deleted (in the image.delete() call
     * 
     * @throws SQLException 
     */
   protected void delete () throws SQLException
   { 
      //  delete images and their  underneath objects
       Iterator <FMImage> it = imageList.iterator();
       while ( it.hasNext())
       {
           FMImage image = it.next();
           image.delete();              // delete the image
       }
       imageList.clear();
       
       // remove self from cache
        dbContext.removeFromCache(getObjectType(), getID());   
        
       // remove entry from database
        DatabaseManager.delete(dbContext, myRow);
   }
   /*--------------------------------------------------------------------------------------------------*/
      /**
     * Find  an ImageExtent object in the system. 
     * First check the object cache, if not found, load from DB
     * Note: This is the way existing imageextents are loaded from the database at initialization time.
     * 
     * @param fmcontext
     *           context object
     * @param id
     *            ID of the image extent
     * 
     * @return the imageextent  object (added to cache), or null if the ID is invalid.
     */
    public static  ImageExtent find(DBContext context, int id) throws SQLException
    {
       // First check the cache
        ImageExtent cachedExtent  = (ImageExtent)context.getFromCache(objectType, id);
        if (cachedExtent  != null)
        {
            return cachedExtent;
        }
        TableRow row = DatabaseManager.find(context, tableName, id);
        if (row == null)
        {
            log.error("No  entry found in database table \"imageextent\" with ID = " + id);
            return null;
        }
         return new ImageExtent(context, row);
    }
    /*------------------------------------------------------------------------------------------------------------/
    // Image related methods for an ImageExtent
    /*------------------------------------------------------------------------------------------------------------*/
     /**
     * Create an Image in this extent
     * 
     * @param name
     *           Image Tag
     * @return the newly created image
     * @throws SQLException
     */
    public  FMImage createImage(String imageTag) throws SQLException
    {
        if ((imageTag == null) || imageTag.isEmpty())
        {
            throw new SQLException("FMImage must be created with non-null tag");
        }
      
        FMImage image = FMImage.create(dbContext, getID());
        image.setUniqueTag(imageTag);
        image.update();
        
        log.info("Created Image: ID = " + image.getID() + ", Tag: " + image.getUniqueTag()
            +", Extent ID: " + image.getExtentID());
        addImage(image);
        return image;
    }

    /**
     * Add  an  image from the database to this Extent's image list. 
     * 
     * The image may be being loaded from the database, or newly created with 
     * the createImage command
     * @param image
     *            the fmimage object to add
     * @throws SQLException
     */
    protected  void addImage(FMImage  image) throws SQLException
    {
        // First check that the  Image isn't already in the list
        for (FMImage existing : imageList)
        {
            if (existing.getID() == image.getID())            // already added
                return;
        }
        // Add the image  to in-memory list
        imageList.add(image);

        return;
    }
/*------------------------------------------------------------------------------------------------------------*/
    /**
     * Delete  an image from the database and its image list.  
     * This is the reverse method of createImage() 
     * @param image
     *            the image to delete
     * @throws SQLException
     */
    public void deleteImage(FMImage image) throws SQLException
    {
        Iterator<FMImage> it = imageList.iterator();
        while(it.hasNext())
        {
            FMImage existing = it.next();
            if (existing.getID() == image.getID())      
            {
                image.delete();
                it.remove();            // remove from imageList;
                break;
            }
        }

    }
    
   /*-----------------------------------------------------------------------------------------------*/
    public static ImageExtent[] findAll(DBContext dbContext) throws SQLException
    {
        TableRowIterator tri = DatabaseManager.queryTable(dbContext, "imageextent",
                "SELECT * FROM imageextent ORDER BY extent_id");

        List <ImageExtent>   imageExtents = new ArrayList();
        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
             ImageExtent  fromCache = (ImageExtent) dbContext.getFromCache(
                 objectType, row.getIntColumn("extent_id"));

            if (fromCache != null)
            {
               imageExtents.add(fromCache);
            }
            else
            {
                imageExtents.add(new ImageExtent(dbContext, row));
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        ImageExtent[] extentArray = new ImageExtent[imageExtents.size()];
        imageExtents.toArray(extentArray);

        return extentArray;
    }
}
