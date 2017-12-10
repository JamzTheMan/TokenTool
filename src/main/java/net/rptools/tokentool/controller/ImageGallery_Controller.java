/*
 * This software Copyright by the RPTools.net development team, and licensed under the Affero GPL Version 3 or, at your option, any later version.
 *
 * TokenTool Source Code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License * along with this source Code. If not, please visit <http://www.gnu.org/licenses/> and specifically the Affero license text
 * at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.tokentool.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;
import net.rptools.tokentool.AppConstants;

public class ImageGallery_Controller {
	@FXML private ScrollPane imageScrollPane;
	@FXML private TilePane imageTilePane;

	@FXML
	void initialize() {
		assert imageScrollPane != null : "fx:id=\"imageScrollPane\" was not injected: check your FXML file '" + AppConstants.IMAGE_GALLERY_FXML + "'.";
		assert imageTilePane != null : "fx:id=\"imageFlowPane\" was not injected: check your FXML file '" + AppConstants.IMAGE_GALLERY_FXML + "'.";
	}

	public TilePane getImageGallery() {
		return imageTilePane;
	}
}
