package valentin.lourteau.application;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

@ApplicationScoped
@Singleton
@Startup
public class ManagerSingleton {

	private static final Logger logger = Logger.getLogger(ManagerSingleton.class.getSimpleName());

	public static final String NOT_PROCESSED_REGEX = "\\d*_\\d{1,2}.(jpg|png|PNG|JPEG|jpeg)(?!\\.processed)";
	public static final String PROCESSED_SUFFIX = ".processed.";

	private String pathToPicturesFolder;
	private List<String> desiredFormats;

	private Long fileCount = 0L;
	private List<String> fileNamesProcessed = new ArrayList<>();

	@Schedule(minute = "*", hour = "*")
	public synchronized void scheduledTaskToMinifyPictures() throws IOException {
		loadProperties();
		if (fileCount.equals(countNumberOfFilesInFolder())) {
			logger.log(Level.INFO, "Aucun fichier à convertir depuis la dernière fois");
			return;
		}
		fileCount = countNumberOfFilesInFolder();
		List<File> toProcess = getAllFilesNotProcessed();
		logger.log(Level.INFO, "Nombre d'images à process : " + toProcess.size());
		if (toProcess.isEmpty()) {
			logger.log(Level.WARNING, "Aucun fichier à traiter");
			return;
		}
		newBatch(toProcess);
		logger.log(Level.INFO, "Nombre d'images traitées : " + fileNamesProcessed.size());
	}

	private void loadProperties() {
		pathToPicturesFolder = PropertiesReader.getPropertie("pathToPicturesFolder");
		logger.log(Level.INFO, pathToPicturesFolder);
		desiredFormats = Arrays.asList(PropertiesReader.getPropertie("desiredFormats").split(";"));
		logger.log(Level.INFO, desiredFormats.toString());
	}

	private void newBatch(List<File> toProcess) {
		toProcess.forEach(file -> processFile(file));
	}

	/**
	 * Méthode pour générer les fichiers images réduits. Pour chaque format, on
	 * va dupliquer l'image de base et ajouter, avant l'extension du fichier, le
	 * format de l'image, puis rajouter l'extension initiale. Chaque nouvelle
	 * image va ensuite être appelée dans une méthode gérant l'image pour être
	 * traitée.
	 * 
	 * @param file
	 * @return
	 */
	private void processFile(File file) {
		desiredFormats.forEach(format -> {
			FileOutputStream os = null;
			File duplicate = new File(pathToPicturesFolder + file.getName().split("\\.")[0] + "_" + format
					+ PROCESSED_SUFFIX + file.getName().split("\\.")[1]);
			try {
				os = new FileOutputStream(duplicate);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			try {
				Files.copy(file.toPath(), os);
				os.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			try {
				processImage(duplicate, format);
				// On ajoute le nom du fichier à la liste des fichiers
				fileNamesProcessed.add(file.getName());

			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		});
	}

	private void processImage(File file, String format) throws IOException {
		BufferedImage resizeMe = ImageIO.read(file);
		Dimension newMaxSize = getDimension(format);
		BufferedImage resizedImg = Scalr.resize(resizeMe, Method.QUALITY,
				resizeMe.getWidth() > newMaxSize.width ? newMaxSize.width : resizeMe.getWidth(),
				resizeMe.getHeight() > newMaxSize.height ? newMaxSize.height : resizeMe.getHeight());
		ImageIO.write(resizedImg, file.getName().split("\\.")[file.getName().split("\\.").length - 1], file);
	}

	private Dimension getDimension(String format) {
		return new Dimension(Integer.valueOf(format.split("_")[0]), Integer.valueOf(format.split("_")[1]));
	}

	private Long countNumberOfFilesInFolder() throws IOException {
		return Files.walk(Paths.get(pathToPicturesFolder)).filter(Files::isRegularFile).count();
	}

	public List<File> getAllFilesNotProcessed() throws IOException {
		List<File> files = getAllFiles();
		return files.stream().filter(file -> file.getName().matches(NOT_PROCESSED_REGEX))
				.filter(file -> !fileNamesProcessed.contains(file.getName())).collect(Collectors.toList());
	}

	public List<File> getAllFiles() throws IOException {
		return Files.walk(Paths.get(pathToPicturesFolder)).filter(Files::isRegularFile).map(Path::toFile)
				.collect(Collectors.toList());
	}
}
