package com.github.westee;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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

            if (isInterestingLink(link)) {
                // 感兴趣的新闻

                if (link.startsWith("//")) {
                    link = "https:" + link;
                }


                Document document = httpGetAndParseHtml(link);
//                    获得当前页面所有a标签
                ArrayList<Element> aTags = document.select("a");

                aTags.stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);

                // 判断是否是新闻页面
                storeIntoDBIfItNewsPage(document);
                processedLinks.add(link);

            } else {
                continue;
            }

        }


    }

    private static void storeIntoDBIfItNewsPage(Document document) {
        ArrayList<Element> titles = document.select("article");
        if (!titles.isEmpty()) {
            for (Element title : titles) {
                System.out.println(title.select("h1").text());
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Mobile Safari/537.36");
        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return isLoginPage(link) && (isNewsPage(link) || isIndexPage(link));

    }

    private static boolean isLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return (link.contains("news.sina.cn"));
    }


}
