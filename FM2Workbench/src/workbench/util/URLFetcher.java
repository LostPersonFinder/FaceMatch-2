/*
Informational Notice:
This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
an agency of the Department of Health and Human Services, United States Government.

- The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.

- The license does not supersede any applicable United States law.

- The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
(FAR) 48 C.F.R. Part52.227-14, Rights in Data—General.
The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
non-commercial products.

LICENSE:

Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
as provided by Federal law.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
•	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.

•	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.

•	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITEDTO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/*
 * URLFetcher.java
 */

package workbench.util;


import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.log4j.Logger;


/**
 * URLFetcher fetches the contents of an URL from the source site to 
 * the caller application.
 * The fetched data may be saved as a local file (faster) or held in a memory buffer.
 */
 
public class URLFetcher
{
    private  static Logger log = Logger.getLogger(URLFetcher.class);
    
    public static int NANO2MILLI = (int) Math.pow(10, 6);
    
    
      /** Download the contents of an URL, containing parameters
       *  (?name1=value1&name2=value2 etc.) as binary data into a byte array using NIO.
      * 
     * @param remoteURL
     * @return byte buffer with URL data
     * @throws MalformedURLException
     * @throws IOException 
     */ 
    
    public static byte[] getURLContentsNIO(String remoteUrl) throws  Exception
    {
         long start = System.nanoTime();
         String charset = java.nio.charset.StandardCharsets.UTF_8.name();
     
        // get parameters and values from the url 
         if (remoteUrl == null || remoteUrl.isEmpty())
         {
             log.error ("No URL value provided");
             return null;
         }
        String[] segments = remoteUrl.split("\\?");
        String query = (segments.length > 1) ?  segments[1] : "";
        
        // open the connection to the URL
        URLConnection connection = new URL(remoteUrl).openConnection();
    
        
        // If there is a query attached, send a post request
        if (query.length() > 0)
        {
            connection.setDoOutput(true); // Triggers POST.
            connection.setRequestProperty("Accept-Charset", charset);
        }
       
        // Open the channel using NIO (for faster performance)  and read the data 
        try 
        {
           // connection.getOutputStream().write(query.getBytes(charset));
            ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());

            // Stream where to store the read data
             ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
         
            // read in chunks
             int maxSize = 100*1024;                // 100 KB fileSize
             byte[] byteData = new byte[maxSize];
             ByteBuffer destBuffer = ByteBuffer.wrap(byteData);     // read data to this buffer
          
            int offset = 0;
            while (true)
            {
                 destBuffer.clear();
                 int bytesRead =  rbc.read(destBuffer);

                // System.out.println ("Read " + bytesRead + " bytes from input channel.");
                 if (bytesRead <=  0)
                   break;

                 outputBytes.write(byteData, 0, bytesRead);
                 offset += bytesRead;  
                 //System.out.println("getURLContent : bytesRead: " + bytesRead);
            }    
            byte[] dataBuffer = outputBytes.toByteArray();
            //System.out.println("Total bytes read: " + dataBuffer.length);;

            long end = System.nanoTime();
            long timeDiff = (end - start);
            float millisec = (float)timeDiff/NANO2MILLI;
            System.out.println("--getURLContent : downloaded " + dataBuffer.length +" bytes, in " 
                        + millisec + " millisec");
            return dataBuffer;
        }   
        catch (Exception e)
        {
            System.out.println("-- Error getting URL connection output.");
            throw (e);
        }
    }
    /*-------------------------------------------------------------------------------------------------------------*/
        
    
         /** Download the contents of an URL as binary data into a byte array using NIO
      * 
     * @param remoteURL
     * @return byte buffer with URL data
     * @throws MalformedURLException
     * @throws IOException 
     */
    public static byte[] getURLContentsNIO_OLD(String remoteURL)
        throws MalformedURLException,  IOException
    {
         long start = System.nanoTime();
         URL url = new URL(remoteURL);
         ReadableByteChannel rbc = Channels.newChannel(url.openStream());
         
         // Stream where to store the read data
         ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
         int offset = 0;
         
         // read in chunks
          int maxSize = 100*1024;                // 100 KB fileSize
         byte[] byteData = new byte[maxSize];
         ByteBuffer destBuffer = ByteBuffer.wrap(byteData);     // read data to this buffer
         //System.out.println("Remote URL: " +  remoteURL);
         while (true)
         {
              destBuffer.clear();
              int bytesRead =  rbc.read(destBuffer);

             // System.out.println ("Read " + bytesRead + " bytes from input channel.");
              if (bytesRead <=  0)
                break;
              
              outputBytes.write(byteData, 0, bytesRead);
              offset += bytesRead;  
           }    
        byte[] dataBuffer = outputBytes.toByteArray();
        //System.out.println("Total bytes read: " + dataBuffer.length);;
        
        long end = System.nanoTime();
        long timeDiff = (end - start);
        float millisec = (float)timeDiff/NANO2MILLI;
        System.out.println("-- getURLContent : URL content download as bytestream  time: " + timeDiff + " nanosonds or "
        + millisec + " milliseconds");
        return dataBuffer;
    }   
    
    /*---------------------------------------------------------------------------------------------------------------*/
      public static  int   saveURLContentsNIO(String remoteURL, String localFile) throws Exception
      {
         // byte[] urlContents = getURLContentsNIO(remoteURL);
          byte[] urlContents = getURLContentsNIO(remoteURL);
          if (urlContents == null ||  urlContents.length == 0)
              return 0;               // an empty file
          
          // save the file                      
          return storeDataInFile(urlContents, localFile);
      }
      
        /*---------------------------------------------------------------------------------------------------------------*/
    protected static int storeDataInFile(byte[] data, String outputFile) throws IOException
    {
        long start = System.nanoTime();
       
        // Store data in the local file
        File outFile = new File(outputFile);
        File dir = outFile.getParentFile();
        if (!dir.exists())
            dir.mkdirs();           // create the directory if not exists
        
        //Save the data in the output file
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outFile));
        outputStream.write(data);
        outputStream.flush();
        outputStream.close();;
        
        long end = System.nanoTime();
        long timeDiff = (end - start);
         float millisec = (float)timeDiff/NANO2MILLI;
        System.out.println("b)  storeDataInFile: byte data storing time at local node: " + timeDiff + " nanosconds or " +
          + millisec + " milliseconds");
        return 1;
    }
    


    /*public static void main(String[] args) throws Exception {
        String content = URLConnectionReader.getText(args[0]);
        System.out.println(content);
    }*/
          
        
  /*-----------------------------------------------------------------------------------------------------*/
    /** Download the contents of an URL and save locally - using NIO
     * 
     * @param remoteURL
     * @param localFile
     * @return
     * @throws MalformedURLException
     * @throws IOException 
     */
    public static  void  copyURLContentsToLocalFile(String remoteURL, String localFile) throws Exception
    {
        long start = System.nanoTime();
        URL url = new URL(remoteURL);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        
        FileOutputStream fos = new FileOutputStream(localFile);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        
        long end = System.nanoTime();
        long timeDiff = (end - start);
        float millisec = (float)timeDiff/NANO2MILLI;
        System.out.println("c) writeURLContentsToFileNIO: Single-step file URL download  time: " + timeDiff + " nanoseconds or  "
            + millisec + " milliseconds");
        
       // readLocalFile(localFile);
    }
    
    /*-----------------------------------------------------------------------------------------------------------*/
    /** Read back a file stored locally - for initial benchmarking only
     * 
     * @param localFile
     * @throws Exception 
     */
      public static  void  readLocalFile(String localFile) throws Exception
      {  
        // read the file back for benchmarking
        long start1 = System.nanoTime();
        FileInputStream infile = new FileInputStream(new File(localFile));
       int size =  infile.available();
       byte[] inputData = new byte[size];
       infile.read(inputData, 0, size);
       long end1 = System.nanoTime();
       long timeDiff1 = (end1 - start1);
       float millisec1 = (float)timeDiff1/NANO2MILLI;
        System.out.println("d) Read local file contents after write: " + timeDiff1 + " nanoseconds or  "
            + millisec1 + " milliseconds");
    }
    
     
    
    /*-----------------------------------------------------------------------------------------------------*/
    
     /** Download the contents of an URL as binary data into a byte array
      * 
     * @param remoteURL
     * @return byte buffer with URL data
     * @throws MalformedURLException
     * @throws IOException 
     *
    public static byte[] getURLContents_old(String remoteURL)
        throws MalformedURLException,  IOException
    {
        URL url = new URL(remoteURL);
        URLConnection conn = url.openConnection();

        // open the stream and put it into BufferedReader
        InputStream inputStream = conn.getInputStream();
      
        // Use the buffered wrapper for buffered IO
       BufferedInputStream  bufferedReader = new BufferedInputStream(inputStream);
       ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        int  offset = 0;
        while (true)
        {
            // get the size in bytes
            int availableSize = inputStream.available();      
            System.out.println("Input URL available data size: " + availableSize + ", at offset: " + offset);
            if (availableSize == 0)
                break;

            byte[] dataBuffer = new byte[availableSize];
            bufferedReader.read(dataBuffer, 0,  availableSize);
            outputBytes.write(dataBuffer, 0, availableSize);
            offset += availableSize;   
        }
        inputStream.close();
        bufferedReader.close();
        byte[] dataBuffer = outputBytes.toByteArray();
        return dataBuffer;
    }
  
    /*---------------------------------------------------------------------------------------*/
    /** Download the contents of an URL and save locally
     * 
     * @param remoteURL
     * @param localFile
     * @return
     * @throws MalformedURLException
     * @throws IOException 
     *
    public static int  writeURLContentsToFile_old(String remoteURL, String localFile)
        throws MalformedURLException,  IOException
    {
        byte[] urlContents = getURLContents_old( remoteURL);
        if (urlContents == null)
            return 0;                               // an empty file
        return storeDataInFile(urlContents, localFile);
    }
   
  
   
    /*-------------------------------------------------------------------------------------------*/
    // Test fetching of an URL
    public static void main(String[] args)
    {
       /* String localURL = "File:///C:/DevWork/dmisra/pictures/TundraSwans.tif";
        String storedFile =   "C:/Devwork/tmp/urldownload/TundraSwans_1.tif";
        String localFile =   "C:/Devwork/tmp/urldownload/TundraSwans_2.tif";
       */ 
        String[] remoteURLs = { "http://www.norathomas.com/wp-content/uploads/2010/04/Sunday.jpg",
//"https://google.org/personfinder/2015-nepal-earthquake/photo?id=5721453577109504",
            "http://google.org/personfinder/2015-nepal-earthquake/photo?id=658091751977/",
            "https://pl.nlm.nih.gov/tmp/pfif_cache/2015-atacama-floods.personfinder.google.orgSLASH"
           +"person.4804389472567296__1865716431.png"
        };
        
        String[] storedLocalFiles = {
            "C:/Devwork/tmp/urldownload/Nora_thomas_1.png",
            "C:/Devwork/tmp/urldownload/person.658091751977_1",
            "C:/Devwork/tmp/urldownload/person.4804389472567296_1.png"
        };
        
        String[] directlyCopiedFiles = {
             "C:/Devwork/tmp/urldownload/Nora_thomas_2.png",
               "C:/Devwork/tmp/urldownload/person.658091751977_2",
                "C:/Devwork/tmp/urldownload/person.4804389472567296_2.png"
        };
        try
        {
            
            // First with local file
         /*  System.out.println("*** Local URL Operations: " + localURL + "***");
           saveURLContentsNIO(localURL,  storedFile);                   // read and write
          System.out.println("** 1. Successfully saved file as" + storedFile);
           writeURLContentsToFileNIO( localURL,  localFile);       // file transfer
           System.out.println("** 2. Successfully copied file to" + localFile);
         */  
           System.out.println("\n---------------------------------------------------------------------------------\n"); 
           // next with URL
           
           for (int i = 0; i < remoteURLs.length; i++)
           {
                System.out.println("*** Remote URL Operations: " + remoteURLs[i] + "***");
                saveURLContentsNIO(remoteURLs[i],  storedLocalFiles[i]);                     // read and write
                System.out.println("** 1. Successfully saved file as" + storedLocalFiles[i]);
                copyURLContentsToLocalFile( remoteURLs[i],  directlyCopiedFiles[i]);        // file transfer
                System.out.println("** 2. Successfully copied file to" + directlyCopiedFiles[i]); 
           }
        }
        catch(MalformedURLException me)
        {
            System.out.println("*** Malformed URL");
            me.printStackTrace();
        }
        catch ( IOException ie)
        {
              System.out.println("*** File I/O error");
            ie.printStackTrace();
        }
         catch ( Exception ie)
        {
              System.out.println("*** URL download error");
            ie.printStackTrace();
        }
  
    }
}
