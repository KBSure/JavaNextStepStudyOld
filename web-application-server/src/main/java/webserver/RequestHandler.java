package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            //index.html 요청하는 path
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = br.readLine();
            if(line == null){
                return;
            }

            String method = HttpRequestUtils.getMethod(line);
            String path = HttpRequestUtils.getUrl(line);

            Map<String, String> headers = new HashMap<>();
            while(!"".equals(line)){
                log.debug("headers: {}", line);
                line = br.readLine();
                String[] splited = line.split(":");
                if(splited.length == 2)
                headers.put(splited[0].trim(), splited[1].trim());
            }

            if("GET".equals(method)) {
                int index = path.indexOf("?");
                if (path.startsWith("/user/create")) {
                    String param = path.substring(index + 1);
                    Map<String, String> paramMap = HttpRequestUtils.parseQueryString(param);
                    User user = new User(paramMap.get("userId"), paramMap.get("password"), paramMap.get("name"), paramMap.get("email"));
                    log.debug("User : {}", user);

                    path = "/index.html";
                }
            }else if("POST".equals(method)){
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                String queryString = IOUtils.readData(br, contentLength);
                Map<String, String> queryStringMap = HttpRequestUtils.parseQueryString(queryString);
                User user = new User(queryStringMap.get("userId"), queryStringMap.get("password"), queryStringMap.get("name"), queryStringMap.get("email"));
                log.debug("User : {}", user);

                path = "/index.html";
            }


            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(new File("web-application-server/webapp" + path).toPath());
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
