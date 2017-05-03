package valentin.lourteau.application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class ManagerSingleton {

	private static final Logger logger = Logger.getLogger(ManagerSingleton.class.getSimpleName());

	private String pathToPicturesFolder;
	private Long fileCount = 0L;
	private static final String NOT_PROCESSED_REGEX = "\\d*_\\d{2,2}.(jpg|png|PNG|JPEG|jpeg)(?!\\.processed)";
	private List<String> desiredFormats;
	private static final ResourceBundle bundle = ResourceBundle.getBundle("configuration");

	@PostConstruct
	public void start() {
	}

	@Schedule(hour = "2", persistent = false)
	private void scheduledTaskToMinifyPictures() throws IOException {

		// On charge les propriétés
		pathToPicturesFolder = bundle.getString("pathToPicturesFolder");
		desiredFormats = Arrays.asList(bundle.getString("desiredFormats").split(";"));

		if (fileCount.equals(countNumberOfFilesInFolder())) {
			logger.log(Level.INFO, "Aucun fichier à convertir depuis la dernière fois");
			return;
		}
		List<File> toProcess = getAllFilesNotProcessed();
		logger.log(Level.INFO, "Nombre d'images à process : " + toProcess.size());
		newBatch(toProcess);
	}

	private synchronized void newBatch(List<File> toProcess) {
		toProcess.forEach(file -> processFile(file));
	}

	/**
	 * Méthode pour générer les fichiers images réduits. Pour chaque format, on
	 * va dupliquer l'image de base et ajouter, avant l'extension du fichier, le
	 * format de l'image, puis rajouter l'extension initiale. Chaque nouvelle
	 * image va ensuite être appelée dans une méthode gérant les graphiques pour
	 * être traité.
	 * 
	 * @param file
	 * @return
	 */
	private void processFile(File file) {
		List<File> newFiles = new ArrayList<File>();
		desiredFormats.forEach(format -> {

			new File(pathToPicturesFolder + file.getName().split("\\.")[0] + format + file.getName().split("\\.")[1]);
		});
	}

	private Long countNumberOfFilesInFolder() throws IOException {
		return Files.walk(Paths.get(pathToPicturesFolder)).filter(Files::isRegularFile).count();
	}

	private List<File> getAllFilesNotProcessed() throws IOException {
		List<File> files = getAllFiles();
		return files.stream().filter(file -> file.getName().matches(NOT_PROCESSED_REGEX)).collect(Collectors.toList());
	}

	public List<File> getAllFiles() throws IOException {
		return Files.walk(Paths.get(pathToPicturesFolder)).filter(Files::isRegularFile).map(Path::toFile)
				.collect(Collectors.toList());
	}
}
