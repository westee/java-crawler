package com.github.westee;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
//        待处理链接池
        ArrayList<String> linkPool = new ArrayList<>();
//        已处理链接池
        Set<String> processedLinks = new HashSet();
        linkPool.add("https://sina.cn");

        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }

            String link = linkPool.remove(linkPool.size() - 1);

            if (processedLinks.contains(link)) {
                continue;
            }

            if (link.contains("sina.cn") && !link.contains("passport.sina.cn") && (link.contains("news.sina.cn") || "https://sina.cn".equals(link) )) {
                // 感兴趣的新闻

                if(link.startsWith("//")){
                    link = "https:" + link;
                }
                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(link);
                httpGet.addHeader("User-Agent","Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Mobile Safari/537.36");
                try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                    HttpEntity entity = response.getEntity();
                    String html = EntityUtils.toString(entity);
                    Document document = Jsoup.parse(html);

//                    获得当前页面所有a标签
                    ArrayList<Element> aTags = document.select("a");

                    // 获得a标签的所有href
                    for (Element aTag : aTags) {
                        linkPool.add(aTag.attr("href"));
                    }

                    // 判断是否是新闻页面
                    ArrayList<Element> titles = document.select("article");
                    if (!titles.isEmpty()) {
                        for (Element title: titles){
                            System.out.println(title.select("h1").text());

                        }
                    }
                    processedLinks.add(link);
                }
            } else {
                continue;
            }

        }


    }


}
