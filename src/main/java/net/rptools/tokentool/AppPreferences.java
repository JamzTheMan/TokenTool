/*
 * This software Copyright by the RPTools.net development team, and licensed under the Affero GPL Version 3 or, at your option, any later version.
 *
 * TokenTool Source Code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License * along with this source Code. If not, please visit <http://www.gnu.org/licenses/> and specifically the Affero license text
 * at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.tokentool;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.prefs.Preferences;

import net.rptools.tokentool.controller.TokenTool_Controller;

public class AppPreferences {
	private static Preferences prefs = Preferences.userNodeForPackage(net.rptools.tokentool.client.TokenTool.class);

	final static String OVERLAY_ASPECT = "overlayAspectToggleButton";
	final static String OVERLAY_WIDTH = "overlayWidthSpinner";
	final static String OVERLAY_HEIGHT = "overlayHeightSpinner";
	final static String OVERLAY_USE_BASE = "overlayUseAsBaseCheckbox";
	final static String RECENT_OVERLAY_COUNT = "recent_overlay_count";
	final static String RECENT_OVERLAY = "recent_overlay_";

	public static void savePreferences(TokenTool_Controller tokentool_Controller) {
		prefs.putBoolean(OVERLAY_ASPECT, tokentool_Controller.getOverlayAspect());
		prefs.putDouble(OVERLAY_WIDTH, tokentool_Controller.getOverlayWidth());
		prefs.putDouble(OVERLAY_HEIGHT, tokentool_Controller.getOverlayHeight());
		prefs.putBoolean(OVERLAY_USE_BASE, tokentool_Controller.getOverlayUseAsBase());

		Set<Path> recentOverlays = tokentool_Controller.getRecentOverlayTreeItems();
		prefs.putInt(RECENT_OVERLAY_COUNT, recentOverlays.size());

		int i = 1;
		for (Path path : recentOverlays) {
			prefs.put(RECENT_OVERLAY + i, path.toString());
		}
	}

	public static void restorePreferences(TokenTool_Controller tokentool_Controller) {
		tokentool_Controller.setOverlayAspect(prefs.getBoolean(OVERLAY_ASPECT, AppConstants.DEFAULT_OVERLAY_ASPECT));
		tokentool_Controller.setOverlayWidth(prefs.getDouble(OVERLAY_WIDTH, AppConstants.DEFAULT_OVERLAY_SIZE));
		tokentool_Controller.setOverlayHeight(prefs.getDouble(OVERLAY_HEIGHT, AppConstants.DEFAULT_OVERLAY_SIZE));
		tokentool_Controller.setOverlayUseAsBase(prefs.getBoolean(OVERLAY_USE_BASE, AppConstants.DEFAULT_OVERLAY_USE_BASE));

		int overlayCount = prefs.getInt(RECENT_OVERLAY_COUNT, 0);
		for (int i = 0; i <= overlayCount; i++) {
			String filePath = prefs.get(RECENT_OVERLAY + i, "");
			if (!filePath.isEmpty())
				tokentool_Controller.addRecentOverlayTreeItem(Paths.get(filePath));
		}

	}
}
