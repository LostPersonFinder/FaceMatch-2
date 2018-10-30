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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.border.Border;

import org.json.simple.JSONObject;

import workbench.reqproc.input.singleimage.Annotation;
import workbench.reqproc.input.singleimage.ArrayListModel;
import workbench.reqproc.input.singleimage.GBC;
import workbench.reqproc.input.singleimage.ImageLabel;
import workbench.reqproc.input.singleimage.MetaImage;
import workbench.reqproc.input.singleimage.ScaledMetaImage;

@SuppressWarnings("serial")
public class SingleRequestResult extends JFrame {
	private final int THUMBNAIL_SIZE = 150;
	private JSONObject result;
	/**
	 * holds the content Panel, and if there's a query, the thumbnails of images
	 * matches in Jlists
	 */
	private JPanel mainContent = new JPanel();
	/**
	 * holds the display of the image submitted for
	 * ingest/query/removal/facefinding, and text output
	 */
	private JPanel content = new JPanel();
	private JPanel textOutput = new JPanel();

	private MetaImage image;

	SingleRequestResult(JSONObject result) {
		this.result = result;
		this.setResizable(false);
		buildResultFrame();
	}

	private void buildResultFrame() {
		int operation = ((Long) this.result.get("operation")).intValue();
		mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
		// mainContent.setMaximumSize(new Dimension(MAX_WIDTH, (int)
		// mainContent.getMaximumSize().getHeight()));
		add(mainContent);
		content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
		mainContent.add(content);

		addImage();

		textOutput.setLayout(new GridBagLayout());
		textOutput.setBorder(textBorder);
		content.add(textOutput);

		switch (operation) {
		case 501:
			buildFaceFindingResult();
			break;
		case 601:
			buildIngestResult();
			break;
		case 602:
			buildQueryResult();
			break;
		case 603:
			buildRemoveResult();
			break;
		}

		pack();
	}

