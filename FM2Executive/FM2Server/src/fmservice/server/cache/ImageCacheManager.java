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
package fmservice.server.cache;

import fmservice.server.global.Scope;
import fmservice.server.global.FMContext;
import fmservice.server.global.DBContext;

import  fmservice.server.cache.dbcontent.FMClient;
import fmservice.server.cache.dbcontent.ImageExtent;
import fmservice.server.cache.dbcontent.FMImage;
import fmservice.server.cache.dbcontent.ImageZone;
import fmservice.server.cache.dbcontent.ZoneDescriptor;
import fmservice.server.ops.imageops.ImageSearchContext;
import fmservice.server.cache.dbcontent.IndexTypeRegistry;

import fmservice.server.result.FaceRegion;
import fmservice.httputils.common.ServiceConstants;
import fmservice.server.storage.index.IndexStoreManager;
import fmservice.server.util.Timer;
import net.spy.memcached.MemcachedClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.awt.Rectangle;

import java.sql.SQLException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;

import org.apache.log4j.Logger;


/*------------------------------------------------------------------------------------------------------------*/
 /** 
   Manage the in-memory  Image related cache by Creating and managing 
  * the InvertedMetadataTree for indexing of loaded images based upon 
  * their metadata.
 * 
 * Main (public) entries are:
 * At initialization time: 
 *      loadExtentImages - also builds the corresponding InvertedMetadata tree
 * Prior to first query
 *      initSearchContextsForQuery
 * During query operation
 *      getSearchContextsForQueryImage (query image)
 * Following ingest/removal of an Image : 
 *      addImageToSearchSet(), removeImageFromSearchSet
 * 
 * @see It also performs initialization for a memcached system for extensibility, but 
 * this feature is currently not used
 * 
 *
 */
public class ImageCacheManager
{
    private static Logger log = Logger.getLogger(ImageCacheManager.class);
    
   // parameters to be retrieved from the Configuration Properties file
    static String NUM_MC_SERVERS = "NumMemcachedServers";           // number of Memcached servers
    static String MEMCACHED_SERVER_ADDR = "MemcachedServerAddress";
    
    // Currently, we don't use Memcached
    public static boolean useMemcache = false;

    protected MemcachedClient  memcClient;
    protected FMContext fmContext;
    protected DBContext dbContext;
    protected Scope  scope;               // FacematchServer  Scope - static information
    
    protected boolean loadInactiveExtents = false;

    // List of  ImageExtents loaded to cache
    protected ArrayList<Integer> cachedExtents = new ArrayList<Integer>();        

    // Key:  composed of the extentId and searchable metadata field values of  ingested images
    // Value:   the InvertedMetadataTree objects containing the Handles of all Image Descriptors with matching metadata
    protected HashMap<Integer, InvertedMetadataTree> extent2metadataTreeMap = new HashMap();         // key: extentId;  
    
    // valid metadat field/valueSet  as defined for each client in the begining (same for all extents)
    // <fieldName, validValues>
    protected HashMap<Integer,  HashMap <String, String[]>>  clientToMetadataMap = new HashMap();
    
    protected HashMap<Integer, String> indexRootMap = null;
    
    //--------------------------------------------------------------------------------------------------------------------------------------
    // Set of image Indexing/Matching objects associated with each image extent, 
    // One ImageMatcher corresponds to one segment of a branch in the metadataTree for the extent
    // Note: ImageMatchers are C++ objects, accessed via JNI. So, we simply store their 
    // handles here in the context objects.
    //----  Note: -----
    // There is a single imageSearchContextMap for the FM2 server, with Unique keys,
    // Each Key is full segment pathname  as:  (treeName$metadataBranchname_segmentNumber)
    //          where treeName=clientName$extentName
    // Segment number starts with zero
    //--------------------------------------------------------------------------------------------------------------------------------------
     protected  HashMap<String, ImageSearchContext>  imageSearchCtxMap =  new HashMap();   
     
     IndexStoreManager indexStoreManager = null;         // manadger of index  files ob disk
     IndexTypeRegistry indexTypeRegistry  = null;
      
      // indexing to be used:  must be set for performing queries
    String indexType  = null;
    String indexVersion = null; 

