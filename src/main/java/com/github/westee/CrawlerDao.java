package com.github.westee;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkThenDelete() throws SQLException;

    void updateDatabase(String href, String s) throws SQLException;

    void insertNewsIntoDatabase(String title, String content, String link) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertProcessedLink(String link);

    void insertLinkToBeProcessed(String href);
}
