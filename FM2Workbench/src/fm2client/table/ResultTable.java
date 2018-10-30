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
 *	ResultDisplay.java
 */

package fm2client.table;


import fm2client.analyzer.ResultAnalyzer;

import java.awt.Color;
import org.json.simple.JSONObject;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.BorderFactory;

import javax.swing.table.DefaultTableModel;
import javax.swing.ListSelectionModel;



import org.apache.log4j.Logger;


/********************************************************************************************
*   This class displays the results of a test, in the form of JSON String, either from a given 
*   file or a String.
*   It is used by the FaceMatch2 Test Client to display data returned by the FM2 server
*
*  It is created as a  modal Dialog to wait until user disposes it.
*
*-*****************************************************************************************/

public abstract class ResultTable extends JPanel   implements FM2TableConstants   
{
    private static Logger log = Logger.getLogger(ResultTable.class);

    public static Color lightYellow =  new Color (255, 255, 204);
    
   
    protected ResultAnalyzer resultAnalyzer;
    
    JPanel resultTable ;
    protected JTable myTable;
    protected String[] columnNames;
    
    protected int operationType = -1;           // set in derived classes
        
    protected  JButton[] actionButtons;
    protected JFrame parentFrame;               // Frame in which the table is displayed
    
    protected static Font ButtonFont  = new Font("MS Sans Serif", Font.BOLD, 14);    //new Font("Arial", Font.BOLD, 14);
    
    public int  resultType;
    protected class TablePanel  extends JPanel
    {
         public TablePanel( int w, int h)   
        {    
            this.setLayout(new BorderLayout(10,10));
            this.setPreferredSize(new Dimension(w,h));   
        }   
    }
 
    /*----------------------------------------------------------------------------------------------------------------------------*
    * Create a TablePanel and add it to its own JFrame created here
     * @param columns  - name of the columns to be displayed corresponding to the entries
     *--------------------------------------------------------------------------------------------------------------------------------*/
    public  ResultTable (int operation, String tableName, String[] columns, ResultAnalyzer analyzer, String[] buttons)
    {
        operationType = operation;
       
        resultTable = this;
        columnNames = columns;
        
        initTable(columnNames);
        JPanel tablePanel = buildTableModel();
     
         // Functions to be performed for a user selected Row by pressing a panel button
        resultAnalyzer = analyzer;
        String[] buttonNames;
        if (analyzer != null)
            buttonNames = resultAnalyzer.getActionNames(operationType);
        else
            buttonNames = buttons;
        
        JPanel actionPanel = buildActionPanel(buttonNames);
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(tablePanel, BorderLayout.CENTER);
        topPanel.add(actionPanel, BorderLayout.SOUTH);
        //displayDialog.getContentPane().add(topPanel);
        resultTable.add(topPanel);
        resultTable.setVisible(true);

        return;
    }
    
    /*----------------------------------------------------------------------------------------------------------------------------*
     * Create a TablePanel and add it to the input panel as the parent.
    * The inputPanel is a  frame not created here.
    * @param columns  - name of the columns to be displayed corresponding to the entries
    *----------------------------------------------------------------------------------------------------------------------------*
    */
     public  ResultTable (JPanel parentPanel, String tableName, String[] columns)
    {
        columnNames = columns;
        
        initTable(columnNames);
        JPanel tablePanel = buildTableModel();
        parentPanel.add(tablePanel);
        return;
    }
    
    //-----------------------------------------------------------------------------------------
    // Set the background color of the table, default: white
    //
    protected void setTableColor(Color color)
    {
                 myTable.setBackground(color);
    }
   //-----------------------------------------------------------------------------------------     
    protected void initTable(String[] columnNames)
    {
        // create a  table object using the DefaultTableModel, whose cells are non-editable
        myTable= new JTable(new DefaultTableModel(null, columnNames)) 
        {
            public boolean isCellEditable(int row, int column) 
            {
                    return false;
            } // end isCellEditable
        }; // end DefaultTableModel
    }

