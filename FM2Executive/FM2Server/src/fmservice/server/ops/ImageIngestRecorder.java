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
package fmservice.server.ops;

import fmservice.httputils.common.ServiceConstants;
import fmservice.server.global.FMContext;
import fmservice.server.global.DBContext;
import fmservice.server.global.Scope;

import fmservice.server.result.ImageIngestResult;
import fmservice.server.result.Status;

import fmservice.server.cache.dbcontent.FMImage;
import fmservice.server.cache.dbcontent.ImageZone;
import fmservice.server.cache.dbcontent.ZoneDescriptor;
import fmservice.server.storage.index.IndexFileInfo;

import fmservice.server.result.FaceRegion;
import fmservice.server.util.ChecksumCalculator;

import fmservice.server.cache.dbcontent.ImageExtent;

import java.util.HashMap;
import java.util.ArrayList;
import java.sql.SQLException;

import java.io.File;

import org.apache.log4j.Logger;

/**
 * This  class records ingest related information in the FM database.
 * This means it performs operations such as  updating the database tables
 * with new entries related to Image,  Zone, Descriptor etc. after ingest of a new Image ,
 * and also records the new IDs in the updated IngestResult object
 * 
 * Notes: 
 *  1. This method is invoked separately for each  zone (face) if the  image, with its own index data
 *  2. If the image is  re-ingested  for a different indexing type and/or version, it is stored in a different
 *      pail     
 *  3. An object is automatically added to the cache when is is created, as well as loaded 
 *      from the database. So, we don't explicitly add it to the cache.
 * 
 *
 */
public class ImageIngestRecorder 
{
    private  static Logger log = Logger.getLogger(ImageIngestRecorder.class);
    
    FMContext fmContext;
    DBContext  dbContext;
    
    // TBD: Store the metadata field name to ID for each client, rather than querying each time
   
    public ImageIngestRecorder (FMContext  fmCtx)
    {
        fmContext = fmCtx;
        dbContext = fmContext.getDBContext();
    }
    
    public synchronized FMImage storeImageIngestInfo( ImageIngestResult ingestResult, 
        HashMap<String, String> metadataMap)
    {
        // create and store database entries for the image, zones and zone descriptors (for index files)
        int extentId = ingestResult.extentId;
            
        String imageURL = ingestResult.imageURL;
        String imageTag = ingestResult.imageTag;
        String indexType = ingestResult.indexType;
        String thumbnailPath = ingestResult.thumbnailPath;
        
        int numRegions = ingestResult.getIngestedRegionCount();
        if (numRegions == 0)
            return null;                // no faces ingested for this image
        try
        {
            // TBD: Check if it is an existing image
            ImageExtent extent = ImageExtent.find(dbContext, extentId);
            FMImage image =extent.createImage( imageTag);
            image.setFacial(true);            // facial image, not wholeImage
            image.setImageSource(imageURL);
            image.setUniqueTag(imageTag);
            image.setThumbnailPath(thumbnailPath);
            image.update();
            int imageId = image.getID();

            dbContext.commit();         // for testing now
            ingestResult.setImageId( imageId);
            
            Scope scope = Scope.getInstance();
            String indexVersion = ingestResult.indexVersion;
            int  clientId = scope.getClientWithExtent(extentId).getID();
            String indexRootPath =  scope.getIndexStoreRootPath(clientId);
            int pathOffset = indexRootPath.length() +1;

            // Add each zone  corresponding to the ingested region
            // skip if it is a false region
           for (int i = 0; i < ingestResult.regionResults.size(); i++)
           {
               String indexFileFullname =  ingestResult.regionResults.get(i).indexFileName;
               if (indexFileFullname == null)       // region not ingested
                    continue;
               
               String regionStr = ingestResult.regionResults.get(i).faceregion;
               int zoneIndex = i;  //  region index  within the image, starts with 0

               // store only the relative path;
               String indexFilePath = indexFileFullname.substring(pathOffset);

               // Create the Zone object corresponding to the image region under the image
               FaceRegion region = FaceRegion.parseFMString(regionStr);
               ImageZone zone = image.createZone();
               zone.setDimensions(region.regionCoord);
               zone.setIndex(zoneIndex);
               zone.setFace(true);
               zone.setProfile(region.isProfile);
               zone.update();
               ingestResult.regionResults.get(i).setRegionId(zone.getID());

               // add the zone descriptor for this index data
               // create the IndexFileInfo object
               File file = new File(indexFileFullname);
               int  fileSize = (int) file.length();
               String checksum = ChecksumCalculator.getMD5Checksum(indexFileFullname);

               IndexFileInfo descriptorInfo =  new IndexFileInfo(imageId, imageTag, zoneIndex,  
                   indexType,  indexVersion,  indexRootPath);
               descriptorInfo.setIFilenfo(indexFilePath, fileSize, checksum, "MD5");

               ZoneDescriptor  descriptor =  zone.createDescriptor();
               descriptor.setIndexType(indexType);
               descriptor.setVersion( indexVersion);         
               descriptor.setImageID(imageId);
               descriptor.setIndexInfo(descriptorInfo);

               descriptor.update();
           }  
               
           // add imageMetadata 
           image.addMetadata(metadataMap);
           image.update();
           dbContext.commit();          // store permanently in the database
           dbContext.complete();        // close the connection to the database  too
           return image;
        }
        
        catch (SQLException sqle)
        {
            log.error("Database exception in saving ingest information", sqle);
            ingestResult.setStatus(ServiceConstants.DATABASE_ACCESS_EXCEPTION, 
               "Error storing imageInformation in database, error: " +  sqle.getMessage());
            return null;
        }
    }
    
    public Status removeImageIngestInfo(FMImage image, ImageZone zone)
    {
        ArrayList<ImageZone> zones;
        if (zone == null)
                zones = image.getZones(); 
        else
        {
            zones = new ArrayList();
            zones.add(zone);
        }
        String imageTag = image.getUniqueTag();
       String zoneID = (zone == null ? "" : " zone ID " + zone.getID());
        try
        {
             String infoMsg;
            // check if we should delete the full image or a single zone
             boolean deleteImage = (zone == null || image.getNumZones() == 1);
             if (!deleteImage)
             {
                 image.deleteZone(zone);
                 image.update();
                 infoMsg = "Deleted zone " +  zone.getID() + " from image with tag: " + imageTag;
             }
             else
             {
                 ImageExtent extent = ImageExtent.find(dbContext, image.getExtentID());
                 extent.deleteImage(image);
                 infoMsg =  " Deleted  image with tag: " + imageTag;
                 if (zone != null)   infoMsg += " and zone " + zone.getIndex();
                 infoMsg += " from ImageExtent  "  + extent.getName();
             }
             
             // commit the result and close the connection
             dbContext.complete();
             log.info(infoMsg);
             // return Success result 
             return new Status(ServiceConstants.SUCCESS, "Successfully removed Image and/or Region.");
        }
       catch (SQLException  sqle)
       {
           log.error("Database exception in saving ingest information", sqle);
           return new Status(ServiceConstants.DATABASE_ACCESS_EXCEPTION, 
               "Error in removing Image region, error: " +  sqle.getMessage());
       }     
    }
}
