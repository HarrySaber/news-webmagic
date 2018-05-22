package module;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.*;
import com.google.inject.name.Names;
import mapper.UserMapper;
import org.mybatis.guice.MyBatisModule;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import com.google.common.collect.Maps.EntryTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.typesafe.config.Config;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * SpMybatisModule
 *
 * @author D.jin
 * @date 2018/5/22
 */
public class SpMybatisModule extends AbstractModule {

    final static Logger logger = LoggerFactory.getLogger(SpMybatisModule.class);
    final static TypeLiteral<Set<String>> dbnamesType = new TypeLiteral<Set<String>>() {
    };


    final Config appconfig;
    final Map<String, Properties> configs;
    final Map<String, List<Class<?>>> mappers;
    final Map<String, Module> modules;


    public SpMybatisModule(Config cfg) {
        this.appconfig = cfg;
        this.configs = Maps.newHashMap();
        this.mappers = Maps.newHashMap();
        this.modules = Maps.newHashMap();
    }

    protected void configMappers(Config mybatisConf) {
        addMapperClass("bdm321280692_db", UserMapper.class);
    }

    @Override
    protected void configure() {

        Config mybatisConf = getConfig(appconfig, "mybatis");
        logger.info(mybatisConf.toString());

        if (mybatisConf == null) {
            return;
        }
        //configDatabases(mybatisConf);
        configMappers(mybatisConf);
    }

    /**
     * Register Mapper Class to the database identified by name. This should be
     * called before initialize() method. Call this method during
     * <code>processConfiguration</code>.
     *
     * @param dbname
     * @param mapper
     */
    public void addMapperClass(String dbname, Class<?> mapper) {
        if (!mappers.containsKey(dbname)) {
            mappers.put(dbname, Lists.newArrayList());
        }
        mappers.get(dbname).add(mapper);
        if (!configs.containsKey(dbname)) {
            logger.error("Registering to a non-existence DB: {}", dbname);
        }
    }

    protected void configDatabases(Config mybatisConf) {

        Set<String> dbNames = FluentIterable.from(mybatisConf.entrySet())
                .transform(entry -> entry.getKey().split("\\.")[0]).toSet();
        logger.debug("MyBatis DB Names: {}", dbNames);
        Map<String, Properties> dbs = Maps.toMap(dbNames, key -> {
            logger.debug("Name: {}", key);
            Config singleConf = getConfig(mybatisConf, key);
            String driver = singleConf.getString("driver");
            String url = singleConf.getString("url");
            String user = singleConf.getString("user");
            String password = singleConf.getString("password");
            String poolSize = getString(singleConf, "poolsize");
            if (poolSize == null || poolSize.isEmpty()) {
                poolSize = "10";
            }
            logger.debug("Driver: {}", driver);
            logger.debug("URL: {}", url);

            Properties prop = new Properties();
            prop.setProperty("JDBC.driver", driver);
            prop.setProperty("JDBC.url", url);
            prop.setProperty("JDBC.username", user);
            prop.setProperty("JDBC.password", password);
            prop.setProperty("JDBC.autoCommit", "true");
            prop.setProperty("hikari.connectionTestStatement", "SELECT 1");
            prop.setProperty("hikari.maxPoolSize", poolSize);
            return prop;
        });

        configs.putAll(dbs);
    }

    protected void configModules(Config mybatisConf) {
        final int no_of_dbs = configs.size();

        modules.putAll(Maps.transformEntries(configs, new EntryTransformer<String, Properties, Module>() {
            @Override
            public Module transformEntry(final String name, final Properties config) {
                MyBatisModule mybatisModule = new MyBatisModule() {
                    @Override
                    protected void initialize() {
                        environmentId("STARPOST");
                        mapUnderscoreToCamelCase(true);
                        bindDataSourceProviderType(HikariDataSourceProvider.class);
                        bindTransactionFactoryType(JdbcTransactionFactory.class);
                        if (mappers.get(name) != null) {
                            for (Class<?> clazz : mappers.get(name)) {
                                addMapperClass(clazz);
                            }
                        }
                    }
                };
                if (no_of_dbs == 1) {
                    logger.info("Only one DB defined, Transactional is working");
                    return new AbstractModule() {
                        @Override
                        protected void configure() {
                            install(mybatisModule);
                            Names.bindProperties(this.binder(), config);
                            bind(SqlSessionFactory.class).annotatedWith(Names.named(name)).to(SqlSessionFactory.class)
                                    .in(Scopes.SINGLETON);
                            bind(DataSource.class).annotatedWith(Names.named(name)).to(DataSource.class)
                                    .in(Scopes.SINGLETON);
                        }
                    };
                } else {
                    logger.warn("More than one DB defined, Transactional is NOT working");
                    return new PrivateModule() {
                        @Override
                        protected void configure() {
                            install(mybatisModule);
                            Names.bindProperties(this.binder(), config);
                            if (mappers.get(name) != null) {
                                for (Class<?> clazz : mappers.get(name)) {
                                    expose(clazz);
                                }
                            }
                            bind(SqlSessionFactory.class).annotatedWith(Names.named(name)).to(SqlSessionFactory.class)
                                    .in(Scopes.SINGLETON);
                            bind(DataSource.class).annotatedWith(Names.named(name)).to(DataSource.class)
                                    .in(Scopes.SINGLETON);
                            expose(SqlSessionFactory.class).annotatedWith(Names.named(name));
                            expose(DataSource.class).annotatedWith(Names.named(name));
                        }
                    };
                }
            }
        }));
    }

    private Config getConfig(Config c, String key) {
        if (c.hasPath(key)) {
            return c.getConfig(key);
        }
        return null;
    }

    private String getString(Config c, String key) {
        if (c.hasPath(key)) {
            return c.getString(key);
        }
        return null;
    }
}
