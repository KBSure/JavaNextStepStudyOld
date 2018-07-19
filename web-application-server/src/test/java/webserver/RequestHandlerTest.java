package webserver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class RequestHandlerTest {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandlerTest.class);
    private static String url;
    @Before
    public void init(){
        url = "/user/create?userId=javajigi&password=password=password";
    }

    @Test
    public void 물음표없을때인덱스테스트 (){
        String notQMurl = "/user/create";
        int index = notQMurl.indexOf("?");
        System.out.println(index);
    }
    @Test
    public void 요청URL과이름값분리 (){
        int index = url.indexOf("?");

        String requestPath = url.substring(0, index);
        String param = url.substring(index + 1);

        assertThat(requestPath, is("/user/create"));
        assertThat(param, is("userId=javajigi&password=password=password"));
    }
}
