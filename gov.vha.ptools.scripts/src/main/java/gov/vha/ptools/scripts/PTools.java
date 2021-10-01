package gov.vha.ptools.scripts;

/*******************************************************************************
 * Copyright (c) 2021 seanmuir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     seanmuir - initial API and implementation
 *
 *******************************************************************************/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.ActionSelectionBehavior;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.api.IBaseBundle;
//import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.util.BundleUtil;

/**
 * @author seanmuir
 *
 */
class PTools {

	public static void uploadTherapies(String fhirEndPoint) throws IOException {

		String XSL = "src/main/resources/Therapies.xlsx";

		FhirContext ctx = FhirContext.forR4();

		ctx.getRestfulClientFactory().setServerValidationModeEnum(ServerValidationModeEnum.NEVER);

		IGenericClient client = ctx.newRestfulGenericClient(fhirEndPoint);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		FileInputStream file = new FileInputStream(new File(XSL));

		// Create Workbook instance holding reference to .xlsx file
		XSSFWorkbook workbook = new XSSFWorkbook(file);

		HashSet<String> ads = new HashSet<String>();

		int totalSheets = workbook.getNumberOfSheets();

		for (int currentSheet = 1; currentSheet < totalSheets; currentSheet++) {
			XSSFSheet sheet = workbook.getSheetAt(currentSheet);
			Iterator<Row> rowIterator = sheet.iterator();
			boolean first = true;
			PlanDefinition planDefinition = null;
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				String resourceCode = "";
				// Iterator<Cell> cellIterator = row.cellIterator();
				// while (cellIterator.hasNext()) {
				Cell cell = row.getCell(0);

				if (cell == null) {
					continue;
				}

				if (ads.contains(cell.getStringCellValue().trim())) {
					continue;
				} else {
					ads.add(cell.getStringCellValue().trim());
				}

				if (first) {
					planDefinition = new PlanDefinition();
					planDefinition.setId(IdType.newRandomUuid());
					planDefinition.setName(sheet.getSheetName());
					planDefinition.setDescription(cell.getStringCellValue());
					planDefinition.setTitle(sheet.getSheetName());
					planDefinition.setStatus(PublicationStatus.ACTIVE);
					CodeableConcept cc = new CodeableConcept();
					cc.addCoding().setCode("clinical-protocol");
					planDefinition.setType(cc);

					planDefinition.setUsage(cell.getStringCellValue());

					UUID uuid = UUID.randomUUID();

					if (row.getCell(1) != null) {
						resourceCode = row.getCell(1).getStringCellValue();
					} else {
						resourceCode = uuid.toString();
					}
					planDefinition.getIdentifierFirstRep().setId(resourceCode);

					Coding contextcode = new Coding();
					contextcode.setCode(uuid.toString());
					contextcode.setDisplay(cell.getStringCellValue());
					contextcode.setSystem("SOLAR");

					CodeableConcept contextcc = new CodeableConcept();
					cc.addCoding().setCode("focus");

					planDefinition.getUseContextFirstRep().setCode(contextcode).setValue(contextcc);

					planDefinition.setUsage(cell.getStringCellValue());

					bundle.addEntry().setResource(planDefinition).getRequest().setMethod(HTTPVerb.POST);
				} else {

					if (!StringUtils.isEmpty(cell.getStringCellValue().trim())) {
						Bundle abundle = new Bundle();
						abundle.setType(BundleType.TRANSACTION);

						ActivityDefinition activityDefinition = new ActivityDefinition();
						activityDefinition.setTitle(cell.getStringCellValue().trim());
						activityDefinition.setDescription(cell.getStringCellValue().trim());
						activityDefinition.setId(IdType.newRandomUuid());
						activityDefinition.setName(cell.getStringCellValue());
						CodeableConcept cc = new CodeableConcept();
						cc.setText(cell.getStringCellValue().trim());
						Coding coding = new Coding();
						coding.setDisplay(cell.getStringCellValue().trim());
						UUID uuid = UUID.randomUUID();
						if (row.getCell(1) != null) {
							resourceCode = row.getCell(1).getStringCellValue();
						} else {
							resourceCode = uuid.toString();
						}
						coding.setCode(resourceCode);
						coding.setSystem("SOLAR");
						cc.getCoding().add(coding);

						activityDefinition.setCode(cc);

						abundle.addEntry().setResource(activityDefinition).getRequest().setMethod(HTTPVerb.POST);
						Bundle result = client.transaction().withBundle(abundle).execute();
						// System.out.println(
						// result.getEntry().get(0).getResponse().getLocation().replace("/_history/1", ""));

						String url = result.getEntry().get(0).getResponse().getLocation().replace("/_history/1", "");

						CanonicalType canonicalType = new CanonicalType();
						canonicalType.setValue(url);
						planDefinition.addAction().setDescription(cell.getStringCellValue()).setDefinition(
							canonicalType).setSelectionBehavior(ActionSelectionBehavior.ALL);

					}
				}
				first = false;
				// }

				if (row.getCell(1) == null) {
					row.createCell(1);
					row.getCell(1).setCellValue(resourceCode);
				}
				// Cell idCell = row.createCell(1);

			}

		}

