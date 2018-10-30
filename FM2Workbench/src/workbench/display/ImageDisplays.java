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

package workbench.display;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

public  class ImageDisplays //implements FM2TableConstants
{
    // Declared static as it does not need access to other members of the outer class
    public static class ImageInfo 
    {
        public String imageFileName;
        public String faceCoordinates;          // with landmarks, if any
        public String imageLabel;                   // Label for the image
        public String imageAnnotation;          // to be written on image, if any
    }
    
    static int DISPLAY_WIDTH = 1024;
    static int  DISPLAY_HEIGHT  = 900;
     //---------------------------------------------------------------------------------------------
    // Display the specified image  in its own frame, and mark the face coordinates
    // which may include landmarks. 
    // @ param offset index determines the LHS corner offset of the Frame from (0,0)
    // in case multiple images are to be frawn on the screen
    //----------------------------------------------------------------------------------------------
   public static  JFrame displayImageInFrame(String title,  int offsetIndex, String imageURI, 
       String coordStr, boolean addLabel)
    { 
        JFrame imageFrame = new JFrame(title);
        
         JPanel  imageDisplayPanel ;
        if (!addLabel)
        {
            imageDisplayPanel   = new ImageDisplayPanel(imageURI, coordStr, coordStr);
        }
        else
        {
            ImageInfo imageInfo = new ImageInfo();
            imageInfo.imageFileName = imageURI;
            imageInfo.faceCoordinates = coordStr;
            imageInfo.imageLabel = "<HTML> URI: "+ imageURI + "<br>"
                + " Coordinates : " + coordStr +"</HTML>";
             imageDisplayPanel =  drawImageWithLabel(imageInfo, 1.0);           // no image scaling
        }
        imageFrame.add(imageDisplayPanel);
        imageFrame.pack();
        
        setLocation( imageFrame, offsetIndex);
        imageFrame.setVisible(true);
        return imageFrame;
    }
   /*--------------------------------------------------------------------------------------------*/
   // Set the location of the image frame
   protected static void setLocation(JFrame frame, int offsetIndex)
   {
        // offset each image according to test number, move to left/north  if the image goes offscreen
        int x = ( offsetIndex*30)%DISPLAY_WIDTH + 10; 
        if (x+100 > DISPLAY_WIDTH) x = x-100;
        int y = (offsetIndex*40)%DISPLAY_HEIGHT+10;  
        if (y+100 > DISPLAY_HEIGHT) y = y-100;

        //System.out.println("-- x: " + x +", y = " +y);
        frame.setLocation(50, 50);
    }
   
   
     
    //*-------------------------------------------------------------------------------------------------------------*/
   // Drawing of multiple images in one or more rows in a Frame.
   // Do not resize the images
   // A Scrollbar is added if the dimension becomes too large
    //*-------------------------------------------------------------------------------------------------------------*/
    public static JFrame displayMultipleImages(String title,  int offsetIndex, int numColumns, ImageInfo[]  imageInfo)
    {
        return displayMultipleImages( title,  offsetIndex, numColumns, imageInfo, 1.0);
    }
    //*-------------------------------------------------------------------------------------------------------------*/
   // Drawing of multiple images in one or more rows in a Frame.
   // A Scrollbar is added if the dimension becomes too large
    //*-------------------------------------------------------------------------------------------------------------*/
    public static JFrame displayMultipleImages(String title,  int offsetIndex, int numColumns, 
        ImageInfo[]  imageInfo, double scaleFactor)
    {
        int n =  imageInfo.length;
        int numRows = ( imageInfo.length+numColumns-1)/numColumns;
        JPanel topPanel = new JPanel(new GridLayout(numRows, numColumns));
        
          JPanel imageNLabelPanel;
        for (int i = 0; i < n; i++)
        {
            if (scaleFactor == 1.0)
            {
            // Draw the image and the Label in a combination panel in a vertical layout
                imageNLabelPanel = drawImageWithLabel( imageInfo[i], null);
            }
            else
            {
                imageNLabelPanel = drawImageWithLabel( imageInfo[i], scaleFactor);
            }
            imageNLabelPanel.setBorder(new BevelBorder(5));
            
            // Add it to the top panel
            topPanel.add(imageNLabelPanel);
        }
        JFrame imageFrame = new JFrame(title);
        imageFrame.add(topPanel, BorderLayout.CENTER);
        imageFrame.pack();
        setLocation(imageFrame, offsetIndex);
        imageFrame.setVisible(true);
        return imageFrame;
    }
    
