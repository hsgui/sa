package cn.bupt.bnrc.mining.weibo.script;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;

public class DataSourceFactory {

	public static enum DATASOURCEType {
		localHostMysql,
		remoteSqlserver,
		SQLSERVER_TOPICDATA_TIAN,
		SQLSERVER_STATUSES_TIAN
	};

	private static String SQLServerDriverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static String MySQLDriverName = "org.gjt.mm.mysql.Driver";
	
	private static Map<DATASOURCEType, BasicDataSource> sources = new HashMap<DATASOURCEType, BasicDataSource>();

	public static BasicDataSource getDataSource(DATASOURCEType type) {
		if (sources.get(type) == null) {
			BasicDataSource dataSource = new BasicDataSource();
			switch (type) {
			case localHostMysql:
				dataSource.setDriverClassName(MySQLDriverName);
				dataSource.setUrl("jdbc:mysql://localhost:3306/statuses?useUnicode=true&amp;characterEncoding=UTF-8");
				dataSource.setUsername("root");
				dataSource.setPassword("bnrc");
				break;
			case remoteSqlserver:
				dataSource.setDriverClassName(SQLServerDriverName);
				dataSource.setUrl("jdbc:sqlserver://10.108.96.126:1433;DatabaseName=temp;useUnicode=true&amp;characterEncoding=UTF-8");
				dataSource.setUsername("sa");
				dataSource.setPassword("bnrc609");
				break;
			case SQLSERVER_TOPICDATA_TIAN:
				dataSource.setDriverClassName(SQLServerDriverName);
				dataSource.setUrl("jdbc:sqlserver://10.108.96.126:1433;DatabaseName=SinaWeiboTopicData;useUnicode=true&amp;characterEncoding=UTF-8");
				dataSource.setUsername("sa");
				dataSource.setPassword("bnrc609");
				break;
			case SQLSERVER_STATUSES_TIAN:
				dataSource.setDriverClassName(SQLServerDriverName);
				dataSource.setUrl("jdbc:sqlserver://10.108.96.126:1433;DatabaseName=SinaWeiboTopicData;useUnicode=true&amp;characterEncoding=UTF-8");
				dataSource.setUsername("sa");
				dataSource.setPassword("bnrc609");
			}
			
			dataSource.setInitialSize(5);
			dataSource.setMaxActive(50);
			dataSource.setMaxIdle(5);
			dataSource.setMinIdle(1);
			
			sources.put(type, dataSource);
		}
		
		return sources.get(type);
	}
}
