/*
 * This software Copyright by the RPTools.net development team, and licensed under the Affero GPL Version 3 or, at your option, any later version.
 *
 * TokenTool Source Code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License * along with this source Code. If not, please visit <http://www.gnu.org/licenses/> and specifically the Affero license text
 * at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.tokentool.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry;

import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import net.rptools.tokentool.controller.ImageGallery_Controller;
import net.rptools.tokentool.controller.TokenTool_Controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Extract all images from a PDF using Apache's PdfBox 2.0 This will also walk through all annotations and extract those images as well which is key, some interactive PDF's, such as from Paizo, store
 * different versions of maps as button icons, which will not normally extract using other methods.
 * 
 * @author Jamz
 *
 */
public final class ExtractImagesFromPDF {
	private static final Logger log = LogManager.getLogger(ImageGallery_Controller.class);

	private final PDDocument document;

	private final Set<COSStream> imageTracker = new HashSet<COSStream>();

	private final static int imageViewSize = 150;
	private final static int imageButtonSize = 200;

	private TokenTool_Controller tokenTool_Controller;
	private TilePane imageTilePane;

	public ExtractImagesFromPDF(PDDocument document, TokenTool_Controller tokenTool_Controller) {
		this.tokenTool_Controller = tokenTool_Controller;
		this.document = document;
	}

	public void addImages(TilePane imageTilePane, int pageNumber) throws IOException {
		imageTracker.clear();
		this.imageTilePane = imageTilePane;

		getImagesFromResources(document.getPage(pageNumber).getResources());
		extractAnnotationImages(document.getPage(pageNumber));
	}

	private void getImagesFromResources(PDResources resources) throws IOException {
		// Testing various Pathfinder PDF's, various page elements like borders and backgrounds generally come first...
		// ...so lets sort them to the bottom and get the images we really want to the top of the TilePane!
		ArrayList<COSName> xObjectNamesReversed = new ArrayList<>();

		for (COSName xObjectName : resources.getXObjectNames()) {
			xObjectNamesReversed.add(xObjectName);
		}

		Collections.reverse(xObjectNamesReversed);

		for (COSName xObjectName : xObjectNamesReversed) {
			PDXObject xObject = resources.getXObject(xObjectName);

			if (xObject instanceof PDFormXObject) {
				getImagesFromResources(((PDFormXObject) xObject).getResources());
			} else if (xObject instanceof PDImageXObject) {
				// log.info("Extracting image... " + xObjectName.getName());

				if (!imageTracker.contains(xObject.getCOSObject())) {
					imageTracker.add(xObject.getCOSObject());

					ToggleButton imageButton = new ToggleButton();
					ImageView imageViewNode = new ImageView(SwingFXUtils.toFXImage(((PDImageXObject) xObject).getImage(), null));
					imageViewNode.setFitWidth(imageViewSize);
					imageViewNode.setFitHeight(imageViewSize);
					imageButton.setPrefWidth(imageButtonSize);
					imageButton.setPrefHeight(imageButtonSize);
					imageViewNode.setPreserveRatio(true);

					imageButton.getStyleClass().add("overlay-toggle-button");
					imageButton.setGraphic(imageViewNode);

					imageButton.addEventHandler(ActionEvent.ACTION, event -> {
						imageButton.setSelected(true);

						tokenTool_Controller.updatePortrait(imageViewNode.getImage());
						event.consume();
					});

					// Finally, add it to the tile pane!
					Platform.runLater(() -> imageTilePane.getChildren().add(imageButton));
				}
			}
		}
	}

	/*
	 * Jamz: A note on what we are doing here...
	 * 
	 * Paizo's Interactive PDF's (amongst others) are sneaky and put map images in the PDF as a "button" with an image resource. So we need to walk through all the forms to find the buttons, then walk
	 * through all the button resources for the images. Also, a 'Button Down' may hold the 'Grid' version of the map and 'Button Up' may hold the 'Non-Grid' version. There may also be Player vs GM
	 * versions of each for a total of up to 4 images per button!
	 * 
	 * This is the REAL beauty of this function as currently no other tools outside of Full Acrobat extracts these raw images!
	 * 
	 */
	private void extractAnnotationImages(PDPage page) throws IOException {
		for (PDAnnotation annotation : page.getAnnotations()) {
			extractAnnotationImages(annotation);
		}
	}

	private void extractAnnotationImages(PDAnnotation annotation) throws IOException {
		PDAppearanceDictionary appearance = annotation.getAppearance();

		if (appearance == null)
			return;

		extractAnnotationImages(appearance.getDownAppearance());
		extractAnnotationImages(appearance.getNormalAppearance());
		extractAnnotationImages(appearance.getRolloverAppearance());
	}

	public void extractAnnotationImages(PDAppearanceEntry appearance) throws IOException {
		PDResources resources = appearance.getAppearanceStream().getResources();
		if (resources == null)
			return;

		for (COSName cosname : resources.getXObjectNames()) {
			PDXObject xObject = resources.getXObject(cosname);

			if (xObject instanceof PDFormXObject)
				extractAnnotationImages((PDFormXObject) xObject);
			else if (xObject instanceof PDImageXObject)
				extractAnnotationImages((PDImageXObject) xObject);
		}
	}

	public void extractAnnotationImages(PDFormXObject form) throws IOException {
		PDResources resources = form.getResources();
		if (resources == null)
			return;

		for (COSName cosname : resources.getXObjectNames()) {
			PDXObject xObject = resources.getXObject(cosname);

			if (xObject instanceof PDFormXObject)
				extractAnnotationImages((PDFormXObject) xObject);
			else if (xObject instanceof PDImageXObject)
				extractAnnotationImages((PDImageXObject) xObject);
		}
	}

	public void extractAnnotationImages(PDImageXObject xObject) throws IOException {
		log.info("Extracting Annotations, eg button images...");

		if (!imageTracker.contains(xObject.getCOSObject())) {
			imageTracker.add(xObject.getCOSObject());

			ToggleButton imageButton = new ToggleButton();
			ImageView imageViewNode = new ImageView(SwingFXUtils.toFXImage(xObject.getImage(), null));
			imageViewNode.setFitWidth(imageViewSize);
			imageViewNode.setFitHeight(imageViewSize);
			imageButton.setPrefWidth(imageButtonSize);
			imageButton.setPrefHeight(imageButtonSize);
			imageViewNode.setPreserveRatio(true);

			imageButton.getStyleClass().add("overlay-toggle-button");
			imageButton.setGraphic(imageViewNode);

			imageButton.addEventHandler(ActionEvent.ACTION, event -> {
				imageButton.setSelected(true);

				tokenTool_Controller.updatePortrait(imageViewNode.getImage());
				event.consume();
			});

			// Finally, add it to the tile pane!
			Platform.runLater(() -> imageTilePane.getChildren().add(imageButton));
		}
	}

}