package hiw.api.test;

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
	private static final String _apiKey = "";
	
	//The ID of the Indicator Description you're interested in (Deaths, all causes (per 100,000);
	private static final int _indicatorDescriptionId = 71;

	public static void main(String[] args) throws Exception {
		if ("".equals(_apiKey)) {
			System.out.println("No API key specified - please set ApiTester._apiKey and try again.");
			System.in.read();
			return;
		}
		
		try{
			(new ApiTester()).run();
		}
		catch (Exception ex) {
			System.out.println("An error occurred.");
			System.out.println(ex.toString());
		}
	}

	/**
	 * Runs the test.
	 */
	private void run() throws Exception {
		JSONObject indicatorDescription = null;
		JSONArray dimensionGraphs = null;
		HashMap<Integer, JSONObject> dimensionGraphMap = new HashMap<Integer, JSONObject>();
		JSONArray ages = null;
		HashMap<Integer, String> ageMap = new HashMap<Integer, String>();
		JSONArray sexes = null;
		HashMap<Integer, String> sexMap = new HashMap<Integer, String>();
		JSONArray indicators = null;
		int currentDisplayedIndex = 0;
		
		//First, get general information about the Indicator Description.
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
		ages = makeGetApiCall("Ages/1");
		
		for (int i = 0; i < ages.length(); i++) {
			JSONObject a = ages.getJSONObject(i);
			
			ageMap.put(a.getInt("ID"), a.getString("Name"));
		}
		
		//Get all of the sexes. These will be used later when we are writing the results to the console.
		System.out.println("Getting list of sexes...");
		sexes = makeGetApiCall("Sexes/1");
		
		for (int i = 0; i < sexes.length(); i++) {
			JSONObject s = sexes.getJSONObject(i);
			
			sexMap.put(s.getInt("ID"), s.getString("Name"));
		}
		
		//Now, query for Indicators for the Indicator Description which also map to the Dimension Graphs.
		//  Note that there is no "in" operator, but we can fake it. Have a look at FilterCriterion.toJSON() for more information.
		//  Also note that we are only requesting the first page (1,000 Indicators) of data. In reality we would put this code in
		//    a loop if we wanted to get all of the data.
		System.out.println("Getting a list of Indicators...");
		indicators = makePostApiCall("Indicators/Filter", new Filter()
			.addCriterion("IndicatorDescriptionID", FilterOperator.Equal, _indicatorDescriptionId)
			.addCriterion("DimensionGraphID", FilterOperator.In, dimensionGraphMap.keySet().toArray())
			.toJSON());
		
		//Output some general information about the Indicator Description.
		System.out.println(indicatorDescription.getString("ShortDescription"));
		System.out.println(indicatorDescription.getString("FullDescription"));
		System.out.println();

		//Output the data 10 records at a time until the user "quits".
		do {
			int stop = Math.min(currentDisplayedIndex + 10, indicators.length());

			System.out.println();
			System.out.print(String.format("%-20s", "Age"));
			System.out.print(String.format("%-10s", "Sex"));
			System.out.print(String.format("%-10s", "Value"));
			System.out.print(String.format("%-10s", "SE"));
			System.out.print(String.format("%s", "CI"));
			System.out.println();

			//For each Indicator (value), we find the associated (cached) Dimension Graph and use that to pull out the (cached) Age and Sex names.
			for (int i = currentDisplayedIndex; i <= stop; i++) {
				JSONObject indicator = indicators.getJSONObject(i);
				JSONObject dimensionGraph = dimensionGraphMap.get(indicator.getInt("DimensionGraphID"));
				String ageName = ageMap.get(dimensionGraph.getInt("AgeID"));
				String sexName = sexMap.get(dimensionGraph.getInt("SexID"));
				String value = indicator.getString("FormattedValue");
				String se = indicator.getString("StandardErrorFormatted");
				String ciLow = indicator.getString("ConfidenceIntervalLowFormatted");
				String ciHigh = indicator.getString("ConfidenceIntervalHighFormatted");

				System.out.print(String.format("%-20s", ageName));
				System.out.print(String.format("%-10s", sexName));
				System.out.print(String.format("%-10s", value));
				System.out.print(String.format("%-10s", se));
				System.out.print(String.format("%s-%s", ciLow, ciHigh));
				System.out.println();
				
				currentDisplayedIndex++;
			}
			
			//Check if there is more data to display.
			if ((currentDisplayedIndex != (indicators.length() - 1)))
				System.out.print("Press 'q' to quit, any other key to show more: ");
			else
				break;
			
		} while (System.in.read() != (int)'q');

		System.out.println("Done, press any key to exit.");
		System.in.read();
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