    /**
     * Constructor.Instantiate the IndexCacheManager. Involves:
  -  Establish connections with the specified Memcached servers, 
  - Create and initialize the InvertedMetadataTree objects for each Client/Image Extents, unless specified otherwise
  - Load index related information to the cache. (Index Filenames and optionally index data)
     */
    public ImageCacheManager(FMContext context,  IndexStoreManager indexStoreMgr) 
      {
          fmContext = context;
          dbContext = fmContext.getDBContext();
          indexStoreManager = indexStoreMgr;
          scope = Scope.getInstance();
          indexTypeRegistry = IndexTypeRegistry.getInstance(dbContext);
          
          if (useMemcache)
          {
            int status =  initMemcachedSystem(fmContext.getFMConfiguration());
            if (status <= 0)
            {
               log.warn("No Memcached Servers found. Proceeding without Memcached service.");
            }
          }
          // indexType/version are checked in case the system/database supports multiple index types
          // 
          indexType = fmContext.getFMConfiguration().getProperty("index.type");
          indexVersion = fmContext.getFMConfiguration().getProperty("index.version");
         if (indexType == null || indexVersion == null)
         {
             log.error("No descriptor index type or version provided for query operation. Cannot perform query");
             return;
         }
         
         loadInactiveExtents = false;               // default, do not load images from unused/inactive extents
        
        String  loadAll = fmContext.getFMConfiguration().getProperty("load.allExtents");
         if (loadAll != null && loadAll.equalsIgnoreCase("true"))
             loadInactiveExtents = true;
      }
  /*---------------------------------------------------------------------------------------------------*/  
     /**
    * Initialize the Memcached system, by connecting to the corresponding servers
    */
    protected int initMemcachedSystem(Properties fmConfig)
    {
        // get the list of servers and corresponding port numbers we want to connect to
        String[] serverList = getMemcachedServers(fmConfig);
        if (serverList == null)
        {
            log.warn("Invalid Memcached Server Address provided. Cannot use  Memcached" );
            return 0;
        }
            
        // concatenate the addresses
        String serverAddr = serverList[0];
        for (int i = 1; i < serverList.length; i++)
            serverAddr += " " + serverList[i];
        try
        {
            // Get a memcached client connected to several servers with the binary protocol
            memcClient = new MemcachedClient(
                   new BinaryConnectionFactory(),
                    AddrUtil.getAddresses(serverAddr));
        }
        catch (Exception e)
        {
            log.fatal("Could not instantiate Memcached Client Cannot continue" );
            return -1;
        }
        return 1;
    }
    
    /*-----------------------------------------------------------------------------------------------------------*/
    /**
     * Return the Memcached server IP addresses and port numbers in Config file.
     * @param config
     * @return  Memcached Server addresses ( Host IP address + server's connection port number)
     */
    protected String[] getMemcachedServers(Properties config)
    {
         int nservers = 0;
        String num_mcServers = config.getProperty(NUM_MC_SERVERS);
        if (num_mcServers == null)
        {
            log.warn("No memcached server numbers specified, assuming 1");
            nservers = 1;
        }
        else   
            nservers = Integer.parseInt(num_mcServers);
        if ( nservers < 1)
        {
            log.error("Invalid Memcached server number specified. Must be 1 or more");
            return null;
        }
        String[]  serverAddrs = new String[nservers];
        for (int i = 0; i < nservers; i++)
        {
            // nth Server address expreseed as MemcachedServerAddress.n=IPAddress:port_number)
           serverAddrs[i] = config.getProperty(MEMCACHED_SERVER_ADDR+"."+(i+1));
            if (serverAddrs[i] == null || (serverAddrs[i].indexOf(":") == -1))            // no port number
            {
                log.error("No port number found for MemcachedServer  in" +   serverAddrs[i] );
                return null;
            }
        }
        return serverAddrs;
    }
    
 /*-------------------------------------------------------------------------------------------------------------*/
    /** Load metadata and index  information for images in specified extents to Cache.
     *  The InvertedMetadataTree is also built for each Extent for search
     * 
     * @param clientExtents  Map of clients and the  list of extent names to load
     *                                       
     * @return number of images loaded to cache
     *----------------------------------------------------------------------------------------------------------*/
    public int loadExtentImages(HashMap<String, ArrayList<String>> clientExtents)
    {
       scope = fmContext.getDomainScope();
       int numImages = 0;
       
       // verify nane of each client
        Iterator <String> it = clientExtents.keySet().iterator();
        while(it.hasNext())
        {
            String clientName =  it.next();
            String clientKey = scope.clientName2Key(clientName);
            if (clientKey == null)
            {
                  log.error("invalid Client name " + clientName + "specified in initial configuration. Skipping loading." );
                   continue;
            } 
            // Loop for each extent
            ArrayList<String> extentNames = clientExtents.get(clientName);
            if (extentNames == null || extentNames.isEmpty())           // none defined yet
                continue;
            
            for (String extentName : extentNames)
            {
                ImageExtent extent = scope.getImageExtent(clientName, extentName);
                if (extent == null )
                    continue;
                numImages += loadExtentImages(extent.getID());
            }
        }    // for all clients
        return numImages;
    }
/*------------------------------------------------------------------------------------------------------------*/
 /** Load All Images from the database to in-memory cache for an extent,
  *  Note: The InveretdMetadataTree is built if it does not exist and the Extent is Active.
  * @param extentId
  * @param loadInactive - load images from extents even if marked as inactive (for reindexing)
  * @return number of images loaded
  /*------------------------------------------------------------------------------------------------------------*/
    
