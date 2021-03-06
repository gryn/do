package data_objects;

import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;


import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyTime;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Additional Utilities for DataObjects JDBC Drivers
 *
 * @author alexbcoles
 */
public final class DataObjectsUtils {

    private static final DateFormat utilDateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private static final DateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private static final GregorianCalendar gregCalendar = new GregorianCalendar();

    /**
     * Create a driver Error
     *
     * @param runtime
     * @param errorName
     * @param message
     * @return
     */
    public static RaiseException newDriverError(Ruby runtime, String errorName,
            String message) {
        RubyClass driverError = runtime.getClass(errorName);
        return new RaiseException(runtime, driverError, message, true);
    }

    /**
     * Create a driver Error from a java.sql.SQLException
     *
     * @param runtime
     * @param errorName
     * @param exception
     * @return
     */
    public static RaiseException newDriverError(Ruby runtime, String errorName,
            SQLException exception) {
        return newDriverError(runtime, errorName, exception, null);
    }

    /**
     * Create a driver Error from an java.sql.SQLException, that displays the
     * sql statement/query.
     *
     * @param runtime
     * @param errorName
     * @param exception
     * @param statement
     * @return
     */
    public static RaiseException newDriverError(Ruby runtime, String errorName,
            SQLException exception, java.sql.Statement statement)
    {
        RubyClass driverError = runtime.getClass(errorName);
        int code = exception.getErrorCode();
        StringBuffer sb = new StringBuffer("(");

        // Append the Vendor Code, if there is one
        // TODO: parse vendor exception codes
        // TODO: replace 'vendor' with vendor name
        if (code > 0) sb.append("vendor_errno=").append(code).append(", ");
        sb.append("sql_state=").append(exception.getSQLState()).append(") ");
        sb.append(exception.getLocalizedMessage());
        // TODO: delegate to the DriverDefinition for this
        if (statement != null) sb.append("\nQuery: ").append(statement.toString());

        return new RaiseException(runtime, driverError, sb.toString(), true);
    }

    // STOLEN FROM AR-JDBC
    static java.sql.Connection getConnection(IRubyObject recv) {
        java.sql.Connection conn = (java.sql.Connection) recv.dataGetStruct();
        return conn;
    }

