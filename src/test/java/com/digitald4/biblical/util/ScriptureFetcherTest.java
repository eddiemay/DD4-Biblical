package com.digitald4.biblical.util;

import static org.mockito.Mockito.mock;

import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.ChangeTracker;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import java.io.BufferedReader;
import java.io.FileReader;
import org.junit.Before;
import org.mockito.Mock;

public class ScriptureFetcherTest {
  @Mock protected final APIConnector apiConnector = mock(APIConnector.class);
  @Mock final SearchIndexer searchIndexer = mock(SearchIndexer.class);
  protected ScriptureStore scriptureStore;
  private static final StaticDataDAO staticDataDAO = new StaticDataDAO();
  private static final BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);

  @Before
  public void setup() {
    DAOTestingImpl dao = new DAOTestingImpl(new ChangeTracker(null, null, searchIndexer, null));
    scriptureStore = new ScriptureStore(
        () -> dao, null, bibleBookStore,
        new ScriptureReferenceProcessorSplitImpl(bibleBookStore),
        new ScriptureFetcherRouter(
            new ScriptureFetcherBibleGateway(apiConnector),
            new ScriptureFetcherBibleHub(apiConnector),
            new ScriptureFetcherJWOrg(apiConnector),
            new ScriptureFetcherKJV1611(apiConnector),
            new ScriptureFetcherOneOff(apiConnector),
            new ScriptureFetcherPseudepigrapha(apiConnector),
            new ScriptureFetcherSefariaOrg(apiConnector),
            new ScriptureFetcherStepBibleOrg(apiConnector)),
        null);
  }

  protected static String getContent(String filename) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(filename));

    StringBuilder content = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      content.append(line).append("\n");
    }

    return content.toString();
  }
}