    public  int loadExtentImages (int extentId)
    {
        Timer t1 = new Timer(); 
        ImageExtent extent = null;
        ArrayList <FMImage> imageList;
        try
        {
           extent =  ImageExtent.find(fmContext.getDBContext(), extentId);              // skips loading if already loaded to Cache
           if (!extent.isActive()  && !loadInactiveExtents)
              return 0;
           
           imageList = extent.getImages();
         }
        catch (Exception sqle)
        {
            log.error("Error loading image Extent data with extent_id " + extentId, sqle );
             return 0;
        }
        float imageAccessTime = t1.getElapsedTime();
        t1.resetStartTime();
        
        int numImages = imageList.size();
        if (numImages == 0)
            return 0;

        addToCachedExtentList(extentId);

        // Also build the InvertedMetadataTree for this extent
        buildInvertedMetadataTree(extent,   imageList);       
        float mdTreeBuildTime = t1.getElapsedTime();
        log.info("Extent: " + extent.getID() + " - Load time for " + numImages + " images: "+ imageAccessTime +" msec, "
                + "MDIndexTree build time: " + mdTreeBuildTime + " msec");

        return numImages;  
     }
    
      /*----------------------------------------------------------------------------------------------------------------*/ 
     protected void addToCachedExtentList(int extentId)
     {
         Integer extentID = new Integer(extentId);
         if (!cachedExtents.contains(extentID))
             cachedExtents.add(extentID);
     }

    /*-----------------------------------------------------------------------------------------------------------------------*/  
    /**
     * Build an InvertedMetadataTree with the given set of images, by placing them in the
     * appropriate branch. The Tree is added to the corresponding map
     * Note: No FaceMatch index information is added at this time.
      * @param extent - The ImageExtent Database object
      * @param imagList - List of images in that imageExtent
      * @return  The correcponding  InvertedetadataTree (one per ImageExtent)
       *-----------------------------------------------------------------------------------------------------------------------*/
        protected  InvertedMetadataTree buildInvertedMetadataTree(ImageExtent extent,  ArrayList <FMImage> imageList)
        {
             InvertedMetadataTree mdTree = extent2metadataTreeMap.get(extent.getID());
             if (mdTree != null)
             {
                 log.warn("MetadataTree for extent  " + extent.getName() + " was already built.");
                 return mdTree;
             }
            
             // Note branch names contain both client name and ExtentName
            String[] branchNames =  getMetadataBranchNames(extent);
            mdTree = new InvertedMetadataTree(extent.getID(), branchNames);
            
            // Add the set of images to appropriate branches of the tree
            for (int i = 0; i < imageList.size(); i++)                    // loop for each image
            { 
                FMImage fmImage = imageList.get(i);
                HashMap<String, String> imageMetadata = fmImage.getMetadataValues();
                mdTree.addImage(fmImage, imageMetadata);
           }         // end while for each image
            extent2metadataTreeMap.put(extent.getID(), mdTree);
           return mdTree;
       }  
        
   /*--------------------------------------------------------------------------------------------------------------*
    * Perform dynamic  ImageExtent related operation such as add/remove/set-inactive etc.
    /*--------------------------------------------------------------------------------------------------------------*/   
     
        public int  setExtentStatus(ImageExtent extent, boolean setActive)
        {
            int extentId = extent.getID();
            if (extentId <= 1)
            {
                log.error("Invalid extent ID " + extentId + " provided for facenatch operations");
                return -1;
            }
            InvertedMetadataTree mdTree =  extent2metadataTreeMap.get(extentId);
            int status =1;
            if (mdTree == null )
            {
                if (setActive)
                    // create a new tree and load images fron the disk to it
                   status =  loadExtentImages(extentId);         // automatically loads and sets active
                else
                {
                    log.warn ("Extent " + extent.getName() + " not currently loaded.");
                    status =  0;
                }
            }   
            else
            {
                mdTree.setTreeActive(setActive);
             }
            return status;  
        }
        
        /*-----------------------------------------------------------------------------------------------------*/
        // Add  a newimage to the system cache for facematch operations
        /*-------------------------corresponding----------------------------------------------------------------------------*/
        public int  addNewExtent(ImageExtent extent)
        {
            return loadExtentImages(extent.getID());
        }
      
