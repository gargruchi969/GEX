package com.energy.exchange.GEX.config;


import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
public class DataSourceConfig {
	
	@Value("${SPRING.DATASOURCE.DRIVER_CLASS_NAME}")
    private String mySqlDatabaseDriverClassName;

    @Value("${SPRING.DATASOURCE.USERNAME}")
    private String mySqlDatabaseUserName;

    @Value("${SPRING.DATASOURCE.PASSWORD}")
    private String mySqlDatabasePassword;

    @Value("${SPRING.DATASOURCE.URL}")
    private String mySqlDatabaseUrl;

    @Value("${SPRING.DATASOURCE.MAXACTIVE}")
    private String maxActive;

    @Value("${SPRING.DATASOURCE.MAXIDLE}")
    private String maxIdle;

    @Value("${SPRING.DATASOURCE.MAXWAIT}")
    private String maxWait;

    @Value("${SPRING.DATASOURCE.TESTONBORROW}")
    private String testOnBorrow;

    @Value("${SPRING.DATASOURCE.TESTONRETURN}")
    private String testOnReturn;

    @Value("${SPRING.DATASOURCE.TESTWHILEIDLE}")
    private String testWhileIdle;

    @Value("${SPRING.DATASOURCE.VALIDATIONQUERY}")
    private String validationQuery;

    @Value("${SPRING.DATASOURCE.TIMEBETWEENEVICTIONRUNSMILLIS}")
    private String timeBetweenEvictionRunsMillis;

    @Value("${SPRING.DATASOURCE.MINEVICTABLEIDLETIMEMILLIS}")
    private String minEvictableIdleTimeMillis;
    
    @Value("${SPRING.DATASOURCE.INITIALSIZE}")
    private Integer initialSize;

	@Bean(name="GEPXDataSource")
	public DataSource gepxDataSource()
	{
		DataSource dataSrc;
		PoolProperties prop = new PoolProperties(); 
        prop.setDriverClassName(mySqlDatabaseDriverClassName);
        prop.setUrl(mySqlDatabaseUrl);
        prop.setUsername(mySqlDatabaseUserName);
        prop.setMaxActive(Integer.parseInt(maxActive));
        prop.setMaxIdle(Integer.parseInt(maxIdle));
        prop.setMaxWait(Integer.parseInt(maxWait));
        prop.setTestOnBorrow(Boolean.parseBoolean(testOnBorrow));
        prop.setTestOnReturn(Boolean.parseBoolean(testOnReturn));
        prop.setTestWhileIdle(Boolean.parseBoolean(testWhileIdle));
        prop.setValidationQuery(validationQuery);
        prop.setTimeBetweenEvictionRunsMillis(Integer.parseInt(timeBetweenEvictionRunsMillis));
        prop.setMinEvictableIdleTimeMillis(Integer.parseInt(minEvictableIdleTimeMillis));
        prop.setPassword(mySqlDatabasePassword);
        prop.setInitialSize(initialSize);
        dataSrc = new DataSource(prop);
		
		return dataSrc;
	}
	
	
	

}
