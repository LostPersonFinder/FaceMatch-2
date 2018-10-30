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
package fmservice.server.storage.index;

import fmservice.server.global.FMContext;
import fmservice.server.global.DBContext;

import fmservice.server.cache.dbcontent.ImageZone;
import fmservice.server.cache.dbcontent.ZoneDescriptor;
import fmservice.server.global.Scope;

import java.util.HashMap;

import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * <P>
 * IndexStoreManager stores, retrieves and deletes indexed data for  FaceMatch images
 * and updates the  database with corresponding information.
 </P>
 * 
 * @version $Revision: 1.0 $
 * @date $2015/03/03$
 */
public class IndexStoreManager
{
    /** log4j log */
    private static Logger log = Logger.getLogger(IndexStoreManager.class);  
    // constructor 
  
    protected FMContext fmContext;              // both memory and database objects
    protected DBContext dbContext;              // database only
    
    static  String fs = "/";                                     // file path separator
    
    // singleton objct for the FaceMatch Server
    public static IndexStoreManager indexStoreManager = null;
    
    public static HashMap <Integer, String> client2IndexRootMap = new HashMap();
    
    public IndexStoreManager(FMContext  context)
    {
       if  (indexStoreManager == null)
       {    
            this.fmContext = context;
            this.dbContext = context.getDBContext();
            indexStoreManager = this;
            
       }
    }
    
    public void addClient(int clientID, String rootPath)
    {
        client2IndexRootMap.put(new Integer(clientID), rootPath);
    }
        

    /*----------------------------------------------------------------------------------------------------------------------*/
      /**
     * <P>
     *  Delete the index data file stored on the disk for a specified descriptor and update the 
     *  database accordingly
     * </P>
     * @param descriptor
     *      Image descriptor
     * @return 
     *      1 = successful, 0 = file does not exist, -1 = any other error
     */
    public  int deleteIndexData(ZoneDescriptor descriptor)
    {
          String relativePath = descriptor.getFilePath();
        int zoneId = descriptor.getZoneID();
        try
        {
            // delete from the database
             ImageZone zone =  ImageZone.find( dbContext, zoneId);;
             if ( zone == null)
                return 0;
            zone.removeDescriptor(descriptor);
            zone.update();
            return 1;
        }
       catch (SQLException e)
       {
           log.error("Error removing descriptor iD:  " + descriptor.getID() + " from zone ID " + zoneId
           + " from database." , e);
           return 0;
       }
    }
      
     /*----------------------------------------------------------------------------------------------------------------------*/
    /**
     *  Update the index data for a specified zone in the file storage for a given client.
     * Then update  the corresponding information in the FM database descriptor table.
     * Note: this happens when image data is reindexed for images for a given index type
     *      with better algorithm etc.). Instead of deleting the corresponding database entry,
     *      we simply store the new data and update the database entry.
     *  
     *  @param zone 
     *     zone data object corresponding to the data
     * @param indexType
     *      Type of index data being stored
     * @param indexData
     *       index data for the given image/face and selected index type - to be stored
     * @return 
     *      ID of the zone  descriptor created in the database, with information about the stored data
     * @ NOTE - not currently used
     */ 
 