        /*-----------------------------------------------------------------------------------------------------*/
        // remove an Extent from  for facematch operations
        // this means deleting  the corresponding Inverted Metadata tree and removing
        // the Extent from cache
        // Note: Actual deletion of its entries in thedatabase is performed else where.
        /*-----------------------------------------------------------------------------------------------------*/    
         public int  removeExtent(ImageExtent extent)
        {
            Integer extId = new Integer(extent.getID());
            InvertedMetadataTree mdTree =  extent2metadataTreeMap.get(extId);
            if (mdTree == null)
                log.warn ("Metadata for Image Extent " + extent.getName() + "was not presently loaded. ");
            else    
            {
                // First detele all entries for this extent  from imageSearchCtxMap, if there were privious ingests
                String[]  branchNames = mdTree.branchNames;
                String clientName = mdTree.clientName;
                String extentName = mdTree.extentName;
                String[] keys= CacheKeyGen.buildUniqueKeys(clientName, extentName, branchNames);
                for (int i = 0; i < keys.length; i++)
                {
                    if ( imageSearchCtxMap.containsKey(keys[i]) )
                    {
                            imageSearchCtxMap.remove(keys[i]);     
                            log.trace("--Removed searchContext " + keys[i] + " from cache");
                    }     
                }
            }
             
            // remove extent from cach
            extent2metadataTreeMap.remove(extId);
            
            if (cachedExtents.contains(extId))
                cachedExtents.remove(extId);


            return 1;
        }
                
    /*----------------------------------------------------------------------------------------------------------*/
    /** Return the applicable metadata branch names for an image extent, based upon
     * Searchable metadata fields and values of associated images.
     * Note: metadata branch names are unique with an Extent's (Inverted) MetadataTree.
     * All Extents for a Client have the same metadata branch  names.
     *----------------------------------------------------------------------------------------------------------*/
     public String[]  getMetadataBranchNames(ImageExtent extent)
     {
       HashMap <String, String[]> searchValuesMap = scope.getSearchableMetadataValueSet(extent.getID());
       String[]  branchNames =  CacheKeyGen.buildMetadataBranchNames(searchValuesMap);
       return branchNames;
     }
   
       /*----------------------------------------------------------------------------------------------------------*/
    /** Return the unique SearchContextKeys  names for an image extent, based upon
     * Searchable metadata fields and values of associated images.
     * Note: SearchContextKeys  are unique across Clients and extents, as they have the format
     * <clientName>$<ExtentName>$[metadata branch names]. They are used as
     * the keys to the singleton ImageMetadataMap object, where the Map's value is an Array with 
     * the imageIds of all images with that metadata.
     *----------------------------------------------------------------------------------------------------------*/
     /*----------------------------------------------------------------------------------------------------------*/
     public String[] getSearchContextKeys(ImageExtent extent)
     {
         String[]  branchNames = getMetadataBranchNames(extent);
         String[] extentInfo  = Scope.getInstance().getExtentNameInfo(extent.getID());
         
         String[] contextKeys = CacheKeyGen.buildUniqueKeys(extentInfo[0], extentInfo[1], branchNames);
         return contextKeys;
     }
     
      public String[] getSearchContextKeys(ImageExtent extent, String[] branchNames)
     {
         String[] extentInfo  = Scope.getInstance().getExtentNameInfo(extent.getID());
         String[] contextKeys = CacheKeyGen.buildUniqueKeys(extentInfo[0], extentInfo[1], branchNames);
         return contextKeys;
     }
     
 /*------------------------------------------------------------------------------------------------------------------------------------*/
 /** Create and initialize search context to perform query for a set of image for an imageExtent. 
  * Note:  A SearchContext  corresponds to one branch of the inverted metadata tree, with  corresponding
  *  index filenames. It is specific to each extent and uses only the branch name (no client/extent names)
  * 
  * Search contexts for all existing extents are created at initialization time 
  **------------------------------------------------------------------------------------------------------------------------------------*/
     