      //-----------------------------------------------------------------------------------------------------------------------------------
    // Draw the given image and its label in a Panel with a vertical layout
    //--------------------------------------------------------------------------------------------------------------------------------------
    protected static JPanel drawImageWithLabel(ImageInfo imageInfo, Dimension imagePaneSize)
    {
        String imageURI = imageInfo.imageFileName;
        String coordStr = imageInfo.faceCoordinates;
        String imageAnnot = imageInfo.imageAnnotation;
            
        String tooltip = coordStr;
        boolean drawLabel = false;
        if (imageAnnot  != null &&  !imageAnnot.isEmpty())
        {
            tooltip = imageAnnot;
            drawLabel = true;
        }
        ImageDisplayPanel  imagePanel = new ImageDisplayPanel(imageURI, coordStr, tooltip, drawLabel);
        JPanel comboPanel = new JPanel( );
        comboPanel.setLayout(new BorderLayout() );
        comboPanel.add(imagePanel, BorderLayout.NORTH);
       
        if (imagePaneSize == null)
            comboPanel.add(imagePanel, BorderLayout.NORTH);
        else
        {
            Dimension paneSize = imagePanel.getSize();
            if (paneSize.width > imagePaneSize.width || paneSize.height > imagePaneSize.height)
            {
                    // put the imagepanel within a scroll pane
                    JScrollPane scrollPane = new JScrollPane();
                    scrollPane.add(imagePanel);
                    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    comboPanel.add(scrollPane, BorderLayout.SOUTH);
             }
            else
                comboPanel.add(imagePanel);
        }
        // add the label
        String imageLabel = imageInfo.imageLabel;
        if (imageLabel != null )
        {
            JLabel label = new JLabel(imageLabel);
            label.setBorder (BorderFactory.createEmptyBorder(5, 10, 5, 10));
            comboPanel.add(label);
        }
        return comboPanel;
    }

    //---------------------------------------------------------------------------------------------
    // Draw the given image and its label in a Panel with a vertical layout
    // Note: Don't use BoxLayout as that is not reliable and adds extra whitespce on right
    //------------------------------------------------------------------------------------------------
    protected static JPanel drawImageWithLabel(ImageInfo imageInfo, double scaleFactor)
    {
        String imageURI = imageInfo.imageFileName;
        String coordStr = imageInfo.faceCoordinates;
        String imageAnnot = imageInfo.imageAnnotation;
            
        String tooltip = coordStr;
        if (imageAnnot  != null &&  !imageAnnot.isEmpty())
        {
            tooltip = imageAnnot;
        }
        ImageDisplayPanel  imagePanel = new ImageDisplayPanel(imageURI, coordStr, tooltip, scaleFactor);
        JPanel comboPanel = new JPanel( );
        comboPanel.setLayout(new BorderLayout() );
        comboPanel.add(imagePanel, BorderLayout.NORTH);

        // add the label
        String imageLabel = imageInfo.imageLabel;
        if (imageLabel != null )
        {
            JLabel label = new JLabel(imageLabel);
            label.setBorder (BorderFactory.createEmptyBorder(10, 0, 10, 0));
            comboPanel.add(label, BorderLayout.SOUTH);
        }
        return comboPanel;
    }
    
    /*--------------------------------------------------------------------------------------------------------------------*/
    // Draw the query and matched images for a given operation, using BorderLayout
    // The query image is drawn as the LEFT panel  and is not expandable.
    // The matching set is on a scroll panel which is added as the CENTER, so it is wxpandable
    //--------------------------------------------------------------------------------------------------------------------*/
   public static JFrame displayQueryNMatchingImages(String title, ImageInfo queryImageInfo,
            ImageInfo[] matchImageInfo)
   {
       return displayQueryNMatchingImages(title, queryImageInfo, matchImageInfo, 1.0);
    }
    
