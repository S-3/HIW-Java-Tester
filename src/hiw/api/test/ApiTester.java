package hiw.api.test;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The main entry point of the application.
 */
public class ApiTester {
	//The base URL of the service.
	private static final String _baseUrl = "http://services.healthindicators.gov/v5/REST.svc/";
	
	//Your API key.
	private String _apiKey = null;
	
	//The ID of the Indicator Description you're interested in (Deaths, all causes (per 100,000);
	private Integer _indicatorDescriptionId = null;
	
	//The number of pages of data to pull from the API (set to null for all pages).
	private Integer _pages = null;

	public static void main(String[] args) throws Exception {
		String apiKey = null;
		Integer indicatorDescriptionId = 71;
		Integer pages = null;
		
		if (args != null) {
			if (args.length >= 1)
				apiKey = args[0];

			if (args.length >= 2)
				indicatorDescriptionId = Integer.parseInt(args[1]);

			if (args.length >= 3)
				pages = Integer.parseInt(args[2]);
		}
		
		try{
			(new ApiTester(apiKey, indicatorDescriptionId, pages)).run();
		}
		catch (Exception ex) {
			System.out.println("An error occurred.");
			System.out.println(ex.toString());
		}
	}
	
	public ApiTester(String apiKey, Integer indicatorDescriptionId, Integer pages) {
		_apiKey = apiKey;
		_indicatorDescriptionId = indicatorDescriptionId;
		_pages = pages;
	}

	/**
	 * Runs the test.
	 */
	private void run() throws Exception {
		JSONObject indicatorDescription = null;
		JSONArray dimensionGraphs = null;
		HashMap<Integer, JSONObject> dimensionGraphMap = new HashMap<Integer, JSONObject>();
		HashMap<Integer, String> ageMap = new HashMap<Integer, String>();
		HashMap<Integer, String> sexMap = new HashMap<Integer, String>();
		HashMap<Integer, String> localeMap = new HashMap<Integer, String>();
		HashMap<Integer, String> timeFrameMap = new HashMap<Integer, String>();
		
		//Make sure we have an API key.
		if (_apiKey == null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.println("No API key specified - please enter your API key: ");
			_apiKey = reader.readLine();
		}
		
		//Make sure we have an Indicator Description Id.
		if (_indicatorDescriptionId == null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.println("No Indicator Description Id specified - please enter an Indicator Description Id: ");
			_indicatorDescriptionId = Integer.parseInt(reader.readLine());
		}
		
		//Get general information about the Indicator Description.
		System.out.println(String.format("Getting information for the Indicator Description with ID = %s...", _indicatorDescriptionId));
		indicatorDescription = makeGetApiCall("IndicatorDescription/" + _indicatorDescriptionId).getJSONObject(0);
		
		//Find the dimension graphs we're interested in - those with both Age and Sex dimensions.
		//  A dimension graph defines the dimensions for which data are available.
		System.out.println("Getting a list of available Dimension Graphs...");
		dimensionGraphs = makePostApiCall("DimensionGraphs/Filter", new Filter()
			.addCriterion("AgeID", FilterOperator.NotNull)
			.addCriterion("SexID", FilterOperator.NotNull)
			.toJSON());
		
		//Pull out the IDs from the dimension graphs. The IDs will be used later to filter the actual Indicator data.
		System.out.println("Extracting IDs from the Dimension Graphs...");
		for (int i = 0; i < dimensionGraphs.length(); i++) {
			JSONObject dg = dimensionGraphs.getJSONObject(i);
			
			dimensionGraphMap.put(dg.getInt("ID"), dg);
		}
		
		//Get all of the ages. These will be used later when we are writing the results to the console.
		System.out.println("Getting list of ages...");
		ageMap = getLookup("Ages");
		
		//Get all of the sexes. These will be used later when we are writing the results to the console.
		System.out.println("Getting list of sexes...");
		sexMap = getLookup("Sexes");
		
		//Get all of the locales. These will be used later when we are writing the results to the console.
		System.out.println("Getting list of locales...");
		localeMap = getLookup("Locales", "ID", "FullName");
		
		//Get all of the time frames. These will be used later when we are writing the results to the console.
		System.out.println("Getting list of time frames...");
		timeFrameMap = getLookup("Timeframes");
		
		//Output some general information about the Indicator Description.
		System.out.println();
		System.out.println();
		
		ExportIndicators(indicatorDescription, dimensionGraphMap, ageMap, sexMap, localeMap, timeFrameMap);

		System.out.println("Done, press any key to exit.");
		System.in.read();
	}
	