    public synchronized  ImageSearchContext[]  initSearchContextsForQuery(ImageExtent extent)
    {
        //----------  get the list of images to be queries for this extent  ----------------
         if (indexType == null || indexVersion == null)
         {
             log.error("No descriptor index type or version set for query operation. Cannot perform query");
             return null;
         }
        String indexRootPath = scope.getIndexStoreRootPath(extent.getClientID());
        String rootPath =  (indexRootPath == null) ? "" : indexRootPath+"/";        // relative or absolute path

         ArrayList<FMImage> imageList ;
        try
        {
             imageList = extent.getImages();
        }
        catch (SQLException sqle)
        {
            log.error("Exception in getting image list for Image Extent: " + extent.getID(), sqle);
            return null;
        }
         if (imageList == null || imageList.isEmpty())
         {
                log.warn("No images exist in the  Image Extent: " + extent.getID());
         }
         
         //-----------  Retrieve the InvertedMetadataTree for these image, if already built  ----------
         // Note:We create the empty Inverted MetadataTree anyway as images may be added to it during ingest
         // A new tree is built for new or reactivated extents
         //
        InvertedMetadataTree metadataTree = extent2metadataTreeMap.get(new Integer(extent.getID()));
        if (metadataTree == null)
        {
            InvertedMetadataTree mdTree = buildInvertedMetadataTree(extent, imageList);
            if (mdTree == null)
            {
                log.error("No InvertedMetadataTree created for ImageExtent ID: " + extent.getID() + "Cannot query for match");
                return null;
            }
            metadataTree = mdTree;
        }
        //----------------  Create the corresponding SearchContexts based upon metadata and number of images ----------
        // create the search contexts with the index descriptor file names of images 
        // in each segment of a metadata branch
        
        String[] extentInfo  = Scope.getInstance().getExtentNameInfo(extent.getID());
        String clientName = extentInfo[0];
        String  extentName = extentInfo[1];
        
        ArrayList <ImageSearchContext> sctx = new  <ImageSearchContext>  ArrayList();
        InvertedMetadataTree.BranchNode[] branches   =  metadataTree.getBranchNodes();
        int nb = branches.length;
        for (int i = 0; i < nb; i++)
        {
             String  branchName = branches[i].branchName;
             int numSeg = branches[i].subNodes.size();
             for (int j  =0; j < numSeg; j++)
             {
                    ImageSearchContext searchContext =  
                        buildSearchContextForBranch(rootPath, extent, metadataTree, branchName, j);
                    if (searchContext == null)
                        continue;
                    searchContext.setIndexInfo(indexType, indexVersion);
                    sctx.add(searchContext);

                    String contextKey = CacheKeyGen.buildUniqueSearchContextKey(clientName, extentName, branchName, j);
                    imageSearchCtxMap.put(contextKey, searchContext);
             }
        }
         int nsctx  = sctx.size();
         ImageSearchContext[] searchContexts = new ImageSearchContext[nsctx];
         sctx.toArray(searchContexts);
        return searchContexts;
    }
  
    /*------------------------------------------------------------------------------------------------------------*/
    // Determine the set of  images that should be searched for the query image,
    // based upon the image's  specified metadata (argument #2)
    // Note:  A metadata value such as "unknown" would force more than one branch to be queried
    /*------------------------------------------------------------------------------------------------------------*/
   public  ImageSearchContext[]  getSearchContextsForQueryImage(ImageExtent extent,
       HashMap<String, String> imageMetadata )
   {
      // Determine the metadata values that should be covered for each metadataField
       // constant for each client
       FMClient client = Scope.getInstance().getClientWithExtent(extent.getID());
       HashMap <String, String[]> metadataMap = new HashMap();
       Iterator <String> it = imageMetadata.keySet().iterator();
       while (it.hasNext())
       {
           String mdField = it.next();
           String mdValue = imageMetadata.get(mdField);
           // given this metadata value (such as "unknown", get the actual set that needs to be searched
           String[] searchValues = client.getMetadataValuesForQuery(mdField, mdValue);
           if (searchValues == null)
               continue;
           metadataMap.put(mdField, searchValues);
       }
       //----------------------------------------------------------------------------------------------------------
       // build the metadataBranchName for each of these entries and add to the list
       //
       String[] extentInfo  = Scope.getInstance().getExtentNameInfo(extent.getID());
       String[] queryBranchNames = CacheKeyGen.buildMetadataBranchNames(metadataMap);
       String[] queryContextKeys = getSearchContextKeys(extent, queryBranchNames);
       int nb =queryContextKeys.length;
       
       //----------------------------------------------------------------------------------------------------------
       // Find the search contexts which has images with this metadata combination
       // Note that there are more than one SearchContent with same metadata but with 
       // different segment numbers
       //----------------------------------------------------------------------------------------------------------
      ArrayList< ImageSearchContext>searchCtxList = new ArrayList();
       for (int i  = 0; i < nb; i++)
       {
           ArrayList<ImageSearchContext> searchCtxs = getImageSearchContexts(queryContextKeys[i]);
           if (searchCtxs != null)
           {
               searchCtxList.addAll(searchCtxs);
               log.trace("Added  " + searchCtxs.size() + " search paths  for image query.");
           }
       }
       
       int n = searchCtxList.size();
       ImageSearchContext[] searchContexts = new ImageSearchContext[n];
       searchCtxList.toArray(searchContexts);
      log.info("Using  " + n + " search contexts for image query." );
       return searchContexts;
   }
 
    /*----------------------------------------------------------------------------------------------------*/
    /* Build (create and initialize) the search context corresponding to a metadata branch
    * which means loading all image IDs for that branch (with corresponding metadata)
   /*----------------------------------------------------------------------------------------------------*/

