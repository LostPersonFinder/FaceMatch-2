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

/***********************************************************************
 * Class DisplayUtils. 
 * A Utility class to provide convenience methods
 * for date, file and message related display for an application.
 *****************************%****************************/

package fm2client.display;

import fm2client.util.Utils;
import java.io.File;
import java.io.IOException;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import java.util.Properties;

import java.awt.Component;
import java.awt.Container;
import java.awt.Color;

import java.awt.Cursor;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

import org.apache.log4j.Logger;
////////////////////////////////////////////////////////////////////////



public class DisplayUtils
{
    private static Logger log = Logger.getLogger(ImageDisplayPanel.class);
    /*------------------------------------------------------*/

    static Cursor sWaitCursor = new Cursor(Cursor.WAIT_CURSOR);
    static Cursor sDefaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    public static String basename(String inFilename)
    {
	    String bname = "";

	    // The file path  frequenly has both "/" and "\" as separators
		// so convert to java format of  "/"
		String filename = inFilename.replace('\\', '/');
		int index1 = filename.lastIndexOf("/");

		// discard the file extension part
		int index2 = filename.lastIndexOf(".");
		if (index2 < 0)
				index2 = filename.length();

		bname = filename.substring(index1+1, index2);
		System.out.println("Filename: "+ filename + "\n basename: " + bname);
		return bname;
	}

  /********************************************************************
   * get the filename sans directory
   ********************************************************************/
   public static String getBaseFilename(String filename)
   {
      return (basename(filename) + getFileExtension(filename));
   }

/**************************************************************/

    public static void displayInfoMessage(String msg)
    {
	    JOptionPane.showMessageDialog(null, msg, "Information...",
			JOptionPane.INFORMATION_MESSAGE);
    }

/**************************************************************/

    public static void displayWarningMessage(String msg)
    {
       if (msg == null || msg.length() == 0)
            msg = "Unknown Warning message.";
	    JOptionPane.showMessageDialog(null, msg, "Warning...",
			JOptionPane.WARNING_MESSAGE);
    }

/**************************************************************/

    public static void displayErrorMessage(String msg)
    {
        if (msg == null || msg.length() == 0)
            msg = "Unknown error, may be due to Null Pointer Exception.";
	    JOptionPane.showMessageDialog(null, msg, "Application Error...",
			JOptionPane.ERROR_MESSAGE);
    }

/**************************************************************/

    public  static int displayConfirmationMessage(String msg, String[] options)
    {
	    return JOptionPane.showOptionDialog(null, msg, "Confirmation...",
		    JOptionPane.YES_NO_OPTION,
		    JOptionPane.QUESTION_MESSAGE, null,
		    options, options[0]);
    }
    
    
/**************************************************************/
    public  static String getUserInput(String msg, String initialValue)
    {
        if (initialValue == null)
            return JOptionPane.showInputDialog(msg);
        else
            return JOptionPane.showInputDialog(msg, initialValue);
    }
/****************************************************************/
    // set background color of a component and its child components
    // recursively
    public static void setBackgroundColor(Component c, Color color)
    {
        if (c instanceof Container)
        {            
            Component[] childComponents = ((Container)c).getComponents();
            for (int i = 0; i < childComponents.length; i++)
                setBackgroundColor(childComponents[i], color);
        }
        else
            c.setBackground(color);     // nothing to recurse
    }

/***********************************************************/
    /* Select a filename through a FileChooser
    * Return file name, or null if no file was selected.
    *******************************************************/

    public  static String selectFile(String directory)
    {
	    String selectedFile = null;
	    JFileChooser inputFileChooser = new JFileChooser(directory);
	    inputFileChooser.setFileSelectionMode(
			    JFileChooser.FILES_AND_DIRECTORIES);
	    int returnVal = inputFileChooser.showOpenDialog(null);
	    if (returnVal == JFileChooser.APPROVE_OPTION)
	    {
	        File file = inputFileChooser.getSelectedFile();
	        selectedFile = file.getPath();
	    }
	    return selectedFile;
    }
  /*********************************************************
    * Select multiple filenames through a FileChooser
    * Return file names, or null if no file was selected.
    *******************************************************/  
     public  static String[] selectFiles(String directory)
     {
        String[] selectedFiles = null;
        
        JFileChooser inputFileChooser = new JFileChooser(directory);
        inputFileChooser.setMultiSelectionEnabled(true); 
	    inputFileChooser.setFileSelectionMode(
                    JFileChooser.FILES_AND_DIRECTORIES);
        
	    int returnVal = inputFileChooser.showOpenDialog(null);
	    if (returnVal == JFileChooser.APPROVE_OPTION)
	    {
	        File[] files = inputFileChooser.getSelectedFiles();
            int n = files.length;
            selectedFiles = new String[n];
            for (int i = 0; i < n; i++)
                selectedFiles[i] = files[i].getPath();
	    }
	    return selectedFiles;
     }
  
