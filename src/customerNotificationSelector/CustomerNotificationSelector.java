package customerNotificationSelector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class CustomerNotificationSelector {

	public static void main(String[] args) {
		URL url;
		try {
			// Setup HTTP connection
			url = new URL(
					"http://api.openweathermap.org/data/2.5/forecast?q=minneapolis,us&units=imperial&APPID=09110e603c1d5c272f94f64305c09436");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			JSONTokener tokener = new JSONTokener(url.openStream());
			JSONObject root = new JSONObject(tokener);
			JSONArray weatherDataList = root.getJSONArray("list");

			// Average temp for each day
			double aveTemp = 0;

			// Keeps track of the current and previous dates for the data points
			LocalDate previousLocalDate = null;
			LocalDate currentLocalDate = null;

			// Counts how many data points for the day
			int dataPointCount = 0;

			// Booleans to determine clear skies, or if there was rain or snow
			boolean snowOrRain = false;
			boolean sunny = true;
			for (int i = 0; i < weatherDataList.length(); i++) {
				try {

					// Gets the first JSON object in the list, and parses out the date for that data point
					JSONObject dataPoint = weatherDataList.getJSONObject(i);
					String dataPointDateString = dataPoint.getString("dt_txt");
					Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(dataPointDateString);
					previousLocalDate = currentLocalDate;
					currentLocalDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

					if (i == 0 || previousLocalDate.getDayOfMonth() == currentLocalDate.getDayOfMonth()) {

						// Add the average temperature to the average
						aveTemp += dataPoint.getJSONObject("main").getDouble("temp");
						dataPointCount++;

						// If if was snowing or raining at this point make note of it
						if (dataPoint.getJSONArray("weather").getJSONObject(0).getString("main").equalsIgnoreCase("Snow")
								|| dataPoint.getJSONArray("weather").getJSONObject(0).getString("main").equalsIgnoreCase("Rain")) {
							snowOrRain = true;
							sunny = false;
						}

						// If it wasn't clear all day it wasn't sunny
						if (!dataPoint.getJSONArray("weather").getJSONObject(0).getString("main").equalsIgnoreCase("Clear")) {
							sunny = false;
						}

						// We've hit a new day, print the stats, and reset the aveTemp and dataPointCount variables.
					} else {

						printDayStats(aveTemp, dataPointCount, sunny, snowOrRain, previousLocalDate);

						// Set the aveTemp and count to the new data point value
						aveTemp = dataPoint.getJSONObject("main").getDouble("temp");
						dataPointCount = 1;
					}
				} catch (ParseException e) {
					System.out.println("Unable to successfully parse date from JSON Object");
					e.printStackTrace();
				}

			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// This is a helper function that prints out that day's statistics and which method should be used to contact the customer.
	private static void printDayStats(double aveTemp, int dataPointCount, boolean sunny, boolean snowOrRain,
			LocalDate previousLocalDate) {

		aveTemp = Math.floor(aveTemp / dataPointCount);
		System.out.println("Average temp on " + previousLocalDate + " will be " + aveTemp + " degrees Fahrenheit");

		// If it's raining (Or snowing, not specified) at any point throughout the day,
		// Or if the temp was less than 55 degrees, do a phone call
		if (snowOrRain || aveTemp < 55) {
			String reason = " temperature was less than 55 degrees.";
			if (snowOrRain) {
				reason = " of snow or rainfall.";
			}
			System.out.println("Phone call for customer because" + reason);
			System.out.println();
			snowOrRain = false;
			sunny = true;

			// If it was clear skies all day, and the temp was greater than 75 degrees,
			// Text the customer
		} else if (sunny && aveTemp > 75) {
			System.out.println("Text the customer because it is sunny.");
			System.out.println();

			// If the ave temp was in-between 55 and 75 (inclusive),it didn't rain, and it wasn't sunny,
			// Email the customer.
		} else if (aveTemp >= 55 && aveTemp <= 75) {
			String reason = " the temp is inbetween 55 and 75 degrees.";
			if (!sunny) {
				reason = " the temp is greater than 75 and it is cloudy.";
			}
			System.out.println("Email the customer, because" + reason);
			System.out.println();
			// For cases not defined by the exercise, such as > 75 degrees and cloudy.
		} else {
			System.out.println("No prefered method for these parameters.");
		}
	}

}
