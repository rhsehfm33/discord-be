package discord.chat.endpoint.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@SpringBootTest
@ActiveProfiles("test")
public class BaseIntegrationTest {
    // MODIFYING THIS VALUE NEEDS FULL CAUTION!!
    private final String TEST_DB = "test-discord";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoMappingContext mongoMappingContext;

    private void dropTestDB() {
        if (mongoTemplate.getDb().getName().equals(TEST_DB)) {
            mongoTemplate.getDb().drop();
            System.out.println("test database[" + TEST_DB + "] 삭제 완료.");
        }
    }

    private void createTestDBIndexes() {
        if (mongoTemplate.getDb().getName().equals(TEST_DB)) {
            // MongoPersistentEntityIndexResolver는 엔티티에 정의된 인덱스 정보를 가져옴
            MongoPersistentEntityIndexResolver indexResolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);

            // 모든 엔티티의 인덱스를 가져와 처리
            mongoMappingContext.getPersistentEntities().forEach(entity -> {
                if (entity.getTypeInformation().getType().getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class) != null) {
                    // 해당 엔티티의 컬렉션 이름
                    String collectionName = entity.getCollection();

                    // 컬렉션에 대해 IndexOperations 가져오기
                    IndexOperations indexOps = mongoTemplate.indexOps(collectionName);

                    // 해당 엔티티의 인덱스 정의를 모두 가져와서 적용
                    for (IndexDefinition indexDefinition : indexResolver.resolveIndexFor(entity.getTypeInformation())) {
                        indexOps.ensureIndex(indexDefinition);  // 인덱스 적용
                    }
                }
            });

            System.out.println("test database[" + TEST_DB + "] 인덱스 생성 완료.");
        }
    }

    @BeforeEach
    public void setupDBBefore() {
        dropTestDB();
        createTestDBIndexes();
    }

    @AfterEach
    public void cleanupDBAfter() {
        dropTestDB();
    }
}
