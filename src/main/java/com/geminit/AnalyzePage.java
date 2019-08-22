package com.geminit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import scala.Tuple2;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AnalyzePage extends Thread {
    private Date fromDate;
    private Date toDate;
    private Map<String, String> cityName;
    private List<Tuple2<String, String>> lines;
    private final String insertFormat = "INSERT INTO XCAirline (airlines, flight, takeoffTime, takeoffCity, takeoffAirport, " +
            "landingTime, landingCity, landingAirport, price, date) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', %d, '%s')";
    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final long perPageTime = 10000;

    public AnalyzePage(String fromDate, String toDate, Map<String, String> cityName, List<Tuple2<String, String>> lines) {
        this.fromDate = new Date();
        this.toDate = new Date();
        try {
            this.fromDate = FORMAT.parse(fromDate);
            this.toDate = FORMAT.parse(toDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.cityName = cityName;
        this.lines = lines;
    }

    public void run() {
        for (int i = 0; i < lines.size(); i++) {
            MysqlConnect.conn("localhost", "AirPlane", "root", "hack");
            String fromCity = lines.get(i)._1();
            String toCity = lines.get(i)._2();

            Calendar cal = Calendar.getInstance();
            cal.setTime(this.fromDate);
            while (true) {
                Date date = cal.getTime();
                if (date.after(toDate)) {
                    break;
                } else {
                    dealOnePage(fromCity, toCity, FORMAT.format(date));
                    cal.add(Calendar.DATE, 1);
                }
            }
            MysqlConnect.deconn();
        }
    }

    /**
     *  fromCity、toCity：城市简称
     *  date : 2019-10-01
     */
    private void dealOnePage(String fromCity, String toCity, String date) {
        Elements allPlanes = null;
        String url = "https://flights.ctrip.com/itinerary/oneway/" + fromCity + "-" + toCity + "?date=" + date;

        System.setProperty("webdriver.chrome.driver", AirPlane.CHROME_DRIVER);
        WebDriver webDriver = new ChromeDriver();

        for (int i = 0; i < 10; i++) {
            webDriver.get(url); //写入你要抓取的网址

            try {
                Thread.sleep(perPageTime);
            } catch (Exception exception) {
                try {
                    Thread.sleep(perPageTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String source = webDriver.getPageSource();
            Document document = Jsoup.parse(source);
            allPlanes = document.select("div.search_table_header");

            if (allPlanes.size() > 0) {
                break;
            } else if (allPlanes.size() == 0) {
                allPlanes = document.select("div.search_transfer_header");
                if (allPlanes.size() > 0) {
                    break;
                }
                if (source.contains("class=\"base_alert11\"") && source.contains("很抱歉，您搜索的")) {
                    break;
                }
            }
        }

        webDriver.close();//必须关闭资源
        allPlanes.stream().forEach(new Consumer<Element>() {
            @Override
            public void accept(Element plane) {
                Element inblogo = plane.select("div.inb.logo").first();
                Element logoElement = inblogo.select("img.pubFlights-logo").first();
                Element idElement = logoElement.parent().parent();
                String airlines = idElement.select("strong").first().text();
                String flight = idElement.select("span").last().text();

                Element inbright = plane.select("div.inb.right").first();
                String takeoffTime = inbright.select("strong.time").first().text();
                String takeoffAirport = inbright.select("div.airport").first().text();

                Element inbLeft = plane.select("div.inb.right").first();
                String landingTime = inbLeft.select("strong.time").first().text();
                String landingAirport = inbLeft.select("div.airport").first().text();

                Element inbPrice = plane.select("div.inb.price").first();
                String price = inbPrice.select("span.base_price02").first().text();
                price = price.substring(1, price.length());

                String fromCityName = cityName.get(fromCity);
                String toCityName = cityName.get(toCity);
                int priceInt = Integer.parseInt(price);

                String sql = String.format(insertFormat, airlines, flight, takeoffTime, fromCityName, takeoffAirport,
                        landingTime, toCityName, landingAirport, priceInt, date);

                MysqlConnect.executeIDUSQL(sql);
            }
        });
    }
}
