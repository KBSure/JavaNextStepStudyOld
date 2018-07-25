package db;

import model.User;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class DataBaseTest {

    @Test
    public void 콜렉션사용하기(){
        Collection<User> all = DataBase.findAll();
    }
}
