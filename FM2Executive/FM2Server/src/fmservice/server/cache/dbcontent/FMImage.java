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
import fmservice.server.storage.rdbms.TableRowIterator;


import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;

import java.awt.Rectangle;

import org.apache.log4j.Logger;

/**
 *
 *  FMImage object represents an fmimage table row, along with its metadata. 
 * 
 *
 */
public class FMImage extends DBContentObject
{
       /** log4j logger */
    private static Logger log = Logger.getLogger(FMImage.class);

    /** Our context */
    private DBContext dbContext;

    /** The table row corresponding to this bundle */
    private TableRow myRow;

    /** The  zones (e.g. faces)   in this image*/
    private ArrayList<ImageZone> zoneList;
    
    /** Metadata associated with the image **/
    private HashMap<String, ImageMetadata> metadataMap;
    
    /** Same info in type/value set */
     private HashMap<String, String> metadataValues;

    
    // Object type of this class
   public static String  objectType = "FMImage";
   public static String  tableName = "fmimage";
   
   boolean performance = false;

   static int ni = 0;
    /*-----------------------------------------------------------------------------------------------*/
     // Static methods
     /*-----------------------------------------------------------------------------------------------*/
    /**
     * Create an FMImage row in  the  database for an existing ImageExtent
     * 
     * @return The corresponding minimal FMImage object
     * Celled from another object (ImageExtent) in the same package
     * 
     * member elements of the Image are to be filled by  the caller and then 
     * the row is to be updated. Adding to ImageExtent is also to be done by caller
     */
    protected static FMImage create(DBContext context, int extent_id) throws SQLException
    {
        TableRow row = DatabaseManager.create(context,  tableName);
        if (row == null)
            return null;
        
        row.setColumn("extent_id",   extent_id);
        row.setColumn("creation_date",   Calendar.getInstance().getTime());     
   
        DatabaseManager.update(context, row);
        
        //context.commit();               // must commit for subsequent operations
        return  new FMImage(context, row);
    }

    /*---------------------------------------------------------------------------------------------------------*/
  /**
     * Construct an  FMImage object from the given table row in the
     *  database. 
     *  
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
     FMImage(DBContext context, TableRow row) throws SQLException
    {
        long startTime = System.nanoTime();
        
        dbContext = context;
        myRow = row;
        zoneList = new ArrayList();
        
        // field name vs. ImageMetadata row entries
        metadataMap = new HashMap<String, ImageMetadata>();
        
        // metadata field name vs. values
        metadataValues = new HashMap<String, String>();
       

        // Add the  zone List
        int image_id = myRow.getIntColumn("image_id"); 
        row.setColumn("is_deleted", false);
        
        TableRowIterator tri = DatabaseManager.queryTable(
                dbContext, "imagezone",
                "SELECT imagezone.* FROM imagezone  WHERE "
                        + "imagezone.image_id="+image_id);
        
        while (tri.hasNext())
        {
            TableRow tr = (TableRow) tri.next();
            addZone(new ImageZone(dbContext, tr));
        }
       // close the TableRowIterator to free up resources
        tri.close();
 
         long startTime2 = System.nanoTime();
         tri = DatabaseManager.queryTable(
                dbContext, "imagemetadata",
                "SELECT imagemetadata.* FROM imagemetadata  WHERE "
                        + "imagemetadata.image_id="+image_id);
        
        while (tri.hasNext())
        {
            TableRow tr = (TableRow) tri.next();
             int mdfieldId =  tr.getIntColumn("metadata_field_id");
             String fieldName = MetadataField.getFieldID2Name(dbContext, mdfieldId);
             metadataMap.put(fieldName, new ImageMetadata(dbContext, tr));
             metadataValues.put(fieldName, tr.getStringColumn("metadata_value"));
        }
        tri.close();
        
        long startTime3 = System.nanoTime();
        
        // add zones if it is an existing image in the database
        

        // add to cache
        dbContext.addToCache(this);
        ni++;
                 
        if (performance && ni < 100)                 // measure time to an an Image
        {
            long finishTime = System.nanoTime();
             float diff = (float)(finishTime - startTime)/(int) Math.pow(10, 6);
             float zoneTime = (startTime2-startTime)/(int) Math.pow(10, 6);
             float metadataTime = (startTime3-startTime2)/(int) Math.pow(10, 6);;

            log.trace("Loaded  image ID " + getID() +
          " in "  + diff +  " msec, Zone load time: " + zoneTime +" msec, " + 
                    "MetadataLoadTime: " + metadataTime + " msec");
         }
    }
     /**
     * Find  an  fmImage row in   the database.  
     * First check the object cache, if not found, load from DB
     * 
     * @param fmcontext
     *           context object
     * @param id
     *           Database row  ID of the image
     * 
     * @return the FMImage  object (added to cache), or null if the ID is invalid.
     */
   
