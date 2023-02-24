package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOApiImpl;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import java.io.IOException;

public class ScriptureDeleter {
  private final static String API_URL = "https://dd4-biblical.appspot.com/_api";
  private final static String API_VERSION = "v1";

  private final ScriptureStore scriptureStore;

  private ScriptureDeleter(ScriptureStore scriptureStore) {
    this.scriptureStore = scriptureStore;
  }

  public void preview(String searchText) throws IOException {
    QueryResult<Scripture> result =
        scriptureStore.search(Query.forSearch(searchText).setPageSize(1000));
    result.getItems().forEach(System.out::println);
    System.out.printf(
        "\nFound %d items to delete.\nProceed with deletion? Y/N\n", result.getItems().size());
    if (System.in.read() == 'Y') {
      scriptureStore.delete(
          result.getItems().stream().map(Scripture::getId).collect(toImmutableList()));
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.out.println("Usage ScriptureDeleter searchText [idtoken]");
    }

    APIConnector apiConnector = new APIConnector(API_URL, API_VERSION, 100);
    apiConnector.setIdToken("220935968");
    DAOApiImpl dao = new DAOApiImpl(apiConnector);
    new ScriptureDeleter(
        new ScriptureStore(() -> dao, null, new ScriptureReferenceProcessorSplitImpl(), null))
            .preview(args[0]);
  }
}
