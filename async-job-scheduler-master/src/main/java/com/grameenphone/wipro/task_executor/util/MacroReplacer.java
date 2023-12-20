package com.grameenphone.wipro.task_executor.util;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MacroReplacer {
    public static String replaceMacros(String input, KV<String, Object>... binding) throws IOException {
        StringBuilder output = new StringBuilder();
        return replaceMacros(input, Arrays.stream(binding).collect(Collectors.toMap(b -> b.key, b -> b.value)), output);
    }

    public static String replaceMacros(String input, Map bindings) throws IOException {
        StringBuilder output = new StringBuilder();
        return replaceMacros(input, bindings, output);
    }

    /**
     * sample: A $xyz$ has created $y>something$ for $if:y>good$good$if$$else$bad$else$ for repeat activity $repeat:z>repeatable$this is $total$, this is current $value$ and this is $index$$repeat$
     * @param input
     * @param bindings
     * @param output
     * @return
     * @throws IOException
     */
    public static String replaceMacros(String input, Map bindings, StringBuilder output) throws IOException {
        boolean lastIfMatched = false;
        StringReader reader = new StringReader(input);
        try {
            while (true) {
                skipUpTo(reader, '$', output);
                String token = captureUpTo(reader, '$');
                String[] tokens;
                if (token.equals("")) {
                    output.append('$');
                } else if ((tokens = token.split(":")).length > 1 && tokens[0].equals("if")) {
                    String block = captureUpTo(reader, "if");
                    String checkValue = (String) convertToken(tokens[1], bindings);
                    boolean ifMatched = false;
                    if (tokens.length > 2) {
                        String op = tokens[2];
                        String matchValue = tokens[3];
                        if (op.equals("eq")) {
                            ifMatched = checkValue.equals(matchValue);
                        } else if (op.equals("ne")) {
                            ifMatched = !checkValue.equals(matchValue);
                        }
                    } else {
                        if (!checkValue.equals("")) {
                            ifMatched = true;
                        }
                    }
                    lastIfMatched = ifMatched;
                    if (ifMatched) {
                        replaceMacros(block, bindings, output);
                    }
                } else if (tokens[0].equals("else")) {
                    String block = captureUpTo(reader, "else");
                    if (!lastIfMatched) {
                        replaceMacros(block, bindings, output);
                    }
                    lastIfMatched = false;
                } else if (tokens[0].equals("repeat")) {
                    String block = captureUpTo(reader, "repeat");
                    Object repeatable = convertToken(tokens[1], bindings);
                    if (repeatable instanceof List) {
                        Map newScope = new HashMap(bindings);
                        int i = 0;
                        int total = ((List) repeatable).size();
                        newScope.put("total", "" + total);
                        for (Object entry : (List) repeatable) {
                            newScope.put("value", entry);
                            newScope.put("index", "" + i++);
                            replaceMacros(block, newScope, output);
                        }
                    }
                } else {
                    output.append(convertToken(token, bindings).toString());
                }
            }
        } catch (EOF e) {
        }
        return output.toString();
    }

    static class EOF extends Error {

        public EOF() {
        }
    }

    private static Object accessObjectUsingReflection(Object source, String part) {
        try {
            if (source instanceof HibernateProxy) {
                LazyInitializer lazyInitializer = ((HibernateProxy) source).getHibernateLazyInitializer();
                if (lazyInitializer.isUninitialized()) {
                    //TODO What to do here
                    //CrudDao.get(source.getClass()).initialize((HibernateProxy) source);
                }
                source = lazyInitializer.getImplementation();
            }
            try {
                PropertyDescriptor descriptor = new PropertyDescriptor(part, source.getClass());
                Method getter = descriptor.getReadMethod();
                if (getter != null) {
                    return getter.invoke(source);
                }
            } catch (IntrospectionException | InvocationTargetException e) {
            }
            Field field = source.getClass().getDeclaredField(part);
            return field.get(source);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private static Object convertToString(Object value) {
        if(value == null) {
            return null;
        }
        if (value.getClass().isPrimitive() || value.getClass().getPackageName().startsWith("java.lang")) {
            value = value.toString();
        } else if (value instanceof Date) {
            value = new SimpleDateFormat("d'X' MMM yy HH:mm").format(value).replace("X", NumberUtil.getOrdinal(((Date)value).getDate()));
        }
        return value;
    }

    private static Object convertToken(String token, Object binding) {
        String[] mapParts = token.split(">");
        Object value = binding;
        for (String part : mapParts) {
            if (value == null) {
                break;
            }
            if (value instanceof Function) {
                try {
                    value = ((Function) value).apply(part);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            if (value instanceof Map) {
                value = ((Map) value).get(part);
            } else {
                value = accessObjectUsingReflection(value, part);
            }
            value = convertToString(value);
        }
        return value == null ? "" : value;
    }

    private static void skipUpTo(Reader reader, char lookup, StringBuilder out) throws IOException {
        int c;
        while (true) {
            c = reader.read();
            if (c == -1) {
                throw new EOF();
            }
            if (c == lookup) {
                return;
            }
            out.append((char) c);
        }
    }

    static String captureUpTo(Reader reader, char lookup) throws IOException {
        StringBuilder builder = new StringBuilder();
        int c;
        while (true) {
            c = reader.read();
            if (c == -1) {
                throw new EOF();
            }
            if (c == lookup) {
                return builder.toString();
            }
            builder.append((char) c);
        }
    }

    static String captureUpTo(Reader reader, String endTag) throws IOException {
        StringBuilder block = new StringBuilder();
        Integer startTagCount = 0;
        while (true) {
            skipUpTo(reader, '$', block);
            String token = captureUpTo(reader, '$');
            String[] tokens;
            if (token.equals(endTag) && startTagCount == 0) {
                break;
            } else if (token.equals(endTag)) {
                startTagCount--;
            } else if ((tokens = token.split(":")).length > 1 && tokens[0].equals(endTag)) {
                startTagCount++;
            }
            block.append("$");
            block.append(token);
            block.append("$");
        }
        return block.toString();
    }
}