	private void ExportIndicators(JSONObject indicatorDescription, HashMap<Integer, JSONObject> dimensionGraphMap, HashMap<Integer, String> ageMap, HashMap<Integer, String> sexMap, HashMap<Integer, String> localeMap, HashMap<Integer, String> timeFrameMap) throws Exception {
		String filename = String.format("export/%s.csv", indicatorDescription.getInt("ID"));
		FileWriter writer = new FileWriter(filename);
		Filter filter = new Filter()
			.addCriterion("IndicatorDescriptionID", FilterOperator.Equal, _indicatorDescriptionId)
			.addCriterion("DimensionGraphID", FilterOperator.In, dimensionGraphMap.keySet().toArray());
		Integer pageCount = (Integer)makePostApiCall("Indicators/Filter/PageCount", filter.toJSON()).get(0);
		Integer pagesToExport = (_pages == null ? pageCount : Math.min(_pages, pageCount));
		Integer currentPage = 1;
		Boolean hasMore = true;
		
		System.out.println(indicatorDescription.getString("ShortDescription"));
		System.out.println(indicatorDescription.getString("FullDescription"));
		System.out.println();
		System.out.println();
		System.out.println(String.format("Exporting %s of %s total page(s) of data to \"%s\"...", pagesToExport, pageCount, filename));

		//Write header.
		writer.append("Indicator Name,Locale, FIPS Code,Time Frame,Value,SE,CI Low,CI High\r\n");

		//Output the data 10 records at a time until the user "quits".
		do {
			System.out.println(String.format("Getting page %s of %s (%s%%)...", currentPage, pagesToExport, Math.round(100 * (currentPage / (double)pagesToExport))));

			//Now, query for Indicators for the Indicator Description which also map to the Dimension Graphs.
			//  Note that there is no "in" operator, but we can fake it. Have a look at FilterCriterion.toJSON() for more information.
			//  Also note that we are only requesting the first page (1,000 Indicators) of data, so we loop until no more data is returned.
			JSONArray indicators = makePostApiCall("Indicators/Filter", filter.setPage(currentPage).toJSON());
			
			//For each Indicator (value), we find the associated (cached) Dimension Graph and use that to pull out the (cached) dimension names.
			for (int i = 0; i < indicators.length(); i++) {
				JSONObject indicator = indicators.getJSONObject(i);
				JSONObject dimensionGraph = dimensionGraphMap.get(indicator.getInt("DimensionGraphID"));
				
				try {
					Object indicatorName = prepareForCsv(dimensionGraph.get("DimensionGraphLabel"));
					String localeName = prepareForCsv(localeMap.get(indicator.getInt("LocaleID")));
					Object fipsCode = prepareForCsv(indicator.get("FIPSCode"));
					String timeFrameName = prepareForCsv(timeFrameMap.get(indicator.getInt("TimeframeID")));
					Object value = prepareForCsv(indicator.get("FormattedValue"));
					Object se = prepareForCsv(indicator.get("StandardErrorFormatted"));
					Object ciLow = prepareForCsv(indicator.get("ConfidenceIntervalLowFormatted"));
					Object ciHigh = prepareForCsv(indicator.get("ConfidenceIntervalHighFormatted"));
	
					writer.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s\r\n", indicatorName, localeName, fipsCode, timeFrameName, value, se, ciLow, ciHigh));
				}
				catch (Exception ex) {
					System.out.println(String.format("\r\nWhile processing the JSON object at index %s, the error below occurred.\r\nJSON:\r\n%s\r\n\r\nError: %s", i, indicator.toString(), ex.getMessage()));
				}
			}

			currentPage++;
			hasMore = (indicators.length() > 0);
		} while (hasMore && pagesToExport >= currentPage);
		
		writer.flush();
		writer.close();
	}

