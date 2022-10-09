package com.digitald4.biblical.store;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.digitald4.biblical.model.Commandment;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.storage.DAO;
import com.google.appengine.api.search.Index;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CommandmentStoreTest {

  @Mock private final DAO dao = mock(DAO.class);
  @Mock private final Index searchIndex = mock(Index.class);
  @Mock private final ScriptureStore scriptureStore = mock(ScriptureStore.class);

  private final ScriptureReferenceProcessor scriptureRefProcessor = new ScriptureReferenceProcessorSplitImpl();
  private CommandmentStore commandmentStore;

  @Before
  public void setup() {
    commandmentStore = new CommandmentStore(() -> dao, searchIndex, scriptureRefProcessor, scriptureStore);

    when(dao.create(any(Commandment.class))).thenAnswer(i -> i.getArgumentAt(0, Commandment.class));
  }

  @Test
  public void create_verifiesScriptureFormat() {
    try {
      commandmentStore.create(
          new Commandment()
              .setSummary("Keep the Sabbath").setScriptures("Tiffin 36U").setTags("ten commandments,worship, sabbath"));
      fail("Should not have got here");
    } catch (Exception e) {
      assertThat(e).hasMessageThat().contains("Unknown bible book: Tiffin");
    }

    verify(dao, never()).create(any(Commandment.class));
  }

  @Test
  public void preprocess_replacesTags() {
    Commandment commandment = commandmentStore.create(
        new Commandment()
            .setSummary("Keep the Sabbath").setScriptures("Exo 20:8").setTags("ten commandments,worship, sabbath"));

    assertThat(commandment.getTags()).isEqualTo("ten commandments worship sabbath");
  }
}
