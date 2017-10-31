/*
 * This software Copyright by the RPTools.net development team, and licensed under the Affero GPL Version 3 or, at your option, any later version.
 *
 * TokenTool Source Code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License * along with this source Code. If not, please visit <http://www.gnu.org/licenses/> and specifically the Affero license text
 * at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.tokentool.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CopyOfTreeViewSample extends Application {

	private TreeView<Path> treeView;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		stage.setTitle("Sample");
		stage.setWidth(300);
		stage.setHeight(500);

		VBox vbox = new VBox();
		vbox.setPadding(new Insets(20));

		TreeItem<Path> root = new SimpleFileTreeItem(
				Paths.get(System.getProperty("user.home")));
		root.setExpanded(true);
		treeView = new TreeView<Path>(root);

		treeView.setCellFactory(treeView -> new TreeCell<Path>() {
			@Override
			public void updateItem(Path path, boolean empty) {
				super.updateItem(path, empty);
				if (empty) {
					setText(null);
				} else {
					setText(path.getFileName().toString());
				}
			}
		});

		Button b = new Button("Change");
		b.disableProperty().bind(Bindings.isNull(treeView.getSelectionModel().selectedItemProperty()));

		b.setOnAction(event -> {
			Path selectedPath = treeView.getSelectionModel().getSelectedItem().getValue();
			// do something with selectedPath...
			System.out.println(selectedPath);
		});

		vbox.getChildren().addAll(treeView, b);
		vbox.setSpacing(10);

		Scene scene = new Scene(vbox);

		stage.setScene(scene);
		stage.show();
	}

	public class SimpleFileTreeItem extends TreeItem<Path> {

		private boolean isFirstTimeChildren = true;
		private boolean isFirstTimeLeaf = true;
		private boolean isLeaf;

		public boolean isDirectory() {
			return Files.isDirectory(getValue());
		}

		public SimpleFileTreeItem(Path f) {
			super(f);
		}

		@Override
		public ObservableList<TreeItem<Path>> getChildren() {
			if (isFirstTimeChildren) {
				isFirstTimeChildren = false;

				/*
				 * First getChildren() call, so we actually go off and determine the children of the File contained in this TreeItem.
				 */
				super.getChildren().setAll(buildChildren());
			}
			return super.getChildren();
		}

		@Override
		public boolean isLeaf() {
			if (isFirstTimeLeaf) {
				isFirstTimeLeaf = false;
				isLeaf = Files.exists(getValue()) && !Files.isDirectory(getValue());
			}
			return isLeaf;
		}

		/**
		 * Returning a collection of type ObservableList containing TreeItems, which represent all children of this TreeITem.
		 * 
		 * 
		 * @return an ObservableList<TreeItem<File>> containing TreeItems, which represent all children available in this TreeItem. If the handed TreeItem is a leaf, an empty list is returned.
		 */
		private ObservableList<TreeItem<Path>> buildChildren() {
			if (Files.isDirectory(getValue())) {
				try {

					return Files.list(getValue())
							.map(SimpleFileTreeItem::new)
							.collect(Collectors.toCollection(() -> FXCollections.observableArrayList()));

				} catch (IOException e) {
					e.printStackTrace();
					return FXCollections.emptyObservableList();
				}
			}

			return FXCollections.emptyObservableList();
		}
	}
}