    protected ImageSearchContext  buildSearchContextForBranch(String indexRootPath,
        ImageExtent extent,   InvertedMetadataTree metadataTree, String branchName, int segment)
    {
        
        ImageSearchContext searchCtx = new ImageSearchContext(extent.getID(),  branchName, segment);
        searchCtx.setIndexInfo(indexType, indexVersion);
        
        ArrayList<String> descriptorFileNames = new ArrayList();
        ArrayList <Integer> imageIDList = metadataTree.getImagesInSegment(branchName, segment);
        if (imageIDList == null || imageIDList.isEmpty())
        {
            return searchCtx;           // nothing to load
        }     
        log.debug("Extent: " + extent.getName()+ ", branch: " + branchName +", number of images loaded: " + imageIDList.size());
      
        for (Integer  imageID : imageIDList)
        {
            // get the descriptor fileNames
            FMImage image = null;
            try
            {
                image = FMImage.find(dbContext, imageID.intValue());
                if (image == null)
                {
                    // should not happen, if so simply ignore the error
                    log.error("Database error: Could not findFMImage with ID: " + imageID);
                    continue;
                }
            }
            catch (Exception e)
            {
                log.error ("Got Exception in retrieving FMImage with ID: " + imageID, e );
                continue;
            }
            ArrayList<String> fileNameList = getDescriptorFileNames(image,  indexType);
            if (fileNameList != null)
                descriptorFileNames.addAll(fileNameList);    
        }

         int ndescr = descriptorFileNames.size();
        ArrayList<String> fileNames = new ArrayList();  
        for (int i = 0; i < ndescr; i++)
        {
            fileNames.add( indexRootPath+descriptorFileNames.get(i));
        }
        searchCtx.storeIndexFileNames(fileNames);
        return searchCtx;
    }
        
    /*----------------------------------------------------------------------------------------------------*/
     // Get the descriptors for all zones of this image for given index type
     protected ArrayList getDescriptorFileNames(FMImage image, String indexType)
     {
         ArrayList<ZoneDescriptor> descriptors =  image.getIndexDescriptors(dbContext);
         if (descriptors == null)
             return null;           // nothing indexed yet -- should not happen
         return image.getIndexFileNames(dbContext, indexType);
     }
           
    /*----------------------------------------------------------------------------------------------------*/
    // Get the index descriptor data, generated by the FaceMatch Library, for 
    // all zones of this image for given index type to cache buffers.
    // Note: This method is not currently used
    //-------------------------------------------------------------------------------------------------------*.
    protected int loadIndexDataToCache(FMImage image, String indexType)
    {
        ArrayList<ZoneDescriptor> descriptors =  image.getIndexDescriptors(dbContext);
        if (descriptors == null)
            return 0;           // nothing indexed yet

        // retrieve and store data corresponding to each descriptor of given type
        ZoneDescriptor descriptor  = null;
        for ( ZoneDescriptor adescriptor : descriptors)
        {
            if (adescriptor.getIndexType().equals(indexType) &&  
                adescriptor.getIndexVersion().equals(indexVersion))
            {
                descriptor = adescriptor;
                break;
            }
        }
        if (descriptor == null)
        {
            log.error(indexType + " descriptors not found for image ID: "+ image.getID());
            return 0;
        }
        byte[] indexedData = loadIndexedDataFromStore(descriptor);
        if (indexedData == null)
        {
            log.error( "No index data found for indexType" + indexType + "  for image ID: "+ image.getID());
            return 0;
        }
        String descriptorHandle = CacheKeyGen.buildHandle(
           descriptor.getObjectType(), descriptor.getID());

          // Store the data as key,value pair under memcached.
         memcClient.add(descriptorHandle, 0, indexedData);
         return 1;
    }
    /*----------------------------------------------------------------------------------------------------*/
    /** Add a new image to be included for followup Queries after ingest is completed .
    // Involves: (a) add it to the  the corresponding InvertedMetadataTree
    //          and (b) update the corresponding Search Contexts
    //-----------------------------------------------------------------------------------------------------*/
    public  int  addIngestedImageToSearchSet(FMImage fmImage, String  ndxType, String ndxVersion)
    {
         // first determine the MetadataTree for this extent
       InvertedMetadataTree metadataTree = getMetadataTree(fmImage.getExtentID());

        // add the image to this tree and get the branch where placed (based upon its metadata)
        // Note: InvertedMetadataTree does not depend upon indexType, so we always place the image
       String segmentName =  metadataTree.addImage(fmImage, fmImage.getMetadataValues());
       if (segmentName == null)
       {
           log.error ("Could not place image " + fmImage.getUniqueTag()+ "  in metadata index tree");
           return 0;
       }
       // If these are no face regions in this image, don't add it to search context)
       if ( fmImage.getNumZones() == 0)
       {
           log.warn ("No face regions found in image " + fmImage.getUniqueTag()
               +"; not added to  search list");
           return 0;
       }
       //Update the search context to include the given image for query
       // We do that only if the ingested image is indexed for the current version of indexing

       if (!ndxType.equalsIgnoreCase(indexType) || !ndxVersion.equalsIgnoreCase(indexVersion))
       {
                log.warn ("Stored index/version types:  (" + indexType +","+indexVersion +  ")   and search types: ( " + ndxType  +","+ndxVersion 
            + ") don't match.  Index data not added to seatch.");      
           return 0;                // do not add for search
       }
        //Update the search context to include the given image for query
        // Note: each segmentName is unique because of its prefix of "clientName$extentName$"
        int nf = addIngestedImageToSearchContext(segmentName, fmImage);
        int status = (nf > 0) ? 1 : 0;
        if ((status == 1) && useMemcache)
             status = loadIndexDataToCache(fmImage,  indexType);
        return status;
    }

