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
 * TextDisplayWindow.java
 *
 * Created on February 6, 2008, 1:32 PM
 */

package fm2client.display;

import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JDialog;
import javax.swing.border.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;

public class TextDisplayWindow 
{
    static Color lightYellow = new Color(255, 255, 225);
       
    Rectangle defaultRect = new Rectangle(20, 20, 500, 400); // default    
    JDialog textWindow;
    JTextArea textArea = null;
    boolean isEditable = false;
    String textString = null;
    Window parentWindow = null;
    
    Color background;
    Rectangle textWindowRect;
    
    WindowAdapter windowCloseListener;

    // To use defaults
    Font  textFont = null;          // default
    int fontSize = -1;
    
    /** Creates a new instance of TextDisplayWindow */
    public TextDisplayWindow(String title, Window parent, WindowAdapter closeListener) 
    {
        textWindow = null;
        windowCloseListener = closeListener;     // to be notified on close
        parentWindow = parent;
        
        // set defaults
        textWindowRect = defaultRect;
        background = lightYellow;        
        
        createTextDisplayWindow(title);
    }
    
    // set background to different than default
    public void setColor(Color bgColor)
    {
        background = bgColor;
    }

    // set the desired font and size for the TextArea
    public void setFont(Font font)
    {
        if (font != null)
            textFont = font;
    }
    
    // Set dimension to somethinf other than default
    public void setBounds(Point location, Dimension size)
    {
        textWindowRect = new Rectangle(defaultRect);        
        if (location != null)
            textWindowRect.setLocation(location);
        if (size != null) 
            textWindowRect.setSize(size);
    }
    
    // Display the specified text in a given window
    // Note: The window may be reused to display different text strings
    // at different times, until the window is closed
    //
     public void createTextDisplayWindow( String title)
    {
       isEditable = false;   
       
       textArea = new JTextArea();
       //textArea.append(text);
       textArea.setBackground(background);        // light yellow
       textArea.setBorder(new EmptyBorder(10, 25, 10, 25));

       if (textFont != null)
           textArea.setFont(textFont);
       
       JScrollPane scrollPane = new JScrollPane(textArea);      
       
       textArea.setEditable(isEditable); 
      // textString = text;

        textWindow = new JDialog((Frame)parentWindow);
        textWindow.getContentPane().add(scrollPane); 

        WindowHandler  closeHandler = new WindowHandler();
        textWindow.addWindowListener(closeHandler); 
        textWindow.setSize(textWindowRect.width, textWindowRect.height);
        textWindow.setLocation(textWindowRect.x, textWindowRect.y);
        textWindow.setVisible(true);

            // also to inform the original listener
            if (windowCloseListener != null)
                textWindow.addWindowListener(windowCloseListener);

       textWindow.setTitle(title);
       textArea.setCaretPosition(0);     // scrolls to top
       textWindow.toFront();
    }
     
     public void addText(String text)
     {
         textArea.append(text+"\n");
     }
 
 /**********************************************************************
  * Return the text being displayed (which might be different
  * from the original due to editing)
  *********************************************************************/
     
     public String getDisplayedText()
     {
         if (isEditable == false)       // no change, return original string
             return textString;

         if (textArea != null)
            return (textArea.getText());
         return null;
     }
     
     // Being the text window to front
     public void toFront()
     {
         if (textWindow != null)
             textWindow.toFront();
     }
     
     public void close()
     {
         if (textWindow != null)
             textWindow.dispose();
     }
     
     public Window getWindow()
     {
         return textWindow;
     }
     
///////////////////////////////////////////////////////////////////////////
//                      Inner Classes
/////////////////////////////////////////////////////////////////////////////

/***  Handler class for window events  ***/
    private class WindowHandler extends WindowAdapter
    {
        /*** 
         * Comes here when the user closes the window explicitly from the menubar
         ***/
        public void windowClosing(WindowEvent e)
        {
            //System.out.println("Received Window Closing event\n" + e.toString());
            if (e.getWindow() == textWindow)
            {
                textWindowRect = textWindow.getBounds(); // for next time
                textString = textArea.getText();
                e.getWindow().dispose();               // simply dispose the window
                textWindow = null;
            }
        }
    }  
}
