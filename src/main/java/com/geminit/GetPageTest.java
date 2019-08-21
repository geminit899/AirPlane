package com.geminit;

import org.jsoup.Jsoup;
import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class GetPageTest {
    public static void main(String[] args) throws Exception {
        String[] city = {"SHA", "BJS", "CTU", "WUH"};

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        for (int i = 0; i <= 60; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(today);
            cal.add(Calendar.DATE, i);
            Date theDate = cal.getTime();
            String time = format.format(theDate);

            for (String from : city) {
                for (String to : city) {
                    if (to == from) {
                        continue;
                    }
                    getLine(from, to, time);
                }
            }
        }

    }

    private static void getLine(String fromCity, String toCity, String time) throws Exception {
        String url = "https://flights.ctrip.com/itinerary/oneway/" + fromCity + "-" + toCity + "?date=" + time;

        System.setProperty("webdriver.chrome.driver", "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        webDriver.get(url);//写入你要抓取的网址
        Thread.sleep(5000);

        Document document = Jsoup.parse(webDriver.getPageSource());
        Element searchBody = document.getElementsByClass("searching-body").first();
        String from = searchBody.getElementsByClass("dcity").first().text();
        String to = searchBody.getElementsByClass("acity").first().text();
        Elements elements = document.getElementsByClass("search_table_header");
        int size = elements.size();

        System.out.println(time + "从" + from + "到" + to + "有" + size + "条航班");
        webDriver.close();//必须关闭资源
    }
}