    public static  FMImage find(DBContext context, int id) throws SQLException
    {
       // First check the cache
        FMImage cachedImage  = (FMImage)context.getFromCache(objectType, id);
        if (cachedImage != null)
        {
            return cachedImage;
        }
        TableRow row = DatabaseManager.find(context, tableName, id);            // use database table name
        if (row == null)
        {
            log.error("No  entry found in database table \"fmimage\" with ID = " + id);
            return null;
        }
         return new FMImage(context, row);
    }    
    
   /*-----------------------------------------------------------------------------------------------------*/     
   // Return the FMImage object with the specified  unique ID in this Extent
   // Note: UniqueID is a client's tag - unique within the extent, not within the database
   /*-----------------------------------------------------------------------------------------------------*/
   public static FMImage findImageByUniqueTag (DBContext  context,  int extentId, String uniqueTag) 
       throws SQLException
   {
       TableRow  tr = DatabaseManager.querySingleTable(
                context, "fmimage",
                "SELECT fmimage.* FROM fmimage  WHERE "
                + "extent_id="+extentId +" and "
                        + "unique_tag =\'"+ uniqueTag+"\'");
        if (tr == null)
            return null;
       
        // Check if this object is already in the cache
        FMImage cachedImage  = (FMImage)context.getFromCache(objectType, tr.getIntColumn("image_id"));
        if (cachedImage != null)
        {
            return cachedImage;
        }
         return new FMImage(context, tr);
   }   
   
   /*------------------------------------------------------------------------------------------------------------*/
     /**
     * Create a zone  in this image - called when the image is inserted to database
     * 
     * @return the newly created zone
     * @throws SQLException
     */
    public  ImageZone  createZone() throws SQLException
    {
        ImageZone zone  = ImageZone.create(dbContext, getID());
        addZone(zone);
        return zone;
    }



    /*-----------------------------------------------------------------------------------------------*/
    // ---- various get methods ----//
    
    public String getObjectType()
    {
        return objectType;
    }
 
    /**
     * Get the internal identifier of this zone
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("image_id");
    }
    
    public String getImageSource()
    {
        return myRow.getStringColumn("image_source");
    }
    
    public  String getThumbnailPath()
    {
        return myRow.getStringColumn("thumbnail_path");
    }
    
    public int getExtentID()
    {
        return myRow.getIntColumn("extent_id");
    }
   
    public String getUniqueTag()
    {
        return myRow.getStringColumn("unique_tag");
    }
    
    public Date getCreationDate()
    {
        return myRow.getDateColumn("creation_date");
    }

    
    public boolean isFacial()
    {
        return myRow.getBooleanColumn("is_facial");
    }
    
    public boolean isDeleted()
    {
        return myRow.getBooleanColumn("is_deleted");
    }
    
    /*-----------------------------------------------------------------------------------------------*/
    public int  getNumZones()
    {
        return zoneList.size();
    }
    /*-----------------------------------------------------------------------------------------------*/
    
    public ArrayList<ImageZone> getZones()
    {
        return zoneList;
    }
   /*----------------------------------------------------------------------------------------*/ 
    public ImageZone getZone(int zoneId)
    {
        for (ImageZone zone :  zoneList)
        {
            if (zone.getID() == zoneId)
                return zone;
        }
        return null;
    }
   /*-------------------------------------------------------------------------------------------*/ 
    public  ImageZone getZoneByRegion(Rectangle zoneRect)
    {
        ArrayList<ImageZone> zones = getZones();
        for ( ImageZone zone : zones)
        {
            if (zone.getDimensions().equals(zoneRect))
                return zone;
        }
        return null;            // no match
    }
 
     /*-------------------------------------------------------------------------------------------*/ 
    // Get the zone indeex of a given region in the image
    public int getZoneIndex(Rectangle rect)
    {
        ImageZone zone = getZoneByRegion(rect);
        return (zone == null ? -1 : zone.getIndex()) ;
     }
    /*-----------------------------------------------------------------------------------------------*/
    // return metadata objects
    public HashMap<String, ImageMetadata> getMetadata()
    {
        return metadataMap;
    }
    
