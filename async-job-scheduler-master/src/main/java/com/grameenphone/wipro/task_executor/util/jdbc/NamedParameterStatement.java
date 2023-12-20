package com.grameenphone.wipro.task_executor.util.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NamedParameterStatement implements AutoCloseable {
    private final PreparedStatement statement;
    private final Map indexMap;

    public NamedParameterStatement(Connection connection, String query) throws SQLException {
        this(connection, query, -1);
    }

    public NamedParameterStatement(Connection connection, String query, int generatedKey) throws SQLException {
        indexMap = new HashMap();
        String parsedQuery = parse(query, indexMap);
        if(generatedKey > 0) {
            statement = connection.prepareStatement(parsedQuery, generatedKey);
        } else {
            statement = connection.prepareStatement(parsedQuery);
        }
    }

    static final String parse(String query, Map paramMap) {
        int length = query.length();
        StringBuffer parsedQuery = new StringBuffer(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int index = 1;

        for (int i = 0; i < length; i++) {
            char c = query.charAt(i);
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == '?' && !inSingleQuote && !inDoubleQuote) {
                    index++;
                } else if (c == ':' && i + 1 < length &&
                        Character.isJavaIdentifierStart(query.charAt(i + 1))) {
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        j++;
                    }
                    String name = query.substring(i + 1, j);
                    c = '?'; // replace the parameter with a question mark
                    i += name.length(); // skip past the end if the parameter

                    List indexList = (List) paramMap.get(name);
                    if (indexList == null) {
                        indexList = new LinkedList();
                        paramMap.put(name, indexList);
                    }
                    indexList.add(index);

                    index++;
                }
            }
            parsedQuery.append(c);
        }

        // replace the lists of Integer objects with arrays of ints
        for (Iterator itr = paramMap.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry entry = (Map.Entry) itr.next();
            List list = (List) entry.getValue();
            int[] indexes = new int[list.size()];
            int i = 0;
            for (Iterator itr2 = list.iterator(); itr2.hasNext(); ) {
                Integer x = (Integer) itr2.next();
                indexes[i++] = x.intValue();
            }
            entry.setValue(indexes);
        }

        return parsedQuery.toString();
    }

    private int[] getIndexes(String name) {
        int[] indexes = (int[]) indexMap.get(name);
        if (indexes == null) {
            throw new IllegalArgumentException("Parameter not found: " + name);
        }
        return indexes;
    }

    public void setObject(String name, Object value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setObject(indexes[i], value);
        }
    }

    public void setString(String name, String value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            if(value == null) {
                statement.setNull(indexes[i], Types.INTEGER);
            } else {
                statement.setString(indexes[i], value);
            }
        }
    }

    public void setInt(String name, Integer value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            if(value == null) {
                statement.setNull(indexes[i], Types.INTEGER);
            } else {
                statement.setInt(indexes[i], value);
            }
        }
    }

    public void setLong(String name, Long value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            if(value == null) {
                statement.setNull(indexes[i], Types.INTEGER);
            } else {
                statement.setLong(indexes[i], value);
            }
        }
    }

    public void setChar(String name, Character value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            if(value == null) {
                statement.setNull(indexes[i], Types.CHAR);
            } else {
                statement.setString(indexes[i], value.toString());
            }
        }
    }

    public void setTimestamp(String name, Timestamp value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            if(value == null) {
                statement.setNull(indexes[i], Types.TIMESTAMP);
            } else {
                statement.setTimestamp(indexes[i], value);
            }
        }
    }

    public void setDouble(String name, Double value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            if(value == null) {
                statement.setNull(indexes[i], Types.DOUBLE);
            } else {
                statement.setDouble(indexes[i], value);
            }
        }
    }

    public void setDate(String name, Date value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            if(value == null) {
                statement.setNull(indexes[i], Types.DATE);
            } else {
                statement.setDate(indexes[i], new java.sql.Date(value.getTime()));
            }
        }
    }

    public void setBoolean(String name, Boolean value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            if(value == null) {
                statement.setNull(indexes[i], Types.DATE);
            } else {
                statement.setBoolean(indexes[i], value);
            }
        }
    }


    public void setObject(int position, Object value) throws SQLException {
        statement.setObject(position, value);
    }

    public void setString(int position, String value) throws SQLException {
        if(value == null) {
            statement.setNull(position, Types.INTEGER);
        } else {
            statement.setString(position, value);
        }
    }

    public void setInt(int position, Integer value) throws SQLException {
        if(value == null) {
            statement.setNull(position, Types.INTEGER);
        } else {
            statement.setInt(position, value);
        }
    }

    public void setLong(int position, Long value) throws SQLException {
        if(value == null) {
            statement.setNull(position, Types.INTEGER);
        } else {
            statement.setLong(position, value);
        }
    }

    public void setChar(int position, Character value) throws SQLException {
        if(value == null) {
            statement.setNull(position, Types.CHAR);
        } else {
            statement.setString(position, value.toString());
        }
    }

    public void setTimestamp(int position, Timestamp value) throws SQLException {
        if(value == null) {
            statement.setNull(position, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(position, value);
        }
    }

    public void setDouble(int position, Double value) throws SQLException {
        if(value == null) {
            statement.setNull(position, Types.DOUBLE);
        } else {
            statement.setDouble(position, value);
        }
    }

    public void setDate(int position, Date value) throws SQLException {
        if(value == null) {
            statement.setNull(position, Types.DATE);
        } else {
            statement.setDate(position, new java.sql.Date(value.getTime()));
        }
    }

    public void setBoolean(int position, Boolean value) throws SQLException {
        if(value == null) {
            statement.setNull(position, Types.DATE);
        } else {
            statement.setBoolean(position, value);
        }
    }


    public PreparedStatement getStatement() {
        return statement;
    }

    public boolean execute() throws SQLException {
        return statement.execute();
    }

    public ResultSet executeQuery() throws SQLException {
        return statement.executeQuery();
    }

    public int executeUpdate() throws SQLException {
        return statement.executeUpdate();
    }

    public void close() throws SQLException {
        statement.close();
    }

    public void addBatch() throws SQLException {
        statement.addBatch();
    }

    public int[] executeBatch() throws SQLException {
        return statement.executeBatch();
    }

    @Override
    public String toString() {
        return statement.toString();
    }
}