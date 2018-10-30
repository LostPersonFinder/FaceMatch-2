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

/**
 *
 *
 */

   // Store information related to the indexed data represented by this descriptor
    public class IndexFileInfo
    {
        public int imageId;                         // image row ID for which this is the index
        public String imageUniqueId;    // unique ID (from client) for this image
        public int zoneNumber;
        public String indexType;
        public String indexVersion;
        public String indexRoot;                // root path
        public String indexFilePath;         // relative path from root
        public long  fileSize;
        public String  fileChecksum;
        public String csAlgorithm;

     
     /**
      * <P> Constructor
      *      * Store the file information related to a specific index type 
     * </P>
     * @param imageId
     *              handle to the input image as stored in the database
     * @param imageUniqueId
     *              Unique ID  input image foe a client as stored in the database
     * @param zoneNumber
     *            zone number within the parent image
     * @param indexType
     *            type of index data
     * @param indexVersion
     *            version number of indexing
     *  @param indexRoot
     *          root path to the stored index file on disk
      */
     
     public IndexFileInfo(int imageId, String uniqueId, int zoneNumber,
         String  indexType, String indexVersion, String indexRoot)
     {
            this.imageId = imageId;
            this.imageUniqueId = uniqueId; 
            this.zoneNumber = zoneNumber;
            this.indexType = indexType;
            this.indexVersion = indexVersion;
            this.indexRoot = indexRoot; 
     }
     
     /**
      * * Set the actual parameters related to the stored file
      * @param indexFilePath
     *         relative path from root to the stored index file on disk
     * @param fileSize
     *          size of file stored on the disk
     * @param fileChecksum
     *           checksum of the stored file
     * @param csMethod
     *          checksum algorithm used (default: MD5)
      * @param path
      * @param size
      * @param checksum
      * @param method 
      */
     public void setIFilenfo(String path, int size, String checksum, String method)
     {
         indexFilePath = path;
         fileSize = size;
         fileChecksum = checksum;
         csAlgorithm = method;
     }
    /*--------------------------------------------------------------------------------------------*/     
  
 }