    // Return metadata field name/value pairs for the image
    public HashMap<String, String> getMetadataValues()
    {
        return metadataValues;
     /*  HashMap<String, String>  metadataSet = new HashMap();
       Iterator<String> it = metadataMap.keySet().iterator();
       while (it.hasNext())
       {
           String field = it.next();
           String value  = metadataMap.get(field).getValue();
           metadataSet.put(field, value);
       }
       return metadataSet; */
    }

/*-----------------------------------------------------------------------------------------------*/
    /**
     * Add an existing zone to this image
     * Should be called after a create or when a new zone is added during face finding.
     * *
     * @param zone
     *            the  zone info to add
     */
    protected void addZone(ImageZone zone) throws SQLException
    {
        // First check that the  ImageZone isn't already in the list
        for (ImageZone existing : zoneList)
        {
            if (existing.getID() == zone.getID())
                return;
        }
        zoneList.add(zone);
        
        myRow.setColumn("num_zones", zoneList.size());
        DatabaseManager.update(dbContext, myRow);
    }
    

    /*-----------------------------------------------------------------------------------------------*/
    /** Add one or more Metadata fields to this image object.
     * Should be invoked once - when the image is created
     * 
     * mdMap is a map of metadata field name and the value of the field for this image
     * note: field name is not unique across all clients
     *----------------------------------------------------------------------------------------------------*/
     public int  addMetadata( HashMap<String, String> mdMap) throws SQLException
    {
         //Add an entry to the metadatavalue table
        ImageExtent extent  = ImageExtent.find(dbContext, getExtentID());
        int clientId = extent.getClientID();
        
        FMClient client = FMClient.find(dbContext, clientId);
        Iterator <String> it = mdMap.keySet().iterator();
        // convert to lower case, as stored in the DB
        while (it.hasNext())
        {
            String mdFieldName = it.next().toLowerCase();
            String fieldValue = mdMap.get(mdFieldName).toLowerCase();
            
           MetadataField mdField = client.getMetadataMap().get(mdFieldName);
            if (mdField == null)
            {
                log.error ("Invalid metadata field name " + mdFieldName + " provided image " + getUniqueTag());
                return 0;
            }
            ImageMetadata metadata = ImageMetadata.create(dbContext, getID(), mdField.getID(), fieldValue); 
            metadataMap.put(mdFieldName, metadata);
            metadataValues.put(mdFieldName, fieldValue);
        }
        DatabaseManager.update(dbContext, myRow);
        return 1;
    }
        /*------------------------------------------------------------------------------------------------*/
   /*    
     protected FMClient getClient()
     {
         try
         {
            if (myClient == null)
            {
                ImageExtent extent = ImageExtent.find(dbContext, getExtentID());
                FMClient  client =FMClient.find(dbContext, extent.getClientID());
                myClient = client;
            }
            return myClient;
         }
         catch (SQLException e)
         {
             log.error("Database exception in trying to client id for image " + getUniqueTag());
            return null;
         }
     }
     */
/*----------------------------------------------------------------------------------------------------*/
    /**
     * Update the image  row in the database
     */
    public void update() throws SQLException
    {
        DatabaseManager.update(dbContext, myRow);
    }

  
    
    /*------------------- Face related data --------------*/
     public void setFacial( boolean isFaceType)  throws SQLException
    {
        myRow.setColumn("is_facial", isFaceType);
        update();
    }

    /*------------------- Other Set methods  --------------*/
    public void setUniqueTag (String  uniqueTag)
    {
        myRow.setColumn("unique_tag", uniqueTag);
    }
    
    public void setImageSource (String  imageSource)
    {
        myRow.setColumn("image_source",  imageSource);
    }
    
    public void setThumbnailPath (String  thumbPath)
    {
        myRow.setColumn("thumbnail_path", thumbPath);
    }

     
    public void setNumZones(int numZones)
    {
        myRow.setColumn("num_zones", numZones);
    }
    
     // mark the image as deleted, but don't remove from the database 
     // currently not used
    public void setDeleted (boolean deleted)
    {
        myRow.setColumn("is_deleted", deleted);
    }

    /*-----------------------------------------------------------------------------------------------*/

     /**
     * Delete the FMImage row (from database).  Lower level objects  corresponding to the 
     * image  are removed first;
    *
    * Note: delete() ia always called from the higher level object (ImageExtent) in the same package
    * That is performed by the caller who deletes the image.
     */
    protected  void delete() throws SQLException
    {
        // delete the underneath zones
        for (int i = 0; i < zoneList.size(); i++)
        {
            ImageZone zone = zoneList.get(i);
            zone.delete();
        } 
        zoneList.clear();

        // delete the associated metadata in imagemetadata table
        for ( ImageMetadata metadata : metadataMap.values())
        {
           metadata.delete();
        }
        metadataMap = null;

        // Finally, delete own row from the database table
        DatabaseManager.delete(dbContext, myRow);
        
        // and remove from cache
        dbContext.removeFromCache(objectType, getID());
    }

