/*
 * This software Copyright by the RPTools.net development team, and licensed under the Affero GPL Version 3 or, at your option, any later version.
 *
 * TokenTool Source Code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License * along with this source Code. If not, please visit <http://www.gnu.org/licenses/> and specifically the Affero license text
 * at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.tokentool.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import net.rptools.tokentool.AppConstants;
import net.rptools.tokentool.model.PdfModel;

public class PdfViewer_Controller implements Initializable {
	private static final Logger log = LogManager.getLogger(PdfViewer_Controller.class);

	@FXML private SplitPane pdfViewSplitPane;
	@FXML private AnchorPane pdfAnchorPane;
	@FXML private Pagination pdfViewPagination;
	@FXML private TextField pageNumberTextField;

	private PdfModel model;
	private ImageGallery_Controller imageGalleryController;
	private ImageView pdfImageView;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		assert pdfViewSplitPane != null : "fx:id=\"pdfViewSplitPane\" was not injected: check your FXML file '" + AppConstants.PDF_VIEW_FXML + "'.";
		assert pdfAnchorPane != null : "fx:id=\"pdfAnchorPane\" was not injected: check your FXML file '" + AppConstants.PDF_VIEW_FXML + "'.";
		assert pdfViewPagination != null : "fx:id=\"pdfViewPagination\" was not injected: check your FXML file '" + AppConstants.PDF_VIEW_FXML + "'.";
		assert pageNumberTextField != null : "fx:id=\"pageNumberTextField\" was not injected: check your FXML file '" + AppConstants.PDF_VIEW_FXML + "'.";

		pdfImageView = new ImageView();
		pdfImageView.fitWidthProperty().bind(pdfViewSplitPane.widthProperty());
		pdfImageView.fitHeightProperty().bind(pdfViewSplitPane.heightProperty());
		pdfImageView.setPreserveRatio(true);
	}

	public void loadPDF(File pdfFile, TokenTool_Controller tokenTool_Controller) {
		try {
			model = new PdfModel(pdfFile, tokenTool_Controller);
		} catch (IOException e) {
			log.error("Error loading PDF " + pdfFile.getAbsolutePath(), e);
		}

		pdfViewPagination.setPageCount(model.numPages());
		pdfViewPagination.setPageFactory(index -> setPageView(index));

		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(AppConstants.IMAGE_GALLERY_FXML));
			ScrollPane imageGallery = fxmlLoader.load();
			imageGalleryController = fxmlLoader.<ImageGallery_Controller> getController();

			pdfViewSplitPane.getItems().add(imageGallery);

			SplitPane.setResizableWithParent(imageGallery, Boolean.FALSE);
		} catch (IOException e) {
			log.error("IO Error in pdfViewer extractImages().", e);
		}
	}

	private ImageView setPageView(int pageIndex) {
		pdfImageView.setImage(model.getImage(pageIndex));
		extractImages();

		return pdfImageView;
	}

	private void extractImages() {
		imageGalleryController.getImageGallery().getChildren().clear();
		model.extractImages(imageGalleryController.getImageGallery(), pdfViewPagination.getCurrentPageIndex());
	}

	public void close() {
		model.close();
	}

	@FXML
	void pdfViewPagination_OnScroll(ScrollEvent event) {
		int delta = 1;
		if (event.getDeltaX() > 1 || event.getDeltaY() > 1)
			delta = -1;

		pdfViewPagination.setCurrentPageIndex(pdfViewPagination.getCurrentPageIndex() + delta);
	}

	@FXML
	void pdfViewPagination_onMouseClick(MouseEvent event) {
		extractImages();
	}

	@FXML
	void pageNumberTextField_onMouseClicked(MouseEvent event) {
		pageNumberTextField.setOpacity(1);
	}

	@FXML
	void pageNumberTextField_onAction(ActionEvent event) {
		int pageNumber = Integer.parseInt(pageNumberTextField.getText());
		log.info("Got some action! " + event.getEventType() + " :: " + pageNumber);

		if (pageNumber > pdfViewPagination.getPageCount())
			pageNumber = pdfViewPagination.getPageCount();

		if (pageNumber > 0)
			pdfViewPagination.setCurrentPageIndex(pageNumber - 1);

		pageNumberTextField.setText(pdfViewPagination.getCurrentPageIndex() + 1 + "");
		pdfViewPagination.requestFocus();
		pageNumberTextField.setOpacity(0);
	}
}