    /*------------------------------------------------------------------------------------------------------------------------*/
    // Get the InvertedMetadataTree where image information related to metadata about a given Extent is  placed
    // If the tree does not exist, create it and its top level nodes (called branches)
    /*------------------------------------------------------------------------------------------------------------------------*/
    public InvertedMetadataTree getMetadataTree(int extentId)
    {
        Integer extentID = new Integer(extentId);
        InvertedMetadataTree mdTree  = extent2metadataTreeMap.get(extentID);
        if (mdTree == null)         // tree for a new 
        {
            mdTree = createInvertedMetadataTree(extentId);
            extent2metadataTreeMap.put(extentID, mdTree);
        }
        return mdTree;    
    }
     
    /*------------------------------------------------------------------------------------------------------------------------*/
      /** Create and initialize a InvertedMetadataTree, with appropriate branches, corresponding
       *   to a given ImageExtent
       * 
       * @param extentId
       * @return 
       *-------------------------------------------------------------------------------------------------------------------------*/
    protected InvertedMetadataTree createInvertedMetadataTree (int extentId)
    {
        int clientId = scope.getClientWithExtent(extentId).getID();
        Integer  clientID = new Integer(clientId);

        // All searchable metadata fields and their valid values - used for building  search branches
        HashMap <String, String[]> metadataFieldValuesMap =  clientToMetadataMap.get(clientID);
        if (metadataFieldValuesMap == null)
        {
             // create a new Tree for this extent (Same for all extents of a client)
             FMClient client = Scope.getInstance().getClientWithExtent(extentId);
             metadataFieldValuesMap =client.getMetadataValuesMap();
             clientToMetadataMap.put(clientID, metadataFieldValuesMap);
         }

        // Note: branch names are the same for all Extents of an FM2 Client -
         // but are made unique (in building the tree) by prepending the extentName
        // For simplicity, we re-generate it for each extent instead of copying from the first instance.
          String[] mdBranchNames = CacheKeyGen.buildMetadataBranchNames(metadataFieldValuesMap);
          InvertedMetadataTree  mdTree = new InvertedMetadataTree(extentId, mdBranchNames);
          return mdTree;
      }
/*------------------------------------------------------------------------------------------------------------------------*/    
     /**
      * Load indexed data  from the indexstore for a specific index type of an image Zone 
      * and save it in the in-memory cache managed by Memcached
      * @param descriprorInfo
      *         object containing descriptor storage information
      * @return status
      * To be implemented if Memcached is used
      */
     public byte[]  loadIndexedDataFromStore( ZoneDescriptor descriptor)
     {
        //IndexFile = new IndexFile(descriptor.getFileName(), descriptor.getSize());
         return null;
     }
     
   /*-------------------------------------------------------------------------------------------------------*
    * Update the search context (i.e. search domain) to include the given image for query 
    * If the context does not exists, create it. Add all index file names to be seached in this context
    * Note: This is an initialization function invoked only before a first search is perormed 
     * for the set of images with specific metadata
     * @param mdSegmentName - Full name of the metadata branch segment
     * @ param image - image object in the cache to be added for search
    *--------------------------------------------------------------------------------------------------------*/
     protected int addIngestedImageToSearchContext(
         String  mdSegmentName, FMImage image)
     { 
         int extentId = image.getExtentID();
         String[] info = Scope.getInstance().getExtentNameInfo(extentId);   
         
         // build the unique context key for the SearchContext
         String contextKey = CacheKeyGen.buildUniqueKey(info[0], info[1], mdSegmentName);
             
         ImageSearchContext searchContext = imageSearchCtxMap.get(contextKey);
         if (searchContext == null)
        {
            // Create a new Search context for this segment
             String[] parts = InvertedMetadataTree.parseSegmentName(mdSegmentName);
             if (parts.length < 2)
             {
                 log.error("Server internal error, invalid metadata Segment name fot image " + image.getImageSource() 
                         +": "  + mdSegmentName);
             }
             String branchName = parts[0];
             int segmentNum = Integer.parseInt(parts[1]);
             searchContext = new ImageSearchContext(image.getExtentID(), branchName, segmentNum);
             searchContext.setIndexInfo(indexType, indexVersion);
             imageSearchCtxMap.put(contextKey, searchContext);
            // log.trace("Created new search context for segment: " + mdSegmentName + ",  contextKey: " + contextKey);
         }
       
         // get the list of index files for the query version (currentVersion) - does not include extentName
         ArrayList< String> indexFileNames = image.getIndexFileNames(dbContext, indexType, indexVersion);
        if (indexFileNames == null || indexFileNames.isEmpty())
        {
            log.error("Could not find index files of type : " + indexType + " for image) " + 
                image.getUniqueTag() + " in the database");
            return 0;
        }
        
        // get the root path for the index files - going to Client level, as descriptor path names are relative to that
        int clientId = scope.getClientWithExtent(extentId).getID();
        String extentIndexStoreRoot =  scope.getIndexStoreRootPath(clientId);
        if (extentIndexStoreRoot == null)
        {
             log.error("No IndexRootPath found for image " + image.getID() + " and extentID = " 
                     + image.getExtentID() +  " in the database");
            return 0;
            
        }
        int ndescr = indexFileNames.size();
        ArrayList<String> fileNames = new ArrayList();  
        for (int i = 0; i < ndescr; i++)
        {
             fileNames.add( extentIndexStoreRoot+"/"+indexFileNames.get(i));
        }
        
        searchContext.storeIndexFileNames(fileNames);
        log.trace("-- added " + fileNames.size() + " ingested files to searchContext " + searchContext.getSearchContextName());
        //log.trace("-- First file name: "  + fileNames.get(0));
        
        return fileNames.size();
     }
   
