// **********************
// * Sheet Column Index *
// **********************
// Production - https://docs.google.com/spreadsheets/d/1jXtRCoEnnFNWj6_oFlVWflsf-b0jkfZpyhN-BXsv7uo/edit#gid=0
// Should be 1-based for writing to sheet, 0-based when working with arrays

var MANUFACTURER = 0;
var MODEL = 1;
var NAME = 2;
var ANDROID_VERSION = 3;
var API_LEVEL = 4;
var GNSS_HARDWARE_YEAR = 5;
var GNSS_HARDWARE_NAME = 6;
var DUAL_FREQUENCY = 7;
var SUPPORTED_GNSS = 8;
var GNSS_CFS = 9;
var SUPPORTED_SBAS = 10;
var SBAS_CFS = 11;
var RAW_MEASUREMENTS = 12;
var CARRIER_PHASE_ADR = 13;
var NAV_MESSAGES = 14;
var NMEA = 15;
var INJECT_PSDS = 16;
var INJECT_TIME = 17;
var DELETE_ASSIST = 18;
// 19 - RESERVED
// 20 - RESERVED
var AUTO_GAIN_CONTROL = 21;
var GNSS_ANTENNA_INFO = 22;
var APP_VERSION_NAME = 23;
var APP_VERSION_CODE = 24;
var APP_BUILD_FLAVOR = 25;
var USER_LOCATION_COUNTRY_CODE = 26;
var DATE = 27;
var DATE_ISO = 28;
var ANDROID_SOFTWARE_BUILD = 29;
var ANDROID_SOFTWARE_CODENAME = 30;
var NUM_GNSS_ANTENNAS = 31;
var GNSS_ANTENNA_CFS = 32;

