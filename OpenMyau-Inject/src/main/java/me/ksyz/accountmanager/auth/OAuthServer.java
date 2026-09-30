package me.ksyz.accountmanager.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class OAuthServer {
  private static final String AUTH_URL = "https://login.live.com/oauth20_authorize.srf";

  private final OAuthHandler handler;
  private final String clientId;
  private final String redirectUri;
  private final String scope;
  private final String state;
  private boolean started;
  private boolean stopped;
  private HttpServer httpServer;
  private ThreadPoolExecutor threadPoolExecutor;

  public OAuthServer(
      OAuthHandler handler, String clientId, String redirectUri, String scope, String state)
      throws IOException {
    this.handler = handler;
    this.clientId = clientId;
    this.redirectUri = redirectUri;
    this.scope = scope;
    this.state = state;
    this.httpServer = HttpServer.create(new InetSocketAddress("localhost", 21919), 0);
    this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
  }

  public synchronized void start() {
    if (stopped || started) {
      return;
    }
    try {
      httpServer.createContext("/login", new OAuthHttpHandler(this));
      httpServer.setExecutor(threadPoolExecutor);
      httpServer.start();
      started = true;
      handler.openUrl(
          AUTH_URL
              + "?client_id="
              + encode(clientId)
              + "&redirect_uri="
              + encode(redirectUri)
              + "&response_type=code&display=touch&scope="
              + encode(scope)
              + "&prompt=select_account&state="
              + encode(state));
    } catch (RuntimeException e) {
      stop(false);
      throw e;
    }
  }

  public synchronized void stop(boolean isInterrupt) {
    if (!stopped) {
      stopped = true;
      if (started) {
        httpServer.stop(0);
      }
      threadPoolExecutor.shutdownNow();
      if (isInterrupt) {
        handler.authError("Has been interrupted");
      }
    }
  }

  private static String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private static class OAuthHttpHandler implements HttpHandler {
    private final OAuthServer server;

    OAuthHttpHandler(OAuthServer server) {
      this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      boolean completed = false;
      try {
        Map<String, String> query = getQueryParams(exchange.getRequestURI().getRawQuery());
        String code = query.get("code");
        String error = query.get("error");
        if (!server.state.equals(query.get("state"))) {
          respond(exchange, 400, "Invalid authentication state");
          return;
        }
        completed = true;
        if (error != null) {
          String description = query.get("error_description");
          String message = description == null ? error : error + ": " + description;
          respond(exchange, 400, message);
          server.handler.authError(message);
        } else if (code == null || code.isEmpty()) {
          respond(exchange, 400, "No authorization code in the callback");
          server.handler.authError("No authorization code in the callback");
        } else {
          server.handler.authResult(code, server.clientId, server.scope);
          respond(exchange, 200, "Login Success");
        }
      } catch (Exception e) {
        server.handler.authError(e.toString());
        respond(exchange, 500, "Login failed");
      } finally {
        exchange.close();
        if (completed) {
          server.stop(false);
        }
      }
    }

    private static void respond(HttpExchange exchange, int status, String response)
        throws IOException {
      byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, bytes.length);
      try (OutputStream output = exchange.getResponseBody()) {
        output.write(bytes);
      }
    }
  }

  private static Map<String, String> getQueryParams(String query) throws IOException {
    Map<String, String> params = new HashMap<>();
    if (query != null) {
      for (String param : query.split("&")) {
        String[] pair = param.split("=", 2);
        if (pair.length == 2) {
          params.put(
              URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name()),
              URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()));
        }
      }
    }
    return params;
  }
}
