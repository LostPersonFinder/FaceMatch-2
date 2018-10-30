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
 * ImageDisplay.java
 */
package workbench.display;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import workbench.util.URLFetcher;

/*-----------------------------------------------------------
 * ImageDisplay displays the image whose URI is specified
 * in the given table.  It is added as a subpanel to the JDialog
 *  given as an argument.
*------------------------------------------------------------*/

public class ImageDisplayPanel extends JPanel
{
    private Logger log = Logger.getLogger(ImageDisplayPanel.class);
    
    private String coordVal;
    private String imageURI;
    private String tooltipStr;
    private boolean drawLabel;
    
    private BufferedImage displayImage = null;
    private static Color  lightYellow = new Color(255, 255, 204);
    private double scaleFactor;

    
  /*-----------------------------------------------------------------------------------------------------------*/
    public ImageDisplayPanel(String uri, String coords,  String tooltips)
    {
        this( uri,  coords,   tooltips, false);
    }
    
    
    /*-----------------------------------------------------------------------------------------------------------
    * Draw an Image into a JPanel using theImage URI.
    * @param drawAsLabel:  Also draw  the tooltop as a label on the image
    *------------------------------------------------------------------------------------------------------------*/
     public ImageDisplayPanel(String uri, String coords,  String tooltips, boolean drawAsLabel)
    {
         drawImage(uri, coords, tooltips, drawAsLabel, 1.0);
    }
     
     /*-----------------------------------------------------------------------------------------------------------
    * Draw an Image into a JPanel using theImage URI.
    * @param drawAsLabel:  Also draw  the tooltop as a label on the image
    *------------------------------------------------------------------------------------------------------------*/
     public ImageDisplayPanel(String uri, String coords,  String tooltips, double scaleFactor)
    {
         drawImage(uri, coords, tooltips, false, scaleFactor);
    }
   
    /*------------------------------------------------------------------------------------------------------------*/
   protected  void drawImage(  String uri, String coords,
       String tooltips, boolean drawAsLabel, double imageScaleFactor)
    {
        imageURI = uri;
        coordVal = coords;
        tooltipStr = tooltips;
        drawLabel = drawAsLabel;
        try
        {
            //String imageURI = (String) table.getModel().getValueAt(row, imgColumn);
            // get the image using its URI
            BufferedImage image = getImage(imageURI);
            if (image == null)              // could not fetech image
                return;
           
           BufferedImage scaledImage = image ;
           if (imageScaleFactor == 0)
               imageScaleFactor = 1.0;     // also assunme 0 means no scaling
               
           if ( imageScaleFactor != 1.0)         
           {
              scaledImage = scaleImage(image, imageScaleFactor);
           }

           displayImage = scaledImage;
           scaleFactor = imageScaleFactor;

           this.setLayout(new FlowLayout());
           this.setPreferredSize(new Dimension(displayImage.getWidth(), displayImage.getHeight()));
           repaint();
        }
        catch (Exception ex)
        {
            log.error("Error drawing image", ex);
        }       

    }
   /*------------------------------------------------------------------------------------------------*/
   public BufferedImage scaleImage (BufferedImage  originalImage, double scaleFactor)
   {
            int imageType = originalImage.getType();
            int scaledWidth = (int) ( originalImage.getWidth()*scaleFactor);
            int scaledHeight = (int) ( originalImage.getHeight()*scaleFactor);
              
            // Draw the scaled image
            BufferedImage newImage = new BufferedImage(scaledWidth, scaledHeight, imageType);
           Graphics2D graphics2D = newImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);