     /*********************************************************
    * Select a directory  through a FileChooser
    * Return directory name, or null if none was selected.
    *******************************************************/     
     public static String selectDirectory(String topDirectory, String title)
     {
        String selectedDir = null;
	    JFileChooser directoryChooser = new JFileChooser(topDirectory);
        directoryChooser.setDialogTitle(title);
	    directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setMultiSelectionEnabled(false);
        directoryChooser.setApproveButtonText("Select");
	    int returnVal = directoryChooser.showOpenDialog(null);
	    if (returnVal == JFileChooser.APPROVE_OPTION)
	    {
	        File dir = directoryChooser.getSelectedFile();
	        selectedDir = dir.getPath();
	    }
	    return selectedDir;
     }
  /******************************************************
    * Specify a new filename through a FileChooser
    * Return file name, or null if no file was selected.
    *******************************************************/

    public  static String getSaveFileName(String directory)
    {
	    String filename = null;
        if (directory == null)
        {
			FileSystemView fsv = FileSystemView.getFileSystemView();
            directory = fsv.getDefaultDirectory().getPath();
        }
	    JFileChooser fileChooser = new JFileChooser(directory);
	    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setApproveButtonText("Save");
		int returnVal = fileChooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = fileChooser.getSelectedFile();
			filename = file.getPath();
		}
		return filename;
    }

/**************************************************************/

    public static String getFileExtension(String fileName)
    {
		int index = fileName.lastIndexOf(".");
		if (index < 1)
			return "";

		String fileExtension = new String(
				fileName.substring(index, fileName.length()));

		return fileExtension;
    }


/**************************************************************/

    public static ArrayList getFileNameList(String dir, String fileExtension)
    {
        ArrayList list = new ArrayList();
        File path = new File(dir);
        if (!path.isDirectory())
            return null;

        File[] fileList = path.listFiles();
        int n = fileList.length;

        for (int i=0; i < n; i++)
        {
            if (fileList[i].isDirectory())
                continue;
            String filename = fileList[i].getName();
            String ext = getFileExtension(filename);
            if (ext.equals(""))
                continue;			// file does not have any extension
            if (fileExtension.equalsIgnoreCase(ext))
                list.add(filename);
        }
        return list;

    }

/**************************************************************/

    public static Cursor waitCursor()
    {
	    return sWaitCursor;
    }

/**************************************************************/

    public static Cursor defaultCursor()
    {
	    return sDefaultCursor;
    }

   /**********************************************************
    * Return currentDate in standard mm/dd/yyyy format
    **********************************************************/

    public static String getCurrentDate()
    {
    	Calendar now = Calendar.getInstance();
	    int year = now.get(Calendar.YEAR);
	    int month = now.get(Calendar.MONTH)+1;  // since returned as index
	    int day = now.get(Calendar.DAY_OF_MONTH);
	    String date = month+"/"+day+"/"+year;
	    return date;
    }

  /**********************************************************

    * Return currentDate and time in mm/dd/yyyy hh:mm format

    **********************************************************/

    public static String getCurrentDateTime()
    {
    	Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR);
		int month = now.get(Calendar.MONTH)+1;  // since returned as index
		int day = now.get(Calendar.DAY_OF_MONTH);
		int hour = now.get(Calendar.HOUR_OF_DAY);
		int min = now.get(Calendar.MINUTE);
		String date = month+"/"+day+"/"+year+" "+hour+":"+min;
		return date;
    }

   /**********************************************************
    * Return currentDate in formatted text string format
    **********************************************************/

    public static String getFormattedDate()
    {
    	Locale locale = Locale.getDefault();
    	DateFormat fmt = DateFormat.getDateTimeInstance
			(DateFormat.LONG, DateFormat.LONG, locale);
    	fmt.setTimeZone(TimeZone.getDefault());
	    String formattedDate = fmt.format(new Date());
        return formattedDate;
    }

	
    /************************************************************************
     * Convert a given filename string to itd canonical form (with '/' and '\\'
     * as file char separators for UNIX and Windows systems respectively
     ***********************************************************************/
    public static String convertToCanonicalPath(String filename)
    {
        try
        {        
            File file = new File(filename);
            return file.getCanonicalPath();
        }
        catch (IOException e)
        {
            System.out.println ("Invalid filename string: "+ filename +
                    ", " +e.getMessage());
            return "";          // not a valid string           
        }
    }
/*--------------------------------------------------------------------------------------------------------*/
// This is a work-around for local testings - for flle path that are not really URLs, 
// but reside on file systems (lhcuserflier) which are accessible under different name
// from different computer systems
 /*--------------------------------------------------------------------------------------------------------*/
  public  static String  getLocalImageFilePath(Properties configProperties, String imageFilePath)
    {
        if (imageFilePath == null)
        {
            DisplayUtils.displayErrorMessage("No image file path provided.");
            return null;
        }
        if (imageFilePath.startsWith("http"))       // an URL 
            return imageFilePath;
        
        int i = 0;
        String serverPath = null;
        String localPath = null;
        while (true)
        {
             i++;
            serverPath = configProperties.getProperty("server.path."+i);
            if (serverPath == null)
                return imageFilePath;           // no substitution needed
            
            if ( imageFilePath.startsWith(serverPath))
            {
                 localPath = configProperties.getProperty("local.path."+i);
                 if (localPath == null)          // no substitution needed
                 {
                        log.error ("Missing property  specfication for " +  localPath+ " in config file.");
                        return imageFilePath;
                 }
                 // now substitute the remote part for the local part
                 String localImagePath = imageFilePath.replace(serverPath, localPath);
                 return localImagePath;
            }
        }   // end-while
    }
}