    /**
     * Converts a JDBC Type to a Ruby Type
     *
     * @param type
     * @param scale
     * @return
     */
    public static RubyType jdbcTypeToRubyType(int type, int scale) {
        RubyType primitiveType;
        switch (type) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                primitiveType = RubyType.FIXNUM;
                break;
            case Types.BIGINT:
                primitiveType = RubyType.BIGNUM;
                break;
            case Types.BIT:
            case Types.BOOLEAN:
                primitiveType = RubyType.TRUE_CLASS;
                break;
            case Types.CHAR:
            case Types.VARCHAR:
                primitiveType = RubyType.STRING;
                break;
            case Types.DATE:
                primitiveType = RubyType.DATE;
                break;
            case Types.TIMESTAMP:
                primitiveType = RubyType.DATE_TIME;
                break;
            case Types.TIME:
                primitiveType = RubyType.TIME;
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
                primitiveType = RubyType.BIG_DECIMAL;
                break;
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
                primitiveType = RubyType.FLOAT;
                break;
            case Types.BLOB:
            case Types.JAVA_OBJECT: // XXX: Not sure this should be here
            case Types.VARBINARY:
            case Types.BINARY:
            case Types.LONGVARBINARY:
                primitiveType = RubyType.BYTE_ARRAY;
                break;
            case Types.NULL:
                primitiveType = RubyType.NIL;
                break;
            default:
                primitiveType = RubyType.STRING;
        }
        // No casting rule for type #{meta_data.column_type(i)} (#{meta_data.column_name(i)}). Please report this."
        return primitiveType;
    }

    public static java.sql.Date utilDateToSqlDate(java.util.Date uDate) throws ParseException {
        return java.sql.Date.valueOf(sqlDateFormatter.format(uDate));
    }

    public static java.util.Date sqlDateToutilDate(java.sql.Date sDate) throws ParseException {
        return (java.util.Date) utilDateFormatter.parse(utilDateFormatter.format(sDate));
    }

    public static IRubyObject parse_date(Ruby runtime, Date dt) {
        RubyTime time = RubyTime.newTime(runtime, dt.getTime());
        time.extend(new IRubyObject[] {runtime.getModule("DateFormatter")});
        return time;
    }

    public static IRubyObject parse_date_time(Ruby runtime, Timestamp ts) {
        RubyTime time = RubyTime.newTime(runtime, ts.getTime());
        time.extend(new IRubyObject[] {runtime.getModule("DatetimeFormatter")});
        return time;
    }

    public static IRubyObject parse_time(Ruby runtime, Time tm) {
        RubyTime time = RubyTime.newTime(runtime, tm.getTime());
        time.extend(new IRubyObject[] {runtime.getModule("TimeFormatter")});
        return (time.getUSec() != 0) ? time : runtime.getNil();
    }

    public static IRubyObject prepareRubyDateFromSqlDate(Ruby runtime,Date date){

       if (date.getTime() == 0) {
           return runtime.getNil();
       }

       gregCalendar.setTime(date);
       int month = gregCalendar.get(Calendar.MONTH);
       month++; // In Calendar January == 0, etc...
       RubyClass klazz = runtime.fastGetClass("Date");
       return klazz.callMethod(
                 runtime.getCurrentContext() ,"civil",
                 new IRubyObject []{ runtime.newFixnum(gregCalendar.get(Calendar.YEAR)),
                 runtime.newFixnum(month),
                 runtime.newFixnum(gregCalendar.get(Calendar.DAY_OF_MONTH))});
    }

    public static IRubyObject prepareRubyDateTimeFromSqlTimestamp(Ruby runtime,Timestamp stamp){

       if (stamp.getTime() == 0) {
           return runtime.getNil();
       }

       gregCalendar.setTime(stamp);
       int month = gregCalendar.get(Calendar.MONTH);
       month++; // In Calendar January == 0, etc...

       int zoneOffset = gregCalendar.get(Calendar.ZONE_OFFSET)/3600000;
       RubyClass klazz = runtime.fastGetClass("DateTime");

       IRubyObject rbOffset = runtime.fastGetClass("Rational")
                .callMethod(runtime.getCurrentContext(), "new",new IRubyObject[]{
            runtime.newFixnum(zoneOffset),runtime.newFixnum(24)
        });

       return klazz.callMethod(runtime.getCurrentContext() , "civil",
                 new IRubyObject []{runtime.newFixnum(gregCalendar.get(Calendar.YEAR)),
                 runtime.newFixnum(month),
                 runtime.newFixnum(gregCalendar.get(Calendar.DAY_OF_MONTH)),
                 runtime.newFixnum(gregCalendar.get(Calendar.HOUR_OF_DAY)),
                 runtime.newFixnum(gregCalendar.get(Calendar.MINUTE)),
                 runtime.newFixnum(gregCalendar.get(Calendar.SECOND)),
                 rbOffset});
    }

    public static IRubyObject prepareRubyTimeFromSqlTime(Ruby runtime, Time time) {

        if (time.getTime() + 3600000 == 0) {
            return runtime.getNil();
        }

        RubyTime rbTime = RubyTime.newTime(runtime, time.getTime());
        rbTime.extend(new IRubyObject[]{runtime.getModule("TimeFormatter")});
        return rbTime;
        // SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss"); // TODO proper format?
        // return runtime.newString(sdf.format(rbTime.getJavaDate()));
    }

    public static IRubyObject prepareRubyTimeFromSqlDate(Ruby runtime, Date date) {

        if (date.getTime() + 3600000 == 0) {
            return runtime.getNil();
        }
        RubyTime rbTime = RubyTime.newTime(runtime, date.getTime());
        rbTime.extend(new IRubyObject[]{runtime.getModule("TimeFormatter")});
        return rbTime;
        // SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss"); // TODO proper format?
        // return runtime.newString(sdf.format(rbTime.getJavaDate()));
    }

    public static String stringOrNull(IRubyObject obj) {
        return (!obj.isNil()) ? obj.asJavaString() : null;
    }

    public static int intOrMinusOne(IRubyObject obj) {
        return (!obj.isNil()) ? RubyFixnum.fix2int(obj) : -1;
    }

    public static Integer integerOrNull(IRubyObject obj) {
        return (!obj.isNil()) ? RubyFixnum.fix2int(obj) : null;
    }

    /**
     * Convert a map of key/values to a URI query string
     *
     * @param map
     * @return
     * @throws java.io.UnsupportedEncodingException
     */
    public static String mapToQueryString(Map<Object, Object> map)
            throws UnsupportedEncodingException {
        Iterator it = map.entrySet().iterator();
        StringBuffer querySb = new StringBuffer();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            String key = (pairs.getKey() != null) ? pairs.getKey().toString() : "";
            String value = (pairs.getValue() != null) ? pairs.getValue().toString() : "";
            querySb.append(java.net.URLEncoder.encode(key, "UTF-8")).append("=");
            querySb.append(java.net.URLEncoder.encode(value, "UTF-8"));
        }
        return querySb.toString();
    }

    /**
     * Convert a query string (e.g. driver=org.postgresql.Driver&protocol=postgresql)
     * to a Map of values.
     *
     * @param query
     * @return
     */
    public static Map<String, String> parseQueryString(String query)
            throws UnsupportedEncodingException {
        if (query == null) {
            return null;
        }
        Map<String, String> nameValuePairs = new HashMap<String, String>();
        StringTokenizer stz = new StringTokenizer(query, "&");

        // Tokenize at and for name / value pairs
        while (stz.hasMoreTokens()) {
            String nameValueToken = stz.nextToken();
            // Split at = to split the pairs
            int i = nameValueToken.indexOf("=");
            String name = nameValueToken.substring(0, i);
            String value = nameValueToken.substring(i + 1);
            // Name and value should be URL decoded
            name = java.net.URLDecoder.decode(name, "UTF-8");
            value = java.net.URLDecoder.decode(value, "UTF-8");
            nameValuePairs.put(name, value);
        }

        return nameValuePairs;
    }

    // private constructor
    private DataObjectsUtils() {
    }
}