    /*----------------------------------------------------------------------------------------------------*/
    /** Remove an  image or a region for followup Queries..
    //-----------------------------------------------------------------------------------------------------*/
    public  int  removeImageFromSearch(FMImage fmImage, String region)
    {
         // first determine the MetadataTree for this extent
        InvertedMetadataTree metadataTree = getMetadataTree(fmImage.getExtentID());
        InvertedMetadataTree.BranchNode bnode  =  metadataTree.getBranchNode(fmImage.getMetadataValues());
        if (bnode == null)          // currently we are not querying this extent
            return 1;
        
        // get the name of the Branch segment  which has this image
        String segmentName = bnode.getSubnodeName(fmImage.getID());
        String contextKey  = metadataTree.getTreeName()+"$"+segmentName;

        // Remove the image from the Search context for follow-up queries
        ImageSearchContext searchContext = imageSearchCtxMap.get(contextKey);
        if (searchContext == null)          // image not being searched for some reason
            return 1;
        
        // If no region specified, it is the whole image
        String imageRegion = fmImage.getUniqueTag();
        if (region != null && region.length() > 0)
        {
            // add the regionTag to imageTag for removal
            String rect = region.replaceFirst("f|p", "");
            Rectangle regionRect = FaceRegion.getCoordinates(rect);
            int zoneIndex  =  fmImage.getZoneIndex(regionRect);
            if (zoneIndex >= 0)
                imageRegion+= ":"+ ServiceConstants.REGION_TAG+zoneIndex;
            log.trace("Tag of Image/Region to remove:" + imageRegion);
            searchContext.removeRegion(imageRegion);
        }
        else        // remove all regions
        {
            ArrayList<ImageZone> imageZones =  fmImage.getZones();
            int nz =imageZones.size();
            for (int i = 0; i < nz; i++)
            {
                ImageZone zone = imageZones.get(i);
                String regionTag = imageRegion+":"+ServiceConstants.REGION_TAG+zone.getIndex();
                searchContext.removeRegion(regionTag);
                 log.trace("Tag of Image/Region to remove:" + regionTag);
            }
        }
        // if there is no other region for this image - remove it from the search path
        if (region == null || fmImage.getNumZones() == 1)     
            bnode.removeImage(fmImage);
        return 1;
    }
    
     /*-------------------------------------------------------------------------------------------------------*/
     /** Get the searchContexts corresponding to this search path , which is the  
      * unique key for search with ClientName$extentName$metadataPath
      * 
      * Note: There are more than one SearchContext with this searchPath since each SearchContext
      * holds a maximum number of  images
      * 
      * A search context may be null if there are no images in the corresponding branch
      * @param - searchPath: clientName$extentName$metadataPath
      * @return  Corresponding SearchContext objects
      */
    public ArrayList<ImageSearchContext> getImageSearchContexts(String searchPath)
    {
        ArrayList<ImageSearchContext> searchContexts = new ArrayList();
        {
            Iterator<String> it  = imageSearchCtxMap.keySet().iterator();
            while (it.hasNext())
            {
                String contextKey = it.next();                  // contextKey is SearchPath+_+segment number
                int index = contextKey.lastIndexOf("_");
                if (index < 5)
                {
                    log.error("Invalid metadata path name " + contextKey + " given for search: " );
                }
                String branchName = contextKey.substring(0, index);
                if (branchName.equals(searchPath))
                {
                    searchContexts.add( imageSearchCtxMap.get(contextKey));
                   // log.trace("Adding searchcontext: " + contextKey + " for search");
                }
            }
             return searchContexts;            
        }
    }
    
    
}

