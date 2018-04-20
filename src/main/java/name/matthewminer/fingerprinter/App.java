package name.matthewminer.fingerprinter;

import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;
import java.io.File;
import java.lang.Exception;
import java.lang.Math;
import java.lang.Runnable;
import java.lang.Thread;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingDeque;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Callback;
import name.matthewminer.fingerprinter.App.Progress;

/**
 * @author		Matthew Miner mminer237@gmail.com
 * @version		1.0
 * @since		1.0
 */
public class App extends Application {
	/** The threshold to consider two prints a "match" */
	private static final double THRESHOLD = 40;
	/** Whether the program was opened in a GUI window */
	private static boolean inWindow = false;
	/** The image of the probe print */
	private static ImageView probeImage;
	/**
	 * The image of the candidate print currently being
	 * looked at
	*/
	private static ImageView candidateImage;
	/**
	 * The two panes containing the pictures of the
	 * prints being viewed
	*/
	private static List<StackPane> imagePanes = new ArrayList<StackPane>();
	/** The checkbox of whether to cache images */
	private static CheckBox cacheCheck;
	/** The checkbox of whether to use TV mode */
	private static final CheckBox tvCheck = new CheckBox("TV Mode (Epilepsy Warning)");
	/** The checkbox of whether to use TV mode slowly */
	private static final CheckBox slowCheck = new CheckBox("Less Fast TV");
	/** The checkbox of whether to use color in TV mode */
	private static CheckBox colorCheck;
	/** The big progress bar */
	private static final ProgressBar progressBar = new ProgressBar(0F);
	/** The GUI box where all processing is logged */
	private static TextArea logBox;
	/** List of fingerprints that have been compared */
	private static final ObservableList<Fingerprint> resultData = FXCollections.observableArrayList();

	/**
	 * Initialize the program when JavaFX is not properly started.
	 * @param	args Command line arguments passed
	*/
	public static void main(String[] args) {
		App.launch(args);
	}

