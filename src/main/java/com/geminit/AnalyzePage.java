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
            "landingTime, landingCity, landingAirport, price) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', d%)";
    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final long perPageTime = 5000;

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
        MysqlConnect.conn("localhost", "AirPlane", "root", "hack");
        for (int i = 0; i < lines.size(); i++) {
            String fromCity = lines.get(i)._1();
            String toCity = lines.get(i)._2();

            Calendar cal = Calendar.getInstance();
            cal.setTime(this.fromDate);
            while (true) {
                Date date = cal.getTime();
                if (date.after(toDate)) {
                    break;
                } else {
                    Document document = getPage(fromCity, toCity, FORMAT.format(date));
                    dealOnePage(document, fromCity, toCity);
                    cal.add(Calendar.DATE, 1);
                }
            }
        }
        MysqlConnect.deconn();
    }

    /**
     *  document : 页面内容
     */
    private void dealOnePage(Document document, String fromCity, String toCity) {
        Elements allPlanes = document.getElementsByClass("search_table_header");
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

                String sql = String.format(insertFormat, airlines, flight, takeoffTime, cityName.get(fromCity), takeoffAirport,
                        landingTime, cityName.get(toCity), landingAirport, Integer.parseInt(price));

                MysqlConnect.executeIDUSQL(sql);
            }
        });
    }

    /**
     *  fromCity、toCity：城市简称
     *  date : 2019-10-01
     */
    private Document getPage(String fromCity, String toCity, String date) {
        String url = "https://flights.ctrip.com/itinerary/oneway/" + fromCity + "-" + toCity + "?date=" + date;

        System.setProperty("webdriver.chrome.driver", AirPlane.CHROME_DRIVER);
        WebDriver webDriver = new ChromeDriver();
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

        Document document = Jsoup.parse(webDriver.getPageSource());
        webDriver.close();//必须关闭资源


        return document;
    }
}
