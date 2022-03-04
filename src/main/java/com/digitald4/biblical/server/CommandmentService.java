package com.digitald4.biblical.server;

import com.digitald4.biblical.model.Commandment;
import com.digitald4.biblical.store.CommandmentStore;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SessionStore;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;

import javax.inject.Inject;

@Api(
    name = "commandments",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    )
)
public class CommandmentService extends EntityServiceImpl<Commandment> {
  private final CommandmentStore store;

  @Inject
  CommandmentService(CommandmentStore store, SessionStore<BasicUser> sessionStore) {
    super(store, sessionStore, true);
    this.store = store;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "search")
  public QueryResult<Commandment> search(
      @Named("searchText") String searchText, @DefaultValue("bookNum,chapter,verse") @Named("orderBy") String orderBy,
      @Named("pageSize") @DefaultValue("50") int pageSize, @Named("pageToken") @DefaultValue("1") int pageToken)
      throws ServiceException {
    try {
      return store.search(Query.forSearch(searchText, orderBy, pageSize, pageToken));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "reindex")
  public void reindex() throws ServiceException {
    try {
      store.reindex();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
