package com.geminit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class GetPageTest {
    public static void main(String[] args) throws Exception {
        String url = "https://flights.ctrip.com/itinerary/oneway/sha-bjs?date=2019-08-21";

        System.setProperty("webdriver.chrome.driver", "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        webDriver.get(url);//写入你要抓取的网址
        Thread.sleep(10000);

        System.out.println(webDriver.getPageSource());
        webDriver.close();//必须关闭资源
    }
}
