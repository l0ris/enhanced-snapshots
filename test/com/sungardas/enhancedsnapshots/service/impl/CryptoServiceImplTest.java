package enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.service.CryptoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:enhancedsnapshots/service/impl/spring-test-config.xml")
public class CryptoServiceImplTest {

    @Autowired
    private CryptoService cryptoService;

    private String value = "Value";
    private String privateKey = "Bar12345Bar12345";

    @Test
    public void encryptTest() throws GeneralSecurityException {
        String encrypted = cryptoService.encrypt(privateKey, value);

        assertNotEquals(encrypted, value);

        assertEquals(value, cryptoService.decrypt(privateKey, encrypted));
    }


    @Test
    public void encryptPropertiesTest() throws GeneralSecurityException, IOException {
        String encrypted = cryptoService.encrypt(privateKey, value);

        assertNotEquals(encrypted, value);

        Properties properties = new Properties();

        properties.setProperty(value, encrypted);

        Path tempFile = Files.createTempFile("temp", "temp");

        properties.store(new FileOutputStream(tempFile.toFile()), "");

        properties = new Properties();
        properties.load(new FileInputStream(tempFile.toFile()));
        tempFile.toFile().delete();

        assertEquals(value, cryptoService.decrypt(privateKey, properties.getProperty(value)));
    }

}
