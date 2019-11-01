package com.github.westee;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public class MyBatisCrawlerDao implements CrawlerDao{
    private SqlSessionFactory sqlSessionFactory;
    public MyBatisCrawlerDao() {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e){
            throw new RuntimeException();
        }

    }

    @Override
    public String getNextLinkThenDelete() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            String url = session.selectOne("com.github.westee.MyMapper.selectNextAvailableLink");
            if(url != null){
                session.delete("com.github.westee.MyMapper.deleteLink", url);
            }
            return url;
        }
    }

    @Override
    public void updateDatabase(String href, String s) throws SQLException {

    }

    @Override
    public void insertNewsIntoDatabase(String title, String content, String link) throws SQLException {

    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        return false;
    }
}
