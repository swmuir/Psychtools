package gov.vha.ptools.scripts;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	public static void main(String[] args) throws ParseException {

		Options options = new Options();
		options.addOption("fhir", true, "FHIR Endpoint");
		options.addOption(
			"transaction", true,
			"Options are UploadTherapies UploadManualizedStrategies ClearActivityDefinitions ClearPlanDefinitions");

		CommandLineParser parser = new DefaultParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			if (line.hasOption("fhir") && line.hasOption("transaction")) {

				switch (line.getOptionValue("transaction")) {
					case "UploadTherapies":
						PTools.uploadTherapies(line.getOptionValue("fhir"));
						break;

					case "ClearActivityDefinitions":
						PTools.clearActivityDefinitions(line.getOptionValue("fhir"));
						break;

					case "ClearPlanDefinitions":
						PTools.clearPlanDefinitions(line.getOptionValue("fhir"));
						break;

					case "uploadManualizedStrategies":
						PTools.uploadManualizedStrategies(line.getOptionValue("fhir"));
						break;

					default:

						HelpFormatter formatter = new HelpFormatter();
						formatter.printHelp("PTools", options);
				}

			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("PTools", options);
			}
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		} catch (IOException exp) {
			System.err.println("Upload failed.  Reason: " + exp.getMessage());
		}

	}

}