	/**
	 * Start the program.
	 * @param	primaryStage The primary stage from JavaFX
	*/
	@Override
	public void start(final Stage primaryStage) {
		/** Command line arguments passed */
		List<String> argList = getParameters().getRaw();
		/** Command line arguments passed */
		String[] args = new String[argList.size()];
		args = argList.toArray(args);

		if (args.length > 0) {
			if (args[0].equals("--default") || args[0].equals("-default") || args[0].equals("-d")) {
				run();
			}
			else if (args[0].equals("--help") || args[0].equals("-help") || args[0].equals("help") || args[0].equals("-h") || args[0].equals("?")) {
				System.out.println("Usage: java -jar fingerprinter.jar [probe [candidates-folder]] (--cache | --default | --help)");
				System.exit(0);
				return;
			}
			else
				run(args);
		}
		else {
			inWindow = true;
			primaryStage.setTitle("Fingerprinter");
			primaryStage.getIcons().addAll(
				new Image(App.class.getResourceAsStream("/images/icons/icon_16.png")),
				new Image(App.class.getResourceAsStream("/images/icons/icon_48.png")),
				new Image(App.class.getResourceAsStream("/images/icons/icon_256.png"))
			);

			/** The main layout grid */
			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(10, 10, 10, 10));
			RowConstraints rowConstraints = new RowConstraints();
			rowConstraints.setVgrow(Priority.ALWAYS);
			grid.getRowConstraints().addAll(new RowConstraints(), new RowConstraints(), rowConstraints);

			probeImage = new ImageView();
			probeImage.setFitHeight(300);
			probeImage.setFitWidth(300);
			probeImage.setPreserveRatio(true);
			StackPane probeImagePane = new StackPane();
			probeImagePane.setMinSize(340, 340);
			probeImagePane.setStyle("-fx-background-color:#AAAAAA");
			probeImagePane.getChildren().add(probeImage);
			grid.add(probeImagePane, 0, 0);
			imagePanes.add(probeImagePane);
			candidateImage = new ImageView();
			candidateImage.setFitHeight(300);
			candidateImage.setFitWidth(300);
			candidateImage.setPreserveRatio(true);
			StackPane candidateImagePane = new StackPane();
			candidateImagePane.setMinSize(340, 340);
			candidateImagePane.setStyle("-fx-background-color:#AAAAAA");
			candidateImagePane.getChildren().add(candidateImage);
			grid.add(candidateImagePane, 2, 0);
			imagePanes.add(candidateImagePane);

			/** The table that shows results */
			TableView resultTable = new TableView();
			resultTable.setRowFactory(tv -> {
				TableRow<Fingerprint> row = new TableRow<Fingerprint>() {
					@Override
					public void updateItem(Fingerprint item, boolean empty) {
						super.updateItem(item, empty);
						if (item == null || empty) {
							setStyle("");
						}
						else {
							setStyle("-fx-background-color: hsb(" + String.valueOf(Math.min(120, getItem().getScore() * 120 / THRESHOLD)) + ",41%,94%);");
						}
					};
				};
				return row;
			});
			
			/** The match score column in the table */
			TableColumn<Fingerprint, Double> scoreCol = new TableColumn("Match Score");
			scoreCol.setPrefWidth(90);
			scoreCol.setCellValueFactory(
				cellData->cellData.getValue().scoreProperty().asObject()
			);
			scoreCol.setSortType(TableColumn.SortType.DESCENDING);
			/** The name column in the table */
			TableColumn<Fingerprint, String> nameCol = new TableColumn("Name");
			nameCol.setPrefWidth(175);
			nameCol.setCellValueFactory(
				cellData->cellData.getValue().nameProperty()
			);
			/** List of fingerprints that have been compared and sorted */
			SortedList<Fingerprint> sortedResultData = new SortedList(resultData);
			sortedResultData.comparatorProperty().bind(resultTable.comparatorProperty());
			resultTable.setItems(sortedResultData);
			resultTable.getSortOrder().addAll(scoreCol);
			resultTable.getColumns().addAll(scoreCol, nameCol);
			resultTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Fingerprint>() {
				public void changed(ObservableValue<? extends Fingerprint> observable,
					Fingerprint oldValue,
					Fingerprint newValue
				) {
					if (newValue != null)
						Fingerprint.setAsImage(newValue, candidateImage);
				}
			});
			grid.add(resultTable, 3, 0, 1, 4);

