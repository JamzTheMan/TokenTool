/*
 * This software Copyright by the RPTools.net development team, and licensed under the Affero GPL Version 3 or, at your option, any later version.
 *
 * TokenTool Source Code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License * along with this source Code. If not, please visit <http://www.gnu.org/licenses/> and specifically the Affero license text
 * at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.tokentool.model;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.layout.TilePane;
import net.rptools.tokentool.controller.TokenTool_Controller;
import net.rptools.tokentool.util.ExtractImagesFromPDF;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfModel {
	private static final Logger log = LogManager.getLogger(PdfModel.class);

	private PDDocument document;
	private PDFRenderer renderer;
	private ExtractImagesFromPDF imageExtractor;

	private int DPI = Toolkit.getDefaultToolkit().getScreenResolution();

	public PdfModel(File pdfFile, TokenTool_Controller tokenTool_Controller) throws IOException {
		try {
			document = PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly());
			renderer = new PDFRenderer(document);
			imageExtractor = new ExtractImagesFromPDF(document, tokenTool_Controller);
		} catch (IOException ex) {
			throw new UncheckedIOException("PDDocument throws IOException file=" + pdfFile.getAbsolutePath(), ex);
		}

		log.info("Current DPI returned by OS: " + DPI);
	}

	public int numPages() {
		return document.getPages().getCount();
	}

	public Image getImage(int pageNumber) {
		BufferedImage pageImage;
		try {
			pageImage = renderer.renderImageWithDPI(pageNumber, DPI);
		} catch (IOException ex) {
			throw new UncheckedIOException("PDFRenderer throws IOException", ex);
		}
		return SwingFXUtils.toFXImage(pageImage, null);
	}

	public void close() {
		try {
			document.close();
		} catch (IOException e) {
			log.error("Error closing PDF Document.", e);
		}
	}

	public void extractImages(TilePane tilePane, int currentPageIndex) {
		try {
			imageExtractor.addImages(tilePane, currentPageIndex);
		} catch (IOException e) {
			log.error("Error extracting images from PDF...", e);
		}
	}
}
