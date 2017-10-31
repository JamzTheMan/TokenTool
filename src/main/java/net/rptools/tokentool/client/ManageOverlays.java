/*
 * This software Copyright by the RPTools.net development team, and licensed under the Affero GPL Version 3 or, at your option, any later version.
 *
 * TokenTool Source Code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License * along with this source Code. If not, please visit <http://www.gnu.org/licenses/> and specifically the Affero license text
 * at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.tokentool.client;

import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.rptools.tokentool.controller.TokenTool_Controller;

/**
 * 
 * @author Jamz
 * 
 *         To see splashscreen during testing, use JVM arg: -Djavafx.preloader=net.rptools.tokentool.fx.view.SplashScreenLoader Otherwise splashscreen will only show when defined as
 *         JavaFX-Preloader-Class in the JAR manifest.
 * 
 */
public class ManageOverlays {
	private static final Logger log = LogManager.getLogger(ManageOverlays.class);

	private static final String MANAGE_OVERLAYS_FXML = "/net/rptools/tokentool/view/ManageOverlays.fxml";
	private static final String MANAGE_OVERLAYS_ICON = "/net/rptools/tokentool/image/token_tool_icon.png";

	private Stage stage;

	public ManageOverlays(TokenTool_Controller tokenTool_Controller) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(MANAGE_OVERLAYS_FXML), ResourceBundle.getBundle(TokenTool.TOKEN_TOOL_BUNDLE));
			Parent root = (Parent) fxmlLoader.load();

			stage = new Stage();
			Scene scene = new Scene(root);

			stage.getIcons().add(new Image(getClass().getResourceAsStream(MANAGE_OVERLAYS_ICON)));
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setScene(scene);

			stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent event) {
					stage.hide();
					tokenTool_Controller.refreshCache();
				}
			});

			stage.show();
		} catch (Exception e) {
			log.error(e);
		}
	}
}