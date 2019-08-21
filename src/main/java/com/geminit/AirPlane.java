package com.geminit;

import scala.Tuple2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AirPlane {
    private static final int THREAD_NUM = 8;
    public static final String CHROME_DRIVER = "/Applications/Google Chrome.app/Contents/MacOS/chromedriver";

    private static String FROM_DATE = "2019-09-30";
    private static String TO_DATE = "2019-10-10";

    private static Map<String, String> getCityName() {
        Map<String, String> cityName = new HashMap<>();

        MysqlConnect.conn("localhost", "AirPlane", "root", "hack");
        try {
            ResultSet resultSet = MysqlConnect.executeSSQL("Select * from CityName");
            while (resultSet.next()) {
                String short_name = resultSet.getString("short_name");
                String city_name = resultSet.getString("city_name");
                cityName.put(short_name, city_name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        MysqlConnect.deconn();
        return cityName;
    }


    public static void main(String[] args) {
        Map<String, String> cityName = getCityName();
        Set<String> citySet = cityName.keySet();

        List<Tuple2<String, String>> lines = new ArrayList<>();

        int perLine = ((citySet.size() - 1) * (citySet.size() - 1)) / THREAD_NUM;
        int count = perLine;

        for (String fromCity : citySet) {
            for (String toCity : citySet) {
                if (fromCity == toCity) {
                    continue;
                }

                lines.add(new Tuple2<>(fromCity, toCity));
                count--;

                if (count == 0) {
                    Thread thread = new AnalyzePage(FROM_DATE, TO_DATE, cityName, lines);
                    thread.start();
                    lines = new ArrayList<>();
                    count = perLine;
                }
            }
        }
        if (lines.size() > 0) {
            Thread thread = new AnalyzePage(FROM_DATE, TO_DATE, cityName, lines);
            thread.start();
        }
    }
}
