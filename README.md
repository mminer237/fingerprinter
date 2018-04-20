# fingerprinter
Fingerprinter is a Java program that compares fingerprint images and finds matches therein.

# Usage
* Install [Java](https://java.com/).
* Run the program. (Double-click to run the GUI.)
	* Run `java -jar fingerprinter.jar [probe [candidates-folder]]` to run it on the command-line.
* Select a probe image of a fingerprint to look for.
* Select a folder containing images of fingerprints to look for the probe amongst.
* Run the comparison.
* The greener the matches, the more likely it is that the prints are the same. It counts them as a match if the score is above 40.

# Options
* **Use Print Templates Cache** – Makes/reads a file with the JSON generated from the file
	* `-c` or `--cache` to use from the command-line
* **TV Mode** – Flash the images while checking—just like on TV!
	* Caps at one image per frame, so if you just want the results, don't use this.
	* Please don't use if it may harm you
* **Less Fast TV** – Slows TV Mode to flashing an image to once every 5 frames
	* Might be worse if you have epilepsy
* **Colorful Failure** – Always shows the color border for the candidate print, even when it's not a match. Otherwise, it will just show green when it matches and gray when it doesn't.

# Attributions
* Uses the great [SourceAFIS](https://github.com/robertvazan/sourceafis-java) engine to actually do the meat of the work of recognizing and comparing fingerprints.
	* SourceAFIS is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). Refer either to that link or `ThirdPartyNotices.txt` for the text thereof.