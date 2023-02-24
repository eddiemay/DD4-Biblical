package com.digitald4.biblical.util;

import static org.mockito.Mockito.mock;

import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import java.io.BufferedReader;
import java.io.FileReader;
import org.junit.Before;
import org.mockito.Mock;

public class ScriptureFetcherTest {
  @Mock protected final APIConnector apiConnector = mock(APIConnector.class);
  protected ScriptureStore scriptureStore;

  @Before
  public void setup() {
    DAOTestingImpl dao = new DAOTestingImpl();
    scriptureStore = new ScriptureStore(
        () -> dao, null,
        new ScriptureReferenceProcessorSplitImpl(),
        new ScriptureFetcherRouter(
            new ScriptureFetcherBibleGateway(apiConnector),
            new ScriptureFetcherBibleHub(apiConnector),
            new ScriptureFetcherJWOrg(apiConnector),
            new ScriptureFetcherKJV1611(apiConnector),
            new ScriptureFetcherOneOff(apiConnector),
            new ScriptureFetcherPseudepigrapha(apiConnector),
            new ScriptureFetcherStepBibleOrg(apiConnector)));
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
