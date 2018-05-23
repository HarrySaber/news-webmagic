package module;


import com.typesafe.config.Config;
import mapper.UserMapper;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * MybatisFactory
 *
 * @author D.jin
 * @date 2018/5/22
 */
public class MybatisFactory {

    static Logger logger = LoggerFactory.getLogger(MybatisFactory.class);

    static MybatisFactory factory = new MybatisFactory();

    public MybatisFactory() {

    }

    public SqlSession init(Config config) {
        if (config.hasPath("mybatis")) {
            try {
                TransactionFactory transactionFactory = new JdbcTransactionFactory();
                String driver = config.getString("mybatis.news.driver");
                String password = config.getString("mybatis.news.password");
                String url = config.getString("mybatis.news.url");
                String username = config.getString("mybatis.news.user");
                DataSource dataSource =new PooledDataSource(driver,url,username,password);
                Environment environment = new Environment("development", transactionFactory, dataSource);
                Configuration configuration = new Configuration(environment);
                configuration.addMapper(UserMapper.class);
                SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
                System.out.println(sqlSessionFactory);
                SqlSession session = sqlSessionFactory.openSession();
                return  session;
           /*     User user = new User();
                user.setId("6");
                session.insert("getUserById", user);
                session.commit();
                session.close();*/
                //logger.info("sqlSessionFactory success");
            } catch (Exception e) {
                logger.error("MybatisFactory erro!", e);
            }
        }
        return  null;
    }

    public static MybatisFactory getFactory() {
        return factory;
    }
}