  /*   public  int updateIndexData( ImageZone zone,   String indexType, byte[] indexedData, String version) 
    {
        // find the database row with this data, if does not exist, create it
        String indexFilePath = "";
        int curVersion = 0;
        ArrayList<ZoneDescriptor>  descriptors = zone.getDescriptors();
        for (ZoneDescriptor  descriptor : descriptors)
        {
            if (descriptor.getIndexType().equals(indexType))            // indexed data exists
            {
                indexFilePath = descriptor.getFilePath();                   // relative path wrt  index root
               descriptor.setVersion(version);
            }
        }

        int imageId = zone.getImageID();
        try
        {
            FMImage image = FMImage.find(dbContext, imageId);
            if (image == null)
            {
               log.error ("Could not find image with ID: " +  imageId + " in facemach system.");
               return 0;
            }
            String imageUniqueId = image.getUniqueTag();
       
            // get higher level info corresponding to this zone
            Scope scope = Scope.getInstance();
            HashMap<String, String>   imageMetadata = image.getMetadataSet();

            // get Root path for all indexed data
            ImageExtent extent =  ImageExtent.find(dbContext, image.getExtentID());
            String rootPath =  scope.getIndexStoreRootPath(extent.getClientID());

            // Create the indexFile object and store the data
            IndexFile indexFile = new IndexFile(rootPath, extent.getName(),  indexType);
            IndexFileInfo fileInfo =  indexFile.updateIndexData(curVersion,
                imageId, imageUniqueId,   zone.getNumber(), 
                indexFilePath, imageMetadata, indexedData);
            if (fileInfo == null)
            {
              log.error("Could not store indexed data  for zone ID: "+ zone.getID() +
                  ", Index type: " + indexType + " in the indexStore" );
              return 0;
            }
            // Store index file information in the database for this zone
            return  updateIndexFileInfoInDB(zone, indexType,  fileInfo, version);
        }
        catch (SQLException  sqle)
        {
            log.error("SQL Exception in updating index data on store for image" + imageId, sqle);
            return 0;
        }     
    }*/
     /*--------------------------------------------------------------------------------------------------------------------------*/
      /**
    * Save information related to the stored index file in the Facematch database
    * @param zone
    * @param indexInfo
    * @return 
    */
 /*     protected int updateIndexFileInfoInDB(ImageZone zone,  String indexType, IndexFileInfo fileInfo, String version)
     {
        try
        {
            // Update the image descriptor object woth this information
            ZoneDescriptor descriptor = null;
           ArrayList< ZoneDescriptor> descriptors = zone.getDescriptors();
           for (ZoneDescriptor descr : descriptors)
           {
               if (descr.getIndexType().equals(indexType))
               {
                   descriptor = descr;
                   break;
               }
           }
           if (descriptor == null)
           {
               log.error ("No image descriptor of type " + indexType + " currently found in database for zone ID : "
                   + zone.getID());
               return 0;
           }
           // update the file information;
           descriptor.setFilePath(fileInfo.indexFilePath);
           descriptor.setFileSize(fileInfo.fileSize);
           descriptor.setFileChecksum(fileInfo.fileChecksum);
           descriptor.setChecksumAlgorithm(fileInfo.csAlgorithm);
           // update the version number and save
           descriptor.setVersion(version);
           descriptor.update();
           
            log.debug(" Updated  indexed data for descriptor:  " + descriptor.getID()); 
            return 1;
         }
         catch (SQLException sqle)
         {
             log.error("Could not insert image descriptor info for Zone ID: " + zone.getID() +
                 ", index Type: " + indexType + " to database.", sqle);
             return 0;
         }
    } 
*/      
    /*    public  static String getIndexFileRelativePath(String imageTag, int zoneNum, String indexType)
      {
             // compose the file name, based upon imageHandle etc.
            String fileExtension = FMConstants.IndexFileExtension;
            String fileName = IndexFile.makeIndexFileName(imageTag, zoneNum,  indexType, fileExtension);   
            return fileName;
      }
      
      public  static String getIndexFileName(String rootPath, String imageTag, int zoneNum, String indexType)
      {
             // compose the file name, based upon imageHandle etc.
          String fileName = IndexFile.makeIndexFileName(imageTag, zoneNum, indexType, FMConstants.IndexFileExtension);
          String fileFullPath = rootPath+File.pathSeparator+fileName;
            
            // Make sure that the directory path exists. If not, create it
            File file = new File(fileFullPath);
            File dir = file.getParentFile();
            if (!dir.exists())
                dir.mkdirs();
            return fileFullPath;
      }
  */
      /*------------------------------------------------------------------------------------*/
      public static IndexStoreManager getIndexStoreManager()
      {
          return indexStoreManager;
      }
        
        /*************************************************************************************************************
    * Generate the directory path where the image indexes are  to be stored, based upon the root directory, 
    * client and other factors. 
    * 
    * @param rootDir  index root for a client
    * @param imageGroup
    * @param imageMetadata
    *       must be given in predefined order
    * @param indexType      - type of Index created by Facematch
    * @return Directory name to store the indexed data of the specific type
    * 
     *************************************************************************************************************  */
   public static String getIndexFileDirName( int extentId, String indexType, String indexVersion)
       // HashMap<String, String>  imageMetadata, )
    {
        String topDir = Scope.getInstance().getIndexStoreRootPathForExtent(extentId);
        String filePath = topDir+fs+indexType+fs+indexVersion;
        return filePath;  
    }
     
}
//************************************************************************************************/