    /*--------------------------------------------------------------------------------*/
    public void deleteZone(ImageZone zone)  throws SQLException
    {
        // First check that the  ImageZone is  in the list
        Iterator<ImageZone>  zoneIt = zoneList.iterator();
        while(zoneIt.hasNext())
        {
            ImageZone existing = zoneIt.next();
            if (existing.getID() == zone.getID())
            {
                zone.delete();
               zoneIt.remove();
               break;
            }
        }
        myRow.setColumn("num_zones", zoneList.size());
        update();
    }
    
   
    /*---------------------------------------------------------------------------------------------*/
    /** Get the set of  Index Descriptors  for a specific type corresponding 
     * to all its underneath zones.
     * Should be called after an image is fully added  to the database.
     * --------------------------------------------------------------------------------------------*/
    public ArrayList< ZoneDescriptor> getIndexDescriptors(DBContext dbContext) 
    {
        ArrayList<String> indexFileNames = new ArrayList();
        ArrayList <ImageZone> zones = getZones();
        if (zones == null || zones.isEmpty())
            return null;

        ArrayList<ZoneDescriptor> descriptors = new ArrayList();
        for (int i = 0; i < zones.size();  i++)
        {
           ArrayList<ZoneDescriptor> zoneDescriptors = zones.get(i).getDescriptors();
           if (zoneDescriptors != null)
               descriptors.addAll(zoneDescriptors);
        }
        return descriptors;
    }              
    
    /*---------------------------------------------------------------------------------------------*/
    /** Get the set of  Index Descriptor Filenames  for a specific type and 
     * current version (being used for query) corresponding  to all its underneath zones.
     * Should be called after an image is fully added  to the database.
     * --------------------------------------------------------------------------------------------*/
    public ArrayList< String> getIndexFileNames(DBContext dbContext, 
                 String indexType, String indexVersion)
    {
         ArrayList<ZoneDescriptor> descriptors = getIndexDescriptors(dbContext);
         if (descriptors == null || descriptors.isEmpty())
            return null;
         
        ArrayList<String> indexFileNames = new ArrayList();
        for (int i = 0; i < descriptors.size();  i++)
        {
            if (descriptors.get(i).getIndexType().equals(indexType) &&
                descriptors.get(i).getIndexVersion().equals(indexVersion))
                indexFileNames.add(descriptors.get(i).getFilePath());
        }
        return indexFileNames;
    }    
    
    /*---------------------------------------------------------------------------------------------*/
    /** Get the set of  Index Descriptor Filenames  for a specific type corresponding 
     * to all its underneath zones.
     * Should be called after an image is fully added  to the database.
     * --------------------------------------------------------------------------------------------*/
    public ArrayList< String> getIndexFileNames(DBContext dbContext, 
                 String indexType)
    {
        // String queryVersion =  IndexTypeRegistry.getInstance(dbContext).getCurrentVersion(indexType);
         ArrayList<ZoneDescriptor> descriptors = getIndexDescriptors(dbContext);
         if (descriptors == null || descriptors.isEmpty())
            return null;
         
        ArrayList<String> indexFileNames = new ArrayList();
        for (int i = 0; i < descriptors.size();  i++)
        {
           /*i -- delete -- not necessary
            f (descriptors.get(i).getIndexType().equals(indexType) &&
                descriptors.get(i).getIndexVersion().equals(queryVersion))*/
            indexFileNames.add(descriptors.get(i).getFilePath());
        }
        return indexFileNames;
    }    

  /*---------------------------------------------------------------------------------------------*/
    /** Get the set of  Index Descriptor Filenames  for a specific type and 
     * current version (being used for query) corresponding  to all its underneath zones.
     * Should be called after an image is fully added  to the database.
     * --------------------------------------------------------------------------------------------*/
    public ArrayList< String> getIndexFileNameList(DBContext dbContext, 
                 String indexType, String indexVersion)
    {
         ArrayList<ZoneDescriptor> descriptors = getIndexDescriptors(dbContext);
         if (descriptors == null || descriptors.isEmpty())
            return null;
         
        ArrayList<String> indexFileNames = new ArrayList();
        for (int i = 0; i < descriptors.size();  i++)
        {
                indexFileNames.add(descriptors.get(i).getFilePath());
        }
        return indexFileNames;
    }   
}
    

