package do_mysql;

import java.sql.PreparedStatement;

import data_objects.drivers.AbstractDriverDefinition;
import java.util.Properties;

public class MySqlDriverDefinition extends AbstractDriverDefinition {

    @Override
    public boolean supportsJdbcGeneratedKeys()
    {
        return true;
    }

    @Override
    public boolean supportsJdbcScrollableResultSets()
    {
        return true;
    }

    @Override
    public boolean supportsConnectionEncodings()
    {
        return true;
    }

    @Override
    public boolean supportsCalendarsInJDBCPreparedStatement() {
        return false;
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        Properties props = new Properties();
        props.put("useUnicode", "yes");
        return props;
    }

    @Override
    public void setEncodingProperty(Properties props, String encodingName) {
        if ("latin1".equals(encodingName)) {
            // example of mapping encoding name to Java-Style character
            // encoding name (see http://dev.mysql.com/doc/refman/5.1/en/connector-j-reference-charsets.html)
            encodingName = "ISO8859_1";
        }
        props.put("characterEncoding", encodingName);
    }

    @Override
    public String quoteString(String str) {
        StringBuffer quotedValue = new StringBuffer(str.length() + 2);
        quotedValue.append("\'");
        quotedValue.append(str.replaceAll("'", "\\\\'"));
        // TODO: handle backslashes
        quotedValue.append("\'");
        return quotedValue.toString();
    }

    @Override
    public String toString(PreparedStatement ps) {
        return ps.toString().replaceFirst(".*].-\\s*", "");
    }

}
