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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Crawler {

    CrawlerDao dao = new DataAccessObject();

    public void run() throws SQLException, IOException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:F:/read-write-files/news", "root", "root");
        String link = null;
        while ((link = dao.getNextLinkThenDelete()) != null) {
            if (dao.isLinkProcessed(link)) {
                continue;
            }

            // 感兴趣的新闻
            if (isInterestingLink(link)) {

                Document document = httpGetAndParseHtml(link);
                //  获得当前页面所有a标签
                ArrayList<Element> aTags = document.select("a");

                parseUrlsFromPageAndInsertIntoDatabase(aTags);

                // 判断是否是新闻页面
                storeIntoDBIfItNewsPage(connection, document, link);
                dao.updateDatabase(link, "insert into LINKS_ALREADY_PROCESSED (LINK) values (?)");

            }
        }
    }

    public void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }


    private void parseUrlsFromPageAndInsertIntoDatabase(ArrayList<Element> aTags) throws SQLException {
        for (Element aTag : aTags) {
            String href = aTag.attr("href");

            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            System.out.println(href);

            if (!href.toLowerCase().startsWith("javascript")) {
                dao.updateDatabase(href, "insert into LINKS_TO_BE_PROCESSED (link) values (?)");

            }

        }
    }


    private void storeIntoDBIfItNewsPage(Connection connection, Document document, String link) throws SQLException {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element article : articleTags) {
//                System.out.println(article.select("h1").text());
                String title = article.select("h1").text();
                String content = article.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));

                dao.insertNewsIntoDatabase(title, content, link);
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