    /*----------------------------------------------------------------------*/
    protected JPanel  buildTableModel()
    {
        myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //createDialog(myTable);

        // set the initial size
        myTable.setPreferredScrollableViewportSize(new Dimension(1200, 560));

        // set the scrollpane in the tablePanel
        JScrollPane scrollPane= new JScrollPane(myTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        TablePanel tablePanel = new TablePanel(1260, 640);
        tablePanel.add(scrollPane);
        return tablePanel;
    } 
    
       
  /*----------------------------------------------------------------------------------------------------------------*/
    // Create a set of Action buttons for this ResultDisplay Table
    // The action is processed ny the resultAnalyzer through callbacks
    /*----------------------------------------------------------------------------------------------------------------*/
    protected JPanel  buildActionPanel(String[] buttonNames)
    {
        JPanel actionPanel  = new JPanel(new GridLayout(1, 0));
   
        //Border cb = BorderFactory.createMatteBorder(2,2,2,2, Color.BLUE);
        Border lineBorder = LineBorder.createGrayLineBorder();
        Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        Border cb = BorderFactory.createCompoundBorder( lineBorder, emptyBorder);
        
        int nb = buttonNames.length;
        actionButtons = new JButton[nb];
        for (int i = 0; i < nb; i++)
        {         
            JButton actionButton = new JButton(buttonNames[i]); 
            actionButton.setBackground(lightYellow);
            actionButton.setBorder(cb);
            actionButton.setName(buttonNames[i]);
            actionButton.setFont(ButtonFont);
            actionPanel.add(actionButton);
            actionButton.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    userActionPerformed(evt);
                }          
            });    
            actionButtons[i] = actionButton;
        }
        actionPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        return actionPanel;
    }
    
    public void setParentFrame(JFrame frame)
    {
        parentFrame = frame;
    }
    
     public  JFrame getParentFrame()
    {
        return parentFrame;
    }
    
    /*------------------------------------------------------------------------------------------------*/
    // User selected an action button. Call the analyzer to process the 
    // corresponding request.
    /*------------------------------------------------------------------------------------------------*/
    protected void userActionPerformed(ActionEvent evt)
    {
        JButton selectedButton = (JButton) evt.getSource();
        for (int i = 0; i < actionButtons.length; i++)
        {
            if (selectedButton.getText().equalsIgnoreCase("Exit"))
            {
                 if (parentFrame != null)
                     parentFrame.dispose();
                 break;
            }
                
            if (selectedButton == actionButtons[i])
            {
                System.out.println("Selected action: " +  selectedButton.getText());
                int selectedRow = myTable.getSelectedRow();
                if (selectedRow < 0)
                {
                    myTable.setRowSelectionInterval(0,0);
                    selectedRow = 0;
                }
                if (resultAnalyzer != null)
                    resultAnalyzer.processUserRequest(this, selectedRow, selectedButton.getText());
                break;
             }
        }
        /*------------------------------------------------------------------------------------------------*/
    }

    /*-------------------------------------------------------------------------------------------*/
        //  make the added rows in the table visible
    //
    protected void scrollToVisible(int rowCount)
    {
       int  columnIndex = 0;
        boolean includeSpacing = true;
        Rectangle cellRect = myTable.getCellRect(rowCount, columnIndex, includeSpacing);
        myTable.scrollRectToVisible(cellRect);
        return;
    }
    
    /*---------------------------------------------------------------------------------------------*/
    public JTable getTable()
    {
        return myTable;
    }

  
    /*--------------------------------------------------------------------------------
     // to be implemented  in derived classes   
    /*--------------------------------------------------------------------------------*/
   public  int getOperationType()
   {
       return operationType;
   }
    

    public abstract int addRow(int testNum,  int testId, JSONObject resultObj);
   
} //end class

