package com.digitald4.biblical.server;

import com.digitald4.biblical.model.Commandment;
import com.digitald4.biblical.store.CommandmentStore;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.QueryResult;
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
public class CommandmentService extends EntityServiceImpl<Commandment, Long> {
  private final CommandmentStore store;

  @Inject
  CommandmentService(CommandmentStore store, LoginResolver loginResolver) {
    super(store, loginResolver);
    this.store = store;
  }

  @Override // Overriding this method because of the default order by.
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "search")
  public QueryResult<Commandment> search(@Named("searchText") String searchText,
      @Named("pageSize") @DefaultValue("50") int pageSize, @Named("pageToken") @DefaultValue("1") int pageToken,
      @DefaultValue("bookNum,chapter,verse") @Named("orderBy") String orderBy,
      @Nullable @Named("idToken") String idToken) throws ServiceException {
    return super.search(searchText, pageSize, pageToken, orderBy, idToken);
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "reindex")
  public void reindex() throws ServiceException {
    try {
      store.reindex();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  protected boolean requiresLogin(String method) {
    return !("get".equals(method) || "list".equals(method) || "search".equals(method)) && super.requiresLogin(method);
  }
}
