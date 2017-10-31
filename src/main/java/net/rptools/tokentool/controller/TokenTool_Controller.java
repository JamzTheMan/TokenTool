/*
 * This software Copyright by the RPTools.net development team, and licensed under the Affero GPL Version 3 or, at your option, any later version.
 *
 * TokenTool Source Code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License * along with this source Code. If not, please visit <http://www.gnu.org/licenses/> and specifically the Affero license text
 * at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.tokentool.controller;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import net.rptools.tokentool.AppConstants;
import net.rptools.tokentool.AppPreferences;
import net.rptools.tokentool.client.ManageOverlays;
import net.rptools.tokentool.client.RegionSelector;
import net.rptools.tokentool.util.FileSaveUtil;
import net.rptools.tokentool.util.ImageUtil;

public class TokenTool_Controller {
	@FXML private MenuItem fileManageOverlaysMenu;
	@FXML private MenuItem fileSaveAsMenu;
	@FXML private MenuItem fileExitMenu;

	@FXML private MenuItem editCaptureScreenMenu;
	@FXML private MenuItem editCopyImageMenu;
	@FXML private MenuItem editPasteImageMenu;

	@FXML private MenuItem helpAboutMenu;

	@FXML private TitledPane saveOptionsPane;
	@FXML private TitledPane overlayOptionsPane;
	@FXML private TitledPane backgroundOptionsPane;
	@FXML private TitledPane zoomOptionsPane;

	@FXML private StackPane compositeTokenPane;
	@FXML private BorderPane tokenPreviewPane;
	@FXML private ScrollPane portraitScrollPane;

	@FXML private Group compositeGroup;

	@FXML private TreeView<Path> overlayTreeView;

	@FXML private ImageView portraitImageView; // The bottom "Portrait" layer
	@FXML private ImageView maskImageView; // The mask layer used to crop the Portrait layer
	@FXML private ImageView overlayImageView; // The overlay layer to apply on top of everything
	@FXML private ImageView tokenImageView;

	@FXML private CheckBox useFileNumberingCheckbox;
	@FXML private CheckBox overlayUseAsBaseCheckbox;

	@FXML private TextField fileNameTextField;
	@FXML private Label fileNameSuffixLabel;
	@FXML private TextField fileNameSuffixTextField;
	@FXML private Label overlayNameLabel;
	@FXML private ColorPicker backgroundColorPicker;
	@FXML private ToggleButton overlayAspectToggleButton;

	@FXML private Slider portraitTransparencySlider;
	@FXML private Slider portraitBlurSlider;
	@FXML private Slider portraitGlowSlider;

	@FXML private Slider overlayTransparencySlider;

	@FXML private Spinner<Double> overlayWidthSpinner;
	@FXML private Spinner<Double> overlayHeightSpinner;

	@FXML private ProgressBar overlayTreeProgressBar;
	@FXML private Label progressBarLabel;

	private static final Logger log = LogManager.getLogger(TokenTool_Controller.class);

	private static ExecutorService executorService;
	private static Thread loadOverlaysThread = new Thread();
	private static AtomicInteger loadCount = new AtomicInteger(0);

	private static int overlayCount;
	private static int recentTreeItemIndex = -1;

	private static TreeItem<Path> treeItems;
	private static TreeItem<Path> lastSelectedItem;

	private static Map<Path, TreeItem<Path>> recentOverlayTreeItems = new LinkedHashMap<Path, TreeItem<Path>>() {
		private static final long serialVersionUID = 2579964060760662199L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<Path, TreeItem<Path>> eldest) {
			return size() > AppConstants.MAX_RECENT_SIZE;
		}
	};

	private Point dragStart = new Point();
	private Point portraitImageStart = new Point();
	private FileSaveUtil fileSaveUtil = new FileSaveUtil();

	@SuppressWarnings("unused") private RegionSelector regionSelector;

	// A custom set of Width/Height sizes to use for Overlays
	private NavigableSet<Double> overlaySpinnerSteps = new TreeSet<Double>(Arrays.asList(50d, 100d, 128d, 150d, 200d,
			256d, 300d, 400d, 500d, 512d, 600d, 700d, 750d, 800d, 900d, 1000d));

	@FXML
	void initialize() {
		// Note: A Pane is added to the compositeTokenPane so the ScrollPane doesn't consume the mouse events
		assert fileManageOverlaysMenu != null : "fx:id=\"fileManageOverlaysMenu\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert fileSaveAsMenu != null : "fx:id=\"fileSaveAsMenu\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert fileExitMenu != null : "fx:id=\"fileExitMenu\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert editCaptureScreenMenu != null : "fx:id=\"editCaptureScreenMenu\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert editCopyImageMenu != null : "fx:id=\"editCopyImageMenu\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert editPasteImageMenu != null : "fx:id=\"editPasteImageMenu\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert helpAboutMenu != null : "fx:id=\"helpAboutMenu\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert saveOptionsPane != null : "fx:id=\"saveOptionsPane\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert overlayOptionsPane != null : "fx:id=\"overlayOptionsPane\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert backgroundOptionsPane != null : "fx:id=\"backgroundOptionsPane\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert zoomOptionsPane != null : "fx:id=\"zoomOptionsPane\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert compositeTokenPane != null : "fx:id=\"compositeTokenPane\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert tokenPreviewPane != null : "fx:id=\"tokenPreviewPane\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert portraitScrollPane != null : "fx:id=\"portraitScrollPane\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert compositeGroup != null : "fx:id=\"compositeGroup\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert overlayTreeView != null : "fx:id=\"overlayTreeview\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert portraitImageView != null : "fx:id=\"portraitImageView\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert maskImageView != null : "fx:id=\"maskImageView\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert overlayImageView != null : "fx:id=\"overlayImageView\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert tokenImageView != null : "fx:id=\"tokenImageView\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert useFileNumberingCheckbox != null : "fx:id=\"useFileNumberingCheckbox\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert overlayUseAsBaseCheckbox != null : "fx:id=\"overlayUseAsBaseCheckbox\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert fileNameTextField != null : "fx:id=\"fileNameTextField\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert fileNameSuffixLabel != null : "fx:id=\"fileNameSuffixLabel\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert fileNameSuffixTextField != null : "fx:id=\"fileNameSuffixTextField\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert overlayNameLabel != null : "fx:id=\"overlayNameLabel\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert backgroundColorPicker != null : "fx:id=\"backgroundColorPicker\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert overlayAspectToggleButton != null : "fx:id=\"overlayAspectToggleButton\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert portraitTransparencySlider != null : "fx:id=\"portraitTransparencySlider\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert portraitBlurSlider != null : "fx:id=\"portraitBlurSlider\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert portraitGlowSlider != null : "fx:id=\"portraitGlowSlider\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert overlayTransparencySlider != null : "fx:id=\"overlayTransparencySlider\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert overlayWidthSpinner != null : "fx:id=\"overlayWidthSpinner\" was not injected: check your FXML file 'TokenTool.fxml'.";
		assert overlayHeightSpinner != null : "fx:id=\"overlayHeightSpinner\" was not injected: check your FXML file 'TokenTool.fxml'.";

		assert overlayTreeProgressBar != null : "fx:id=\"overlayTreeProgressIndicator\" was not injected: check your FXML file 'ManageOverlays.fxml'.";

		// executorService = Executors.newSingleThreadScheduledExecutor(runable -> {
		executorService = Executors.newCachedThreadPool(runable -> {
			loadOverlaysThread = Executors.defaultThreadFactory().newThread(runable);
			loadOverlaysThread.setDaemon(true);
			return loadOverlaysThread;
		});

		overlayTreeView.setShowRoot(false);
		overlayTreeView.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> updateCompositImageView((TreeItem<Path>) newValue));

		addPseudoClassToLeafs(overlayTreeView);

		// Bind color picker to compositeTokenPane background fill
		backgroundColorPicker.setValue(Color.TRANSPARENT);
		ObjectProperty<Background> background = compositeTokenPane.backgroundProperty();
		background.bind(Bindings.createObjectBinding(() -> {
			BackgroundFill fill = new BackgroundFill(backgroundColorPicker.getValue(), CornerRadii.EMPTY, Insets.EMPTY);
			return new Background(fill);
		}, backgroundColorPicker.valueProperty()));

		// Bind transparency slider to portraitImageView opacity
		portraitTransparencySlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				portraitImageView.setOpacity(new_val.doubleValue());
				updateTokenPreviewImageView();
			}
		});

		// Effects
		GaussianBlur gaussianBlur = new GaussianBlur(0);
		Glow glow = new Glow(0);
		gaussianBlur.setInput(glow);

		// Bind blur slider to portraitImageView opacity
		portraitBlurSlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				gaussianBlur.setRadius(new_val.doubleValue());
				portraitImageView.setEffect(gaussianBlur);
				updateTokenPreviewImageView();
			}
		});

		// Bind glow slider to portraitImageView opacity
		portraitGlowSlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				glow.setLevel(new_val.doubleValue());
				portraitImageView.setEffect(gaussianBlur);
				updateTokenPreviewImageView();
			}
		});

		// Bind transparency slider to overlayImageView opacity
		overlayTransparencySlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				overlayImageView.setOpacity(new_val.doubleValue());
				updateTokenPreviewImageView();
			}
		});

		// Bind width/height spinners to overlay width/height
		overlayWidthSpinner.getValueFactory().valueProperty()
				.bindBidirectional(overlayHeightSpinner.getValueFactory().valueProperty());
		overlayWidthSpinner.valueProperty()
				.addListener((observable, oldValue, newValue) -> overlayWidthSpinner_onTextChanged(oldValue, newValue));
		overlayHeightSpinner.valueProperty().addListener(
				(observable, oldValue, newValue) -> overlayHeightSpinner_onTextChanged(oldValue, newValue));

		// Restore saved preferences
		AppPreferences.restorePreferences(this);

		// updateOverlayTreeViewRecentFolder();
	}

	@FXML
	void removeBackgroundButton_onAction(ActionEvent event) {
		backgroundColorPicker.setValue(Color.TRANSPARENT);
		updateTokenPreviewImageView();
	}

	@FXML
	void fileManageOverlaysMenu_onAction(ActionEvent event) {
		@SuppressWarnings("unused")
		ManageOverlays manageOverlays = new ManageOverlays(this);
	}

	@FXML
	void fileSaveAsMenu_onAction(ActionEvent event) {
		saveToken();
	}

	@FXML
	void fileExitMenu_onAction(ActionEvent event) {
		exitApplication();
	}

	@FXML
	void editCaptureScreenMenu_onAction(ActionEvent event) {
		regionSelector = new RegionSelector(this);
	}

	@FXML
	void editCopyImageMenu_onAction(ActionEvent event) {
		Clipboard clipboard = Clipboard.getSystemClipboard();
		ClipboardContent content = new ClipboardContent();

		// for paste as file, e.g. in Windows Explorer
		try {
			File tempTokenFile = fileSaveUtil.getTempFileName(false, useFileNumberingCheckbox.isSelected(),
					fileNameTextField.getText(), fileNameSuffixTextField);
			ImageIO.write(SwingFXUtils.fromFXImage(tokenImageView.getImage(), null), "png", tempTokenFile);

			content.putFiles(java.util.Collections.singletonList(tempTokenFile));
			tempTokenFile.deleteOnExit();
		} catch (Exception e) {
			log.error(e);
		}

		// for paste as image, e.g. in GIMP
		content.putImage(tokenImageView.getImage());

		// Finally, put contents on clip board
		clipboard.setContent(content);
	}

	@FXML
	void editPasteImageMenu_onAction(ActionEvent event) {
		Clipboard clipboard = Clipboard.getSystemClipboard();
		Image originalImage = portraitImageView.getImage();

		// Strangely, we get an error if we try to paste an image we put in the clipboard ourselves but File works ok?
		// -Dprism.order=sw also fixes it but not sure why...
		// So lets just check for File first...
		if (clipboard.hasFiles()) {
			clipboard.getFiles().forEach(file -> {
				try {
					Image cbImage = new Image(file.toURI().toURL().toExternalForm());

					if (cbImage != null)
						updatePortrait(cbImage);

					String tokenName = FilenameUtils.getBaseName(file.toURI().toURL().toExternalForm());
					if (!tokenName.isEmpty())
						fileNameTextField.setText(tokenName);
					else
						fileNameTextField.setText("token");
				} catch (Exception e) {
					log.error("Could not load image " + file);
					e.printStackTrace();
				}
			});
		} else if (clipboard.hasImage()) {
			try {
				Image cbImage = clipboard.getImage();
				if (cbImage != null)
					updatePortrait(cbImage);
			} catch (IllegalArgumentException e) {
				log.info(e);
				updatePortrait(originalImage);
			}
		} else if (clipboard.hasUrl()) {
			try {
				Image cbImage = new Image(clipboard.getUrl());
				if (cbImage != null)
					updatePortrait(cbImage);

				String tokenName = FilenameUtils.getBaseName(clipboard.getUrl());
				if (!tokenName.isEmpty())
					fileNameTextField.setText(tokenName);
				else
					fileNameTextField.setText("token");
			} catch (IllegalArgumentException e) {
				log.info(e);
			}
		} else if (clipboard.hasString()) {
			try {
				Image cbImage = new Image(clipboard.getString());
				if (cbImage != null)
					updatePortrait(cbImage);

				String tokenName = FilenameUtils.getBaseName(clipboard.getString());
				if (!tokenName.isEmpty())
					fileNameTextField.setText(tokenName);
				else
					fileNameTextField.setText("token");
			} catch (IllegalArgumentException e) {
				log.info(e);
			}
		}
	}

	@FXML
	void helpAboutMenu_onAction(ActionEvent event) {
		log.info("helpAboutMenu_onAction invoked!");
		// TokenTool.getFrame().showAboutDialog();
	}

	@FXML
	void useFileNumberingCheckbox_onAction(ActionEvent event) {
		fileNameSuffixLabel.setDisable(!useFileNumberingCheckbox.isSelected());
		fileNameSuffixTextField.setDisable(!useFileNumberingCheckbox.isSelected());
	}

	@FXML
	void compositeTokenPane_MouseDragged(MouseEvent event) {
		portraitImageView.setTranslateX(event.getX() - dragStart.x + portraitImageStart.x);
		portraitImageView.setTranslateY(event.getY() - dragStart.y + portraitImageStart.y);

		updateTokenPreviewImageView();
	}

	@FXML
	void compositeTokenPane_MousePressed(MouseEvent event) {
		dragStart.setLocation(event.getX(), event.getY());
		portraitImageStart.setLocation(portraitImageView.getTranslateX(), portraitImageView.getTranslateY());
		portraitImageView.setCursor(Cursor.MOVE);
	}

	@FXML
	void compositeTokenPane_MouseReleased(MouseEvent event) {
		portraitImageView.setCursor(Cursor.HAND);
		updateTokenPreviewImageView();
	}

	@FXML
	void compositeTokenPane_MouseEntered(MouseEvent event) {
		portraitImageView.setCursor(Cursor.HAND);
	}

	@FXML
	void compositeTokenPane_MouseDragExited(MouseDragEvent event) {
	}

	@FXML
	void compositeTokenPane_MouseExited(MouseEvent event) {
	}

	@FXML
	void compositeTokenPane_MouseMoved(MouseEvent event) {
	}

	@FXML
	void compositeTokenPane_OnScroll(ScrollEvent event) {
		if (event.isShiftDown()) {
			Double r = portraitImageView.getRotate() + event.getDeltaX() / 20;

			if (r < -360d || r > 360d)
				r = 0d;

			portraitImageView.setRotate(r);
		} else {
			Double scale = portraitImageView.getScaleY() * Math.pow(1.001, event.getDeltaY());
			portraitImageView.setScaleX(scale);
			portraitImageView.setScaleY(scale);
		}

		updateTokenPreviewImageView();
	}

	@FXML
	void compositeTokenPane_DragDropped(DragEvent event) {
		Dragboard db = event.getDragboard();

		// Strangely, we get an error if we try to paste an image we put in the clipboard ourselves but File works ok?
		// -Dprism.order=sw also fixes it but not sure why...
		// So lets just check for File first...
		if (db.hasFiles()) {
			db.getFiles().forEach(file -> {
				try {
					String tokenName = FilenameUtils.getBaseName(file.toURI().toURL().toExternalForm());
					if (!tokenName.isEmpty())
						fileNameTextField.setText(tokenName);
					else
						fileNameTextField.setText("token");

					updatePortrait(new Image(file.toURI().toURL().toExternalForm()));
				} catch (Exception e) {
					log.error("Could not load image " + file, e);
				}
			});
			event.setDropCompleted(true);
		} else if (db.hasImage()) {
			updatePortrait(db.getImage());
			event.setDropCompleted(true);
		} else if (db.hasUrl()) {
			String tokenName = FilenameUtils.getBaseName(db.getUrl());
			if (!tokenName.isEmpty())
				fileNameTextField.setText(tokenName);
			else
				fileNameTextField.setText("token");

			updatePortrait(new Image(db.getUrl()));
			event.setDropCompleted(true);
		}
	}

	@FXML
	void compositeTokenPane_DragDone(DragEvent event) {
		updateTokenPreviewImageView();
	}

	@FXML
	void compositeTokenPane_DragOver(DragEvent event) {
		if (event.getDragboard().hasImage() || event.getDragboard().hasFiles() || event.getDragboard().hasUrl()) {
			// Set Pane color to an alpha green
			event.acceptTransferModes(TransferMode.COPY);
		} else {
			// Set Pane color to an alpha red?
			event.acceptTransferModes(TransferMode.ANY);
		}
	}

	@FXML
	void tokenImageView_OnDragDetected(MouseEvent event) {
		Dragboard db = tokenImageView.startDragAndDrop(TransferMode.ANY);
		ClipboardContent content = new ClipboardContent();

		boolean saveAsToken = false;

		try {
			File tempTokenFile = fileSaveUtil.getTempFileName(saveAsToken, useFileNumberingCheckbox.isSelected(),
					fileNameTextField.getText(), fileNameSuffixTextField);

			// if (saveAsToken) {
			// AppActions.saveToken(tempTokenFile, true);
			// } else {
			ImageIO.write(SwingFXUtils.fromFXImage(tokenImageView.getImage(), null), "png", tempTokenFile);
			// log.debug("Drag size: " + tokenImageView.getImage().getWidth() + ",
			// " + tokenImageView.getImage().getHeight());
			// }

			content.putFiles(java.util.Collections.singletonList(tempTokenFile));
			tempTokenFile.deleteOnExit();
		} catch (Exception e) {
			log.error(e);
		} finally {
			content.putImage(tokenImageView.getImage());
			db.setContent(content);
			event.consume();
		}
	}

	@FXML
	void overlayUseAsBaseCheckbox_onAction(ActionEvent event) {
		if (overlayUseAsBaseCheckbox.isSelected())
			compositeGroup.toBack();
		else
			portraitScrollPane.toBack();

		updateTokenPreviewImageView();
	}

	@FXML
	void backgroundColorPicker_onAction(ActionEvent event) {
		updateTokenPreviewImageView();
	}

	@FXML
	void overlayAspectToggleButton_onAction(ActionEvent event) {
		if (overlayAspectToggleButton.isSelected()) {
			overlayImageView.setPreserveRatio(true);
			maskImageView.setPreserveRatio(true);
			overlayWidthSpinner.getValueFactory().valueProperty()
					.bindBidirectional(overlayHeightSpinner.getValueFactory().valueProperty());
		} else {
			overlayImageView.setPreserveRatio(false);
			maskImageView.setPreserveRatio(false);
			overlayWidthSpinner.getValueFactory().valueProperty()
					.unbindBidirectional(overlayHeightSpinner.getValueFactory().valueProperty());
		}

		updateTokenPreviewImageView();
	}

	void overlayWidthSpinner_onTextChanged(double oldValue, double newValue) {
		if (newValue < overlaySpinnerSteps.first())
			newValue = overlaySpinnerSteps.first();

		if (newValue > overlaySpinnerSteps.last())
			newValue = overlaySpinnerSteps.last();

		if (newValue > oldValue)
			overlayWidthSpinner.getValueFactory().setValue(overlaySpinnerSteps.ceiling(newValue));
		else
			overlayWidthSpinner.getValueFactory().setValue(overlaySpinnerSteps.floor(newValue));

		overlayImageView.setFitWidth(overlayWidthSpinner.getValue());
		maskImageView.setFitWidth(overlayWidthSpinner.getValue());

		updateTokenPreviewImageView();
	}

	void overlayHeightSpinner_onTextChanged(double oldValue, double newValue) {
		if (newValue < overlaySpinnerSteps.first())
			newValue = overlaySpinnerSteps.first();

		if (newValue > overlaySpinnerSteps.last())
			newValue = overlaySpinnerSteps.last();

		if (newValue > oldValue)
			overlayHeightSpinner.getValueFactory().setValue(overlaySpinnerSteps.ceiling(newValue));
		else
			overlayHeightSpinner.getValueFactory().setValue(overlaySpinnerSteps.floor(newValue));

		overlayImageView.setFitHeight(overlayHeightSpinner.getValue());
		maskImageView.setFitHeight(overlayHeightSpinner.getValue());

		updateTokenPreviewImageView();
	}

	/*
	 * getter/setter methods, mainly for user preferences
	 */
	public double getOverlayWidth() {
		return overlayWidthSpinner.getValue();
	}

	public void setOverlayWidth(double newValue) {
		overlayWidthSpinner.getValueFactory().setValue(overlaySpinnerSteps.ceiling(newValue));
	}

	public double getOverlayHeight() {
		return overlayHeightSpinner.getValue();
	}

	public void setOverlayHeight(double newValue) {
		overlayHeightSpinner.getValueFactory().setValue(overlaySpinnerSteps.ceiling(newValue));
	}

	public boolean getOverlayAspect() {
		return overlayAspectToggleButton.isSelected();
	}

	public void setOverlayAspect(boolean selected) {
		// UI normally starts this toggle as selected == aspect locked
		if (!selected)
			overlayAspectToggleButton.fire();
	}

	public boolean getOverlayUseAsBase() {
		return overlayUseAsBaseCheckbox.isSelected();

	}

	public void setOverlayUseAsBase(boolean selected) {
		if (selected)
			overlayUseAsBaseCheckbox.fire();
	}

	public Set<Path> getRecentOverlayTreeItems() {
		return recentOverlayTreeItems.keySet();
	}

	public void addRecentOverlayTreeItem(Path filePath) {
		try {
			TreeItem<Path> recentOverlay = new TreeItem<Path>(filePath, ImageUtil.getOverlayThumb(new ImageView(), filePath));
			recentOverlayTreeItems.put(filePath, recentOverlay);
		} catch (IOException e) {
			log.error("Error loading recent overlay preference for " + filePath.toString());
		}
	}

	public void expandOverlayOptionsPane(boolean expand) {
		overlayOptionsPane.setExpanded(expand);
	}

	public void expandBackgroundOptionsPane(boolean expand) {
		backgroundOptionsPane.setExpanded(expand);
	}

	public void updateOverlayTreeview(TreeItem<Path> overlayTreeItems) {
		overlayTreeView.setRoot(overlayTreeItems);
	}

	public void updateTokenPreviewImageView() {
		tokenImageView.setImage(ImageUtil.composePreview(compositeTokenPane, backgroundColorPicker.getValue(),
				portraitImageView, maskImageView, overlayImageView, overlayUseAsBaseCheckbox.isSelected()));
		tokenImageView.setPreserveRatio(true);
	}

	private void saveToken() {
		boolean asToken = false; // TODO: Get from checkbox Save Option if this is implemented later
		FileChooser fileChooser = new FileChooser();

		try {
			File tokenFile = fileSaveUtil.getFileName(asToken, useFileNumberingCheckbox.isSelected(),
					fileNameTextField.getText(), fileNameSuffixTextField);
			fileChooser.setInitialFileName(tokenFile.getName());
			if (tokenFile.getParentFile() != null)
				if (tokenFile.getParentFile().isDirectory())
					fileChooser.setInitialDirectory(tokenFile.getParentFile());
		} catch (IOException e1) {
			log.error("Error writing token!", e1);
			;
		}

		fileChooser.getExtensionFilters().addAll(AppConstants.IMAGE_EXTENSION_FILTER);

		// if (asToken) {
		// fileChooser.setTitle("Save as Token");
		// fileChooser.setSelectedExtensionFilter(AppConstants.TOKEN_EXTENSION_FILTER);
		// } else {
		fileChooser.setTitle("Save as Image");
		fileChooser.setSelectedExtensionFilter(AppConstants.IMAGE_EXTENSION_FILTER);
		// }

		File tokenSaved = fileChooser.showSaveDialog(saveOptionsPane.getScene().getWindow());

		if (tokenSaved == null)
			return;

		log.debug("selected: " + fileChooser.getSelectedExtensionFilter().getDescription());
		log.debug("Saving token to : " + tokenSaved.getAbsolutePath());

		try {
			// if (fileChooser.getSelectedExtensionFilter().equals(AppConstants.TOKEN_EXTENSION_FILTER)) {
			// AppActions.saveToken(tokenSaved, true);
			// } else {
			ImageIO.write(SwingFXUtils.fromFXImage(tokenImageView.getImage(), null), "png", tokenSaved);
			// }

			if (!useFileNumberingCheckbox.isSelected()) {
				fileNameTextField.setText(FilenameUtils.getBaseName(tokenSaved.getName()));
				fileSaveUtil.setLastFile(tokenSaved);
			}

			updateOverlayTreeViewRecentFolder();

			log.debug("tokenSaved is " + tokenSaved);

		} catch (Exception e) {
			log.error(e);
		}
	}

	public void updateOverlayTreeViewRecentFolder() {
		if (lastSelectedItem != null) {
			// Remove before adding to put it at the top of the list
			recentOverlayTreeItems.remove(lastSelectedItem.getValue());
			recentOverlayTreeItems.put(lastSelectedItem.getValue(), lastSelectedItem);
		}

		// Update Recent Overlay List
		if (!recentOverlayTreeItems.isEmpty()) {
			log.info("recent: " + recentOverlayTreeItems.keySet().toString());

			TreeItem<Path> recentFolder = new TreeItem<>(new File(AppConstants.OVERLAY_DIR, "Recent").toPath(), null);

			// convert to ArrayList of key set
			List<TreeItem<Path>> recentOverlayTreeItemsReversed = new ArrayList<TreeItem<Path>>(recentOverlayTreeItems.values());

			// reverse order of keys
			Collections.reverse(recentOverlayTreeItemsReversed);

			// Add recent overlays sorted most recent to top
			recentFolder.getChildren().addAll(recentOverlayTreeItemsReversed);

			// Remember current selection (adding/removing tree items messes with the selection model)
			int selectedItem = overlayTreeView.getSelectionModel().getSelectedIndex();
			overlayTreeView.getSelectionModel().clearSelection();

			// If Recent folder item already exists, remove it
			if (recentTreeItemIndex >= 0)
				overlayTreeView.getRoot().getChildren().remove(recentTreeItemIndex);

			// Add the Recent folder item
			recentFolder.setExpanded(true);
			overlayTreeView.getRoot().getChildren().add(recentFolder);
			addPseudoClassToLeafs(overlayTreeView);

			// Set the selected index back to what it was
			overlayTreeView.getSelectionModel().clearAndSelect(selectedItem);

			// Save the index of the Recent folder item just added
			recentTreeItemIndex = overlayTreeView.getRoot().getChildren().indexOf(recentFolder);
		}
	}

	private void addPseudoClassToLeafs(TreeView<Path> tree) {
		PseudoClass leaf = PseudoClass.getPseudoClass("leaf");

		tree.setCellFactory(tv -> {
			TreeCell<Path> cell = new TreeCell<>();
			cell.itemProperty().addListener((obs, oldValue, newValue) -> {
				if (newValue == null) {
					cell.setText("");
					cell.setGraphic(null);
				} else {
					cell.setText(newValue.toFile().getName());
					cell.setGraphic(cell.getTreeItem().getGraphic());
				}
			});
			cell.treeItemProperty().addListener((obs, oldTreeItem, newTreeItem) -> cell.pseudoClassStateChanged(leaf,
					newTreeItem != null && newTreeItem.isLeaf()));
			return cell;
		});
	}

	public void updatePortrait(Image newPortraitImage) {
		double w = newPortraitImage.getWidth();
		double h = newPortraitImage.getHeight();
		double pw = portraitScrollPane.getWidth();
		double ph = portraitScrollPane.getHeight();

		portraitImageView.setImage(newPortraitImage);

		portraitImageView.setTranslateX((pw - w) / 2);
		portraitImageView.setTranslateY((ph - h) / 2);
		portraitImageView.setScaleX(1);
		portraitImageView.setScaleY(1);
		portraitImageView.setRotate(0d);

		updateTokenPreviewImageView();
	}

	private void updateCompositImageView(TreeItem<Path> treeNode) {
		// Node removed...
		if (treeNode == null)
			return;

		// I'm not a leaf on the wind! (Sub directory node)
		if (treeNode.getChildren().size() > 0)
			return;

		try {
			Path filePath = treeNode.getValue();
			lastSelectedItem = treeNode;

			// Set the Image Views
			maskImageView = ImageUtil.getMaskImage(maskImageView, filePath);
			overlayImageView = ImageUtil.getOverlayImage(overlayImageView, filePath);

			// Set the text label
			overlayNameLabel.setText(FilenameUtils.getBaseName(filePath.toFile().getName()));

			updateTokenPreviewImageView();
		} catch (IOException e) {
			// Not a valid URL, most likely this is just because it's a directory node.
			e.printStackTrace();
		}
	}

	public Color getBackgroundColor() {
		return backgroundColorPicker.getValue();
	}

	public void refreshCache() {
		overlayTreeProgressBar.setStyle("");
		overlayTreeProgressBar.setVisible(true);
		overlayTreeProgressBar.setOpacity(1.0);
		overlayNameLabel.setOpacity(0.0);
		progressBarLabel.setVisible(true);
		updateOverlayTreeview(null);

		try {
			loadCount.set(0);
			overlayCount = (int) Files.walk(AppConstants.OVERLAY_DIR.toPath()).filter(Files::isRegularFile).count();
			log.info("overlayCount: " + overlayCount);

			treeItems = cacheOverlays(AppConstants.OVERLAY_DIR, null, AppConstants.THUMB_SIZE);
		} catch (IOException e) {
			log.error("Error reloading overlay cache!", e);
		}
	}

	private void treeViewFinish() {
		// Sort the nodes off of root
		treeItems = sortTreeNodes(treeItems);

		updateOverlayTreeview(treeItems);
		addPseudoClassToLeafs(overlayTreeView);

		// overlayNameLabel.setVisible(true);
		overlayTreeProgressBar.setStyle("-fx-accent: forestgreen;");
		progressBarLabel.setVisible(false);

		FadeTransition fadeOut = new FadeTransition(Duration.millis(2000));
		fadeOut.setNode(overlayTreeProgressBar);
		fadeOut.setFromValue(1.0);
		fadeOut.setToValue(0.0);
		fadeOut.setCycleCount(1);
		fadeOut.setAutoReverse(false);
		fadeOut.playFromStart();

		FadeTransition fadeIn = new FadeTransition(Duration.millis(4000));
		fadeIn.setNode(overlayNameLabel);
		fadeIn.setFromValue(0.0);
		fadeIn.setToValue(1.0);
		fadeIn.setCycleCount(1);
		fadeIn.setAutoReverse(false);
		fadeIn.playFromStart();

	}

	private TreeItem<Path> cacheOverlays(File dir, TreeItem<Path> parent, int THUMB_SIZE) throws IOException {
		log.info("Caching " + dir.getAbsolutePath());

		TreeItem<Path> root = new TreeItem<>(dir.toPath());
		root.setExpanded(false);
		File[] files = dir.listFiles();

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				for (File file : files) {
					if (loadOverlaysThread.isInterrupted())
						break;

					// log.info("Caching " + file.getAbsolutePath());

					if (file.isDirectory()) {
						cacheOverlays(file, root, THUMB_SIZE);
					} else {
						Path filePath = file.toPath();
						TreeItem<Path> imageNode = new TreeItem<>(filePath, ImageUtil.getOverlayThumb(new ImageView(), filePath));
						root.getChildren().add(imageNode);
						loadCount.getAndIncrement();
					}

					updateProgress(loadCount.doubleValue(), overlayCount);
					// log.info("loadCount: " + loadCount + ", " + ((double) loadCount/overlayCount));
				}

				if (parent != null) {
					// When we show the overlay image, the TreeItem value is empty so we need to
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

					parent.getChildren().sort(new Comparator<TreeItem<Path>>() {
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
				}

				return null;
			}
		};

		overlayTreeProgressBar.progressProperty().bind(task.progressProperty());

		// Only call this once, in this case, the "parent" task
		overlayTreeProgressBar.progressProperty().addListener(observable -> {
			Platform.runLater(() -> progressBarLabel.setText("Caching " + Math.round(overlayCount - loadCount.doubleValue()) + "..."));

			if (overlayTreeProgressBar.getProgress() >= 1 && parent == null) {
				treeViewFinish();
			}
		});

		executorService.execute(task);
		return root;
	}

	private TreeItem<Path> sortTreeNodes(TreeItem<Path> tree) {
		// Sort the nodes off of root
		tree.getChildren().sort(new Comparator<TreeItem<Path>>() {
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

		return tree;
	}

	public void exitApplication() {
		AppPreferences.savePreferences(this);
		Platform.exit();
	}
}
