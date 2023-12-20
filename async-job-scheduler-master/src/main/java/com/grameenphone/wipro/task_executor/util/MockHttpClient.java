package com.grameenphone.wipro.task_executor.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MockHttpClient {
    private static ConcurrentHashMap<String, List<MockCondition>> mockConditions = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, List<NotifyCondition>> notifyConditions = new ConcurrentHashMap<>();

    public static HttpClient mockIt(final HttpClient original) {
        return (HttpClient) Proxy.newProxyInstance(original.getClass().getClassLoader(), new Class[]{HttpClient.class}, (proxy, method, args) -> {
            if (method.getName().equals("execute")) {
                HttpUriRequest request = (HttpUriRequest) args[0];
                HttpResponse response = getMockedResponse(request);
                if (response != null) {
                    return response;
                }
            }
            return method.invoke(original, args);
        });
    }

    public static HttpResponse getMockedResponse(HttpUriRequest request) throws Exception {
        String requestedUrl = new URIBuilder(request.getURI()).removeQuery().build().toString();
        List<NotifyCondition> notifies = notifyConditions.get(requestedUrl);
        Stream<NotifyCondition> matchedNotifies = null;
        if (notifies != null) {
            matchedNotifies = notifies.stream().filter(n -> n.match(request));
        }
        if (matchedNotifies != null) {
            //Copying streams for re use
            matchedNotifies = matchedNotifies.filter(n -> {
                n.notify(request);
                return true;
            });
        }
        List<MockCondition> mocks = mockConditions.get(requestedUrl);
        HttpResponse response = null;
        try {
            if (mocks != null) {
                for (MockCondition mock : mocks) {
                    if (mock.match(request)) {
                        return response = mock.response();
                    }
                }
            }
        } finally {
            if (matchedNotifies != null) {
                HttpResponse passin = response;
                matchedNotifies.forEach(n -> n.notify(passin));
            }
        }
        return null;
    }

    public static void reset(String url) {
        if (url == null) {
            mockConditions.clear();
            notifyConditions.clear();
        } else {
            mockConditions.remove(url);
            notifyConditions.remove(url);
        }
    }

    public static MockCondition forUrl(String url) {
        MockCondition condition = new MockCondition();
        List<MockCondition> mocks = mockConditions.get(url);
        if (mocks == null) {
            mocks = new ArrayList<>();
            mockConditions.put(url, mocks);
        }
        mocks.add(condition);
        return condition;
    }

    public static NotifyCondition notifyFor(String url) {
        NotifyCondition condition = new NotifyCondition();
        List<NotifyCondition> notifies = notifyConditions.get(url);
        if (notifies == null) {
            notifies = new ArrayList<>();
            notifyConditions.put(url, notifies);
        }
        notifies.add(condition);
        return condition;
    }

    public static String getContent(HttpPost post) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try {
            post.getEntity().writeTo(body);
            return body.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public interface Checker {
        boolean match(HttpUriRequest request);
    }

    private abstract static class CheckerCondition<T extends CheckerCondition> {
        private List<Checker> checkers = new ArrayList<>();

        protected boolean match(HttpUriRequest request) {
            for (Checker checker : checkers) {
                if (!checker.match(request)) {
                    return false;
                }
            }
            return true;
        }

        public T isPost() {
            checkers.add(new PostChecker());
            return (T) this;
        }

        public T isGet() {
            checkers.add(new GetChecker());
            return (T) this;
        }

        /**
         * Exact Match
         *
         * @param body
         * @return
         */
        public T content(String body) {
            checkers.add(new RequestContentChecker(body));
            return (T) this;
        }

        /**
         * partialMatch
         *
         * @param body
         * @param isRegex
         * @return
         */
        public T content(String body, Boolean isRegex) {
            RequestContentChecker checker = new RequestContentChecker(body);
            checkers.add(checker);
            checker.setPartial(isRegex);
            return (T) this;
        }

        public T match(Checker checker) {
            checkers.add(checker);
            return (T) this;
        }

        public T minlength(int threshold) {
            checkers.add(new RequestContentLengthChecker(threshold, true, true));
            return (T) this;
        }
    }

    public static class MockCondition extends CheckerCondition<MockCondition> {
        private HttpResponse response;

        private HttpResponse response() throws Exception {
            if (response == null) {
                response(200, "");
            }
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == -1) {
                throw new ConnectTimeoutException();
            }
            return response;
        }

        public void response(int status, String text) {
            try {
                HttpResponseFactory factory = new DefaultHttpResponseFactory();
                response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, status, null), null);
                response.setEntity(new StringEntity(text));
            } catch (Throwable h) {
            }
        }

        public void timeout() {
            response(-1, null);
        }
    }

    public static class NotifyCondition extends CheckerCondition<NotifyCondition> {
        Consumer<HttpUriRequest> requestNotifier;
        Consumer<HttpResponse> responseNotifier;
        private int callCount = 0;

        public boolean isCalled() {
            return getCallCount() > 0;
        }

        public int getCallCount() {
            return callCount;
        }

        private void notify(HttpUriRequest request) {
            callCount++;
            if (requestNotifier != null) {
                requestNotifier.accept(request);
            }
        }

        private void notify(HttpResponse response) {
            if (responseNotifier != null) {
                responseNotifier.accept(response);
            }
        }

        public void notify(Consumer<HttpUriRequest> requestNotifier, Consumer<HttpResponse> responseNotifier) {
            this.requestNotifier = requestNotifier;
            this.responseNotifier = responseNotifier;
        }

        public void resetCount() {
            callCount = 0;
        }
    }

    public static class PostChecker implements Checker {
        public boolean match(HttpUriRequest request) {
            return request.getMethod().equals("POST");
        }
    }

    public static class GetChecker implements Checker {
        public boolean match(HttpUriRequest request) {
            return request.getMethod().equals("GET");
        }
    }

    public static class RequestContentChecker implements Checker {
        String bodyToMatch;
        Boolean partialIsRegex;

        public RequestContentChecker(String content) {
            bodyToMatch = content;
        }

        public void setPartial(boolean isRegex) {
            partialIsRegex = isRegex;
        }

        public boolean match(HttpUriRequest request) {
            if (!(request instanceof HttpPost)) {
                return false;
            }
            String body = getContent((HttpPost) request);
            if (partialIsRegex != null) {
                if (partialIsRegex) {
                    return Pattern.compile(bodyToMatch).matcher(body).matches();
                } else {
                    return body.contains(bodyToMatch);
                }
            }
            return bodyToMatch.equals(body);
        }
    }

    public static class RequestContentLengthChecker implements Checker {
        int length;
        boolean greater;
        boolean equals;

        public RequestContentLengthChecker(int length, boolean greater, boolean equals) {
            this.equals = equals;
            this.greater = greater;
            this.length = length;
        }

        public boolean match(HttpUriRequest request) {
            if (!(request instanceof HttpPost)) {
                return false;
            }
            String content = getContent((HttpPost) request);
            return (greater ? length < content.length() : length > content.length()) || (equals ? length == content.length() : false);
        }
    }
}