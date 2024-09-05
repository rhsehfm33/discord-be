package team.discordbe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

@Component
public class MongoDatabaseCleanerTestExecutionListener extends AbstractTestExecutionListener {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        // Drop the database before each test method
        mongoTemplate.getDb().drop();
        System.out.println("Database " + mongoTemplate.getDb().getName() + " has been dropped before test method.");
    }
}
