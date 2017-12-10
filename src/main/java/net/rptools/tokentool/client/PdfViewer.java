/*
 * This software Copyright by the RPTools.net development team, and licensed under the Affero GPL Version 3 or, at your option, any later version.
 *
 * TokenTool Source Code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License * along with this source Code. If not, please visit <http://www.gnu.org/licenses/> and specifically the Affero license text
 * at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.tokentool.client;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.rptools.tokentool.AppConstants;
import net.rptools.tokentool.controller.PdfViewer_Controller;
import net.rptools.tokentool.controller.TokenTool_Controller;

public class PdfViewer {
	private Stage stage;

	public PdfViewer(File selectedPDF, TokenTool_Controller tokenTool_Controller) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(AppConstants.PDF_VIEW_FXML), ResourceBundle.getBundle(AppConstants.TOKEN_TOOL_BUNDLE));
		Parent root = fxmlLoader.load();
		PdfViewer_Controller pdfViewerController = fxmlLoader.<PdfViewer_Controller> getController();

		stage = new Stage();
		Scene scene = new Scene(root);

		stage.getIcons().add(new Image(getClass().getResourceAsStream(AppConstants.TOKEN_TOOL_ICON)));
		stage.initModality(Modality.NONE);
		stage.setTitle(selectedPDF.getName());
		stage.setScene(scene);

		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				stage.hide();
				pdfViewerController.close();
			}
		});

		pdfViewerController.loadPDF(selectedPDF, tokenTool_Controller);
		stage.show();

		// Adjusts for tile pane images after loading PDF which adjusts width
		stage.setWidth(stage.getWidth() + 215);
	}
}