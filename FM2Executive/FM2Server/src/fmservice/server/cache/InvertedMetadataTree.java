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

import fmservice.server.cache.dbcontent.FMImage;
import fmservice.server.global.Scope;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.Serializable;

import java.util.Iterator;
import java.util.Collection;

import org.apache.log4j.Logger;

/**
 *
 *
 */


/* ----------------------------------------------------------------------------------------------------------------------------*/
/**
 * InvertedMetadataTree -This is an inverted index tree of image metadata versus 
 * the indexed regions of images with such  metadata
 * <p>
 * The Tree is built as a set of branches, where each branch has a rootname and a sequence number. 
 * The rootname represents a unique metadata path corresponding to all combination of  metadata fieldname vs field values
 * The sequence number shows the distribution of images with dame metadata among different branches, as follows.
 * Each branch may hold a max number of 1,000 images for faster image matching. 
 * Thus sequence#1 holds images(1-1000), 2 holds (1001-2000) etc.
 * <p>
 *  Thus: if n1, n2, n3... are the number of unique values of  "searchable" metadata field f1, f2, f3...,
 *   the total number of metadata branch roots  will be n1xn2xn3. The root names are client dependent 
 *  since searchable metadata fields/values are defined as the FM2 client level. But, each Extent is assigned 
 * a unique set of branch names by a clientName_extentName_  prefix.
 * <p>
* Note: This is a shallow tree as the number of unique metadata paths for an image set is relatively small.
**
-------------------------------------------------------------------------------------------------------------------------------
 Tree Structure
  InvertedMetadataTree -> (metadataBranchName, branchNode) -   ( constant  set for each ImageExtent)
 *                                         BranchNode -> a set of SegmentNodes 
 *                                         SegmentNode ->List of image IDs with this metadata 
 *       
 * Note: We use hashMaps rather than arrayList for faster performance in  accessing an entry
 * 
 *  Branch name:  consists of concatenated list of unique values of each metadata field
 * as follows. Note that It does not contain the client or extent names
*       node1  = mdFieldName1:value1/mdFieldName2:value1...
*       node2  = mdFieldName1:value2/mdFieldName2:value1...
*       node3  = mdFieldName1:value3/mdFieldName2:value1...
* </P>
*/
    //---------------------------------------------------------------------------------------------------------------------------*/
    public class InvertedMetadataTree implements Serializable
    {
        private Logger log = Logger.getLogger(InvertedMetadataTree.class);
        
        private static int DefaultSegmentSize = 1000;
        private static String SegmentPrefix = CacheKeyGen.branchSegmentPrefix;
       int imagesPerSubnode;
       
       
       // A segmentName : <BranchName>_<SegmentNumber>
       public static String[] parseSegmentName(String  segmentPath)
       {
            return segmentPath.split(SegmentPrefix);
       }
             
 
       /*------------------------------------------------------------------------------------------------*/
    /***
    * Class  SegmentNode -  Contains a subset of images with their zones and descriptors 
    * in a given Metadata Branch. Each node contains a max number of images, after which a new 
    * segment node is created by the parent branch
    */
     protected class SegmentNode
     {
         BranchNode branch;
         int segmentNum;
         ArrayList<Integer> imageIDList;            // list of all applicable images 
         
         public SegmentNode(BranchNode parent, int snum)
         {
             branch = parent;
             segmentNum = snum;
             imageIDList = new ArrayList();
         }
         
         public int addImage(FMImage image)
         {
              imageIDList.add( new Integer(image.getID()));
              return imageIDList.size();
         }
         
         protected int  remove(Integer imageID)
        {
            if (!imageIDList.contains(imageID))
                return 0;               // invalid image (may be already removed
               
            imageIDList.remove( imageID);
            return 1;
        }
     }
     //-------------------------------------------------------------------------------------------------------
    /***
    * Class  BranchNode -  Contains all images  with their zones and descriptors for a single 
    * Metadata Branch.
    */
     protected  class BranchNode
     {
         String branchName;
         ArrayList<SegmentNode>subNodes;
         int numImages;                                      // number  of all applicable images
        
         
         // Create a new Branch, with the first (empty) sequence node.
        protected BranchNode(String name)
        {
            branchName = name;
            subNodes = new ArrayList();             // initialize
            subNodes.add(new SegmentNode(this, 0));
            numImages = 0;
        }
        
        /*--------------------------------------------------------------------------------------
        // Add a new image to this branch - in the appropriate subnode
        /*--------------------------------------------------------------------------------------*/

        protected synchronized  int  addImage(FMImage image)
        {
            //System.out.println("Branch: " + branchName + ", numImages: " + numImages + ", seqNum: " +  seqNum);
            // Note: first subNode always exists - creted when the Branch is created
            int ns = subNodes.size();
            int segNum = ns; 
            SegmentNode lastSubNode = subNodes.get(ns-1);
            if (lastSubNode.imageIDList.size() == imagesPerSubnode)        
            {
                SegmentNode subNode = new SegmentNode(this, ns);
                this.subNodes.add(subNode);
                lastSubNode = subNode;
                log.debug("Created new Subnode for segment:" + segNum + " For metadata Branch: " +this.branchName);
            }
            lastSubNode.addImage(image);
            numImages++;
            
             // return the name of the segment where placed
             return lastSubNode.segmentNum;
        }
        /*------------------------------------------------------------------------------------------------*/    
         protected int  removeImage(FMImage image)
        {
            Integer imageID = new Integer (image.getID());
           return remove(imageID);
        }
          
         /*------------------------------------------------------------------------------------------------*/    
         protected boolean  contains(Integer imageID)
        {
             for (SegmentNode subNode : subNodes)
            {
                if (subNode.imageIDList.contains(imageID))
                    return true;
            }
            return false;           // image not found, so not removed
        }
      
        /*------------------------------------------------------------------------------------------------*/       
         protected synchronized int  remove(Integer imageID)
        {
            for (SegmentNode subNode : subNodes)
            {
                if (subNode.imageIDList.contains(imageID))
                {
                     subNode.remove(imageID);
                     --numImages;
                     return 1;
                }
            }
            return 0;           // image not found, so not removed
        }
   
         /*------------------------------------------------------------------------------------------------*/    
         // return names of all current segment nodes in this branch
         //
         protected ArrayList<String>  getSubnodeNames()
        {
            ArrayList <String> subnodeNames = new <String> ArrayList();
            for (SegmentNode subNode : subNodes)
            {
                subnodeNames.add(CacheKeyGen.buildUniqueSegmentKey(
                        branchName, subNode.segmentNum));
            }
            return subnodeNames;  
        }
         
                   
         /*------------------------------------------------------------------------------------------------*/
         // Get the name of the subnode containing a given image.
         // May be eeded to relate to the SearchContext containig this image
         //*-----------------------------------------------------------------------------------------------*/
          protected String getSubnodeName(Integer imageID)
         {
             for (SegmentNode subNode : subNodes)
            {
                if (subNode.imageIDList.contains(imageID))
                    return CacheKeyGen.buildUniqueSegmentKey(branchName, subNode.segmentNum);    
            }
             return  null;
         }
         
     }
     //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
       /*------------------------------------------------------------------------------------------------*/
       // main class - InvertedMetadataTree
        /*-----------------------------------------------------------------------------------------------*/
        int  extentId;                            // image extent for the descriptors
        String extentName;
        String clientName;   
        String treeName;                   // unique name, based upon client+extent
        String[]  branchNames;        // name (cachekey) of metadata Index tree node - based upon field values
        boolean isActive;                   // is the  Extent active for any face ingest/query
        
        public String segmentSeparator = "_";
        
        // metdataNodeName vs.descriptors for images at  this node
        HashMap <String, BranchNode>branches;       
       
        /*-----------------------------------------------------------------------------------------------*/
        /**
         * Constructor.
         * @param imageExtentId
         *                  Database ID of the image extent 
         * @param branchNames 
         *                   names of the metadata branches for this extent (without client/extent prefix)
         *                  Note: The branches are the top level nodes in a  metadata  tree
         *-------------------------------------------------------------------------------------------------*/
        public InvertedMetadataTree(int imageExtentId,  String[] mdBranchNames)
        {
            this.extentId = imageExtentId;
            String[] extentInfo = Scope.getInstance().getExtentNameInfo(extentId);
            clientName = extentInfo[0].toLowerCase();
            extentName = extentInfo[1].toLowerCase();
            treeName = clientName+"$"+extentName;
            
            branchNames = mdBranchNames ;       // names of top nodes
            // Map of all images, in different branches
            branches = new HashMap();
            
            // initialize each node to an empty list of images
            for (String branchName : branchNames)
            {
                BranchNode bnode = new BranchNode(branchName);
                branches.put(branchName.toLowerCase(), bnode);      
            }
            String segmentSize = Scope.getInstance().getFMConfig().getProperty("query.segment.size");
            imagesPerSubnode = (segmentSize  == null) ? DefaultSegmentSize :  Integer.parseInt(segmentSize);
            
        }

        /*---------------------------------------------------------------------------------------------------*
        * Find which branch in this tree corresponds to the given metadata combination
        *----------------------------------------------------------------------------------------------------*/
       public  BranchNode getBranchNode (HashMap <String, String>imageMetadata)
       {
            // check in which top node  this zone should be placed
            if (imageMetadata == null)
                return null;

            String branchName = CacheKeyGen.findMetadataBranchName(branchNames, imageMetadata); 
            BranchNode metadataBranch = branches.get(branchName);
            if (metadataBranch == null)
            {
                log.warn("No metadata node entry found for branch " + branchName + " for given metadata field/values");
                return null;
            }
            return metadataBranch;
       }
       
       /*-------------------------------------------------------------------------------------------------------*/
       /** Get images in a given branch of the metadata tree.
        * 
        * @return List of imageIDs in the branch
        */
       public ArrayList<Integer> getImagesInBranch(String branchName)
       {
            BranchNode node = branches.get(branchName);
            if (node == null)
                return null;            // no such branch
            
            // if there is only one subnode, just return its IDs
            if (node.subNodes.size() == 1)
                return (node.subNodes.get(0)).imageIDList;
            
            ArrayList<Integer> branchImageIDs = new ArrayList();
            for (int i = 0; i < node.subNodes.size(); i++)
            {
                branchImageIDs.addAll( (node.subNodes.get(i)).imageIDList);
            }
            return branchImageIDs;
       }
       
              /*-------------------------------------------------------------------------------------------------------*/
       /** Get images in a given branch of the metadata tree.
        * 
        * @return List of imageIDs in the branch
        */
       public ArrayList<Integer> getImagesInSegment(String branchName, int segNum)
       {
            BranchNode node = branches.get(branchName);
            if (node == null)
                return null;            // no such branch
            
            ArrayList<Integer> segmentImageIDs = new ArrayList();
            for (int i = 0; i < node.subNodes.size(); i++)
            {
                if (node.subNodes.get(i).segmentNum ==segNum)
               {
                   return node.subNodes.get(i).imageIDList;
               } 
            }
            return null;
       }


        /*-------------------------------------------------------------------------------------------------------*/
        /** Remove a given Image node from the metadataIndexTree
         * 
         * @param imageId
         * @param metadataMap of the image
         * @return status
         */
        // Delete a given zone  for the image, given its ID (value)
        public synchronized int deleteImageByID (int imageId, HashMap <String, String>imageMetadata)
        {
            Integer imageID = new Integer(imageId); 
            BranchNode branch = getBranchNode( imageMetadata);
            if (branch == null )
                return 0;           // not found;
            branch.remove(imageID);
            return 1;
        }   

    /*-------------------------------------------------------------------------------------------------------*/
        /** Add an  image to  the metadataIndexTree
         * 
         * @param imageId
         * @param metadataMap of the image
         * @return  name of the (branch_segment)  where the image was placed
         */
        public synchronized String addImage (FMImage image, HashMap <String, String>imageMetadata)
        {
            Integer imageID  = new Integer(image.getID());
            
            BranchNode branch = getBranchNode( imageMetadata);
            if (branch == null )
                return null;           // not found;
            if (branch.contains(imageID))
            {
                    log.warn ("Image with ID " + imageID + " already exists in the Metadata tree. Duplicate not added");
                    return null;
            }

            int segNum = branch.addImage(image);
            return (branch.branchName + "_"+segNum);
        } 
    
        /*------------------------------------------------------------------------------------------------------------*/  
        // Activate/deactivate the tree from further ingest/query
        /*------------------------------------------------------------------------------------------------------------*/
        public void  setTreeActive(boolean setActivate)
        {
            isActive = setActivate;
            log.info( "Status of Metadata Index Tree for Image Extent "  + this.extentName + " set to " +
               ( isActive ? "true" : "false"));
        }
        
        
         /*------------------------------------------------------------------------------------------------------------*/
        // get all branch bodes in a tree
        /*------------------------------------------------------------------------------------------------------------*/
        public String[] getBranchNames()
        {
            return branchNames;
        }
        /*------------------------------------------------------------------------------------------------------------*/
        // get all branches in a tree
        /*------------------------------------------------------------------------------------------------------------*/
        public BranchNode[] getBranchNodes()
        {
            Collection <BranchNode> branchNodes =branches.values();
            int size = branches.values().size();
            if (size ==0)
                return null;
            BranchNode[] bnodes = new BranchNode[size];
            branchNodes.toArray(bnodes);
            return bnodes;
        }
        
          /*------------------------------------------------------------------------------------------------------------*/
        // get all branches with the sequenceNumbers  in a tree
        // Even if a branch is empty, it has the first SequenceNode, sitting empty
        /*------------------------------------------------------------------------------------------------------------*/
        public String[] getBranchSegmentNames()
        {
            Iterator <BranchNode> it =branches.values().iterator();
            ArrayList<String> segNodeNames = new ArrayList();
            while (it.hasNext())
            {
                BranchNode branch = it.next();
                segNodeNames.addAll(branch.getSubnodeNames());
            }
            int size = segNodeNames.size();
            if (size ==0)
                return null;
            String[] segNodeNameArray = new String[size];
            segNodeNames.toArray(segNodeNameArray);
            return segNodeNameArray;
        }
        
        /*------------------------------------------------------------------------------------------------------------*/
        public BranchNode getBranchNodeWithName(String branchName)
        {
            return branches.get(branchName);  
        } 
        
        /*------------------------------------------------------------------------------------------------------------*/
         public BranchNode getBranchNodeWithSegmentName(String segmentName)
        {
            Iterator <BranchNode> it =branches.values().iterator();
            ArrayList<String> seqNodeNames = new ArrayList();
            while (it.hasNext())
            {
                BranchNode branch = it.next();
                if (branch.getSubnodeNames().contains(segmentName))
                    return branch;
            }
           return null;             // not found
        } 
      
        /*------------------------------------------------------------------------------------------------------------*/
        // Get the unique name of this MetadataIndexTree
       /*------------------------------------------------------------------------------------------------------------*/
        public String getTreeName()
        {
            return treeName;
        }
        /*------------------------------------------------------------------------------------------------------------*/
        // get the uniqueKey  for this metadata branch among all sets for all clients and extents
        // Note: The same is also generated (and must match with the key genetated by CacheKeyGen)
        // This method is used here for convenience.
       /*------------------------------------------------------------------------------------------------------------*/ 
        public String getUniqueKeyForBranch(String branchName)
        {
            return treeName+"$"+branchName;
        }
        
    }
     