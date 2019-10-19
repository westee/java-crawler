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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:F:/read-write-files/news", "root", "root");

        while (true) {
            //  待处理链接池  从数据库加载要处理的链接
            List<String> linkPool = loadUrlsFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");

            //  已处理链接池
            if (linkPool.isEmpty()) {
                break;
            }
            String link = linkPool.remove(linkPool.size() - 1);

            // 写一个从数据库删除数据的方法
            insertLinkIntoDataBase(connection, link, "delete from LINKS_TO_BE_PROCESSED where link = ?");
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            // 感兴趣的新闻
            if (isInterestingLink(link)) {

                if (link.startsWith("//")) {
                    link = "https:" + link;
                }

                Document document = httpGetAndParseHtml(link);
                //  获得当前页面所有a标签
                ArrayList<Element> aTags = document.select("a");

                parseUrlsFromPageAndInsertIntoDatabase(connection, aTags);

                // 判断是否是新闻页面
                storeIntoDBIfItNewsPage(document);
                insertLinkIntoDataBase(connection, link, "insert into LINKS_ALREADY_PROCESSED (LINK) values (?)");

            } else {
                continue;
            }

        }

    }

    private static void parseUrlsFromPageAndInsertIntoDatabase(Connection connection, ArrayList<Element> aTags) throws SQLException {
        for (Element aTag : aTags) {
            String href = aTag.attr("href");
            insertLinkIntoDataBase(connection, href, "insert into LINKS_TO_BE_PROCESSED (link) values (?)");
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        // 查看数据看，当前连接是否处理过
        try ( PreparedStatement statement = connection.prepareStatement("select LINK from LINKS_ALREADY_PROCESSED where LINK = ?")){
            statement.setString(1, link );
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                return true;
            }
        }
        return false;
    }

    private static void insertLinkIntoDataBase(Connection connection, String href, String s) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(s)) {
            statement.setString(1, href);
            statement.executeUpdate();
        }
    }

    private static List<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        }

        return results;
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