  /*------------------------------------------------------------------------------------------------------------------------*/
    protected static JPanel drawImagesInPanel(ImageInfo[]  imageInfo,  double resizeFactor) 
    {
        int numMatches = imageInfo.length;
        
        JPanel imageSetContainer = new JPanel(new GridLayout(0, 5));
        for (int i = 0; i < numMatches; i++)
        {
        //----------------------------------------------------------------------------------------------------------------//
        /* Draw the given set of images in a Panel, 
        * @param paneSize: Size of  subpanels for each image in the set
       //---------------------------------------------------------------------------------------------------------------- */
            // Draw the image and the Label in a combination panel in a horizontal layout
            JPanel imageNLabelPanel = drawImageWithLabel( imageInfo[i], resizeFactor);
            imageNLabelPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
            // Add it to the top panel
            imageSetContainer.add(imageNLabelPanel);
            System.out.println("MachImagePanel size = " + imageSetContainer.getPreferredSize());
        }
        return imageSetContainer;
    }
   
    /*--------------------------------------------------------------------------------------------------------------------*/
    // Draw the query and matched images for a given operation, using BorderLayout
    // The query image is drawn as the LEFT panel  and is not expandable.
    // The matching set is on a scroll panel which is added as the CENTER, so it is expandable
    
   // if  resizeFactor = 1,  images in each panel are resized to the specified size.
    // It is useful for large images
    //--------------------------------------------------------------------------------------------------------------------*/
   public static JFrame displayQueryNMatchingImages(String title, ImageInfo queryImageInfo,
            ImageInfo[] matchImageInfo, double  imageScaleFactor)
   {
       JFrame queryMatchFrame = new JFrame(title);
       JPanel topPanel = new JPanel(new BorderLayout());
      
       JPanel queryImagePanel =  drawImageWithLabel(queryImageInfo, imageScaleFactor);
       
       // get the image dimension - which is the first component in the query imagePanel
       queryImagePanel.validate();          // to get the curret size
       Dimension imageDimension = queryImagePanel.getComponent(0).getPreferredSize();
       // make matchImageSize the same
       JPanel matchPanel = drawImagesInPanel( matchImageInfo, imageScaleFactor);
       System.out.println("MatchImagePanel preferred size: " +  matchPanel.getPreferredSize());
       
       // put the matchingImagePanel in a scrollPane;
       JScrollPane matchScrollPane = new JScrollPane(matchPanel);
       matchScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); 
       matchScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

       JPanel queryPanelWrapper = new JPanel(new BorderLayout());
       queryPanelWrapper.add(queryImagePanel, BorderLayout.NORTH);
       
       topPanel.add(queryPanelWrapper, BorderLayout.WEST);            // on the left size
       topPanel.add(matchScrollPane, BorderLayout.CENTER);
       //topPanel.add(matchPanel, BorderLayout.CENTER);
       queryMatchFrame.add(topPanel);
       
       queryMatchFrame.setPreferredSize( new Dimension(DISPLAY_WIDTH-10, DISPLAY_HEIGHT-100));
       queryMatchFrame.pack();
       queryMatchFrame.setLocation(50, 50);
       queryMatchFrame.setVisible(true);
       return queryMatchFrame;
   }

   /*------------------------------------------------------------------------------------------------------------------------------------*/
   // Not used 
   /*------------------------------------------------------------------------------------------------------------------------------------*/
    protected static JPanel drawImagesInPanel(ImageInfo[]  imageInfo, Dimension paneSize) 
    {
        int numMatches = imageInfo.length;
        
        JPanel imageSetContainer = new JPanel(new FlowLayout());

        for (int i = 0; i < numMatches; i++)
        {
            //----------------------------------------------------------------------------------------------------------------//
            /* Draw the given set of images in a Panel, 
            * @param paneSize: Size of  subpanels for each image in the set
           //---------------------------------------------------------------------------------------------------------------- */
           // Draw the image and the Label in a combination panel in a horizontal layout
            JPanel imageNLabelPanel = drawImageWithLabel( imageInfo[i], null);
            imageNLabelPanel.setBorder(new BevelBorder(5));
            
            // Add it to the top panel
            imageSetContainer.add(imageNLabelPanel);
            System.out.println("MachImagePanel size = " + imageSetContainer.getPreferredSize());
        }
        return imageSetContainer;
    }
}