function doGet(e) {
  var GOOGLE_DOC_ID = "1jXtRCoEnnFNWj6_oFlVWflsf-b0jkfZpyhN-BXsv7uo";

  var ss = SpreadsheetApp.openById(GOOGLE_DOC_ID);
  var sheet = ss.getSheets()[0];

  var manufacturer = e.parameter.manufacturer;
  var model = e.parameter.model;
  var androidVersion = e.parameter.androidVersion;
  var apiLevel = e.parameter.apiLevel;
  var gnssHardwareYear = e.parameter.gnssHardwareYear;
  var gnssHardwareModelName = e.parameter.gnssHardwareModelName;
  var duelFrequency = e.parameter.duelFrequency; // Yes, this is misspelled but needs to be in sync with typo on Android app
  var supportedGnss = e.parameter.supportedGnss;
  var gnssCfs = e.parameter.gnssCfs;
  var supportedSbas = e.parameter.supportedSbas;
  var sbasCfs = e.parameter.sbasCfs;
  var rawMeasurements = e.parameter.rawMeasurements;
  var navigationMessages = e.parameter.navigationMessages;
  var nmea = e.parameter.nmea;
  var injectPsds = e.parameter.injectPsds;
  var injectTime = e.parameter.injectTime;
  var deleteAssist = e.parameter.deleteAssist;
  var accumulatedDeltaRange = e.parameter.accumulatedDeltaRange;
  // var hardwareClock = e.parameter.hardwareClock;
  // var hardwareClockDiscontinuity = e.parameter.hardwareClockDiscontinuity;
  var automaticGainControl = e.parameter.automaticGainControl;
  var gnssAntennaInfo = e.parameter.gnssAntennaInfo;
  var appVersionName = e.parameter.appVersionName;
  var appVersionCode = e.parameter.appVersionCode;
  var appBuildFlavor = e.parameter.appBuildFlavor;
  var userCountry = e.parameter.userCountry;

  // Ignore any submissions from emulators
  if (model.includes('sdk_gphone') || gnssHardwareModelName.includes('Emulator') ||
      (model.includes('Android') && model.includes('SDK')) ||
       model.includes('Emulator')) {
    Logger.log('Ignoring emulator submission - ' + model);
    return ContentService.createTextOutput("NO EMULATOR SUBMISSIONS").setMimeType(ContentService.MimeType.TEXT);
  }

  Logger.log('Inserting new record - ' + model);

  var d = new Date();
  var date = Utilities.formatDate(d, "GMT", "MM/dd/yyyy hh:mm:ss a");
  var dateIso = Utilities.formatDate(d, "GMT", "yyyy-MM-dd'T'HH:mm:ss'Z'");

  var emptyRow = getFirstEmptyRowByColumnB(sheet);
  // Indexes are 1-based when writing to sheet, so add 1 to each index field
  sheet.getRange(emptyRow,MANUFACTURER+1).setValue(capitalizeFirstLetter(manufacturer));
  sheet.getRange(emptyRow,MODEL+1).setValue(model);
  // Device name will be auto-populated in column 3 via the addDeviceName() method on a timer
  sheet.getRange(emptyRow,ANDROID_VERSION+1).setValue(androidVersion);
  sheet.getRange(emptyRow,API_LEVEL+1).setValue(apiLevel);
  sheet.getRange(emptyRow,GNSS_HARDWARE_YEAR+1).setValue(gnssHardwareYear);
  sheet.getRange(emptyRow,GNSS_HARDWARE_NAME+1).setValue(gnssHardwareModelName);
  sheet.getRange(emptyRow,DUAL_FREQUENCY+1).setValue(duelFrequency);
  sheet.getRange(emptyRow,SUPPORTED_GNSS+1).setValue(supportedGnss);
  sheet.getRange(emptyRow,GNSS_CFS+1).setValue(gnssCfs);
  sheet.getRange(emptyRow,SUPPORTED_SBAS+1).setValue(supportedSbas);
  sheet.getRange(emptyRow,SBAS_CFS+1).setValue(sbasCfs);
  sheet.getRange(emptyRow,RAW_MEASUREMENTS+1).setValue(rawMeasurements);

  sheet.getRange(emptyRow,NAV_MESSAGES+1).setValue(navigationMessages);
  sheet.getRange(emptyRow,NMEA+1).setValue(nmea);
  sheet.getRange(emptyRow,INJECT_PSDS+1).setValue(injectPsds);
  sheet.getRange(emptyRow,INJECT_TIME+1).setValue(injectTime);
  sheet.getRange(emptyRow,DELETE_ASSIST+1).setValue(deleteAssist);

  // sheet.getRange(emptyRow,20).setValue(hardwareClock);
  // sheet.getRange(emptyRow,21).setValue(hardwareClockDiscontinuity);

  // v3.9.7 was the first to handle these fields correctly, so ignore older versions (https://github.com/barbeau/gpstest/issues/478)
  if (!(appVersionName == "3.9.0" || appVersionName == "3.9.1" ||
        appVersionName == "3.9.2" || appVersionName == "3.9.3" ||
        appVersionName == "3.9.4" || appVersionName == "3.9.5" ||
        appVersionName == "3.9.6")) {
    sheet.getRange(emptyRow,AUTO_GAIN_CONTROL+1).setValue(automaticGainControl);
  }

  // v3.9.9 was the first to handle these fields correctly, so ignore older versions (https://github.com/barbeau/gpstest/issues/478)
  if (!(appVersionName == "3.9.0" || appVersionName == "3.9.1" ||
        appVersionName == "3.9.2" || appVersionName == "3.9.3" ||
        appVersionName == "3.9.4" || appVersionName == "3.9.5" ||
        appVersionName == "3.9.6" || appVersionName == "3.9.7" ||
        appVersionName == "3.9.8")) {
    sheet.getRange(emptyRow,CARRIER_PHASE_ADR+1).setValue(accumulatedDeltaRange);
  }

  sheet.getRange(emptyRow,GNSS_ANTENNA_INFO+1).setValue(gnssAntennaInfo);
  sheet.getRange(emptyRow,APP_VERSION_NAME+1).setValue(appVersionName);
  sheet.getRange(emptyRow,APP_VERSION_CODE+1).setValue(appVersionCode);
  sheet.getRange(emptyRow,APP_BUILD_FLAVOR+1).setValue(appBuildFlavor);
  sheet.getRange(emptyRow,USER_LOCATION_COUNTRY_CODE+1).setValue(userCountry);
  sheet.getRange(emptyRow,DATE+1).setValue(date + " (GMT)");
  sheet.getRange(emptyRow,DATE_ISO+1).setValue(dateIso);

  // Check to make sure request contains the below parameters (they were added later in chunks)
  if (typeof(e.parameter.androidBuildIncremental) != "undefined") {
    var androidBuildIncremental = e.parameter.androidBuildIncremental;
    var androidBuildCodename = e.parameter.androidBuildCodename;
    sheet.getRange(emptyRow,ANDROID_SOFTWARE_BUILD+1).setValue(androidBuildIncremental);
    sheet.getRange(emptyRow,ANDROID_SOFTWARE_CODENAME+1).setValue(androidBuildCodename);
  }
  if (typeof(e.parameter.numAntennas) != "undefined") {
    var numAntennas = e.parameter.numAntennas;
    var antennaCfs = e.parameter.antennaCfs;
    sheet.getRange(emptyRow,NUM_GNSS_ANTENNAS+1).setValue(numAntennas);
    sheet.getRange(emptyRow,GNSS_ANTENNA_CFS+1).setValue(antennaCfs);
  }

  return ContentService.createTextOutput("STATUS OK").setMimeType(ContentService.MimeType.TEXT);
}