	/**
	 * {"extent": "christchurch",
	 * <p>
	 * "faceFindTimeMsec":6522.0,
	 * <p>
	 * "databaseTimeMsec": 56. 0,
	 * <p>
	 * "serviceTimeMsec":7171.0,
	 * <p>
	 * "ingestTimeMsec":6913.0,
	 * <p>
	 * "statusMessage": "Image pence ingested in 6913.0 msec.",
	 * <p>
	 * "url":
	 * "http:\/\/dlwsd4gyk2o60.cloudfront.net\/63\/94\/93b4c6934858a3f151ac1947fc96\/governor-pence-official-headshot-high-res.jpg"
	 * ,
	 * <p>
	 * "gpuUsed":true,
	 * <p>
	 * "performance": "optimal",
	 * <p>
	 * "faceRegions":
	 * "f{[591,788;1238,1238]\ti[654,471;309,309]\ti[218,464;338,338]\tn[457,788;295,246]}\tf[387,1582;380,380]"
	 * ,
	 * <p>
	 * "service":600,
	 * <p>
	 * "tag": "pence",
	 * <p>
	 * "ingestedRegions":2,
	 * <p>
	 * "operation":601, "statusCode":1}
	 */
	@SuppressWarnings("unchecked")
	private void addImage() {
		// try to load image and display it
		try {
			if (result.containsKey("url"))
				image = new MetaImage((String) result.get("url"));
			else if (result.containsKey("queryUrl")) {
				image = new MetaImage((String) result.get("queryUrl"));
			} else {
				image = new MetaImage((String) result.get("originalURL"));
				result.put("url", result.get("originalURL"));
			}
			List<String> notes = new ArrayList<>();
			if (result.containsKey("faceRegions")) {
				Object o = result.get("faceRegions");
				if (o instanceof String)
					notes.add((String) o);
				else if (notes instanceof Collection)
					notes.addAll((Collection<? extends String>) o);
			}
			if (result.containsKey("displayRegions")) {
				Object o = result.get("displayRegions");
				if (o instanceof String)
					notes.add((String) o);
				else if (notes instanceof Collection)
					notes.addAll((Collection<? extends String>) o);
			}

			for (String note : notes)
				image.getAnnotations().addAll(Annotation.parseAnnotationList(note));

			ImageLabel displayImage = new ImageLabel(image);
			content.add(displayImage);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	Border textBorder = BorderFactory.createEtchedBorder();

	/**
	 * {
	 * <p>
	 * "extent": "christchurch",
	 * <p>
	 * "databaseTimeMsec":0.0,
	 * <p>
	 * "serviceTimeMsec":1.0,
	 * <p>
	 * "numRegions":0,
	 * <p>
	 * "service":600,
	 * <p>
	 * "tag": "pence",
	 * <p>
	 * "operation":603,
	 * <p>
	 * "statusMessage": "ImageExtent christchurchdoes not exist or was deleted",
	 * <p>
	 * "statusCode":1404
	 * <p>
	 * }
	 * 
	 */
	private void buildRemoveResult() {
		// display all other output for the user to see
		int row = 0;
		addOutput("Extent: ", "extent", "", 0, row++);
		row = addCommonTextOutput(row);
		addOutput("Database Time: ", "databaseTimeMsec", "ms", 0, row++);
		addOutput("Regions: ", "numRegions", "", 0, row++);
		addOutput("Tag removed: ", "tag", "", 0, row++);

	}

	/**
	 * {
	 * <p>
	 * "extent":"christchurch",
	 * <p>
	 * "faceFindTimeMsec":0.0,
	 * <p>
	 * "serviceTimeMsec":1117.0,
	 * <p>
	 * "indexVersion":"V1",
	 * <p>
	 * "statusMessage": "1 face regions in input image matched in 981.0 msec." ,
	 * <p>
	 * "gpuUsed":true,
	 * <p>
	 * "queryUrl":
	 * "http:\/\/www.uspolitico.com\/wp-content\/uploads\/2016\/11\/ivanka_trump_1.jpg"
	 * ,
	 * <p>
	 * "maxmatches":100,
	 * <p>
	 * "indexType":"DIST",
	 * <p>
	 * "performance":"optimal",
	 * <p>
	 * "numRegions" :1,
	 * <p>
	 * "totalQueryTimeMsec":981.0,
	 * <p>
	 * "service":600,
	 * <p>
	 * "indexUploadTime":0.0,
	 * <p>
	 * "allMatches":[],
	 * <p>
	 * "operation":602,
	 * <p>
	 * "tolerance":1.0,
	 * <p>
	 * "statusCode":1
	 * <p>
	 * }
	 * 
	 */
	private void buildQueryResult() {
		int row = 0;

		addOutput("Extent: ", "extent", "", 0, row++);
		row = addCommonTextOutput(row);
		addOutput("Query Time: ", "totalQueryTimeMsec", "ms", 0, row++);
		addOutput("Face Find Time: ", "faceFindTimeMsec", "ms", 0, row++);
		addOutput("Index Upload Time", "indexUploadTime", "ms", 0, row++);
		addOutput("Indexing: ", "indexType", "", 0, row++);
		addOutput("Index Version: ", "indexVersion", "", 0, row++);
		addOutput("Regions Found: ", "numRegions", "", 0, row++);
		addOutput("Tolerance: ", "tolerance", "", 0, row++);
		addOutput("Maximum Matches: ", "maxmatches", "", 0, row++);

		// add in film strip of query result matches
		if (result.containsKey("allMatches")) {

			this.pack();
			int width = this.mainContent.getWidth();

			JPanel queryResultsContainer = new JPanel();// main container

			JPanel imageArea = new JPanel();// holds each JList
			imageArea.setLayout(new BoxLayout(imageArea, BoxLayout.Y_AXIS));
			JScrollPane queryResults = new JScrollPane(imageArea);
			queryResultsContainer.setLayout(new BorderLayout());
			queryResultsContainer.add(queryResults, BorderLayout.CENTER);
			mainContent.add(queryResultsContainer);

			List<ListModel<ScaledMetaImage>> queryResultImages = parseQueryMatches(
					(List<JSONObject>) result.get("allMatches"));

			for (ListModel<ScaledMetaImage> list : queryResultImages) {
				JList<ScaledMetaImage> output = new JList<>(list);
				output.setVisibleRowCount(1);
				output.setLayoutOrientation(JList.HORIZONTAL_WRAP);
				output.setCellRenderer(new MetaImageRenderer());
				output.setFixedCellWidth(THUMBNAIL_SIZE);
				output.setFixedCellHeight(THUMBNAIL_SIZE+75);
				imageArea.add(output);
			}
			Dimension size = new Dimension(width, THUMBNAIL_SIZE * 3);

			queryResultsContainer.setMaximumSize(size);
			queryResultsContainer.setPreferredSize(size);
			queryResultsContainer.setMinimumSize(size);
		}
	}

	private List<ListModel<ScaledMetaImage>> parseQueryMatches(List<JSONObject> queryMatches) {
		List<ListModel<ScaledMetaImage>> result = new ArrayList<>();
		for (JSONObject match : queryMatches) {
			result.add(parseQuaryMatch(match));
		}
		return result;
	}

	private ListModel<ScaledMetaImage> parseQuaryMatch(JSONObject queryMatch) {
		ArrayListModel<ScaledMetaImage> ret = new ArrayListModel<>();
		ScaledMetaImage queryImage;
		try {
			queryImage = new ScaledMetaImage((String) result.get("queryUrl"), THUMBNAIL_SIZE);
			queryImage.getAnnotations().addAll(Annotation.parseAnnotationList((String) queryMatch.get("queryRegion")));
			queryImage.setTag("Query_Image");
			ret.add(queryImage);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for (JSONObject match : (List<JSONObject>) queryMatch.get("regionMatches")) {
			try {
				ScaledMetaImage otherMatch = new ScaledMetaImage((String) match.get("ingestUrl"), THUMBNAIL_SIZE);
				otherMatch.setAnnotations(Annotation.parseAnnotationList((String) match.get("region")));
				otherMatch.matchValue = (Double) match.get("distance");
				otherMatch.setTag((String) match.get("tag"));
				ret.add(otherMatch);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Collections.sort(ret);
		return ret;
	}

	/**
	 * {
	 * <p>
	 * "extent":"christchurch",
	 * <p>
	 * "faceFindTimeMsec":527.0,
	 * <p>
	 * "databaseTimeMsec":39.0 ,
	 * <p>
	 * "serviceTimeMsec":736.0,
	 * <p>
	 * "ingestTimeMsec":570.0,
	 * <p>
	 * "statusMessage": "Image ivanka_trump ingested in 570.0 msec.",
	 * <p>
	 * "url":
	 * "http:\/\/www.uspolitico.com\/wp-content\/uploads\/2016\/11\/ivanka_trump_1.jpg"
	 * ,
	 * <p>
	 * "gpuUsed":true,
	 * <p>
	 * "performance":"optimal",
	 * <p>
	 * "faceRegions": "f{[455,94;295,295]\tn[108,192;82,68]}",
	 * <p>
	 * "service":600,
	 * <p>
	 * "tag": "ivanka_trump",
	 * <p>
	 * "ingestedRegions":1,
	 * <p>
	 * "operation":601,
	 * <p>
	 * "statusCode":1
	 * <p>
	 * }
	 * 
	 */
	private void buildIngestResult() {
		int row = 0;

		addOutput("Extent: ", "extent", "", 0, row++);
		row = addCommonTextOutput(row);
		addOutput("Face Find Time: ", "faceFindTimeMsec", "ms", 0, row++);
		addOutput("Ingest Time: ", "ingestTimeMsec", "ms", 0, row++);
		addOutput("Database Time: ", "databaseTimeMsec", "ms", 0, row++);
		addOutput("Tag: ", "tag", "", 0, row++);
		addOutput("Number of Regions Ingested: ", "ingestedRegions", "", 0, row++);
		addOutput("Regions Ingested: ", "faceRegions", "", 0, row++);

		this.image.getAnnotations().addAll(Annotation.parseAnnotationList((String) this.result.get("faceRegions")));

	}

	/**
	 * Sample FaceFinder Result: {
	 * <p>
	 * "faceFindTimeMsec":185.0,
	 * <p>
	 * "gpuUsed":true,
	 * <p>
	 * "serviceTimeMsec":344.0,
	 * <p>
	 * "performance":"progressive",
	 * <p>
	 * "faceRegions":[
	 * "f{[466,141;281,281]\tn[96,145;82,68]\tm[82,209;108,63]}"],
	 * <p>
	 * "displayRegions":[
	 * "f{[466,141;281,281]\tn[96,145;82,68]\tm[82,209;108,63]}"],
	 * <p>
	 * "service":500,
	 * <p>
	 * "operation":501,
	 * <p>
	 * "statusMessage":"Detected 1 face(s) successfully.",
	 * <p>
	 * "url":
	 * "http:\/\/www.uspolitico.com\/wp-content\/uploads\/2016\/11\/ivanka_trump_1.jpg"
	 * ,
	 * <p>
	 * "urlFetchMsec":141.0,
	 * <p>
	 * "statusCode":1
	 * <p>
	 * }
	 * 
	 * }
	 */
	private void buildFaceFindingResult() {
		int row = 0;
		row = addCommonTextOutput(row);
		addOutput("Face Find Time: ", "faceFindTimeMsec", "ms", 0, row++);
		addOutput("URL Fetch Time: ", "urlFetchMsec", "ms", 0, row++);
		addOutput("Face Regions: ", "faceRegions", "", 0, row++);
		addOutput("Display Regions: ", "displayRegions", "", 0, row++);

	}

	private GBC param = new GBC(0, 0).setAnchor(GridBagConstraints.FIRST_LINE_START).setInsets(1).setIpad(1, 1);

	private final static int textWidth = 250;
	private final static String htmlOpen = "<html><body style='width:" + textWidth + "px'>";
	private final static String htmlClose = "</body></html>";

	private void addOutput(String label, String key, String suffix, int x, int y) {
		this.textOutput.add(new JLabel(label), param.setLocation(x, y));
		String output = "";
		if (result.containsKey(key))
			output = "" + result.get(key) + suffix;
		if (output.trim().equals(""))
			output = "NA";
		output = output.replace("\\t", "    ");
		output = htmlOpen + output + htmlClose;
		this.textOutput.add(new JLabel(output), param.setX(x + 1));
	}

	public static void showResult(JSONObject ret) {

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				new SingleRequestResult(ret).setVisible(true);
			}
		});
		t.start();

	}

	private int addCommonTextOutput(int row) {
		String urlKey = null;
		if (result.containsKey("url"))
			urlKey = "url";
		else
			urlKey = "queryUrl";

		addOutput("URL: ", "url", "", 0, row++);
		addOutput("Status Message: ", "statusMessage", "", 0, row++);
		addOutput("Status Code: ", "statusCode", "", 0, row++);
		addOutput("Performance: ", "performance", "", 0, row++);
		addOutput("GPU Used: ", "gpuUsed", "", 0, row++);
		addOutput("Service: ", "service", "", 0, row++);
		addOutput("Operation: ", "operation", "", 0, row++);
		addOutput("Service Time: ", "serviceTimeMsec", "ms", 0, row++);
		return row;
	}

}