			/** The HBox containing the elements to select a probe */
			HBox probeChooserPane = new HBox();
			probeChooserPane.setPrefWidth(300.0);
			grid.add(probeChooserPane, 0, 1);
			/** The TextField containing the path of the probe */
			final TextField probeBox = new TextField();
			probeBox.setPrefWidth(220.0);
			probeChooserPane.getChildren().add(probeBox);
			/** The Button to select a probe */
			Button probeButton = new Button();
			probeButton.setPrefWidth(120.0);
			probeButton.setText("Choose Probe");
			probeButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					FileChooser probeChooser = new FileChooser();
					probeChooser.setTitle("Select Probe Print Image");
					probeChooser.getExtensionFilters().addAll(
						new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
						new ExtensionFilter("All Files", "*.*")
					);
					if (
						!probeBox.getText().isEmpty() &&
						Paths.get(probeBox.getText()).getParent() != null
					)
						probeChooser.setInitialDirectory(Paths.get(probeBox.getText()).getParent().toFile());
					else
						probeChooser.setInitialDirectory(Paths.get(".").toFile());
					try {
						File selectedFile = probeChooser.showOpenDialog(primaryStage);
						if (selectedFile != null) {
							Path selectedPath = selectedFile.toPath();
							probeBox.setText(selectedPath.normalize().toString());
						}
					} catch(Exception e) {
						System.out.println(e.getMessage());
						e.printStackTrace();
					}
				}
			});
			probeChooserPane.getChildren().add(probeButton);

			/** The HBox containing the elements to select candidates */
			HBox candidatesChooserPane = new HBox();
			candidatesChooserPane.setPrefWidth(300.0);
			grid.add(candidatesChooserPane, 2, 1);
			/** The TextField containing the path of candidates */
			final TextField candidatesBox = new TextField();
			candidatesBox.setPrefWidth(220.0);
			candidatesChooserPane.getChildren().add(candidatesBox);
			/** The Button to select candidates */
			Button candidatesButton = new Button();
			candidatesButton.setPrefWidth(120.0);
			candidatesButton.setText("Choose Candidates");
			candidatesButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					DirectoryChooser candidatesChooser = new DirectoryChooser();
					candidatesChooser.setTitle("Select Candidate Prints Folder");
					if (
						!candidatesBox.getText().isEmpty() &&
						Files.isDirectory(Paths.get(candidatesBox.getText()))
					)
						candidatesChooser.setInitialDirectory(Paths.get(candidatesBox.getText()).toFile());
					else
						candidatesChooser.setInitialDirectory(Paths.get(".").toFile());
					try {
						File selectedFile = candidatesChooser.showDialog(primaryStage);
						if (selectedFile != null) {
							Path selectedPath = selectedFile.toPath();
							candidatesBox.setText(selectedPath.normalize().toString());
						}
					} catch(Exception e) {
						System.out.println(e.getMessage());
						e.printStackTrace();
					}
				}
			});
			candidatesChooserPane.getChildren().add(candidatesButton);

			/** Main button to run the program */
			Button runButton = new Button();
			runButton.setMinWidth(105.0);
			runButton.setText("Run Comparison");
			runButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					try {
						/** Arguments to run the comparison with */
						ArrayList<String> args = new ArrayList();
						args.add(probeBox.getText());
						args.add(candidatesBox.getText());
						run(args);
					}
					catch(Exception e) {
						System.out.println(e.getMessage());
					}
				}
			});
			grid.add(runButton, 1, 1);

			/** The HBox containing the program options */
			HBox options = new HBox();
			options.setSpacing(10);
			grid.add(options, 0, 2, 3, 1);
			cacheCheck = new CheckBox("Use Print Templates Cache");
			options.getChildren().add(cacheCheck);
			options.getChildren().add(tvCheck);
			options.getChildren().add(slowCheck);
			colorCheck = new CheckBox("Colorful Failure");
			options.getChildren().add(colorCheck);

			logBox = new TextArea();
			logBox.setPrefHeight(20000.0);
			logBox.setEditable(false);
			logBox.setWrapText(true);
			grid.add(logBox, 0, 3, 3, 1);

			progressBar.setPrefWidth(20000.0);
			grid.add(progressBar, 0, 4, 3, 1);

			/** Link to author's website */
			Hyperlink credit = new Hyperlink("Made by Matthew Miner");
			credit.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent t) {
					getHostServices().showDocument("https://matthewminer.name");
				}
			});
			credit.prefWidthProperty().bind(grid.widthProperty());
			credit.setStyle("-fx-alignment: CENTER_RIGHT");
			grid.add(credit, 3, 4);
			
			Scene primaryScene = new Scene(grid, 1105, 640);
			primaryStage.setScene(primaryScene);
			primaryStage.show();
		}
	}

	/**
	 * Run the comparison.
	 */
	private void run() {
		run(new ArrayList());
	}
	/**
	 * Run the comparison.
	 * @param	args Command line arguments passed
	 */
	private void run(String[] args) {
		run(Arrays.asList(args));
	}
	/**
	 * Run the comparison.
	 * @param	args Command line arguments passed
	 */
	private void run(List<String> a) {
		/** Stream of images named "probe" if none is specified */
		DirectoryStream<Path> probesStream;
		/** Path of the probe image file */
		Path probePath;
		/** Command-line arguments passed */
		List<String> args = new ArrayList<>(a);
		/** Command-line options passed */
		List<String> options = new ArrayList();
		for (Iterator<String> iter = args.listIterator(); iter.hasNext(); ) {
			String arg = iter.next();
			if (arg.startsWith("-")) {
				options.add(arg);
				iter.remove();
			}
		}
		try {
			progressBar.setProgress(-1F);
			printMessage("Looking for probe...");
			if (args.size() > 0) {
				printMessage(String.format("Looking for \"%s\" as probe...", args.get(0)));
				probePath = Paths.get(args.get(0));
				if (Files.exists(probePath) && !Files.isDirectory(probePath))
					printMessage(String.format("Using \"%s\" as probe...", args.get(0)));
				else
					throw new Exception();
			}
			else {
				printMessage("Looking for \"probe.(png|jpg|jpeg|gif)\" as probe...");
				probesStream = Files.newDirectoryStream(Paths.get("."), "probe.{png,jpg,jpeg,gif}");
				probePath = probesStream.iterator().next();
				if (Files.exists(probePath)) {
					printMessage(String.format("Using \"%s\" as probe...", probePath.getFileName().toString()));
				}
				else
					throw new Exception();
			}
		}
		catch(Exception e) {
			printMessage("No probe found.");
			if (!inWindow)
				System.exit(0);
			return;
		}

		/** Path of the candidates folder */
		Path candidatesPath;
		try {
			printMessage("Looking for candidates folder...");
			if (args.size() > 1) {
				candidatesPath = Paths.get(args.get(1));
				if (Files.exists(candidatesPath))
					printMessage(String.format("Using \"%s\" as candidates folder...", args.get(1)));
				else
					throw new Exception();
			}
			else {
				candidatesPath = Paths.get("candidates");
				if (Files.exists(candidatesPath))
					printMessage("Using \"candidates\" as candidates folder...");
				else
					throw new Exception();
			}
		}
		catch(Exception e) {
			if (!inWindow)
				System.exit(0);
			printMessage("No candidates folder found.");
			return;
		}

		try {
			printMessage("Loading probe...");
			/** JSON data of the data in the probe image */
			String json = null;
			/** Fingerprint to try to load a cached fingerprint into */
			Fingerprint testProbe;
			/** Whether the program was told to cache images on the command-line */
			boolean useCache = false;
			if (inWindow && cacheCheck.isSelected())
				useCache = true;
			else if (options.size() > 0)
				if (args.contains("-c") || args.contains("--cache"))
					useCache = true;
			if (useCache) {
				Path cachePath = Paths.get(probePath.getParent().toString() + "/cache/" + probePath.getFileName().toString() + ".json");
				if (Files.exists(cachePath)) {
					try {
						json = String.join("", Files.readAllLines(
							cachePath,
							Charset.forName("UTF-8")
						));
						testProbe = new Fingerprint(probePath, json);
						printMessage("Loaded probe template cache...");
					} catch(Exception e) {
						printMessage(String.format("Failed to load template cache for \"%s\".", probePath));
					}
				}
			}
			/** Fingerprint to search for */
			final Fingerprint probe = new Fingerprint(probePath, json);
			printMessage("Loaded probe...");
			Fingerprint.setAsImage(probe, probeImage);
			
			printMessage("Loading candidates...");
			DirectoryStream<Path> candidatesStream = Files.newDirectoryStream(candidatesPath, "*.{png,jpg,jpeg,gif}");
			final Progress loadCandidatesProgress = new Progress(candidatesPath.toFile().list().length);
			Task<List<Fingerprint>> loadCandidates = new Task<List<Fingerprint>>() {
				@Override
				protected List<Fingerprint> call() throws Exception {
					/** List of fingerprints to compare the probe print against */
					List<Fingerprint> candidates = new ArrayList<Fingerprint>();
					/** Whether any cached prints were loaded successfully */
					boolean loadedOne = false;
					/** Whether any cached prints failed to load */
					boolean failedOne = false;
					for (Path candidatePath : candidatesStream) {
						if (inWindow && cacheCheck.isSelected()) {
							Path cachePath = Paths.get(candidatesPath.toString() + "/cache/" + candidatePath.getFileName().toString() + ".json");
							if (Files.exists(cachePath)) {
								try {
									/** JSON data of the data in the probe image */
									String json = String.join("", Files.readAllLines(
										cachePath,
										Charset.forName("UTF-8")
									));
									candidates.add(new Fingerprint(candidatePath, json));
									loadedOne = true;
								}
								catch(Exception e) {
									failedOne = true;
									printMessage(String.format("Failed to load template cache for \"%s\".", candidatePath));
								}
							}
							else {
								failedOne = true;
								candidates.add(new Fingerprint(candidatePath));
							}
							loadCandidatesProgress.increment();
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									progressBar.setProgress(loadCandidatesProgress.getProgress());
								}
							});
						}
						else {
							candidates.add(new Fingerprint(candidatePath));
						}
					}
					if (loadedOne)
						if (failedOne)
							printMessage("Loaded partial print template cache...");
						else {
							printMessage("Loaded all candidates from print template cache...");
						}
					return candidates;
				}
			};
			loadCandidates.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent t) {
					printMessage("Comparing fingerprints...");
					/** List of successfully loaded candidates */
					List<Fingerprint> candidates = loadCandidates.getValue();
					/** Progress of the comparison's run */
					Progress runProgress = new Progress(candidates.size());
					/** Comparison model to run the comparison on */
					final Model model = new Model(probe.template, candidates, runProgress);
					/** Timer to use in animating the images */
					final TVAnimation timer = new TVAnimation(model, runProgress);
					timer.start();
					model.completedProperty().addListener(new ChangeListener<Boolean>() {
						@Override
						public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									if (newValue) {
										timer.stop();
										Fingerprint match = model.getMatch().getScore() >= THRESHOLD ? model.getMatch() : null;
										if (match != null) {
											printMessage("Match found!");
											printMessage(String.format("Match Name: %s", match.getName()));
											printMessage(String.format("Match Score: %f", match.getScore()));
											Fingerprint.setAsImage(match, candidateImage);
										}
										else {
											printMessage(String.format("No match found that exceeded threshold of %.2f.", THRESHOLD));
										}
										if (inWindow)
											Fingerprint.listResults(candidates, resultData);
										else {
											System.out.print("Would you like to see all results? (Y/N): ");
											if (Character.toLowerCase(new Scanner(System.in).nextLine().charAt(0)) == 'y')
												Fingerprint.listResults(candidates);
											System.exit(0);
										}
									}
								}
							});
						}
					});
					model.start();
				}
			});
			new Thread(loadCandidates).start();
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			if (!inWindow)
				System.exit(0);
			return;
		}
	}

	/** Stores and computes the percentage of prints processed */
	public class Progress {
		/** Total number of items to process */
		private int total;
		/** Number of items already processed */
		private int done;

		/**
		 * Constructor for Progress
		 * @param total Total number of items to process
		 */
		public Progress(int total) {
			this(total, 0);
		}
		/**
		 * Constructor for Progress
		 * @param total Total number of items to process
		 * @param done Number of items already processed
		 */
		public Progress(int total, int done) {
			this.total = total;
			this.done = done;
		}

		/**
		 * Get the total number of items
		 * @return	The total number of items
		 */
		public int getTotal() {
			return total;
		}
		/**
		 * Get the number of items processed
		 * @return	The number of items processed
		 */
		public int getDone() {
			return done;
		}
		/**
		 * Get the percentage of items processed
		 * @return	The percentage of items processed
		 */
		public double getProgress() {
			return (double)done / (double)total;
		}
		/**
		 * Increment the number of items processed
		 * @return	The number of items processed
		 */
		public int increment() {
			return ++done;
		}
	}

	/** Time the TV mode processing */
	private class TVAnimation extends AnimationTimer {
		/**
		 * The processing model to get the results from
		 * and allow to advance
		 */
		final private Model model;
		/** The Progress object to update */
		final private Progress progress;
		/** Default number of frames to wait before switching images */
		final private int framesToSkipStart = 5;
		/** Counter of frames to wait before switching images */
		private int framesToSkip = 0;
		/**
		 * Constructor for TVAnimation
		 * @param model The processing model to get the results from
		 * and allow to advance
		 * @param progress The Progress object to update
		 */
		TVAnimation(Model model, Progress progress) {
			this.model = model;
			this.progress = progress;
		}

		@Override
		public void handle(long now) {
			if (framesToSkip == 0 || !slowCheck.isSelected()) {
				framesToSkip = framesToSkipStart;
				Fingerprint result = model.pollResult();
				double p = progress.getProgress();
				progressBar.setProgress(p);
				if (result != null)
					Fingerprint.setAsImage(result, candidateImage);
			}
			else
				framesToSkip--;
		}
	}

	/**
	 * Processing model that does the comparison
	 * in a separate thread
	 */
	private class Model extends Thread {
		/** Whether the comparison has finished running */
		private BooleanProperty completedProperty = new SimpleBooleanProperty(false);
		/** The fingerprint to use as the probe */
		private ObjectProperty<FingerprintTemplate> probeProperty = new SimpleObjectProperty<>();
		/** The best-matched fingerprint */
		private ObjectProperty<Fingerprint> matchProperty = new SimpleObjectProperty<>();
		/** The list of candidate prints to compare against */
		private ListProperty<Fingerprint> candidatesProperty = new SimpleListProperty<>();
		/**
		 * Double-ended queue to hold the next candidate and
		 * limit the processing speed if TV mode is enabled
		 */
		private LinkedBlockingDeque<Fingerprint> deque = new LinkedBlockingDeque(5);
		/** The Progress object to store results in */
		final private Progress progress;

		/**
		 * Constructor for Model
		 * @param probe The fingerprint to use as the probe
		 * @param candidates The list of candidate prints to compare against
		 * @param progress The Progress object to store results in
		 */
		public Model(FingerprintTemplate probe, final List<Fingerprint> candidates, Progress progress) {
			probeProperty.set(probe);
			candidatesProperty.set(FXCollections.observableArrayList(candidates));
			this.progress = progress;
			setDaemon(true);
		}

		/**
		 * Get the BooleanProperty of whether the comparison has finished
		 * @return The BooleanProperty of whether the comparison has finished
		 */
		public BooleanProperty completedProperty() {
			return completedProperty;
		}

		/**
		 * Get the best-matched fingerprint
		 * @return The best-matched fingerprint
		 */
		public Fingerprint getMatch() {
			return matchProperty.get();
		}

		@Override
		public void run() {
			try {
				/** The FingerprintMatcher to use for the comparison */
				final FingerprintMatcher matcher = new FingerprintMatcher()
					.index(probeProperty.get());
				/** The best-matched fingerprint */
				Fingerprint match = null;
				/** The highest score found */
				double high = 0;

				for (Fingerprint candidate : candidatesProperty) {
					/** The score of the current matchup */
					double score = matcher.match(candidate.template);
					candidate.setScore(score);
					if (score > high) {
						high = score;
						match = candidate;
					}
					progress.increment();
					if (tvCheck.isSelected())
						try{
							deque.put(candidate);
						} catch(Exception e) {
							System.out.println(e.getMessage());
							e.printStackTrace();
						}
				}
				matchProperty.set(match);
			} catch(Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			completedProperty.set(true);
			progress.increment();
			if (tvCheck.isSelected())
				try{
					deque.put(getMatch());
				} catch(Exception e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			else
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						progressBar.setProgress(progress.getProgress());
					}
				});
		}

		/**
		 * Take one fingerprint from the result deque
		 * @return The oldest tested fingerprint in the deque
		 */
		Fingerprint pollResult() {
			return deque.poll();
		}
	}

	/**
	 * Print a message to the console and log box
	 * @param message The message to print out
	 */
	private static void printMessage(String message) {
		System.out.println(message);
		try {
			logBox.appendText(message + "\n");
		} catch(Exception e) {}
	}

	/** A fingerprint object */
	private static class Fingerprint {
		/** The name of the fingerprint */
		private final StringProperty name;
		/** The filepath of the fingerprint image */
		private final Path path;
		/** The score of the fingerprint versus the probe */
		private DoubleProperty score = new SimpleDoubleProperty();
		/** The template for the fingerprint */
		FingerprintTemplate template;

		/**
		 * Constructor for Fingerprint without a JSON cache
		 * @param path The filepath of the fingerprint
		 */
		public Fingerprint(Path path) {
			this(path, null);
		}
		public Fingerprint(Path path, String jsonTemplate) {
			name = new SimpleStringProperty(path.getFileName().toString());
			this.path = path;
			if (jsonTemplate != null) {
				try {
					template = new FingerprintTemplate().deserialize(jsonTemplate);
				}
				catch(Exception e) {
					jsonTemplate = null;
					printMessage(String.format("Had a problem reading the cached version of %s. Trying actual image...", getName()));
				}
			}
			else {
				try {
					/** Binary data of fingerprint image */
					byte[] image = Files.readAllBytes(path);
					template = new FingerprintTemplate()
						.dpi(500)
						.create(image);
				}
				catch(Exception e) {
					printMessage(String.format("Failed to create template for \"%s\".", getName()));
					System.out.println(e.getMessage());
					return;
				}
			}
			if (inWindow && cacheCheck.isSelected()) {
				try {
					/** String of the cache directory path */
					String cache = path.getParent().toString() + "/cache/";
					Files.createDirectories(Paths.get(cache));
					Files.write(
						Paths.get(cache + getName() + ".json"),
						Arrays.asList(template.serialize()),
						Charset.forName("UTF-8")
					);
				}
				catch(Exception e) {
					printMessage(String.format("Failed to save template cache for \"%s\".", getName()));
				}
			}
		}

		/**
		 * Get the name of the fingerprint
		 * @return The name of the fingerprint
		 */
		public String getName() {
			return name.get();
		}

		/**
		 * Get the match score of the fingerprint
		 * @return The match score of the fingerprint
		 */
		public double getScore() {
			return score.get();
		}

		/**
		 * Get the name property of the fingerprint
		 * @return The name property of the fingerprint
		 */
		public StringProperty nameProperty() {
			return name;
		}

		/**
		 * Get the match score property of the fingerprint
		 * @return The match score property of the fingerprint
		 */
		public DoubleProperty scoreProperty() {
			return score;
		}

		/**
		 * Set the match score of the fingerprint
		 * @param s	Match score of the fingerprint
		 */
		public void setScore(double s) {
			score.setValue(s);
		}
		
		/**
		 * Print a list of fingerprint match results
		 * @param candidates The list of fingerprint match results to list
		 */
		public static void listResults(List<Fingerprint> candidates) {
			Collections.sort(candidates, Comparator.comparingDouble(Fingerprint::getScore));
			Collections.reverse(candidates);
			System.out.println("Match Score\tFingerprint Name\n");
			for (Fingerprint candidate : candidates) {
				System.out.format("%f\t%s\n", candidate.getScore(), candidate.getName());
			}
		}
		
		/**
		 * Print a list of fingerprint match results and put them in the result data list
		 * @param candidates The list of fingerprint match results to list
		 * @param resultData The ObservableList of result data to fill
		 */
		public static void listResults(List<Fingerprint> candidates, ObservableList resultData) {
			listResults(candidates);
			resultData.clear();
			resultData.addAll(candidates);
		}

		/**
		 * Set a fingerprint as a displayed image
		 * @param print	The fingerprint to display
		 * @param i    	The ImageView to set the fingerprint at
		 */
		public static void setAsImage(Fingerprint print, ImageView i) {
			setImage(print.path.toString(), print.getScore(), i);
		}
	}

	/**
	 * Set a fingerprint as a displayed image
	 * @param path	The filepath of the image to display
	 * @param score	The match score of the fingerprint to display
	 * @param i    	The ImageView to set the fingerprint at
	 */
	public static void setImage(String path, double score, ImageView i) {
		if (inWindow) {
			i.setImage(new Image("file:" + path));
			if (colorCheck.isSelected())
				if (score == 0)
					imagePanes.forEach(pane -> pane.setStyle("-fx-background-color:#AAAAAA"));
				else
					imagePanes.forEach(pane -> pane.setStyle("-fx-background-color: hsb(" + String.valueOf(Math.min(120, score * 120 / THRESHOLD)) + ",41%,94%);"));
			else
				if (score > THRESHOLD)
					imagePanes.forEach(pane -> pane.setStyle("-fx-background-color:#8EF290"));
				else
					imagePanes.forEach(pane -> pane.setStyle("-fx-background-color:#AAAAAA"));
		}
	}
}