// Based on code from https://stackoverflow.com/questions/6882104/faster-way-to-find-the-first-empty-row-in-a-google-sheet-column
function getFirstEmptyRowByColumnB(sheet) {
  return sheet.getLastRow() + 1;
}

// Populate device names based on model using https://storage.googleapis.com/play_public/supported_devices.csv
// Runs on a timer every 12 hours
function addDeviceName() {
  var GOOGLE_DOC_ID = "1jXtRCoEnnFNWj6_oFlVWflsf-b0jkfZpyhN-BXsv7uo";

  var ss = SpreadsheetApp.openById(GOOGLE_DOC_ID);
  var sheet = ss.getSheets()[0];

  // Download and parse Google CSV that contains model->name mapping
  var googleResponse = UrlFetchApp.fetch("https://storage.googleapis.com/play_public/supported_devices.csv");
  var googleCsvData = Utilities.parseCsv(googleResponse.getContentText("UTF-16"));

  // Download and parse GPSTest CSV that contains model->name mapping for devices not in the Google CSV
  var gpsTestResponse = UrlFetchApp.fetch("https://github.com/barbeau/gpstest/raw/master/data/device_models_supplement.csv");
  var gpsTestCsvData = Utilities.parseCsv(gpsTestResponse.getContentText("UTF-8"));

  // Loop through rows with empty device names and try to look them up in CSV
  var emptyRow = getFirstEmptyDeviceNameRow(sheet);
  while (emptyRow <= sheet.getDataRange().getLastRow()) {
    // Get model and name from sheet
    var model = sheet.getRange(emptyRow,2).getValue();
    var name = sheet.getRange(emptyRow,3).getValue();
    if (model == "") {
      // No more model data - we're done
      break;
    }
    if (name != "") {
      // We already filled in this name - skip to next row
      emptyRow++;
      continue;
    }

    // Find the name from the CSVs based on the model - try the Google CSV file first
    Logger.log('Checking Google file...');
    var foundDevice = setDeviceName(sheet, emptyRow, model, googleCsvData);
    if (!foundDevice) {
      Logger.log('Checking GPSTest file...');
      // Try the GPSTest supplement file
      setDeviceName(sheet, emptyRow, model, gpsTestCsvData);
    }

    emptyRow++;
  }
  return;
}

// Find the first sheet row that has an empty value for device name
function getFirstEmptyDeviceNameRow(sheet) {
  var column = sheet.getRange('C:C')
  var values = column.getValues(); // get all data in one call for fast performance
  var row = 1;
  while (values[row] && values[row][0] != "") {
    row++;
  }
  return (row + 1);
}

// Tries to find and set the device name in the provided sheet
// based on the model in the provided CSV dataset
// Returns true if a model was found and name was set to sheet, false if not
function setDeviceName(sheet, emptyRow, model, csvData) {
  var csvNameColumn = 1;
  var csvModelColumn = 3;
  for (var i = 0; i < csvData.length; i++) {
    if (i % 1000 == 0) {
      Logger.log('Sheet row ' + emptyRow + ' CSV row ' + i + ': ' + csvData[i][0] + ' | ' + csvData[i][1] + ' | ' + + csvData[i][2] + ' | ' + csvData[i][3]);
    }

    if (csvData[i][csvModelColumn] == model) {
      // Found the device model! Set the name in the sheet
      sheet.getRange(emptyRow,3).setValue(csvData[i][csvNameColumn]);
      return true;
    }
  }
  return false;
}

