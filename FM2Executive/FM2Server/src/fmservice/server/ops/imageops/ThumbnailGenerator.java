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

/*
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.awt.Graphics2D;


import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

/**
 * This utility class generates a Thumbnail image from  a local Image File and
 * stores it using the given fileName
 *
 */
/* public class ThumbnailGenerator
{
    public static int  createThumbnail(String sourceImageName, String destImageName)
    {
       try
        {
            BufferedImage img = ImageIO.read(new File( sourceImageName));
            BufferedImage destImage = new BufferedImage(
                 img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
       
            Graphics2D g = destImage.createGraphics();
            g.drawImage(in, 0, 0, null);
            g.dispose();
            
                
        }     
*/
import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
/*
*   Quality indicate that the scaling implementation should do everything
*  create as nice of a result as possible , other options like speed
*  will return result as fast as possible
* Automatic mode will calculate the resultant dimensions according
* to image orientation .so resultant image may be size of 50*36.if you want
* fixed size like 50*50 then use FIT_EXACT
* other modes like FIT_TO_WIDTH..etc also available.
 * 
 */
public class ThumbnailGenerator
{
    
    private static Logger log = Logger.getLogger(ThumbnailGenerator.class);
    
    // Create a thumbnail of the input image, with the given dimension 
    // Note: the dimension is approximate, and the height/width ratio is preserved
    public static int createThumbnail(String imageFileName, String thumbnailFileName, 
        int maxWidth, int maxHeight, String  thumbType )
    {
        try
        {
            File sourceFile = new File(imageFileName);
            BufferedImage sourceImage = ImageIO.read(sourceFile);       // load image

            BufferedImage thumbImage = Scalr.resize(sourceImage, Method.QUALITY,
               Mode.AUTOMATIC,  maxWidth, maxHeight, Scalr.OP_ANTIALIAS);

           // wrtite to the file
           File thumbFile = new File(thumbnailFileName);
           thumbFile.mkdirs();          // make sure the directory exists 
           ImageIO.write(thumbImage, thumbType, thumbFile);
           
           // -- alternative: convert bufferedImage to outpurstream --
           // ByteArrayOutputStream os = new ByteArrayOutputStream();
          // ImageIO.write((thumbImage, thumbType,os);
  
           return 1;
        }
        catch (IOException ioe)
        {
            log.error("IOException in creating thumbnail for image: " + imageFileName, ioe);
            return 0;
        }
    }
  /*-----------------------------------------------------------------------------------------------------------------*/      

    public static void main(String[] args) throws IOException 
    {
       long startTime = System.currentTimeMillis();
       String imageFile = "C:\\DevWork\\FaceMatch2\\testdata\\imagefiles\\pl\\shyam.jpg";
       String thumbFile =  "C:\\DevWork\\FaceMatch2\\testdata\\imagefiles\\pl\\shyam_thumb_80.jpg";

       int status = ThumbnailGenerator.createThumbnail(imageFile, thumbFile, 80, 80, "jpg");
       System.out.println("status: " + status +" - time : " +(System.currentTimeMillis()-startTime) + " msec");
    }
}