		file.close();

		FileOutputStream out = new FileOutputStream(XSL);
		workbook.write(out);
		out.close();

		client.transaction().withBundle(bundle).execute();

	}

	void readXLS2() throws IOException {

		FhirContext ctx = FhirContext.forR4();

		Bundle bundle = new Bundle();

		// Enumeration<BundleType> BundleType = new Enumeration<BundleType>();

		// bundle
		bundle.setType(BundleType.TRANSACTION);

		FileInputStream file = new FileInputStream(new File("src/test/resources/MentalHealthExams.xlsx"));

		// Create Workbook instance holding reference to .xlsx file
		XSSFWorkbook workbook = new XSSFWorkbook(file);

		new HashSet<String>();

		new HashMap<String, String>();

		int totalSheets = workbook.getNumberOfSheets();

		ctx.getRestfulClientFactory().setServerValidationModeEnum(ServerValidationModeEnum.NEVER);

		// ctx.getRestfulClientFactory().
		IGenericClient client = ctx.newRestfulGenericClient("https://api.logicahealth.org/PTools4/open");

		Encounter encounter = new Encounter();
		Narrative n = new Narrative();
		n.setDivAsString("Mental Health Exam Test Data");
		;
		encounter.setText(n);
		encounter.setSubject(new Reference("Patient/cf-1626969857942"));

		Bundle ebundle = new Bundle();

		// Enumeration<BundleType> BundleType = new Enumeration<BundleType>();

		// bundle
		ebundle.setType(BundleType.TRANSACTION);
		ebundle.addEntry().setResource(encounter).getRequest().setMethod(HTTPVerb.POST);

		Bundle aresult = client.transaction().withBundle(ebundle).execute();

		String encounterid = aresult.getEntry().get(0).getResponse().getLocation().replace("/_history/1", "");

		for (int currentSheet = 0; currentSheet < totalSheets; currentSheet++) {
			XSSFSheet sheet = workbook.getSheetAt(currentSheet);

			Iterator<Row> rowIterator = sheet.iterator();

			int rowctr = 0;
			while (rowIterator.hasNext()) {

				Row row = rowIterator.next();

				if (rowctr > 2) {

					if (row.getCell(1) != null) {
						Observation observation = new Observation();

						observation.setStatus(ObservationStatus.FINAL);
						CodeableConcept category = new CodeableConcept();
						category.setText("exam");

						observation.getCategory().add(category);

						observation.setSubject(new Reference("Patient/cf-1626969857942"));
						observation.setEncounter(new Reference(encounterid));
						// observation.setSubject(new Reference("Patient/smart-1134281"));

						// observation.setco

						CodeableConcept cc = new CodeableConcept();
						System.out.println(row.getCell(1));
						// cc.setText(row.getCell(1).getStringCellValue());
						cc.setText(row.getCell(1).getStringCellValue());
						Coding coding = new Coding();
						coding.setDisplay(row.getCell(1).getStringCellValue());
						coding.setCode(row.getCell(2).getStringCellValue());
						coding.setSystem(row.getCell(3).getStringCellValue());
						cc.getCoding().add(coding);
						observation.setCode(cc);

						CodeableConcept vv = new CodeableConcept();
						vv.setText(row.getCell(4).toString());

						Coding vcoding = new Coding();
						System.out.println(row.getCell(4).getStringCellValue());
						vcoding.setDisplay(row.getCell(4).getStringCellValue());
						vcoding.setCode(row.getCell(5).getStringCellValue());
						vcoding.setSystem(row.getCell(6).getStringCellValue());
						vv.getCoding().add(vcoding);

						observation.setValue(vv);
						bundle.addEntry().setResource(observation).getRequest().setMethod(HTTPVerb.POST);

					}

					// Iterator<Cell> cellIterator = row.cellIterator();
					// while (cellIterator.hasNext()) {
					// Cell cell = cellIterator.next();
					//
					//// System.out.println(cell);
					//// System.out.println(cell.getStringCellValue());
					// }

				}

				rowctr++;

			}

			// // Iterate through each rows one by one
			// // System.out.println("SHEET " + sheet.getSheetName());
			// Iterator<Row> rowIterator = sheet.iterator();
			// boolean first = true;
			// PlanDefinition planDefinition = null;
			// int rowCtr = 0;
			// while (rowIterator.hasNext()) {
			// Row row = rowIterator.next();
			// // For each row, iterate through all the columns
			// Iterator<Cell> cellIterator = row.cellIterator();
			//
			// while (cellIterator.hasNext()) {
			// Cell cell = cellIterator.next();
			//
			// if (first) {
			// planDefinition = new PlanDefinition();
			// planDefinition.setId(IdType.newRandomUuid());
			// planDefinition.setName(sheet.getSheetName());
			// planDefinition.setDescription(cell.getStringCellValue());
			// planDefinition.setTitle(sheet.getSheetName());
			// planDefinition.setStatus(PublicationStatus.ACTIVE);
			// CodeableConcept cc = new CodeableConcept();
			// cc.addCoding().setCode("clinical-protocol");
			// planDefinition.setType(cc);
			//
			// planDefinition.setUsage(cell.getStringCellValue());
			//
			// bundle.addEntry().setResource(planDefinition).getRequest().setMethod(HTTPVerb.POST);
			// // System.out.println("THERAPY NAME " + cell.getStringCellValue());
			// } else {
			//
			// if (!StringUtils.isEmpty(cell.getStringCellValue().trim())) {
			//
			// if (!activities.containsKey(cell.getStringCellValue().trim())) {
			//
			// Bundle abundle = new Bundle();
			//
			// // Enumeration<BundleType> BundleType = new Enumeration<BundleType>();
			//
			// // bundle
			// abundle.setType(BundleType.TRANSACTION);
			//
			// ActivityDefinition activityDefinition = new ActivityDefinition();
			// activityDefinition.setTitle(cell.getStringCellValue().trim());
			// activityDefinition.setDescription(cell.getStringCellValue().trim());
			// // activityDefinition.set
			//
			// // activityDefinition.getIdentifier().add(id);
			// activityDefinition.setId(IdType.newRandomUuid());
			// activityDefinition.setName(cell.getStringCellValue());
			//
			// abundle.addEntry().setResource(activityDefinition).getRequest().setMethod(
			// HTTPVerb.POST);
			//
			// // List<ActivityDefinition> a = new ArrayList<ActivityDefinition>();
			//
			// Bundle result = client.transaction().withBundle(abundle).execute();
			//
			// // if (results.isEmpty()) {
			// System.out.println(
			// result.getEntry().get(0).getResponse().getLocation().replace("/_history/1", ""));
			//
			// activities.put(
			// cell.getStringCellValue().trim(),
			// result.getEntry().get(0).getResponse().getLocation().replace("/_history/1", ""));
			// // }
			//
			// }
			//
			// CanonicalType canonicalType = new CanonicalType();
			// canonicalType.setValue(activities.get(cell.getStringCellValue()));
			// planDefinition.addAction().setDescription(cell.getStringCellValue()).setDefinition(
			// canonicalType).setSelectionBehavior(ActionSelectionBehavior.ALL);
			// // bundle.addEntry().setResource(activityDefinition).getRequest().setMethod(HTTPVerb.POST);
			// // ;
			//
			// // if (!ads.contains(cell.getStringCellValue())) {
			// // ads.add(cell.getStringCellValue());
			// // } else {
			// // System.out.println(
			// // planDefinition.getName() + " has Possible Duplicate Activity \"" +
			// // cell.getStringCellValue() + "\"");
			// // }
			// }
			//
			// // System.out.println("STEP " + cell.getStringCellValue());
			// }
			//
			// // if (!first) {
			// // break;
			// // }
			// first = false;
			//
			// // Check the cell type and format accordingly
			// // switch (cell.getCellType())
			// // {
			// // case Cell.CELL_TYPE_NUMERIC:
			// // System.out.print(cell.getNumericCellValue() + "t");
			// // break;
			// // case Cell.CELL_TYPE_STRING:
			// // System.out.print(cell.getStringCellValue() + "t");
			// // break;
			// // }
			// }
			// // System.out.println("");
			//
			// if (rowCtr++ > 1) {
			// // break;
			// }
			// }
			// break;
		}
		// Get first/desired sheet from the workbook

		file.close();

		System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle));

		client.transaction().withBundle(bundle).execute();

		// Create a client and post the transaction to the server
		// IGenericClient client = ctx.newRestfulGenericClient("https://api.logicahealth.org/PTools3/open");
		// Bundle resp = client.transaction().withBundle(bundle).execute();
		//
		// // Log the response
		// System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resp));

	}

	//
	// void readClearActivities() throws IOException {
	//
	// FhirContext ctx = FhirContext.forR4();
	// ctx.getRestfulClientFactory().setServerValidationModeEnum(ServerValidationModeEnum.NEVER);
	//
	// IGenericClient client = ctx.newRestfulGenericClient("https://api.logicahealth.org/PTools4/open");
	//
	// // We'll populate this list
	// // List<IBaseResource> activityDefinitions = new ArrayList<IBaseResource>();
	//
	// // We'll do a search for all Patients and extract the first page
	// Bundle bundle = client.search().forResource(ActivityDefinition.class).returnBundle(Bundle.class).execute();
	//
	// List<ActivityDefinition> activityDefinitions = BundleUtil.toListOfResourcesOfType(
	// ctx, bundle, ActivityDefinition.class);
	// // bundle.get
	//
	// // activityDefinitions.addAll((Collection<? extends IBaseResource>) BundleUtil.toListOfResources(ctx, bundle));
	// //
	// // // Load the subsequent pages
	// // while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
	// // bundle = client.loadPage().next(bundle).execute();
	// // activityDefinitions.addAll((Collection<? extends IBaseResource>) BundleUtil.toListOfResources(ctx, bundle));
	// // }
	// //
	// System.out.println("Loaded " + activityDefinitions.size() + " ActivityDefinition!");
	//
	// for (ActivityDefinition dad : activityDefinitions) {
	//
	// // client.delete().resourceById(null)d
	// client.delete().resourceById(dad.getIdElement());
	//
	// }
	//
	// }

	public static void clearPlanDefinitions(String fhirEndPoint) throws IOException {

		FhirContext ctx = FhirContext.forR4();
		ctx.getRestfulClientFactory().setServerValidationModeEnum(ServerValidationModeEnum.NEVER);
		IGenericClient client = ctx.newRestfulGenericClient(fhirEndPoint);
		Bundle bundle = client.search().forResource(PlanDefinition.class).returnBundle(Bundle.class).execute();

		List<PlanDefinition> planDefinitions = BundleUtil.toListOfResourcesOfType(ctx, bundle, PlanDefinition.class);
		while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
			bundle = client.loadPage().next(bundle).execute();
			planDefinitions.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, PlanDefinition.class));
		}
		for (PlanDefinition dad : planDefinitions) {
			client.delete().resourceById(dad.getIdElement()).execute();
			System.out.println("Deleted " + dad.getId());
		}

	}

	public static void clearActivityDefinitions(String fhirEndPoint) throws IOException {
		FhirContext ctx = FhirContext.forR4();
		ctx.getRestfulClientFactory().setServerValidationModeEnum(ServerValidationModeEnum.NEVER);
		IGenericClient client = ctx.newRestfulGenericClient(fhirEndPoint);
		Bundle bundle = client.search().forResource(ActivityDefinition.class).returnBundle(Bundle.class).execute();
		List<ActivityDefinition> activityDefinitions = BundleUtil.toListOfResourcesOfType(
			ctx, bundle, ActivityDefinition.class);
		while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
			bundle = client.loadPage().next(bundle).execute();
			activityDefinitions.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, ActivityDefinition.class));
		}
		System.out.println("Deleting " + activityDefinitions.size() + " ActivityDefinitions!");
		for (ActivityDefinition dad : activityDefinitions) {
			client.delete().resourceById(dad.getIdElement()).execute();
			System.out.println("Deleted " + dad.getId());
		}
	}

	// non-manualized.xlsx

	public static void uploadManualizedStrategies(String fhirEndPoint) throws IOException {

		FhirContext ctx = FhirContext.forR4();

		ctx.getRestfulClientFactory().setServerValidationModeEnum(ServerValidationModeEnum.NEVER);
		IGenericClient client = ctx.newRestfulGenericClient(fhirEndPoint);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		FileInputStream file = new FileInputStream(new File("src/test/resources/non-manualized.xlsx"));
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		int totalSheets = workbook.getNumberOfSheets();

		for (int currentSheet = 1; currentSheet < totalSheets; currentSheet++) {
			XSSFSheet sheet = workbook.getSheetAt(currentSheet);

			if (!sheet.getSheetName().equalsIgnoreCase("SHEET2")) {
				continue;
			}

			// Iterate through each rows one by one
			// System.out.println("SHEET " + sheet.getSheetName());
			Iterator<Row> rowIterator = sheet.iterator();
			boolean first = true;
			PlanDefinition planDefinition = null;
			// int rowCtr = 0;
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				// For each row, iterate through all the columns
				Iterator<Cell> cellIterator = row.cellIterator();

				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();

					if (first) {
						planDefinition = new PlanDefinition();
						planDefinition.setId(IdType.newRandomUuid());
						planDefinition.setName(sheet.getSheetName());
						planDefinition.setDescription(cell.getStringCellValue());
						planDefinition.setTitle(sheet.getSheetName());
						planDefinition.setStatus(PublicationStatus.ACTIVE);
						CodeableConcept cc = new CodeableConcept();
						cc.addCoding().setCode("clinical-protocol");
						planDefinition.setType(cc);

						UUID uuid = UUID.randomUUID();
						planDefinition.getIdentifierFirstRep().setId(uuid.toString());

						Coding contextcode = new Coding();
						contextcode.setCode(uuid.toString());
						contextcode.setDisplay(cell.getStringCellValue());
						contextcode.setSystem("SOLAR");

						CodeableConcept contextcc = new CodeableConcept();
						cc.addCoding().setCode("focus");

						planDefinition.getUseContextFirstRep().setCode(contextcode).setValue(contextcc);

						planDefinition.setUsage(cell.getStringCellValue());

						bundle.addEntry().setResource(planDefinition).getRequest().setMethod(HTTPVerb.POST);

					} else {

						if (!StringUtils.isEmpty(cell.getStringCellValue().trim())) {

							Bundle abundle = new Bundle();

							abundle.setType(BundleType.TRANSACTION);

							ActivityDefinition activityDefinition = new ActivityDefinition();
							activityDefinition.setTitle(cell.getStringCellValue().trim());
							activityDefinition.setDescription(cell.getStringCellValue().trim());

							activityDefinition.setId(IdType.newRandomUuid());
							activityDefinition.setName(cell.getStringCellValue());

							CodeableConcept cc = new CodeableConcept();

							cc.setText(cell.getStringCellValue().trim());
							Coding coding = new Coding();
							coding.setDisplay(cell.getStringCellValue().trim());
							UUID uuid = UUID.randomUUID();
							coding.setCode(uuid.toString());
							coding.setSystem("SOLAR");
							cc.getCoding().add(coding);

							activityDefinition.setCode(cc);

							abundle.addEntry().setResource(activityDefinition).getRequest().setMethod(HTTPVerb.POST);

							Bundle result = client.transaction().withBundle(abundle).execute();

							System.out.println(
								result.getEntry().get(0).getResponse().getLocation().replace("/_history/1", ""));

							String url = result.getEntry().get(0).getResponse().getLocation().replace(
								"/_history/1", "");

							CanonicalType canonicalType = new CanonicalType();
							canonicalType.setValue(url);
							planDefinition.addAction().setDescription(cell.getStringCellValue()).setDefinition(
								canonicalType).setSelectionBehavior(ActionSelectionBehavior.ALL);
						}

					}

					first = false;

				}

			}

		}

		file.close();

		// System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle));

		client.transaction().withBundle(bundle).execute();

	}

}