function capitalizeFirstLetter(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

// Prunes the spreadsheet by removing duplicate records and keeping the "better"
// version of near duplicates (same device but different info)
// Runs on a timer every 12 hours
function removeDuplicates() {
  var GOOGLE_DOC_ID = "1jXtRCoEnnFNWj6_oFlVWflsf-b0jkfZpyhN-BXsv7uo";

  var ss = SpreadsheetApp.openById(GOOGLE_DOC_ID);
  var sheet = ss.getSheets()[0];

  var data = sheet.getDataRange().getValues();
  var newData = [];
  var duplicateCount = 0;
  var nearDuplicateCount = 0;
  for (var i in data) {
    var row = data[i];
    var duplicate = false;
    var skipInsertion = false;
    var sameDevice = false;
    for (var j in newData) {
      if (isDuplicate(row, newData, j)) {
        duplicate = true;
        skipInsertion = true;
        break;
      } else {
        sameDevice = isSameDevice(row, newData, j);
        if (sameDevice) {
          // Figure out which record to keep - true keeps the row, while false keeps the newData
          var whichKeep = whichToKeep(row, newData, j);
          if (!whichKeep) {
            // Keep the newData by skipping insertion (as if it was a duplicate)
            Logger.log('Keeping near duplicate:');
            printNewData(newData, j);
            Logger.log('...instead of:');
            printRow(row);
            skipInsertion = true;
            break;
          } else {
            Logger.log('Keeping near duplicate:');
            printRow(row);
            Logger.log('...instead of:');
            printNewData(newData, j);
            // Keep the row by copying the fields over to the newData record, which will then be inserted
            copyRowToNewData(row, newData, j);
            break;
          }
        }
      }
    }
    if (!skipInsertion && !sameDevice) {
      // Keep the record
      newData.push(row);
    }
    if (duplicate) {
      duplicateCount++;
    }
    if (sameDevice) {
      nearDuplicateCount++;
    }
  }
  Logger.log('Removed ' + duplicateCount + ' duplicate(s)');
  Logger.log('Removed ' + nearDuplicateCount + ' near duplicate(s)');
  sheet.clearContents();
  sheet.getRange(1, 1, newData.length, newData[0].length).setValues(newData);
}

// Returns  true if this row is functionally a duplicate as the record in newData specified by the j index, false if it's not
function isDuplicate(row, newData, j) {
  if ( // Manufacturer                                      Model                            Name
        row[MANUFACTURER] == newData[j][MANUFACTURER] && row[MODEL] == newData[j][MODEL] && row[NAME] == newData[j][NAME] &&
        // Android version                                     API Level                                  GNSS hardware year
        row[ANDROID_VERSION] == newData[j][ANDROID_VERSION] && row[API_LEVEL] == newData[j][API_LEVEL] && row[GNSS_HARDWARE_YEAR] == newData[j][GNSS_HARDWARE_YEAR] &&
        // GNSS hardware name                                        Dual-frequency                                       Supported GNSS
        row[GNSS_HARDWARE_NAME] == newData[j][GNSS_HARDWARE_NAME] && row[DUAL_FREQUENCY] == newData[j][DUAL_FREQUENCY] && row[SUPPORTED_GNSS] == newData[j][SUPPORTED_GNSS] &&
        // GNSS CFs                                      Supported SBAS                                       SBAS CFs
        row[GNSS_CFS] == newData[j][GNSS_CFS] && row[SUPPORTED_SBAS] == newData[j][SUPPORTED_SBAS] && row[SBAS_CFS] == newData[j][SBAS_CFS] &&
        // Raw measurements                                   Nav messages                                     NMEA
        row[RAW_MEASUREMENTS] == newData[j][RAW_MEASUREMENTS] && row[NAV_MESSAGES] == newData[j][NAV_MESSAGES] && row[NMEA] == newData[j][NMEA] &&
        // Inject PSDS                                 Inject time                                    Delete assist
        row[INJECT_PSDS] == newData[j][INJECT_PSDS] && row[INJECT_TIME] == newData[j][INJECT_TIME] && row[DELETE_ASSIST] == newData[j][DELETE_ASSIST] &&
        // Carrier phase (ADR)
        row[CARRIER_PHASE_ADR] == newData[j][CARRIER_PHASE_ADR] &&
        // Auto gain control                                       GnssAntennaInfo
        row[AUTO_GAIN_CONTROL] == newData[j][AUTO_GAIN_CONTROL] && row[GNSS_ANTENNA_INFO] == newData[j][GNSS_ANTENNA_INFO] &&
        // Android software build
        row[ANDROID_SOFTWARE_BUILD] == newData[j][ANDROID_SOFTWARE_BUILD] &&
        // Android software codenm                                                 Number GNSS antennas                              GNSS antenna CFs
        row[ANDROID_SOFTWARE_CODENAME] == newData[j][ANDROID_SOFTWARE_CODENAME] && row[NUM_GNSS_ANTENNAS] == newData[j][NUM_GNSS_ANTENNAS] && row[GNSS_ANTENNA_CFS] == newData[j][GNSS_ANTENNA_CFS]) {
        Logger.log('Removing duplicate row:');
        printRow(row);
    return true;
  } else {
    return false;
  }
}

// Returns  true if this row has the same overall device profile (hardware) as the record in newData specified by the j index, false if it's not the same
function isSameDevice(row, newData, j) {
  if ( // Manufacturer                                   Model                              Name
        row[MANUFACTURER] == newData[j][MANUFACTURER] && row[MODEL] == newData[j][MODEL] && row[NAME] == newData[j][NAME] &&
        // GNSS hardware year
        row[GNSS_HARDWARE_YEAR] == newData[j][GNSS_HARDWARE_YEAR] &&
        // GNSS hardware name
        row[GNSS_HARDWARE_NAME] == newData[j][GNSS_HARDWARE_NAME]) {
    return true;
  } else {
    return false;
  }
}

// Returns true if the row record should be kept, otherwise returns false if the newData record should be kept
// Intended to be used after the devices are already confirmed to be the same hardware and Android OS level
function whichToKeep(row, newData, j) {
  // If dual-frequency is now supported when it wasn't previously, replace the previous record
  if (  // Dual-frequency
        row[DUAL_FREQUENCY] == "SUPPORTED" && newData[j][DUAL_FREQUENCY] == "NOT_SUPPORTED" &&
        // ADR
        isAdrSame(row, newData, j))  {
    Logger.log('Dual-frequency is now supported (ADR same)');
    return true;
  }
  // If more GNSS support is now detected, replace the previous record
  if (isGnssBetter(row, newData, j) &&
      // ADR
      isAdrSame(row, newData, j))  {
    Logger.log('More GNSS detected (ADR same)');
    return true;
  }
  // If the GNSS are the same or better and SBAS is better, replace the previous record
  if (isGnssSameOrBetter(row, newData, j) &&
      isSbasBetter(row, newData, j) &&
      // ADR
      isAdrSame(row, newData, j))  {
    Logger.log('More SBAS detected (and GNSS same or better, ADR same)');
    return true;
  }
  // If the ADR is now reported, replace the previous record
  if (//  Carrier phase (ADR)
      row[CARRIER_PHASE_ADR].length > 0 && newData[j][CARRIER_PHASE_ADR].length == 0
  )  {
    Logger.log('ADR now reported');
    return true;
  }
  // If the ADR is now known, replace the previous unknown record
  if (//  Carrier phase (ADR)
      row[CARRIER_PHASE_ADR].length > 0 && row[CARRIER_PHASE_ADR] != "UNKNOWN" && newData[j][CARRIER_PHASE_ADR] == "UNKNOWN"
  )  {
    Logger.log('ADR now known (previously UNKNOWN)');
    return true;
  }
  // If the ADR is SUPPORTED and was previously NOT_SUPPORTED, replace the previous unknown record
  if (//  Carrier phase (ADR)
      row[SUPPORTED_GNSS].length > 0 &&
      row[CARRIER_PHASE_ADR] == "SUPPORTED" && newData[j][CARRIER_PHASE_ADR] == "NOT_SUPPORTED"
  )  {
    Logger.log('ADR now SUPPORTED (previously NOT_SUPPORTED)');
    return true;
  }
  // Keep newData record
  return false;
}

// Returns true if the GNSS support and CFs are better in row and newData at the j index, and false if they are not
function isGnssBetter(row, newData, j) {
  if (// Supported GNSS
    (row[SUPPORTED_GNSS].length > newData[j][SUPPORTED_GNSS].length) &&
    // GNSS CFs
    (row[GNSS_CFS].length >= newData[j][GNSS_CFS].length)) {
    return true;
  } else {
    return false;
  }
}

// Returns true if the GNSS support and CFs are the same in row and newData at the j index, and false if they are not
function isGnssSameOrBetter(row, newData, j) {
  if (// Supported GNSS
    (row[SUPPORTED_GNSS].length >= newData[j][SUPPORTED_GNSS].length) &&
    // GNSS CFs
    (row[GNSS_CFS].length >= newData[j][GNSS_CFS].length)) {
    return true;
  } else {
    return false;
  }
}

// Returns true if the SBAS support and CFs are better in row and newData at the j index, and false if they are not
function isSbasBetter(row, newData, j) {
  if (// Supported SBAS
    (row[SUPPORTED_SBAS].length > newData[j][SUPPORTED_SBAS].length) &&
      // SBAS CFs
      (row[SBAS_CFS].length > newData[j][SBAS_CFS].length)) {
    return true;
  } else {
    return false;
  }
}

// Returns true if the accumulated delta range support is the same in row and newData at the j index, and false if they are not
function isAdrSame(row, newData, j) {
  if (//  Carrier phase (ADR)
    row[CARRIER_PHASE_ADR] == newData[j][CARRIER_PHASE_ADR]) {
   return true;
  } else {
   return false;
  }
}

// Copies the contents of row to newData at the index j
function copyRowToNewData(row, newData, j) {
  var k;
  for (k = 0; k < newData[j].length; k++) {
    newData[j][k] = row[k];
  }
}

// Prints the provided row to the logger
function printRow(row) {
  Logger.log(
        row[MANUFACTURER] + ' ' + row[MODEL] + ' ' + row[NAME] + ' ' +
        row[ANDROID_VERSION] + ' ' + row[API_LEVEL] + ' ' + row[GNSS_HARDWARE_YEAR] + ' ' +
        row[GNSS_HARDWARE_NAME] + ' ' + row[DUAL_FREQUENCY] + ' ' + row[SUPPORTED_GNSS] + ' ' +
        row[GNSS_CFS] + ' ' + row[SUPPORTED_SBAS] + ' ' + row[SBAS_CFS] + ' ' +
        row[RAW_MEASUREMENTS] + ' ' + row[CARRIER_PHASE_ADR] + ' ' + row[NAV_MESSAGES] + ' ' + row[NMEA] + ' ' +
        row[INJECT_PSDS] + ' ' + row[INJECT_TIME] + ' ' + row[DELETE_ASSIST] + ' ' +
        row[AUTO_GAIN_CONTROL] + ' ' + row[GNSS_ANTENNA_INFO] + ' ' +
        row[ANDROID_SOFTWARE_BUILD] + ' ' + row[ANDROID_SOFTWARE_CODENAME] + ' ' +
        row[NUM_GNSS_ANTENNAS] + ' ' + row[GNSS_ANTENNA_CFS]
        );
}

// Prints the provided newData to the logger
function printNewData(newData, j) {
  Logger.log(
        newData[j][MANUFACTURER] + ' ' + newData[j][MODEL] + ' ' + newData[j][NAME] + ' ' +
        newData[j][ANDROID_VERSION] + ' ' + newData[j][API_LEVEL] + ' ' + newData[j][GNSS_HARDWARE_YEAR] + ' ' +
        newData[j][GNSS_HARDWARE_NAME] + ' ' + newData[j][DUAL_FREQUENCY] + ' ' + newData[j][SUPPORTED_GNSS] + ' ' +
        newData[j][GNSS_CFS] + ' ' + newData[j][SUPPORTED_SBAS] + ' ' + newData[j][SBAS_CFS] + ' ' +
        newData[j][RAW_MEASUREMENTS] + ' ' + newData[j][CARRIER_PHASE_ADR] + ' ' + newData[j][NAV_MESSAGES] + ' ' + newData[j][NMEA] + ' ' +
        newData[j][INJECT_PSDS] + ' ' + newData[j][INJECT_TIME] + ' ' + newData[j][DELETE_ASSIST] + ' ' +
        newData[j][AUTO_GAIN_CONTROL] + ' ' + newData[j][GNSS_ANTENNA_INFO] + ' ' +
        newData[j][ANDROID_SOFTWARE_BUILD] + ' ' + newData[j][ANDROID_SOFTWARE_CODENAME] + ' ' +
        newData[j][NUM_GNSS_ANTENNAS] + ' ' + newData[j][GNSS_ANTENNA_CFS]
        );
}