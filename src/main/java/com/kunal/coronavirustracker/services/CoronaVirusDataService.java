package com.kunal.coronavirustracker.services;

import com.kunal.coronavirustracker.models.LocationStats;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpRetryException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class CoronaVirusDataService {

    private static String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_19-covid-Confirmed.csv";

    private List<LocationStats> allStats = new ArrayList<>();

    @PostConstruct
    @Scheduled(cron = "* * 1 * * *")
    public void fetchVirusData() throws IOException, InterruptedException {

        List<LocationStats> newStats = new ArrayList<>();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VIRUS_DATA_URL))
                .build();


        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        StringReader csvBodyReader = new StringReader(httpResponse.body());
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);



        // Using Streams
        Stream<CSVRecord> recordsStream = StreamSupport.stream(records.spliterator(), false);
        recordsStream
                .map(record -> {
                    LocationStats locationStat = new LocationStats();

                    locationStat.setState(record.get("Province/State"));
                    locationStat.setCountry(record.get("Country/Region"));

                    int latestCases = Integer.parseInt(record.get(record.size() - 1));
                    int prevDayCases = Integer.parseInt(record.get(record.size() - 2));

                    locationStat.setLatestTotalCases(latestCases);
                    locationStat.setDiffFromPrevDay(latestCases - prevDayCases);

                    return locationStat;
                })
                //.peek(System.out::println)
                .forEach(newStats::add);


        // Using for-each loop
//        for (CSVRecord record : records) {
//            LocationStats locationStat = new LocationStats();
//            locationStat.setState(record.get("Province/State"));
//            locationStat.setCountry(record.get("Country/Region"));
//            locationStat.setLatestTotal(Integer.parseInt(record.get(record.size() - 1)));
//
//            System.out.println(locationStat);
//            newStats.add(locationStat);
//        }

        this.allStats = newStats;

    }

    public List<LocationStats> getAllStats() {
        return allStats;
    }
}
