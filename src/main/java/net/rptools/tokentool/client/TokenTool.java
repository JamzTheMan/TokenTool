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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ResourceBundle;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;

import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.rptools.tokentool.AppConstants;
import net.rptools.tokentool.AppSetup;
import net.rptools.tokentool.controller.TokenTool_Controller;
import net.rptools.tokentool.util.ImageUtil;

/**
 * 
 * @author Jamz
 * 
 *         To see splashscreen during testing, use JVM arg: -Djavafx.preloader=net.rptools.tokentool.fx.view.SplashScreenLoader Otherwise splashscreen will only show when defined as
 *         JavaFX-Preloader-Class in the JAR manifest.
 * 
 */
public class TokenTool extends Application {
	private static Logger log; // Don't instantiate until AppSetup gets and sets user_home/logs directory in AppSetup

	private static final String TOKEN_TOOL_ICON = "/net/rptools/tokentool/image/token_tool_icon.png";
	private static final String TOKEN_TOOL_FXML = "/net/rptools/tokentool/view/TokenTool.fxml";
	public static final String TOKEN_TOOL_BUNDLE = "net.rptools.tokentool.i18n.TokenTool";

	private BorderPane root;
	private TokenTool_Controller tokentool_Controller;

	private static String VERSION = "";
	private static String VENDOR = "";

	private static final int THUMB_SIZE = 100;
	private int overlayCount = 0;
	private int loadCount = 1;
	private TreeItem<Path> overlayTreeItems;

	@Override
	public void init() throws Exception {
		VERSION = getVersion();

		// Lets install/update the overlays if newer version
		AppSetup.install(VERSION);
		log = LogManager.getLogger(TokenTool.class);
		log.info("3D Hardware Available? " + Platform.isSupported(ConditionalFeature.SCENE3D));

		// Now lets cache any overlays we find and update preLoader with progress
		overlayCount = (int) Files.walk(AppConstants.OVERLAY_DIR.toPath()).filter(Files::isRegularFile).count();
		overlayTreeItems = cacheOverlays(AppConstants.OVERLAY_DIR, null, THUMB_SIZE);

		// All Done!
		notifyPreloader(new Preloader.ProgressNotification(1.0));
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		setUserAgentStylesheet(STYLESHEET_MODENA); // Setting the style back to the new Modena
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(TOKEN_TOOL_FXML), ResourceBundle.getBundle(TOKEN_TOOL_BUNDLE));
		root = fxmlLoader.load();
		tokentool_Controller = (TokenTool_Controller) fxmlLoader.getController();

		Scene scene = new Scene(root);
		primaryStage.setTitle("TokenTool");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream(TOKEN_TOOL_ICON)));
		primaryStage.setScene(scene);

		// Load all the overlays into the treeview
		tokentool_Controller.updateOverlayTreeview(overlayTreeItems); // TODO: oh oh init is called before this! move this to init!

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				tokentool_Controller.exitApplication();
			}
		});

		primaryStage.show();
		tokentool_Controller.expandOverlayOptionsPane(true);
		tokentool_Controller.updateTokenPreviewImageView();
	}

	/**
	 * 
	 * @author Jamz
	 * @throws IOException
	 * @since 2.0
	 * 
	 *        This method loads and processes all the overlays found in user.home/overlays and it can take a minute to load as it creates thumbnail versions for the comboBox so we call this during the
	 *        init and display progress in the preLoader (splash screen).
	 * 
	 */
	private TreeItem<Path> cacheOverlays(File dir, TreeItem<Path> parent, int THUMB_SIZE) throws IOException {
		TreeItem<Path> root = new TreeItem<>(dir.toPath());
		root.setExpanded(false);

		log.debug("caching " + dir.getAbsolutePath());

		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				cacheOverlays(file, root, THUMB_SIZE);
			} else {
				Path filePath = file.toPath();
				TreeItem<Path> imageNode = new TreeItem<>(filePath, ImageUtil.getOverlayThumb(new ImageView(), filePath));
				root.getChildren().add(imageNode);

				notifyPreloader(new Preloader.ProgressNotification((double) loadCount++ / overlayCount));
			}
		}

		if (parent != null) {
			// When we show the overlay image, the TreeItem value is "" so we need to
			// sort those to the bottom for a cleaner look and keep sub dir's at the top.
			// If a node has no children then it's an overlay, otherwise it's a directory...
			root.getChildren().sort(new Comparator<TreeItem<Path>>() {
				@Override
				public int compare(TreeItem<Path> o1, TreeItem<Path> o2) {
					if (o1.getChildren().size() == 0 && o2.getChildren().size() == 0)
						return 0;
					else if (o1.getChildren().size() == 0)
						return Integer.MAX_VALUE;
					else if (o2.getChildren().size() == 0)
						return Integer.MIN_VALUE;
					else
						return o1.getValue().compareTo(o2.getValue());
				}
			});

			parent.getChildren().add(root);
		}

		return root;
	}

	public static String getVersion() {
		if (!VERSION.isEmpty())
			return VERSION;

		VERSION = "DEVELOPMENT";

		if (TokenTool.class.getPackage().getImplementationVersion() != null) {
			VERSION = TokenTool.class.getPackage().getImplementationVersion().trim();
		}

		return VERSION;
	}

	public static String getVendor() {
		if (!VENDOR.isEmpty())
			return VENDOR;

		if (TokenTool.class.getPackage().getImplementationVendor() != null) {
			VENDOR = TokenTool.class.getPackage().getImplementationVendor().trim();
		}

		return VENDOR;
	}

	private static String getCommandLineStringOption(Options options, String searchValue, String[] args) {
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption(searchValue)) {
				return cmd.getOptionValue(searchValue);
			}
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return "";
	}

	public static String getLoggerFileName() {
		org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) log;
		Appender appender = loggerImpl.getAppenders().get("LogFile");

		if (appender != null)
			return ((FileAppender) appender).getFileName();
		else
			return "NOT_CONFIGURED";
	}

	/**
	 * Legacy, it simply launches the FX Application which calls init() then start()
	 * 
	 * @author Jamz
	 * @since 2.0
	 * 
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		Options cmdOptions = new Options();
		cmdOptions.addOption("v", "version", true, "override version number");
		cmdOptions.addOption("n", "vendor", true, "override vendor");

		VERSION = getCommandLineStringOption(cmdOptions, "version", args);
		VENDOR = getCommandLineStringOption(cmdOptions, "vendor", args);

		launch(args);
	}
}