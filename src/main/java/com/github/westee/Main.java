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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException, SQLException {
        System.out.println("开始");
        Connection connection = DriverManager.getConnection("jdbc:h2:file:F:/read-write-files/news", "root", "root");

        while (true) {
            //  待处理链接池  从数据库加载要处理的链接
            List<String> linkPool = loadUrlsFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");

            //  已处理链接池
//            Set<String> processedLinks = new HashSet(loadUrlsFromDatabase(connection, "select link from LINKS_ALREADY_PROCESSED"));
            if (linkPool.isEmpty()) {
                break;
            }
            System.out.println("没有break");
            String link = linkPool.remove(linkPool.size() - 1);

            // 写一个从数据库删除数据的方法
            try (PreparedStatement statement = connection.prepareStatement("delete from LINKS_TO_BE_PROCESSED where link = ?")) {
                statement.setString(1, link);
                statement.executeUpdate();
            }

            // 查看数据看，当前连接是否处理过
            boolean flag = false;
            try ( PreparedStatement statement = connection.prepareStatement("select LINK from LINKS_ALREADY_PROCESSED where LINK = ?")){
                statement.setString(1, link );
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()){
                    flag = true;
                }
            }

            if (flag) {
                continue;
            }

            // 感兴趣的新闻
            if (isInterestingLink(link)) {

                if (link.startsWith("//")) {
                    link = "https:" + link;
                }


                Document document = httpGetAndParseHtml(link);
//                    获得当前页面所有a标签
                ArrayList<Element> aTags = document.select("a");

                for (Element aTag : aTags) {
                    String href = aTag.attr("href");
                    linkPool.add(href);
                    try(PreparedStatement statement = connection.prepareStatement("insert into LINKS_TO_BE_PROCESSED (link) values (?)")){
                        statement.setString(1, href);
                        statement.executeUpdate();
                    }

                }

                // 判断是否是新闻页面
                storeIntoDBIfItNewsPage(document);
                try (PreparedStatement statement = connection.prepareStatement("insert into LINKS_ALREADY_PROCESSED (LINK) values (?)")) {
                    statement.setString(1, link);
                    statement.executeUpdate();
                }

            } else {
                continue;
            }

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
