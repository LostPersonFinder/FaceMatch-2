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
package fmservice.server.ops.imageops;

import fmservice.httputils.common.ServiceConstants;
import fmservice.server.util.URLFetcher;
import fmservice.server.global.Scope;

import java.util.Properties;
import java.io.File;

import java.net.URL;
import org.apache.log4j.Logger;
/**
 * This class in called  on the server side to perform face finding operation by invoking
 * the FaceFinder object in the C++ FaceMatch library. T
 * The linking is performed using JNI modules connecting java classes to C++ DLL.
 * 
 * Note: Each of FM operations is performed on a separate thread to be non-blocking for
 * other incoming requests.
 * 
 *
 */
public class ImageLoader implements ServiceConstants
{
    private static Logger log = Logger.getLogger(ImageLoader.class.getName());
 
    
    public static  String  loadClientImage( Properties fmConfig, String localImageName,  
        String clientKey, String imageSubdir, String imageFilename, boolean isURL)
    {
        // read the imageFile and send data to the Facematch lib
        // Store in the temporary disk area for this client's image storage
       
        // replace all file separators in an image tag to "_" to avoid confusion with a subdirectory
        String localName = localImageName.replaceAll(File.separator, "_");
        if (isURL)
        {  
            
            String  localImageFileName = retrieveImageFile(fmConfig,  localName, 
                clientKey,  imageSubdir, imageFilename);
            return localImageFileName;
        }
        else // a local file
        {
            // make sure that the file exists and not null
            // verify that the local file exists
             File  localFile = new File(imageFilename);
             if (!localFile.exists() || localFile.length() == 0)
             { 
                 String errorStr = ("Local file " + imageFilename + " does not exists or is not accessible");
                 log.error(errorStr);
                 return null;
             }
             return imageFilename;      // file okay
        }
    }
   
/*----------------------------------------------------------------------------------------------------------------*
 /** Retrieve a file, either from a local path or retrieve from a remote node and saved locally.
 */     
  protected  static String retrieveImageFile( Properties fmConfig, String localImageTag, 
      String clientKey, String subdir, String imageUrl)
  {
        // find the local temp directory to which the file should be downloaded
        String tempImageDir = fmConfig.getProperty("temp.image.dir");
        if (tempImageDir == null)
        {
            log.error("No temporary directory specified in FM configuration to download image URLs");
            return null;  
        }
        
        // Retrieve data from remote node and save locally
        Scope scope = Scope.getInstance();
        String clientName = scope.getClientWithKey(clientKey).getName();   
        String localFileName = "";
        String fs = "/";
        try
        {
            URL url =new URL(imageUrl);
             File file = new File(url.getFile());
             String fileName = file.getName();              // get the name component
             String[] parts = fileName.split("\\.");
             String fileExt = "";
             if (parts.length > 1)
                 fileExt = "."+parts[parts.length -1] ;

             String tempFileDir = tempImageDir+fs+clientName+fs+subdir;
             File dir = new File(tempFileDir);
             if (!dir.exists())
                    dir.mkdirs();
             localFileName = tempFileDir+fs+localImageTag+fileExt;

             // copy and temporarily store locally
            URLFetcher.copyURLContentsToLocalFile(imageUrl, localFileName);
            return localFileName;  
        }
        catch (Exception e)
        {
             log.error("Exception in downloading  image to local system..\"URL\": " + imageUrl + 
                        "\n  Local Fle: " + localFileName + "\n Error: " + e.getMessage());
             return null;
        }       
    }
  // imagetag is the last component of the file name,except the file extemsion
   public static  String  getImageTagFromFilename(String localImageFilePath)
   {
       File file = new File(localImageFilePath);
       String filename = file.getName();
       
       // remove the file extension if present
       String localImageTag = filename;
       int index = filename.lastIndexOf(".");
       if(index > 0)
           localImageTag = localImageTag.substring(0,index);
       return localImageTag;  
   }
}