	private HashMap<Integer, String> getLookup(String resource) throws Exception {
		return getLookup(resource, "ID", "Name");
	}

	private HashMap<Integer, String> getLookup(String resource, String keyField, String nameField) throws Exception {
		Integer page = 1;
		HashMap<Integer, String> lookup = new HashMap<Integer, String>();
		Boolean hasMore = true;
		
		do {
			JSONArray items = makeGetApiCall(String.format("%s/%s", resource, page));
			
			for (int i = 0; i < items.length(); i++) {
				JSONObject o = items.getJSONObject(i);
				
				lookup.put(o.getInt("ID"), o.getString("Name"));
			}
			
			page++;
			hasMore = (items.length() > 0);
		} while (hasMore);
		
		return lookup;
	}
	
	private String prepareForCsv(Object value) {
		if (value == null)
			return "";
		
		return String.format("\"%s\"", value);
	}

	/**
	 * Shortcut to make a "GET" call to the API.
	 * @param path	The relative path to the API end-point.
	 * @return		A JSONArray containing the data returned from the API.
	 */
	private JSONArray makeGetApiCall(String path) throws Exception {
		return makeApiCall(path, "GET", null);
	}

	/**
	 * Shortcut to make a "POST" call to the API.
	 * @param path		The relative path to the API end-point.
	 * @param postData	
	 * @return			A JSONArray containing the data returned from the API.
	 */
	private JSONArray makePostApiCall(String path, JSONObject postData) throws Exception {
		return makeApiCall(path, "POST", postData);
	}

	/**
	 * Makes an API call.
	 * @param path		The relative path to the API end-point.
	 * @param method	The HTTP method to use (GET or POST).
	 * @param postData	The data to post to the API (only used for POST calls).
	 * @return			A JSONArray containing the data returned from the API.
	 */
	private JSONArray makeApiCall(String path, String method, JSONObject postData) throws Exception {
		//Create the full URL and open a connection.
		URL url = new URL(_baseUrl + path);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		String responseText = null;
		Scanner scanner = null;
		JSONObject result = null;
		Object data = null;
		
		//Set connection properties including the API Key and the Accept header so we get JSON back from the API.
		connection.setRequestMethod(method);
		connection.setRequestProperty("X-HIW-API-Key", _apiKey);
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Connection", "close");
		
		//Only write data to the connection if 1) we are POSTing, and 2) there is data to write.
		if ("POST".equals(method) && postData != null) {
			OutputStreamWriter writer = null;

			//Set more properties on the connection.
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");
			
			//Open the stream and write the data.
			writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(postData.toString(0));
			writer.flush();
			writer.close();
		}
		
		//Use a scanner to easily read the response stream.
		scanner = new Scanner(connection.getInputStream());
		responseText = scanner.useDelimiter("\\A").next();
		scanner.close();
		
		//Parse the response as JSON.
		result = new JSONObject(responseText);
		
		//Check if the API returned an error.
		if ("Error".equals(result.getString("Status")))
			throw new Exception(result.getString("Message"));
		
		//Get the underlying data from the returned JSON.
		data = result.get("Data");
		
		//We always want to return a JSONArray, so check if the data is already a JSONArray. If not, make it one.
		if (data instanceof JSONArray)
			return result.getJSONArray("Data");
		else
			return new JSONArray().put(data);
	}
}