        return newImage;
   }

   /*----------------------------------------------------------------------------------------------------------------*/
   // Called by the  external renderer
   /*----------------------------------------------------------------------------------------------------------------*/
    @Override
    public void paintComponent(Graphics g)
    {
        //System.out.println("-- called paintComponent--" );
        if (displayImage == null)
            return;             // nothing to display
        //int x2 = (getWidth() - displayImage.getWidth()) / 2;
        //int y2 = (getHeight() - displayImage.getHeight()) / 2;
     
        g.drawImage(displayImage, 0, 0,  this);             //   "this" as  image observer - not used
         if (drawLabel &&tooltipStr != null && !tooltipStr.isEmpty())
        { 
            addLabelToImage(g, displayImage, tooltipStr);
        }   
        drawRects(g, coordVal);
         

    g.dispose();
    
       // System.out.println("paintComponent: :  ImageDisplayPanel size " + this.getSize());
       this.setToolTipText(tooltipStr);
    }
    
    
    protected BufferedImage getImage(String imageUrl)
    {
        try
        {
            byte[] imageData = URLFetcher.getURLContentsNIO(imageUrl);
            if (imageData == null)
                return null;
            
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            return image;
        }
        catch (Exception e)
        {
            log.error("Could not get image " + imageUrl, e);
            return null;
        }
    }
    
    protected void drawRects(Graphics g, String landmarkStr)
    {
        Graphics2D g2 = (Graphics2D) g;
        if (landmarkStr == null || landmarkStr.isEmpty())
            return;
            
        String[] faceRegions =landmarkStr.trim().split("\\s(f|p)");
        for (int i = 0; i < faceRegions.length; i++)            
        {
            String regionStr = faceRegions[i].trim();
            regionStr = regionStr.replaceAll("f|p","").replaceAll("\\{|\\}", "");       // strip the f|p and outer bracket
            boolean hasLandmarks = regionStr.contains("i") || regionStr.contains("n") || regionStr.contains("m");
            
            String faceCoords = "";
            Point leftCorner = new Point(0,0);
            if (!hasLandmarks)           // region has no landmarks
            {
                faceCoords = regionStr;
               leftCorner = drawRect (g2, Color.RED, 2,  new Point(0, 0), faceCoords);
            }

            else
            {
                // split into individual landmarks through tabs
                String[]  landmarks = regionStr.split("\t");           // separeted by tabs
                if (landmarks.length == 0)           // not a properly formatted string
                    return;
                faceCoords = landmarks[0];
                leftCorner = drawRect (g2, Color.RED, 2, new Point(0, 0), faceCoords);

                Color color = Color.MAGENTA;      // unknown region
                
                // Don't scale the offset twice
               Point scaledOffset = new Point ( (int)(leftCorner.x / scaleFactor) , (int) (leftCorner.y/scaleFactor));
                
                for (int j = 1; j < landmarks.length; j++)
                {
                    String landmark =  landmarks[j];
                    char type = landmark.charAt(0);         // landmark type
                    String landmarkRect = landmarks[j].substring(1);
                    if (type == 'i') 
                        color = Color.BLUE;
                    else if  (type == 'n')
                        color = Color.YELLOW;
                    else if (type == 'm')
                        color = Color.GREEN;
                     drawRect (g2, color, 1, scaledOffset, landmarkRect);
                    
                }
            }   // end else
        }   // end for
        
        // if a scaling factor is given, apply it to g2
    }
        
    //---------------------------------------------------------------------------------------------------------------//
    // Draw a rectangle with given graphics and color      
    // return the x,y coordinates of the top corner
        protected Point  drawRect(Graphics2D g2, Color color, int width, Point offset, String rectStr)
        {
            rectStr = rectStr.replaceAll("\\[|\\]", "").replaceAll(" ", "");        // delete blank spaces, if any
            String[] rect = rectStr.split(",|;");
            if (rect.length != 4)           // ignore if parsing problem
                return new Point (0, 0);
            
            // if theimage was scaled, we must scae the cpprdinates accordingly
            
            int x = Integer.valueOf(rect[0]).intValue();
            int y = Integer.valueOf(rect[1]).intValue();
            int w = Integer.valueOf(rect[2]).intValue();
            int h = Integer.valueOf(rect[3]).intValue(); 
            
            if (scaleFactor != 1.0)
            {
                x = (int) ( x*scaleFactor);
                y = (int) ( y*scaleFactor);
                w = (int) ( w*scaleFactor);
                h = (int) ( h*scaleFactor);
            }
            
            Point scaledOffset = new Point ( (int)(offset.x * scaleFactor) , (int) (offset.y*scaleFactor));
            g2.setColor(color);
            g2.setStroke(new BasicStroke(width));
            g2.drawRect(x+scaledOffset.x, y+scaledOffset.y, w, h);
            
            return new Point(x, y);
        }
        
               
   public void addLabelToImage(Graphics g, BufferedImage image, String label)
   {
        g.setColor(Color.red);
        g.setFont(new Font("Serif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int x =(image.getWidth() - fm.stringWidth(label))/2 - 5;
        //int x = 5;
        int y = fm.getHeight();
        
        // fill the background color as light yellow
        Rectangle2D rect = fm.getStringBounds(label, g);

        g.setColor(lightYellow);
        g.fillRect(x,
                   y - fm.getAscent(),
                   (int) rect.getWidth(),
                   (int) rect.getHeight());

        g.setColor(Color.DARK_GRAY);
        
        g.drawString(label, x, y);
   }

   //----------------------------------------------------------------------------------------------------------//         
    
    
    public static void main(String[] args)
    {

        String[] imageURIs = new String[] {
           "http://<myhost_dir>/Yolanda/2013-yolanda.personfinder.google.orgSLASHperson.78766119__1311891851.png",
           "http://<myhost_dir>/Yolanda/2013-yolanda.personfinder.google.orgSLASHperson.78526071__1656217982.png",
           "http://<myhost_dir>/Yolanda/2013-yolanda.personfinder.google.orgSLASHperson.79076043__1855236831.png"
            
    };
            
        
        String[]  coordStrs =  new String[] {
           
            "f[68,67;164,164]",
            "f[153,22;38,38], f[192,25;38,38], f[33,25;43,43]",
             "f{[34,54;155,155]\ti[29,38;42,42]\ti[88,44;34,34]\tm[47,108;58,35]\ti[63,71;39,39]\tm[44,14;67,40]}}"
        };
        
         String[] distances =  new String[] {
            "distance: .5623348",
            "distance: .6795231",
            "distance:  0"
        };
       
       // 
         // Test only the case with landmarks
        for (int i = 0; i < imageURIs.length; i++)
        {
            ImageDisplayPanel  imageDisplayPanel = new ImageDisplayPanel( imageURIs[i], coordStrs[i],  distances[i],  0.5);
            JFrame frame = new JFrame("TEST CASE.: " + i);
            frame.setLayout(new BorderLayout());
            frame.add(imageDisplayPanel, BorderLayout.CENTER);
            frame.pack();

            System.out.println("ImageDisplayPanel size: " + imageDisplayPanel.getSize());
            System.out.println("Frame size: " + frame.getSize());
            
            frame.setLocation(i*10+10,  i*10+10);
            frame.setVisible(true);
        }

        // repeat with no scaling
        for (int i = 0; i < imageURIs.length; i++)
        {
            ImageDisplayPanel  imageDisplayPanel = new ImageDisplayPanel( imageURIs[i], coordStrs[i],  distances[i]);
            JFrame frame = new JFrame("TEST CASE.: " + i);
            frame.setLayout(new BorderLayout());
            frame.add(imageDisplayPanel, BorderLayout.CENTER);
            frame.pack();

            System.out.println("ImageDisplayPanel size: " + imageDisplayPanel.getSize());
            System.out.println("Frame size: " + frame.getSize());
            
          frame.setLocation(i*10+10,  i*40+40);
            frame.setVisible(true);
        }

         

        // frame.getContentPane().setSize(imageDisplayPanel.getSize());
        try
        {
            Thread.sleep(50000);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

}
