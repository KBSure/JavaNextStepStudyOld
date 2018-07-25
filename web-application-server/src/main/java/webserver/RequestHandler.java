package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.DatabaseMetaData;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sun.org.apache.xpath.internal.operations.Bool;
import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import javax.xml.crypto.Data;

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
            DataOutputStream dos = new DataOutputStream(out);
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

            Map<String, String> cookies = HttpRequestUtils.parseCookies(headers.get("Cookie"));
            boolean isLogin = Boolean.parseBoolean(cookies.get("logined"));

            if (path.startsWith("/user/create")) {
                if ("GET".equals(method)) {
                    int index = path.indexOf("?");
                    String param = path.substring(index + 1);
                    Map<String, String> paramMap = HttpRequestUtils.parseQueryString(param);
                    User user = new User(paramMap.get("userId"), paramMap.get("password"), paramMap.get("name"), paramMap.get("email"));
                    log.debug("User : {}", user);
                    DataBase.addUser(user);
                    response302Header(dos, "/index.html");
                    return;
                }else if("POST".equals(method)){
                        int contentLength = Integer.parseInt(headers.get("Content-Length"));
                        String queryString = IOUtils.readData(br, contentLength);
                        Map<String, String> queryStringMap = HttpRequestUtils.parseQueryString(queryString);
                        User user = new User(queryStringMap.get("userId"), queryStringMap.get("password"), queryStringMap.get("name"), queryStringMap.get("email"));
                        log.debug("User : {}", user);
                        DataBase.addUser(user);
                        response302Header(dos, "/index.html");
                        return;
                }
            }else if(path.startsWith("/user/login")){
                if ("POST".equals(method)){
                    //로그인 비교 해서 로그인 성공실패 유무 처리
                    int contentLength = Integer.parseInt(headers.get("Content-Length"));
                    String queryString = IOUtils.readData(br, contentLength);
                    Map<String, String> queryStringMap = HttpRequestUtils.parseQueryString(queryString);
                    log.debug("userId : {}, password : {}", queryStringMap.get("userId"), queryStringMap.get("password"));
                    if(isLogin(queryStringMap.get("userId"), queryStringMap.get("password"))){
                        response302HeaderWithLogined(dos, true, "/index.html");
                    }else{
                        response302HeaderWithLogined(dos, false, "/user/login_failed.html");
                    }
                    //로그인 성공 응답 테스트
//                    byte[] body = Files.readAllBytes(new File("web-application-server/webapp" + "/index.html").toPath());
//                    response302Header(dos, true, );
//                    responseBody(dos, body);
                    return;
                }
            }else if(path.startsWith("/user/list")){
                if("GET".equals(method)){
                    //로그인 되어 있으면 넘기고 안되어있으면 로그인 페이지
                    if(isLogin){
//                        byte[] body = Files.readAllBytes(new File("web-application-server/webapp/user/list.html").toPath());
//                        response200Header(dos, false, body.length); //수정 필요
//                        responseBody(dos, body);
                        Collection<User> users = DataBase.findAll();
                        StringBuilder sb = new StringBuilder();
                        sb.append("<table border='1'>");
                        for(User user : users){
                            sb.append("<tr>");
                            sb.append("<td>"+user.getUserId()+"</td>");
                            sb.append("<td>"+user.getName()+"</td>");
                            sb.append("<td>"+user.getEmail()+"</td>");
                            sb.append("</tr>");
                        }
                        sb.append("</table>");
                        byte[] body = sb.toString().getBytes();
                        response200Header(dos, headers, body.length);
                        responseBody(dos, body);
                    }else if(!isLogin){
                        response302Header(dos, "/user/login.html");
                    }
                }
            }

            byte[] body = Files.readAllBytes(new File("web-application-server/webapp" + path).toPath());
            response200Header(dos, headers, body.length); //수정 필요
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private boolean isLogin(String userId, String password){
        User user = DataBase.findUserById(userId);
        if(user == null){
            log.debug("User Not Found!");
            return false;
        }else{
            if(password.equals(user.getPassword())){
                log.debug("User Match!");
                return true;
            }
            log.debug("Password Mismatch!");
            return false;
        }
    }


    private void response302Header(DataOutputStream dos, String path){
        try {
            dos.writeBytes("HTTP/1.1 302 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Location: " + path +"\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void response302HeaderWithLogined(DataOutputStream dos, boolean isLogin, String path){
        try {
            dos.writeBytes("HTTP/1.1 302 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Location: " + path +"\r\n");
            if(isLogin) {
                dos.writeBytes("Set-Cookie: logined=true\r\n");
            }else if(!isLogin){
                dos.writeBytes("Set-Cookie: logined=false\r\n");
            }
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void response200Header(DataOutputStream dos, Map<String, String> headers, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: " + headers.get("Accept") + ";charset=utf-8\r\n");
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
