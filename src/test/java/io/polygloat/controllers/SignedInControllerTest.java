package io.polygloat.controllers;

import io.polygloat.helpers.JsonHelper;
import io.polygloat.model.UserAccount;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.testng.annotations.BeforeMethod;

import static io.polygloat.controllers.LoggedRequestFactory.loggedGet;
import static io.polygloat.controllers.LoggedRequestFactory.loggedPost;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class SignedInControllerTest extends AbstractControllerTest {
    UserAccount userAccount;

    @BeforeMethod
    public void beforeEach() throws Exception {
        //populate to create the user if not created
        dbPopulator.autoPopulate();
        if (userAccount == null) {
            logAsUser(polygloatProperties.getAuthentication().getInitialUsername(), initialPassword);
        }
        commitTransaction();
    }

    public void logAsUser(String userName, String password) throws Exception {
        DefaultAuthenticationResult defaultAuthenticationResult = login(userName, password);
        LoggedRequestFactory.init(defaultAuthenticationResult.getToken());
        this.userAccount = defaultAuthenticationResult.getEntity();
    }

    public void logout() {
        userAccount = null;
    }

    public ResultActions performPost(String url, Object content) {
        try {
            return mvc.perform(loggedPost(url).contentType(MediaType.APPLICATION_JSON).content(JsonHelper.asJsonString(content)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResultActions performGet(String url) {
        try {
            return mvc.perform(loggedGet(url));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
